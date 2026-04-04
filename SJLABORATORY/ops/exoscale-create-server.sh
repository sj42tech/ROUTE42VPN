#!/usr/bin/env bash
set -euo pipefail

GLOBAL_API_HOST="api-ch-gva-2.exoscale.com"
DEFAULT_KEY_CANDIDATES=(
  "$HOME/.ssh/sergei-macbook-vps.pub"
  "$HOME/.ssh/id_ed25519_2026.pub"
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
  local host="$1"
  local method="$2"
  local path="$3"
  local query_json="${4:-}"
  local body="${5:-}"

  if [[ -z "$query_json" ]]; then
    query_json='{}'
  fi

  EXO_HOST="$host" \
  EXO_METHOD="$method" \
  EXO_PATH="$path" \
  EXO_QUERY_JSON="$query_json" \
  EXO_BODY="$body" \
  EXO_API_KEY="$EXOSCALE_API_KEY" \
  EXO_API_SECRET="$EXOSCALE_API_SECRET" \
  python3 - <<'PY'
import base64
import hashlib
import hmac
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

host = os.environ["EXO_HOST"]
method = os.environ["EXO_METHOD"]
path = os.environ["EXO_PATH"]
query = json.loads(os.environ.get("EXO_QUERY_JSON", "{}") or "{}")
body = os.environ.get("EXO_BODY", "")
api_key = os.environ["EXO_API_KEY"]
api_secret = os.environ["EXO_API_SECRET"]

query_values = "".join(str(value) for value in query.values())
signed_query_args = ";".join(query.keys())
expires = str(int(time.time()) + 60)
message = "\n".join([
    f"{method} {path}",
    body,
    query_values,
    "",
    expires,
])
signature = base64.b64encode(
    hmac.new(api_secret.encode(), message.encode(), hashlib.sha256).digest()
).decode()

auth = f"EXO2-HMAC-SHA256 credential={api_key}"
if signed_query_args:
    auth += f",signed-query-args={signed_query_args}"
auth += f",expires={expires},signature={signature}"

url = f"https://{host}{path}"
if query:
    url += "?" + urllib.parse.urlencode(query)

headers = {"Authorization": auth}
data = None
if body:
    headers["Content-Type"] = "application/json"
    data = body.encode()

req = urllib.request.Request(url, method=method, headers=headers, data=data)

try:
    with urllib.request.urlopen(req, timeout=60) as response:
        raw = response.read().decode("utf-8", "replace")
        print(raw or "{}")
except urllib.error.HTTPError as exc:
    detail = exc.read().decode("utf-8", "replace")
    print(detail or f"HTTP {exc.code}", file=sys.stderr)
    raise SystemExit(1)
PY
}

security_group_rule_present() {
  local sg_json="$1"
  local flow_direction="$2"
  local protocol="$3"
  local start_port="$4"
  local end_port="$5"
  local network="$6"

  JSON_INPUT="$sg_json" python3 - "$flow_direction" "$protocol" "$start_port" "$end_port" "$network" <<'PY'
import json
import os
import sys

target_flow = sys.argv[1]
target_protocol = sys.argv[2]
target_start = int(sys.argv[3])
target_end = int(sys.argv[4])
target_network = sys.argv[5]

rules = json.loads(os.environ["JSON_INPUT"]).get("rules", [])
for rule in rules:
    if (
        rule.get("flow-direction") == target_flow
        and rule.get("protocol") == target_protocol
        and int(rule.get("start-port", -1)) == target_start
        and int(rule.get("end-port", -1)) == target_end
        and rule.get("network") == target_network
    ):
        print("yes")
        break
PY
}

