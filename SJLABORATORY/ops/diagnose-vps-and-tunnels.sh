#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

xray_bin="${XRAY_BIN:-$(command -v xray || true)}"
ssh_key="${XRAY_FLEET_SSH_KEY:-$HOME/.ssh/sergei-macbook-vps}"
aws_ssh_key="${AWS_SSH_KEY:-$HOME/.ssh/id_rsa}"
aws_ssh_user="${AWS_SSH_USER:-ubuntu}"
http_base_port="${HTTP_BASE_PORT:-19281}"
socks_base_port="${SOCKS_BASE_PORT:-19280}"

entries=(
  "hostkey|${repo_root}/secrets/PROXY/xray/config.working_baseline.json|5.39.219.74"
  "exoscale|${repo_root}/secrets/PROXY/xray/config.exoscale.json|debian@194.182.174.240"
  "aws|${repo_root}/secrets/PROXY/xray/config.aws.json|"
)

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

require_cmd curl
require_cmd python3

if [[ -z "$xray_bin" ]]; then
  echo "xray binary not found in PATH." >&2
  exit 1
fi

read_config_address() {
  local path="$1"
  CONFIG_PATH="$path" python3 - <<'PY'
import json
import os

with open(os.environ["CONFIG_PATH"], "r", encoding="utf-8") as fh:
    data = json.load(fh)

print(data["outbounds"][0]["settings"]["vnext"][0]["address"])
PY
}

write_canary_config() {
  local source_path="$1"
  local output_path="$2"
  local socks_port="$3"
  local http_port="$4"

  SOURCE_PATH="$source_path" OUTPUT_PATH="$output_path" SOCKS_PORT="$socks_port" HTTP_PORT="$http_port" python3 - <<'PY'
import json
import os

source_path = os.environ["SOURCE_PATH"]
output_path = os.environ["OUTPUT_PATH"]
socks_port = int(os.environ["SOCKS_PORT"])
http_port = int(os.environ["HTTP_PORT"])

with open(source_path, "r", encoding="utf-8") as fh:
    data = json.load(fh)

for inbound in data.get("inbounds", []):
    if inbound.get("protocol") == "socks":
        inbound["port"] = socks_port
    elif inbound.get("protocol") == "http":
        inbound["port"] = http_port

with open(output_path, "w", encoding="utf-8") as fh:
    json.dump(data, fh, indent=2)
    fh.write("\n")
PY
}

probe_proxy() {
  local http_port="$1"
  local name="$2"
  local url="$3"
  local max_time="${4:-20}"

  curl --silent --show-error --output /dev/null \
    --max-time "${max_time}" \
    --proxy "http://127.0.0.1:${http_port}" \
    --write-out "${name}|%{http_code}|%{time_connect}|%{time_appconnect}|%{time_starttransfer}|%{time_total}|%{size_download}|%{speed_download}\n" \
    "$url"
}

proxy_exit_ip_for_port() {
  local http_port="$1"
  local candidate

  for candidate in \
    "https://ifconfig.me/ip" \
    "https://api.ipify.org" \
    "https://icanhazip.com"
  do
    if curl --silent --show-error --max-time 10 --proxy "http://127.0.0.1:${http_port}" "$candidate" 2>/dev/null; then
      return 0
    fi
  done

  return 1
}

wait_for_listener() {
  local port="$1"
  local attempt
  for attempt in {1..20}; do
    if lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

tcp_check() {
  local host="$1"
  if nc -z -G 5 "$host" 443 >/dev/null 2>&1; then
    echo "tcp443=ok"
  else
    echo "tcp443=fail"
  fi
}

audit_server() {
  local label="$1"
  local ssh_target="$2"
  if [[ -z "$ssh_target" ]]; then
    echo "server_audit=skip"
    return 0
  fi

  local ssh_opts=(-o BatchMode=yes -o ConnectTimeout=8)
  local selected_key="$ssh_key"
  if [[ "$label" == "aws" && -n "$aws_ssh_key" && -f "$aws_ssh_key" ]]; then
    selected_key="$aws_ssh_key"
  fi

  if [[ -n "$selected_key" && -f "$selected_key" ]]; then
    ssh_opts+=(-i "$selected_key")
  fi

  local result
  result="$(
    ssh "${ssh_opts[@]}" "$ssh_target" 'bash -se' <<'EOF' 2>/dev/null || true
set -euo pipefail
SUDO=""
if [[ "$(id -u)" -ne 0 ]]; then
  SUDO=sudo
fi
SERVICE="$($SUDO systemctl is-active xray 2>/dev/null || echo unknown)"
LISTEN="down"
if $SUDO ss -ltn 2>/dev/null | grep -q ':443'; then
  LISTEN="up"
fi
VERSION="$($SUDO /usr/local/bin/xray version 2>/dev/null | head -n1 || true)"
if [[ -z "$VERSION" ]]; then
  VERSION="$($SUDO xray version 2>/dev/null | head -n1 || true)"
fi
printf '%s|%s|%s\n' "$SERVICE" "$LISTEN" "${VERSION:-unavailable}"
EOF
  )"

  if [[ -z "$result" ]]; then
    echo "server_audit=ssh-fail"
  else
    echo "server_audit=$result"
  fi
}

