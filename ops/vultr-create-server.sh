#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOCAL_ENV_FILE="${PROJECT_ROOT}/secrets/.env"

if [[ -f "$LOCAL_ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$LOCAL_ENV_FILE"
fi

API_ROOT="https://api.vultr.com/v2"
DEFAULT_KEY_CANDIDATES=(
  "$HOME/.ssh/sergei-macbook-vps.pub"
  "$HOME/.ssh/id_ed25519.pub"
  "$HOME/.ssh/id_rsa.pub"
)

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

require_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: $name" >&2
    exit 1
  fi
}

pick_default_key_path() {
  local candidate
  for candidate in "${DEFAULT_KEY_CANDIDATES[@]}"; do
    if [[ -f "$candidate" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 1
}

api() {
  local method="$1"
  local path="$2"
  local payload="${3:-}"

  if [[ -n "$payload" ]]; then
    curl -fsS \
      -X "$method" \
      -H "Authorization: Bearer $VULTR_API_KEY" \
      -H "Content-Type: application/json" \
      --data "$payload" \
      "${API_ROOT}${path}"
  else
    curl -fsS \
      -X "$method" \
      -H "Authorization: Bearer $VULTR_API_KEY" \
      "${API_ROOT}${path}"
  fi
}

require_cmd curl
require_cmd python3
require_env VULTR_API_KEY

REGION="${VULTR_REGION:-fra}"
SERVER_NAME="${VULTR_SERVER_NAME:-xray-$(date +%m%d-%H%M)}"
HOSTNAME="${VULTR_HOSTNAME:-$SERVER_NAME}"
PLAN="${VULTR_PLAN:-vc2-1c-1gb}"
PREFERRED_PLANS="${VULTR_PREFERRED_PLANS:-vc2-1c-1gb,vc2-1c-2gb,vc2-2c-2gb}"
OS_MATCH="${VULTR_OS_MATCH:-Debian 12}"
SSH_KEY_PATH="${VULTR_SSH_KEY_PATH:-}"

if [[ -z "$SSH_KEY_PATH" ]]; then
  SSH_KEY_PATH="$(pick_default_key_path || true)"
fi

if [[ -z "$SSH_KEY_PATH" || ! -f "$SSH_KEY_PATH" ]]; then
  echo "No SSH public key found. Set VULTR_SSH_KEY_PATH to a .pub file." >&2
  exit 1
fi

PUBLIC_KEY="$(<"$SSH_KEY_PATH")"
SSH_KEY_NAME="${VULTR_SSH_KEY_NAME:-$(basename "$SSH_KEY_PATH" .pub)-$(hostname -s 2>/dev/null || hostname)}"

SSH_KEYS_JSON="$(api GET "/ssh-keys")"
SSH_KEY_ID="$(
  JSON_INPUT="$SSH_KEYS_JSON" python3 -c '
import json
import os
import sys

target_name = sys.argv[1]
target_key = sys.argv[2].strip()
data = json.loads(os.environ["JSON_INPUT"])

for item in data.get("ssh_keys", []):
    if item.get("name") == target_name or item.get("ssh_key", "").strip() == target_key:
        print(item["id"])
        break
' "$SSH_KEY_NAME" "$PUBLIC_KEY"
)"

if [[ -z "$SSH_KEY_ID" ]]; then
  SSH_KEY_PAYLOAD="$(
    python3 - "$SSH_KEY_NAME" "$PUBLIC_KEY" <<'PY'
import json
import sys

print(json.dumps({
    "name": sys.argv[1],
    "ssh_key": sys.argv[2],
}))
PY
  )"
  CREATE_KEY_JSON="$(api POST "/ssh-keys" "$SSH_KEY_PAYLOAD")"
  SSH_KEY_ID="$(
    JSON_INPUT="$CREATE_KEY_JSON" python3 -c '
import json
import os

print(json.loads(os.environ["JSON_INPUT"])["ssh_key"]["id"])
'
  )"
fi

if [[ -z "${VULTR_PLAN:-}" ]]; then
  PLANS_JSON="$(api GET "/plans?type=vc2")"
  PLAN="$(
    JSON_INPUT="$PLANS_JSON" python3 -c '
import json
import os
import sys

preferred = [name.strip() for name in sys.argv[1].split(",") if name.strip()]
data = json.loads(os.environ["JSON_INPUT"])
plans = {item.get("id"): item for item in data.get("plans", [])}

for name in preferred:
    if name in plans:
        print(name)
        break
' "$PREFERRED_PLANS"
  )"
fi

if [[ -z "$PLAN" ]]; then
  echo "Could not pick a plan automatically. Set VULTR_PLAN explicitly." >&2
  exit 1
fi

OS_JSON="$(api GET "/os")"
OS_ID="$(
  JSON_INPUT="$OS_JSON" python3 -c '
import json
import os
import sys

needle = sys.argv[1].lower()
data = json.loads(os.environ["JSON_INPUT"])

for item in data.get("os", []):
    name = item.get("name", "")
    if needle in name.lower():
        print(item["id"])
        break
' "$OS_MATCH"
)"

if [[ -z "$OS_ID" ]]; then
  echo "Could not find an OS matching: $OS_MATCH" >&2
  exit 1
fi

INSTANCE_PAYLOAD="$(
  python3 - "$REGION" "$PLAN" "$OS_ID" "$SERVER_NAME" "$HOSTNAME" "$SSH_KEY_ID" <<'PY'
import json
import sys

print(json.dumps({
    "region": sys.argv[1],
    "plan": sys.argv[2],
    "os_id": int(sys.argv[3]),
    "label": sys.argv[4],
    "hostname": sys.argv[5],
    "sshkey_id": [sys.argv[6]],
    "activation_email": False,
    "enable_ipv6": False,
}))
PY
)"

CREATE_INSTANCE_JSON="$(api POST "/instances" "$INSTANCE_PAYLOAD")"

read -r INSTANCE_ID MAIN_IP STATUS <<EOF
$(
  JSON_INPUT="$CREATE_INSTANCE_JSON" python3 -c '
import json
import os

instance = json.loads(os.environ["JSON_INPUT"])["instance"]
print(instance["id"], instance.get("main_ip", ""), instance.get("status", ""))
'
)
EOF

for _ in $(seq 1 60); do
  INSTANCE_JSON="$(api GET "/instances/${INSTANCE_ID}")"
  read -r STATUS MAIN_IP <<EOF
$(
  JSON_INPUT="$INSTANCE_JSON" python3 -c '
import json
import os

instance = json.loads(os.environ["JSON_INPUT"])["instance"]
print(instance.get("status", ""), instance.get("main_ip", ""))
'
)
EOF
  if [[ "$STATUS" == "active" && -n "$MAIN_IP" ]]; then
    break
  fi
  sleep 2
done

cat <<EOF
Created Vultr instance:
  label:      $SERVER_NAME
  id:         $INSTANCE_ID
  region:     $REGION
  plan:       $PLAN
  os_id:      $OS_ID
  ipv4:       $MAIN_IP
  ssh key:    $SSH_KEY_NAME ($SSH_KEY_PATH)

Next:
  ssh root@$MAIN_IP
  /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/ops/migrate-xray-from-old-vps.sh root@$MAIN_IP
EOF
