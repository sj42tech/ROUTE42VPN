#!/usr/bin/env bash
set -euo pipefail

DEFAULT_KEY_CANDIDATES=(
  "$HOME/.ssh/id_rsa.pub"
  "$HOME/.ssh/sergei-macbook-vps.pub"
  "$HOME/.ssh/id_ed25519_2026.pub"
  "$HOME/.ssh/id_ed25519.pub"
)

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
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

aws_json() {
  AWS_PAGER="" aws --no-cli-pager --output json --region "$AWS_REGION" "$@"
}

require_cmd aws
require_cmd python3
require_cmd base64

AWS_REGION="${AWS_REGION:-ap-northeast-2}"
INSTANCE_NAME="${AWS_LIGHTSAIL_INSTANCE_NAME:-xray-aws-$(date +%m%d-%H%M)}"
AVAILABILITY_ZONE="${AWS_LIGHTSAIL_AVAILABILITY_ZONE:-}"
BLUEPRINT_MATCHES="${AWS_LIGHTSAIL_BLUEPRINT_MATCHES:-Debian 12|Debian GNU/Linux 12|Ubuntu 24.04}"
PREFERRED_BUNDLES="${AWS_LIGHTSAIL_PREFERRED_BUNDLES:-micro_3_0,small_3_0,nano_3_0}"
ATTACH_STATIC_IP="${AWS_LIGHTSAIL_ATTACH_STATIC_IP:-1}"
STATIC_IP_NAME="${AWS_LIGHTSAIL_STATIC_IP_NAME:-${INSTANCE_NAME}-ip}"
DRY_RUN="${AWS_LIGHTSAIL_DRY_RUN:-0}"
SSH_KEY_PATH="${AWS_LIGHTSAIL_SSH_KEY_PATH:-}"

if [[ -z "$SSH_KEY_PATH" ]]; then
  SSH_KEY_PATH="$(pick_default_key_path || true)"
fi

if [[ -z "$SSH_KEY_PATH" || ! -f "$SSH_KEY_PATH" ]]; then
  echo "No SSH public key found. Set AWS_LIGHTSAIL_SSH_KEY_PATH to a .pub file." >&2
  exit 1
fi

SSH_KEY_TYPE="$(awk '{print $1}' "$SSH_KEY_PATH")"
if [[ "$SSH_KEY_TYPE" != "ssh-rsa" ]]; then
  cat >&2 <<EOF
Lightsail import-key-pair expects an ssh-rsa public key.
Current key: $SSH_KEY_PATH ($SSH_KEY_TYPE)

Use for example:
  export AWS_LIGHTSAIL_SSH_KEY_PATH="\$HOME/.ssh/id_rsa.pub"
EOF
  exit 1
fi

KEY_PAIR_NAME="${AWS_LIGHTSAIL_KEY_PAIR_NAME:-$(basename "$SSH_KEY_PATH" .pub)-lightsail}"

aws_json sts get-caller-identity >/dev/null

if aws_json lightsail get-instance --instance-name "$INSTANCE_NAME" >/dev/null 2>&1; then
  echo "Lightsail instance already exists: $INSTANCE_NAME" >&2
  exit 1
fi

REGIONS_JSON="$(aws_json lightsail get-regions --include-availability-zones)"
read -r REGION_NAME CHOSEN_AZ <<EOF
$(
  JSON_INPUT="$REGIONS_JSON" python3 - "$AWS_REGION" "$AVAILABILITY_ZONE" <<'PY'
import json
import os
import sys

target_region = sys.argv[1]
requested_az = sys.argv[2]
data = json.loads(os.environ["JSON_INPUT"])

for region in data.get("regions", []):
    region_name = region.get("name") or region.get("regionName")
    if region_name != target_region:
        continue
    zones = region.get("availabilityZones", []) or []
    if requested_az:
        for zone in zones:
            zone_name = zone.get("zoneName") or zone.get("name")
            if zone_name == requested_az:
                print(region_name, zone_name)
                raise SystemExit(0)
        raise SystemExit(1)
    for zone in zones:
        zone_name = zone.get("zoneName") or zone.get("name")
        state = str(zone.get("state", "available")).lower()
        if zone_name and state in ("available", "active", ""):
            print(region_name, zone_name)
            raise SystemExit(0)
    print(region_name, "")
    raise SystemExit(0)
raise SystemExit(1)
PY
)
EOF

if [[ -z "$REGION_NAME" || -z "$CHOSEN_AZ" ]]; then
  echo "Could not resolve an availability zone for region $AWS_REGION." >&2
  exit 1
fi