ensure_security_group_rule() {
  local api_host="$1"
  local security_group_id="$2"
  local flow_direction="$3"
  local protocol="$4"
  local start_port="$5"
  local end_port="$6"
  local network="$7"
  local description="$8"
  local sg_json
  local present
  local rule_payload

  sg_json="$(api "$api_host" GET "/v2/security-group/${security_group_id}")"
  present="$(security_group_rule_present "$sg_json" "$flow_direction" "$protocol" "$start_port" "$end_port" "$network")"

  if [[ "$present" == "yes" ]]; then
    return 0
  fi

  rule_payload="$(
    python3 - "$flow_direction" "$protocol" "$start_port" "$end_port" "$network" "$description" <<'PY'
import json
import sys

print(json.dumps({
    "flow-direction": sys.argv[1],
    "protocol": sys.argv[2],
    "start-port": int(sys.argv[3]),
    "end-port": int(sys.argv[4]),
    "network": sys.argv[5],
    "description": sys.argv[6],
}))
PY
  )"

  api "$api_host" POST "/v2/security-group/${security_group_id}/rules" "{}" "$rule_payload" >/dev/null
}

require_cmd python3
require_env EXOSCALE_API_KEY
require_env EXOSCALE_API_SECRET

ZONE="${EXOSCALE_ZONE:-at-vie-1}"
SERVER_NAME="${EXOSCALE_SERVER_NAME:-xray-$(date +%m%d-%H%M)}"
PREFERRED_SIZES="${EXOSCALE_PREFERRED_SIZES:-tiny,small,micro}"
DISK_SIZE_GB="${EXOSCALE_DISK_SIZE_GB:-10}"
TEMPLATE_MATCHES="${EXOSCALE_TEMPLATE_MATCHES:-Linux Debian 12 (Bookworm) 64-bit|Linux Ubuntu 24.04 LTS 64-bit}"
DRY_RUN="${EXOSCALE_DRY_RUN:-0}"
SSH_KEY_PATH="${EXOSCALE_SSH_KEY_PATH:-}"

if [[ -z "$SSH_KEY_PATH" ]]; then
  SSH_KEY_PATH="$(pick_default_key_path || true)"
fi

if [[ -z "$SSH_KEY_PATH" || ! -f "$SSH_KEY_PATH" ]]; then
  echo "No SSH public key found. Set EXOSCALE_SSH_KEY_PATH to a .pub file." >&2
  exit 1
fi

PUBLIC_KEY="$(<"$SSH_KEY_PATH")"
SSH_KEY_NAME="${EXOSCALE_SSH_KEY_NAME:-$(basename "$SSH_KEY_PATH" .pub)-$(hostname -s 2>/dev/null || hostname)}"

ZONES_JSON="$(api "$GLOBAL_API_HOST" GET "/v2/zone")"
ZONE_API_HOST="$(
  JSON_INPUT="$ZONES_JSON" python3 - "$ZONE" <<'PY'
import json
import os
import sys
import urllib.parse

target = sys.argv[1]
zones = json.loads(os.environ["JSON_INPUT"]).get("zones", [])
for zone in zones:
    if zone.get("name") == target:
        print(urllib.parse.urlparse(zone["api-endpoint"]).netloc)
        break
PY
)"

if [[ -z "$ZONE_API_HOST" ]]; then
  echo "Unknown Exoscale zone: $ZONE" >&2
  exit 1
fi

INSTANCE_TYPES_JSON="$(api "$ZONE_API_HOST" GET "/v2/instance-type")"
INSTANCE_TYPE_ID="$(
  JSON_INPUT="$INSTANCE_TYPES_JSON" python3 - "$PREFERRED_SIZES" <<'PY'
import json
import os
import sys

preferred = [size.strip() for size in sys.argv[1].split(",") if size.strip()]
items = json.loads(os.environ["JSON_INPUT"]).get("instance-types", [])

for preferred_size in preferred:
    for item in items:
        if item.get("authorized") and item.get("size") == preferred_size:
            print(item["id"])
            raise SystemExit(0)
PY
)"

INSTANCE_TYPE_SIZE="$(
  JSON_INPUT="$INSTANCE_TYPES_JSON" python3 - "$INSTANCE_TYPE_ID" <<'PY'
import json
import os
import sys

