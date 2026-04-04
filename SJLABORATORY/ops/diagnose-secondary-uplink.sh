#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

source_config="${XRAY_SOURCE_CONFIG:-${repo_root}/secrets/PROXY/xray/config.working_baseline.json}"
test_interface="${TEST_INTERFACE:-en0}"
http_port="${HTTP_PORT:-49081}"
socks_port="${SOCKS_PORT:-49080}"
xray_bin="${XRAY_BIN:-$(command -v xray || true)}"
diag_script="${repo_root}/ops/diagnose-proxy.sh"

temp_config=""
temp_log=""
temp_pid=""

require_file() {
  local path="$1"
  if [[ ! -f "${path}" ]]; then
    echo "Missing required file: ${path}" >&2
    exit 1
  fi
}

require_command() {
  local name="$1"
  if ! command -v "${name}" >/dev/null 2>&1; then
    echo "Missing required command: ${name}" >&2
    exit 1
  fi
}

detect_interface_ip() {
  local iface="$1"
  local ip=""

  ip="$(ipconfig getifaddr "${iface}" 2>/dev/null || true)"
  if [[ -n "${ip}" ]]; then
    printf '%s\n' "${ip}"
    return 0
  fi

  ip="$(ifconfig "${iface}" 2>/dev/null | awk '/inet / && $2 != "127.0.0.1" { print $2; exit }')"
  if [[ -n "${ip}" ]]; then
    printf '%s\n' "${ip}"
    return 0
  fi

  return 1
}

read_default_interface() {
  route -n get default 2>/dev/null | awk '/interface:/{print $2; exit}'
}

read_server_address() {
  local path="$1"
  CONFIG_PATH="${path}" python3 - <<'PY'
import json
import os

with open(os.environ["CONFIG_PATH"], "r", encoding="utf-8") as fh:
    data = json.load(fh)

for outbound in data.get("outbounds", []):
    if outbound.get("protocol") not in {"freedom", "blackhole", "dns"}:
        print(outbound["settings"]["vnext"][0]["address"])
        raise SystemExit(0)

raise SystemExit("No proxy outbound found in config")
PY
}

write_bound_canary_config() {
  local source_path="$1"
  local output_path="$2"
  local iface="$3"
  local socks="$4"
  local http="$5"

  SOURCE_PATH="${source_path}" OUTPUT_PATH="${output_path}" TEST_INTERFACE="${iface}" SOCKS_PORT="${socks}" HTTP_PORT="${http}" python3 - <<'PY'
import json
import os

source_path = os.environ["SOURCE_PATH"]
output_path = os.environ["OUTPUT_PATH"]
iface = os.environ["TEST_INTERFACE"]
socks_port = int(os.environ["SOCKS_PORT"])
http_port = int(os.environ["HTTP_PORT"])

with open(source_path, "r", encoding="utf-8") as fh:
    data = json.load(fh)

for inbound in data.get("inbounds", []):
    if inbound.get("protocol") == "socks":
        inbound["port"] = socks_port
    elif inbound.get("protocol") == "http":
        inbound["port"] = http_port

for outbound in data.get("outbounds", []):
    if outbound.get("protocol") in {"freedom", "blackhole", "dns"}:
        continue
    stream_settings = outbound.setdefault("streamSettings", {})
    sockopt = stream_settings.setdefault("sockopt", {})
    sockopt["interface"] = iface
    break
else:
    raise SystemExit("No proxy outbound found in config")

with open(output_path, "w", encoding="utf-8") as fh:
    json.dump(data, fh, indent=2)
    fh.write("\n")
PY
}

cleanup() {
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

run_raw_interface_tests() {
  local iface="$1"
  local source_ip="$2"
  local server_ip="$3"

  echo "raw_tests"
  echo "interface=${iface}"
  echo "source_ip=${source_ip}"
  echo "server_ip=${server_ip}"
  echo "default_interface=$(read_default_interface)"

  echo "Testing TCP reachability to ${server_ip}:443 from ${source_ip}..."
  if nc -zvw5 -s "${source_ip}" "${server_ip}" 443; then
    echo "server_443=reachable"
  else
    echo "server_443=blocked_or_failed"
  fi

  echo "Testing direct interface egress IP..."
  if curl --interface "${iface}" -4 --max-time 10 -fsSL https://ifconfig.me/ip; then
    printf '\n'
  else
    echo "direct_exit_ip=failed"
  fi

  echo "name|http_code|connect_s|tls_s|ttfb_s|total_s|bytes|bytes_per_s"
  curl --interface "${iface}" -4 -o /dev/null -sS -w "direct_google_204|%{http_code}|%{time_connect}|%{time_appconnect}|%{time_starttransfer}|%{time_total}|%{size_download}|%{speed_download}\n" --max-time 12 https://www.google.com/generate_204 || true
  curl --interface "${iface}" -4 -o /dev/null -sS -w "direct_github|%{http_code}|%{time_connect}|%{time_appconnect}|%{time_starttransfer}|%{time_total}|%{size_download}|%{speed_download}\n" --max-time 12 https://github.com/ || true
  curl --interface "${iface}" -4 -o /dev/null -sS -w "direct_apple|%{http_code}|%{time_connect}|%{time_appconnect}|%{time_starttransfer}|%{time_total}|%{size_download}|%{speed_download}\n" --max-time 12 https://www.apple.com/ || true
}

require_file "${source_config}"
require_file "${diag_script}"
require_command python3
require_command curl
require_command xray
require_command nc
require_command lsof
require_command route

if [[ -z "${xray_bin}" ]]; then
  echo "xray binary not found in PATH." >&2
  exit 1
fi

if ! source_ip="$(detect_interface_ip "${test_interface}")"; then
  echo "Interface ${test_interface} has no IPv4 address." >&2
  echo "Connect the Tele2 hotspot or set TEST_INTERFACE explicitly." >&2
  exit 1
fi

server_ip="$(read_server_address "${source_config}")"

temp_base="$(mktemp /tmp/xray-secondary-uplink.XXXXXX)"
temp_config="${temp_base}.json"
temp_log="${temp_base}.log"
mv "${temp_base}" "${temp_config}"

write_bound_canary_config "${source_config}" "${temp_config}" "${test_interface}" "${socks_port}" "${http_port}"
"${xray_bin}" run -test -config "${temp_config}" >/dev/null

trap cleanup EXIT

run_raw_interface_tests "${test_interface}" "${source_ip}" "${server_ip}"

echo
echo "Starting bound canary Xray on ${test_interface} using ports ${socks_port}/${http_port}..."
"${xray_bin}" run -c "${temp_config}" >"${temp_log}" 2>&1 &
temp_pid="$!"

wait_for_listener "${socks_port}" || {
  echo "Canary SOCKS listener failed to start." >&2
  cat "${temp_log}" >&2 || true
  exit 1
}

wait_for_listener "${http_port}" || {
  echo "Canary HTTP listener failed to start." >&2
  cat "${temp_log}" >&2 || true
  exit 1
}

echo
echo "proxy_tests_http"
HTTP_PORT="${http_port}" SOCKS_PORT="${socks_port}" "${diag_script}"

echo
echo "proxy_tests_socks"
MODE=socks HTTP_PORT="${http_port}" SOCKS_PORT="${socks_port}" "${diag_script}"