BLUEPRINTS_JSON="$(aws_json lightsail get-blueprints)"
read -r BLUEPRINT_ID BLUEPRINT_NAME <<EOF
$(
  JSON_INPUT="$BLUEPRINTS_JSON" python3 - "$BLUEPRINT_MATCHES" <<'PY'
import json
import os
import re
import sys

patterns = [p.strip() for p in sys.argv[1].split("|") if p.strip()]
items = json.loads(os.environ["JSON_INPUT"]).get("blueprints", [])

def text(item):
    return " ".join([
        item.get("name", ""),
        item.get("blueprintId", ""),
        item.get("description", ""),
        item.get("group", ""),
    ])

active_items = [
    item for item in items
    if item.get("isActive", True) and item.get("type", "os") == "os"
]

for pattern in patterns:
    regex = re.compile(pattern, re.IGNORECASE)
    for item in active_items:
        if regex.search(text(item)):
            print(item["blueprintId"], item.get("name", item["blueprintId"]))
            raise SystemExit(0)

for item in active_items:
    if "debian" in text(item).lower():
        print(item["blueprintId"], item.get("name", item["blueprintId"]))
        raise SystemExit(0)

for item in active_items:
    print(item["blueprintId"], item.get("name", item["blueprintId"]))
    raise SystemExit(0)

raise SystemExit(1)
PY
)
EOF

if [[ -z "$BLUEPRINT_ID" ]]; then
  echo "Could not find an active Debian or Ubuntu blueprint in Lightsail." >&2
  exit 1
fi

BUNDLES_JSON="$(aws_json lightsail get-bundles)"
read -r BUNDLE_ID BUNDLE_NAME BUNDLE_CPU BUNDLE_RAM BUNDLE_DISK BUNDLE_PRICE <<EOF
$(
  JSON_INPUT="$BUNDLES_JSON" python3 - "$PREFERRED_BUNDLES" <<'PY'
import json
import os
import sys

preferred = [p.strip() for p in sys.argv[1].split(",") if p.strip()]
items = json.loads(os.environ["JSON_INPUT"]).get("bundles", [])

linux = [
    item for item in items
    if item.get("isActive", True)
    and "LINUX_UNIX" in item.get("supportedPlatforms", ["LINUX_UNIX"])
    and int(item.get("publicIpv4AddressCount", 1)) >= 1
]

def emit(item):
    print(
        item["bundleId"],
        item.get("name", item["bundleId"]),
        int(item.get("cpuCount", 0)),
        float(item.get("ramSizeInGb", 0)),
        int(item.get("diskSizeInGb", 0)),
        float(item.get("price", 0)),
    )
    raise SystemExit(0)

for bundle_id in preferred:
    for item in linux:
        if item.get("bundleId") == bundle_id:
            emit(item)

exact_range = [
    item for item in linux
    if 1.0 <= float(item.get("ramSizeInGb", 0)) <= 2.0
    and 20 <= int(item.get("diskSizeInGb", 0)) <= 60
    and 1 <= int(item.get("cpuCount", 0)) <= 2
]
exact_range.sort(key=lambda item: (float(item.get("price", 0)), int(item.get("diskSizeInGb", 0))))
for item in exact_range:
    emit(item)

linux.sort(key=lambda item: float(item.get("price", 0)))
for item in linux:
    emit(item)

raise SystemExit(1)
PY
)
EOF

if [[ -z "$BUNDLE_ID" ]]; then
  echo "Could not pick a Lightsail bundle automatically." >&2
  exit 1
fi

KEY_PAIRS_JSON="$(aws_json lightsail get-key-pairs)"
KEY_EXISTS="$(
  JSON_INPUT="$KEY_PAIRS_JSON" python3 - "$KEY_PAIR_NAME" <<'PY'
import json
import os
import sys

target = sys.argv[1]
for item in json.loads(os.environ["JSON_INPUT"]).get("keyPairs", []):
    if item.get("name") == target:
        print("yes")
        break
PY
)"

if [[ "$DRY_RUN" == "1" ]]; then
  cat <<EOF
AWS Lightsail dry run only.

Planned instance:
  name:              $INSTANCE_NAME
  region:            $AWS_REGION
  availability zone: $CHOSEN_AZ
  blueprint:         $BLUEPRINT_NAME ($BLUEPRINT_ID)
  bundle:            $BUNDLE_NAME ($BUNDLE_ID)
  bundle specs:      ${BUNDLE_CPU} vCPU / ${BUNDLE_RAM} GB RAM / ${BUNDLE_DISK} GB disk
  price:             \$$BUNDLE_PRICE per month
  ssh public key:    $SSH_KEY_PATH
  key pair name:     $KEY_PAIR_NAME
  static ip:         $ATTACH_STATIC_IP ($STATIC_IP_NAME)
EOF
  exit 0
fi

if [[ "$KEY_EXISTS" != "yes" ]]; then
  PUBLIC_KEY_B64="$(base64 < "$SSH_KEY_PATH" | tr -d '\n')"
  aws_json lightsail import-key-pair \
    --key-pair-name "$KEY_PAIR_NAME" \
    --public-key-base64 "$PUBLIC_KEY_B64" >/dev/null
fi

aws_json lightsail create-instances \
  --instance-names "$INSTANCE_NAME" \
  --availability-zone "$CHOSEN_AZ" \
  --blueprint-id "$BLUEPRINT_ID" \
  --bundle-id "$BUNDLE_ID" \
  --key-pair-name "$KEY_PAIR_NAME" >/dev/null