target = sys.argv[1]
for item in json.loads(os.environ["JSON_INPUT"]).get("instance-types", []):
    if item.get("id") == target:
        print(item.get("size", ""))
        break
PY
)"

INSTANCE_MEMORY_MB="$(
  JSON_INPUT="$INSTANCE_TYPES_JSON" python3 - "$INSTANCE_TYPE_ID" <<'PY'
import json
import os
import sys

target = sys.argv[1]
for item in json.loads(os.environ["JSON_INPUT"]).get("instance-types", []):
    if item.get("id") == target:
        print(int(item.get("memory", 0) / 1024 / 1024))
        break
PY
)"

if [[ -z "$INSTANCE_TYPE_ID" ]]; then
  echo "Could not pick an Exoscale instance type automatically. Set EXOSCALE_PREFERRED_SIZES explicitly." >&2
  exit 1
fi

TEMPLATES_JSON="$(api "$ZONE_API_HOST" GET "/v2/template")"
TEMPLATE_ID="$(
  JSON_INPUT="$TEMPLATES_JSON" python3 - "$TEMPLATE_MATCHES" <<'PY'
import json
import os
import sys

patterns = [item.strip() for item in sys.argv[1].split("|") if item.strip()]
templates = json.loads(os.environ["JSON_INPUT"]).get("templates", [])

for pattern in patterns:
    for template in templates:
        if template.get("name") == pattern:
            print(template["id"])
            raise SystemExit(0)
PY
)"

TEMPLATE_NAME="$(
  JSON_INPUT="$TEMPLATES_JSON" python3 - "$TEMPLATE_ID" <<'PY'
import json
import os
import sys

target = sys.argv[1]
for template in json.loads(os.environ["JSON_INPUT"]).get("templates", []):
    if template.get("id") == target:
        print(template.get("name", ""))
        break
PY
)"

if [[ -z "$TEMPLATE_ID" ]]; then
  echo "Could not find a matching Exoscale template. Set EXOSCALE_TEMPLATE_MATCHES explicitly." >&2
  exit 1
fi

SSH_KEYS_JSON="$(api "$GLOBAL_API_HOST" GET "/v2/ssh-key")"
SSH_KEY_PRESENT="$(
  JSON_INPUT="$SSH_KEYS_JSON" python3 - "$SSH_KEY_NAME" <<'PY'
import json
import os
import sys

target = sys.argv[1]
for item in json.loads(os.environ["JSON_INPUT"]).get("ssh-keys", []):
    if item.get("name") == target:
        print("yes")
        break
PY
)"

if [[ "$DRY_RUN" == "1" ]]; then
  cat <<EOF
Dry run only. No resources were created.

Resolved Exoscale plan:
  zone:         $ZONE
  api host:     $ZONE_API_HOST
  server name:  $SERVER_NAME
  size:         $INSTANCE_TYPE_SIZE
  memory:       ${INSTANCE_MEMORY_MB} MB
  disk:         ${DISK_SIZE_GB} GB
  template:     $TEMPLATE_NAME
  ssh key:      $SSH_KEY_NAME ($SSH_KEY_PATH)
  key exists:   ${SSH_KEY_PRESENT:-no}
EOF
  exit 0
fi

if [[ -z "$SSH_KEY_PRESENT" ]]; then
  SSH_KEY_PAYLOAD="$(
    python3 - "$SSH_KEY_NAME" "$PUBLIC_KEY" <<'PY'
import json
import sys

print(json.dumps({
    "name": sys.argv[1],
    "public-key": sys.argv[2],
}))
PY
  )"
  api "$GLOBAL_API_HOST" POST "/v2/ssh-key" "{}" "$SSH_KEY_PAYLOAD" >/dev/null

  for _ in $(seq 1 20); do
    SSH_KEYS_JSON="$(api "$GLOBAL_API_HOST" GET "/v2/ssh-key")"
    SSH_KEY_PRESENT="$(
      JSON_INPUT="$SSH_KEYS_JSON" python3 - "$SSH_KEY_NAME" <<'PY'
