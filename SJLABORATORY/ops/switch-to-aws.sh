#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

source_config="${AWS_SOURCE_CONFIG:-${repo_root}/secrets/PROXY/xray/config.aws.json}"
target_config="${repo_root}/secrets/PROXY/xray/config.json"
baseline_config="${repo_root}/secrets/PROXY/xray/config.working_baseline.json"
backup_dir="${repo_root}/secrets/PROXY/xray/backups"
rollback_script="${repo_root}/ops/rollback-to-baseline.sh"
timestamp="$(date +%Y%m%d-%H%M%S)"
backup_config="${backup_dir}/config.json.before-aws-switch-${timestamp}"
xray_bin="${XRAY_BIN:-$(command -v xray || true)}"
launchctl_label="${LAUNCHCTL_LABEL:-local.xray}"
launchctl_plist="${LAUNCHCTL_PLIST:-$HOME/Library/LaunchAgents/xray.plist}"
canary_socks_port="${CANARY_SOCKS_PORT:-19180}"
canary_http_port="${CANARY_HTTP_PORT:-19181}"
allow_live_switch="${ALLOW_LIVE_SWITCH:-0}"
required_baseline_ip="${REQUIRED_BASELINE_IP:-5.39.219.74}"
temp_config=""
temp_log=""
temp_pid=""
service_target=""

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "Missing required file: $path" >&2
    exit 1
  fi
}

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

cleanup_canary() {
  if [[ -n "${temp_pid}" ]] && kill -0 "${temp_pid}" >/dev/null 2>&1; then
    kill "${temp_pid}" >/dev/null 2>&1 || true
    wait "${temp_pid}" >/dev/null 2>&1 || true
  fi
  if [[ -n "${temp_config}" && -f "${temp_config}" ]]; then
    rm -f "${temp_config}"
  fi
  if [[ -n "${temp_log}" && -f "${temp_log}" ]]; then
    rm -f "${temp_log}"
  fi
}