PORT_INFOS_JSON="$(mktemp)"
trap 'rm -f "$PORT_INFOS_JSON"' EXIT
cat >"$PORT_INFOS_JSON" <<'EOF'
[
  {
    "fromPort": 22,
    "toPort": 22,
    "protocol": "tcp",
    "cidrs": ["0.0.0.0/0"]
  },
  {
    "fromPort": 443,
    "toPort": 443,
    "protocol": "tcp",
    "cidrs": ["0.0.0.0/0"]
  }
]
EOF

INSTANCE_STATE=""
PUBLIC_IP=""
PRIVATE_IP=""
for _ in $(seq 1 60); do
  INSTANCE_JSON="$(aws_json lightsail get-instance --instance-name "$INSTANCE_NAME")"
  read -r INSTANCE_STATE PUBLIC_IP PRIVATE_IP <<EOF
$(
  JSON_INPUT="$INSTANCE_JSON" python3 - <<'PY'
import json
import os

instance = json.loads(os.environ["JSON_INPUT"]).get("instance", {})
state = (
    instance.get("state", {}).get("name")
    or instance.get("state", {}).get("code")
    or ""
)
public_ip = instance.get("publicIpAddress", "") or ""
private_ip = instance.get("privateIpAddress", "") or ""
print(state, public_ip, private_ip)
PY
)
EOF
  if [[ "$INSTANCE_STATE" == "running" && -n "$PUBLIC_IP" ]]; then
    break
  fi
  sleep 5
done

if [[ "$INSTANCE_STATE" != "running" ]]; then
  echo "Instance did not reach running state in time." >&2
  exit 1
fi

aws_json lightsail put-instance-public-ports \
  --instance-name "$INSTANCE_NAME" \
  --port-infos "file://$PORT_INFOS_JSON" >/dev/null

if [[ "$ATTACH_STATIC_IP" == "1" ]]; then
  STATIC_IP_JSON=""
  ATTACHED_TO=""
  STATIC_IP_ADDRESS=""
  if STATIC_IP_JSON="$(aws_json lightsail get-static-ip --static-ip-name "$STATIC_IP_NAME" 2>/dev/null)"; then
    read -r ATTACHED_TO STATIC_IP_ADDRESS <<EOF
$(
  JSON_INPUT="$STATIC_IP_JSON" python3 - <<'PY'
import json
import os

static_ip = json.loads(os.environ["JSON_INPUT"]).get("staticIp", {})
print(static_ip.get("attachedTo", "") or "", static_ip.get("ipAddress", "") or "")
PY
)
EOF
    if [[ -n "$ATTACHED_TO" && "$ATTACHED_TO" != "$INSTANCE_NAME" ]]; then
      echo "Static IP $STATIC_IP_NAME already exists and is attached to $ATTACHED_TO." >&2
      exit 1
    fi
  else
    aws_json lightsail allocate-static-ip --static-ip-name "$STATIC_IP_NAME" >/dev/null
  fi

  if [[ "$ATTACHED_TO" != "$INSTANCE_NAME" ]]; then
    aws_json lightsail attach-static-ip \
      --static-ip-name "$STATIC_IP_NAME" \
      --instance-name "$INSTANCE_NAME" >/dev/null
  fi

  STATIC_IP_JSON="$(aws_json lightsail get-static-ip --static-ip-name "$STATIC_IP_NAME")"
  STATIC_IP_ADDRESS="$(
    JSON_INPUT="$STATIC_IP_JSON" python3 - <<'PY'
import json
import os

static_ip = json.loads(os.environ["JSON_INPUT"]).get("staticIp", {})
print(static_ip.get("ipAddress", "") or "")
PY
)"
  if [[ -n "$STATIC_IP_ADDRESS" ]]; then
    PUBLIC_IP="$STATIC_IP_ADDRESS"
  fi
fi

cat <<EOF
Created AWS Lightsail server:
  name:              $INSTANCE_NAME
  region:            $AWS_REGION
  availability zone: $CHOSEN_AZ
  blueprint:         $BLUEPRINT_NAME ($BLUEPRINT_ID)
  bundle:            $BUNDLE_NAME ($BUNDLE_ID)
  bundle specs:      ${BUNDLE_CPU} vCPU / ${BUNDLE_RAM} GB RAM / ${BUNDLE_DISK} GB disk
  public ip:         $PUBLIC_IP
  private ip:        $PRIVATE_IP
  ssh public key:    $SSH_KEY_PATH
  key pair name:     $KEY_PAIR_NAME
  static ip:         $ATTACH_STATIC_IP ($STATIC_IP_NAME)

Lightsail Debian SSH user:
  admin

Next:
  ssh -i "${SSH_KEY_PATH%.pub}" admin@$PUBLIC_IP
  /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/migrate-xray-from-old-vps.sh admin@$PUBLIC_IP
EOF
