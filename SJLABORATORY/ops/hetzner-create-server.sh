#!/usr/bin/env bash
set -euo pipefail

API_ROOT="https://api.hetzner.cloud/v1"
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
      -H "Authorization: Bearer $HCLOUD_TOKEN" \
      -H "Content-Type: application/json" \
      --data "$payload" \
      "${API_ROOT}${path}"
  else
    curl -fsS \
      -X "$method" \
      -H "Authorization: Bearer $HCLOUD_TOKEN" \
      "${API_ROOT}${path}"
  fi
}

require_cmd curl
require_cmd python3
require_env HCLOUD_TOKEN

SERVER_NAME="${HCLOUD_SERVER_NAME:-xray-$(date +%m%d-%H%M)}"
LOCATION="${HCLOUD_LOCATION:-hel1}"
IMAGE="${HCLOUD_IMAGE:-debian-12}"
PREFERRED_TYPES="${HCLOUD_PREFERRED_TYPES:-cx23,cx22,cpx11,cpx21,cpx22,cpx31,cax11,ccx13}"
SSH_KEY_PATH="${HCLOUD_SSH_KEY_PATH:-}"

if [[ -z "$SSH_KEY_PATH" ]]; then
  SSH_KEY_PATH="$(pick_default_key_path || true)"
fi

if [[ -z "$SSH_KEY_PATH" || ! -f "$SSH_KEY_PATH" ]]; then
  echo "No SSH public key found. Set HCLOUD_SSH_KEY_PATH to a .pub file." >&2
  exit 1
fi

PUBLIC_KEY="$(<"$SSH_KEY_PATH")"
SSH_KEY_NAME="${HCLOUD_SSH_KEY_NAME:-$(basename "$SSH_KEY_PATH" .pub)-$(hostname -s 2>/dev/null || hostname)}"

if [[ -n "${HCLOUD_SERVER_TYPE:-}" ]]; then
  SERVER_TYPE="$HCLOUD_SERVER_TYPE"
else
  SERVER_TYPES_JSON="$(api GET "/server_types?per_page=100")"
  SERVER_TYPE="$(
    JSON_INPUT="$SERVER_TYPES_JSON" python3 -c '
import json
import os
import sys

preferred = [name.strip() for name in sys.argv[1].split(",") if name.strip()]
data = json.loads(os.environ["JSON_INPUT"])
available = {item["name"] for item in data.get("server_types", [])}

for name in preferred:
    if name in available:
        print(name)
        break
' "$PREFERRED_TYPES"
  )"
fi

if [[ -z "$SERVER_TYPE" ]]; then
  echo "Could not pick a server type automatically. Set HCLOUD_SERVER_TYPE explicitly." >&2
  exit 1
fi

SSH_KEYS_JSON="$(api GET "/ssh_keys?per_page=100")"
SSH_KEY_ID="$(
  JSON_INPUT="$SSH_KEYS_JSON" python3 -c '
import json
import os
import sys

target_name = sys.argv[1]
target_key = sys.argv[2].strip()
data = json.loads(os.environ["JSON_INPUT"])

for item in data.get("ssh_keys", []):
    if item.get("name") == target_name or item.get("public_key", "").strip() == target_key:
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
    "public_key": sys.argv[2],
}))
PY
  )"
  CREATE_KEY_JSON="$(api POST "/ssh_keys" "$SSH_KEY_PAYLOAD")"
  SSH_KEY_ID="$(
    JSON_INPUT="$CREATE_KEY_JSON" python3 -c '
import json
import os

print(json.loads(os.environ["JSON_INPUT"])["ssh_key"]["id"])
'
  )"
fi

SERVER_PAYLOAD="$(
  python3 - "$SERVER_NAME" "$SERVER_TYPE" "$IMAGE" "$LOCATION" "$SSH_KEY_ID" <<'PY'
import json
import sys

print(json.dumps({
    "name": sys.argv[1],
    "server_type": sys.argv[2],
    "image": sys.argv[3],
    "location": sys.argv[4],
    "ssh_keys": [int(sys.argv[5])],
    "start_after_create": True,
}))
PY
)"

CREATE_SERVER_JSON="$(api POST "/servers" "$SERVER_PAYLOAD")"

read -r SERVER_ID SERVER_IP <<EOF
$(
  JSON_INPUT="$CREATE_SERVER_JSON" python3 -c '
import json
import os

server = json.loads(os.environ["JSON_INPUT"])["server"]
print(server["id"], server["public_net"]["ipv4"]["ip"])
'
)
EOF

for _ in $(seq 1 30); do
  STATUS_JSON="$(api GET "/servers/${SERVER_ID}")"
  STATUS="$(
    JSON_INPUT="$STATUS_JSON" python3 -c '
import json
import os

print(json.loads(os.environ["JSON_INPUT"])["server"]["status"])
'
  )"
  if [[ "$STATUS" == "running" ]]; then
    break
  fi
  sleep 2
done

cat <<EOF
Created Hetzner server:
  name:       $SERVER_NAME
  id:         $SERVER_ID
  type:       $SERVER_TYPE
  image:      $IMAGE
  location:   $LOCATION
  ipv4:       $SERVER_IP
  ssh key:    $SSH_KEY_NAME ($SSH_KEY_PATH)

Next:
  ssh root@$SERVER_IP
  /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/migrate-xray-from-old-vps.sh root@$SERVER_IP
EOF