echo "live_tunnel_exit_ip=$(curl -fsS --proxy http://127.0.0.1:8080 https://ifconfig.me/ip 2>/dev/null || echo unavailable)"
echo "direct_exit_ip=$(curl -fsS --noproxy '*' https://ifconfig.me/ip 2>/dev/null || echo unavailable)"
echo

index=0
for entry in "${entries[@]}"; do
  IFS='|' read -r label config_path ssh_target <<<"$entry"

  if [[ ! -f "$config_path" ]]; then
    echo "== ${label} =="
    echo "config=missing:$config_path"
    echo
    index=$((index + 1))
    continue
  fi

  target_ip="$(read_config_address "$config_path")"
  if [[ "$label" == "aws" && -z "$ssh_target" ]]; then
    ssh_target="${aws_ssh_user}@${target_ip}"
  fi

  socks_port=$((socks_base_port + index * 2))
  http_port=$((http_base_port + index * 2))
  temp_base="$(mktemp "/tmp/xray-${label}.XXXXXX")"
  temp_config="${temp_base}.json"
  temp_log="${temp_base}.log"
  temp_pid=""

  echo "== ${label} =="
  echo "config=${config_path}"
  echo "target_ip=${target_ip}"
  echo "$(tcp_check "$target_ip")"
  echo "$(audit_server "$label" "$ssh_target")"

  "$xray_bin" run -test -config "$config_path" >/dev/null
  write_canary_config "$config_path" "$temp_config" "$socks_port" "$http_port"
  "$xray_bin" run -test -config "$temp_config" >/dev/null

  "$xray_bin" run -config "$temp_config" >"$temp_log" 2>&1 &
  temp_pid="$!"
  sleep 2

  if ! kill -0 "$temp_pid" >/dev/null 2>&1 || ! wait_for_listener "$http_port"; then
    echo "canary=failed-to-start"
    sed -n '1,80p' "$temp_log" || true
  else
    echo "canary_expected_ip=${target_ip}"
    echo "canary_exit_ip=$(proxy_exit_ip_for_port "${http_port}" 2>/dev/null || echo unavailable)"
    echo "name|http_code|connect_s|tls_s|ttfb_s|total_s|bytes|bytes_per_s"
    probe_proxy "$http_port" "google_204" "https://www.google.com/generate_204" || true
    probe_proxy "$http_port" "github" "https://github.com/" || true
    probe_proxy "$http_port" "cloudflare" "https://www.cloudflare.com/" || true
    probe_proxy "$http_port" "telegram_org" "https://telegram.org/" || true
    probe_proxy "$http_port" "t_me" "https://t.me/" || true
    probe_proxy "$http_port" "web_telegram" "https://web.telegram.org/" || true
    probe_proxy "$http_port" "youtube" "https://www.youtube.com/" || true
    probe_proxy "$http_port" "speed_cf_5mb" "https://speed.cloudflare.com/__down?bytes=5000000" "40" || true
  fi

  if [[ -n "$temp_pid" ]] && kill -0 "$temp_pid" >/dev/null 2>&1; then
    kill "$temp_pid" >/dev/null 2>&1 || true
    wait "$temp_pid" >/dev/null 2>&1 || true
  fi
  rm -f "$temp_config" "$temp_log"

  echo
  index=$((index + 1))
done