resolve_service_target() {
  local user_domain="gui/$(id -u)"
  if launchctl print "${user_domain}/${launchctl_label}" >/dev/null 2>&1; then
    service_target="${user_domain}/${launchctl_label}"
    return 0
  fi
  if launchctl print "system/${launchctl_label}" >/dev/null 2>&1; then
    service_target="system/${launchctl_label}"
    return 0
  fi
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

proxy_exit_ip() {
  local http_port="$1"
  curl --silent --show-error --max-time 10 --proxy "http://127.0.0.1:${http_port}" https://ifconfig.me/ip
}

run_proxy_health_checks() {
  local http_port="$1"
  local socks_port="$2"
  local expected_ip="$3"
  local failed=0
  local actual_ip=""

  echo "Checking exit IP via port ${http_port}..."
  if ! actual_ip="$(proxy_exit_ip "${http_port}")"; then
    failed=1
  elif [[ "${actual_ip}" != "${expected_ip}" ]]; then
    echo "Expected exit IP ${expected_ip}, got ${actual_ip}" >&2
    failed=1
  else
    echo "Exit IP is ${actual_ip}"
  fi

  echo "Checking HTTPS to Google via port ${http_port}..."
  if ! curl --silent --show-error --max-time 10 --proxy "http://127.0.0.1:${http_port}" -I https://www.google.com >/dev/null; then
    failed=1
  fi

  echo "Checking HTTPS to Apple via port ${http_port}..."
  if ! curl --silent --show-error --max-time 10 --proxy "http://127.0.0.1:${http_port}" -I https://www.apple.com >/dev/null; then
    failed=1
  fi

  echo "Checking SOCKS reachability via port ${socks_port}..."
  if ! curl --silent --show-error --max-time 10 --socks5 "127.0.0.1:${socks_port}" -I https://www.google.com >/dev/null; then
    failed=1
  fi

  return "${failed}"
}

restart_launchctl_service() {
  local expected_ip="$1"

  launchctl kickstart -k "${service_target}" >/dev/null 2>&1 || return 1
  wait_for_listener 1080 || return 1
  wait_for_listener 8080 || return 1
  run_proxy_health_checks 8080 1080 "${expected_ip}" || return 1
  return 0
}

hard_restart_launchctl_service() {
  local expected_ip="$1"
  local gui_domain="gui/$(id -u)"

  if [[ "${service_target}" == ${gui_domain}/* && -f "${launchctl_plist}" ]]; then
    launchctl bootout "${service_target}" >/dev/null 2>&1 || true
    launchctl bootstrap "${gui_domain}" "${launchctl_plist}" >/dev/null 2>&1 || true
    launchctl kickstart -k "${service_target}" >/dev/null 2>&1 || true
  else
    launchctl kickstart -k "${service_target}" >/dev/null 2>&1 || true
  fi

  wait_for_listener 1080 || return 1
  wait_for_listener 8080 || return 1
  run_proxy_health_checks 8080 1080 "${expected_ip}" || return 1
  return 0
}

rollback_to_baseline() {
  echo "Rolling back to ${required_baseline_ip}..." >&2
  LAUNCHCTL_LABEL="${launchctl_label}" \
  LAUNCHCTL_PLIST="${launchctl_plist}" \
  XRAY_BIN="${xray_bin}" \
  "${rollback_script}"
}

require_file "${source_config}"
require_file "${target_config}"
require_file "${baseline_config}"
require_file "${rollback_script}"

mkdir -p "${backup_dir}"

if [[ -z "${xray_bin}" ]]; then
  echo "xray binary not found in PATH." >&2
  exit 1
fi

expected_aws_ip="$(read_config_address "${source_config}")"
current_target_ip="$(read_config_address "${target_config}")"
baseline_ip="$(read_config_address "${baseline_config}")"

if [[ "${baseline_ip}" != "${required_baseline_ip}" ]]; then
  echo "Unexpected baseline IP in ${baseline_config}: ${baseline_ip}" >&2
  exit 1
fi

"${xray_bin}" run -test -config "${source_config}"
echo "AWS source config validated successfully."

temp_config="$(mktemp /tmp/xray-aws-cutover.XXXXXX.json)"
temp_log="$(mktemp /tmp/xray-aws-cutover.XXXXXX.log)"
write_canary_config "${source_config}" "${temp_config}" "${canary_socks_port}" "${canary_http_port}"
"${xray_bin}" run -test -config "${temp_config}" >/dev/null

trap cleanup_canary EXIT

echo "Starting AWS canary on ports ${canary_socks_port}/${canary_http_port}..."
"${xray_bin}" run -config "${temp_config}" >"${temp_log}" 2>&1 &
temp_pid="$!"
sleep 2

if ! kill -0 "${temp_pid}" >/dev/null 2>&1; then
  echo "Canary Xray exited unexpectedly." >&2
  sed -n '1,120p' "${temp_log}" >&2 || true
  exit 1
fi

if ! run_proxy_health_checks "${canary_http_port}" "${canary_socks_port}" "${expected_aws_ip}"; then
  echo "AWS canary checks failed. Nothing was changed in ${target_config}." >&2
  exit 1
fi
echo "AWS canary checks passed."

if [[ "${allow_live_switch}" != "1" ]]; then
  cat <<EOF
Canary checks passed, but live cutover is blocked by default.

Nothing was changed in:
  ${target_config}

To perform the real cutover with automatic rollback on failure:
  ALLOW_LIVE_SWITCH=1 ${repo_root}/ops/switch-to-aws.sh
EOF
  exit 3
fi

if [[ "${current_target_ip}" != "${required_baseline_ip}" ]]; then
  echo "Live cutover is only allowed from the baseline ${required_baseline_ip}. Current config points to ${current_target_ip}." >&2
  exit 1
fi

if ! resolve_service_target; then
  echo "Launchctl label not found: ${launchctl_label}" >&2
  exit 2
fi

echo "Checking current baseline exit IP via port 8080..."
if [[ "$(proxy_exit_ip 8080)" != "${required_baseline_ip}" ]]; then
  echo "Current live tunnel is not on the expected baseline ${required_baseline_ip}. Aborting cutover." >&2
  exit 1
fi

cp "${target_config}" "${backup_config}"
echo "Backed up current config to ${backup_config}"

cp "${source_config}" "${target_config}"
echo "Switched active config to ${target_config}"

echo "Restarting ${service_target}..."
if restart_launchctl_service "${expected_aws_ip}"; then
  cat <<EOF
AWS cutover completed.

Active server:
  ${expected_aws_ip}

Rollback:
  ${rollback_script}
EOF
  exit 0
fi

echo "Soft restart failed, trying a harder restart path..." >&2
if hard_restart_launchctl_service "${expected_aws_ip}"; then
  cat <<EOF
AWS cutover completed after hard restart.

Active server:
  ${expected_aws_ip}

Rollback:
  ${rollback_script}
EOF
  exit 0
fi

echo "Live health checks failed after cutover." >&2
rollback_to_baseline