import json
import os
import sys

target = sys.argv[1]
for item in json.loads(os.environ["JSON_INPUT"]).get("ssh-keys", []):
    if item.get("name") == target:
        print("yes")
        break
PY
    )"
    if [[ "$SSH_KEY_PRESENT" == "yes" ]]; then
      break
    fi
    sleep 2
  done
fi

if [[ "$SSH_KEY_PRESENT" != "yes" ]]; then
  echo "Exoscale SSH key import did not become visible in time." >&2
  exit 1
fi

INSTANCE_PAYLOAD="$(
  python3 - "$SERVER_NAME" "$INSTANCE_TYPE_ID" "$TEMPLATE_ID" "$DISK_SIZE_GB" "$SSH_KEY_NAME" <<'PY'
import json
import sys

print(json.dumps({
    "name": sys.argv[1],
    "instance-type": {"id": sys.argv[2]},
    "template": {"id": sys.argv[3]},
    "disk-size": int(sys.argv[4]),
    "ssh-keys": [{"name": sys.argv[5]}],
}))
PY
)"

CREATE_JSON="$(api "$ZONE_API_HOST" POST "/v2/instance" "{}" "$INSTANCE_PAYLOAD")"
read -r OP_STATE INSTANCE_ID <<EOF
$(
  JSON_INPUT="$CREATE_JSON" python3 - <<'PY'
import json
import os

data = json.loads(os.environ["JSON_INPUT"])
print(data.get("state", ""), data.get("reference", {}).get("id", ""))
PY
)
EOF

if [[ -z "$INSTANCE_ID" || ( "$OP_STATE" != "success" && "$OP_STATE" != "pending" ) ]]; then
  echo "Exoscale instance creation failed:" >&2
  echo "$CREATE_JSON" >&2
  exit 1
fi

INSTANCE_JSON=""
INSTANCE_STATE=""
INSTANCE_IP=""
SECURITY_GROUP_ID=""

for _ in $(seq 1 30); do
  INSTANCE_JSON="$(api "$ZONE_API_HOST" GET "/v2/instance/${INSTANCE_ID}")"
  read -r INSTANCE_STATE INSTANCE_IP SECURITY_GROUP_ID <<EOF
$(
    JSON_INPUT="$INSTANCE_JSON" python3 - <<'PY'
import json
import os

data = json.loads(os.environ["JSON_INPUT"])
security_groups = data.get("security-groups", [])
security_group_id = security_groups[0].get("id", "") if security_groups else ""
print(data.get("state", ""), data.get("public-ip", ""), security_group_id)
PY
  )
EOF
  if [[ "$INSTANCE_STATE" == "running" && -n "$INSTANCE_IP" ]]; then
    break
  fi
  sleep 5
done

if [[ -n "$SECURITY_GROUP_ID" ]]; then
  ensure_security_group_rule "$ZONE_API_HOST" "$SECURITY_GROUP_ID" ingress tcp 22 22 "0.0.0.0/0" "Allow SSH"
  ensure_security_group_rule "$ZONE_API_HOST" "$SECURITY_GROUP_ID" ingress tcp 443 443 "0.0.0.0/0" "Allow Xray HTTPS"
fi

cat <<EOF
Created Exoscale server:
  name:        $SERVER_NAME
  id:          $INSTANCE_ID
  zone:        $ZONE
  size:        $INSTANCE_TYPE_SIZE
  memory:      ${INSTANCE_MEMORY_MB} MB
  disk:        ${DISK_SIZE_GB} GB
  template:    $TEMPLATE_NAME
  ipv4:        ${INSTANCE_IP:-pending}
  ssh key:     $SSH_KEY_NAME ($SSH_KEY_PATH)
  security:    ${SECURITY_GROUP_ID:-default}

Next:
  ssh debian@${INSTANCE_IP:-<pending-ip>}
  /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/migrate-xray-from-old-vps.sh debian@${INSTANCE_IP:-<pending-ip>}
EOF
