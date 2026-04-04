#!/usr/bin/env bash
set -euo pipefail

http_port="${HTTP_PORT:-8080}"
socks_port="${SOCKS_PORT:-1080}"
test_interface="${TEST_INTERFACE:-}"
compare_reference="${COMPARE_REFERENCE:-1}"
reference_host="${REFERENCE_HOST:-5.39.219.74}"
reference_ssh_key="${REFERENCE_SSH_KEY:-$HOME/.ssh/sergei-macbook-vps}"

TARGETS=(
  "google|www.google.com|https://www.google.com/generate_204"
  "github|github.com|https://github.com/"
  "telegram_web|telegram.org|https://telegram.org/"
  "telegram_api|api.telegram.org|https://api.telegram.org/"
  "telegram_transport|pluto.web.telegram.org|https://pluto.web.telegram.org/api"
)

SSH_OPTS=(-o BatchMode=yes -o ConnectTimeout=8)
if [[ -n "${reference_ssh_key}" && -f "${reference_ssh_key}" ]]; then
  SSH_OPTS+=(-i "${reference_ssh_key}")
fi

require_command() {
  local name="$1"
  if ! command -v "${name}" >/dev/null 2>&1; then
    echo "Missing required command: ${name}" >&2
    exit 1
  fi
}

curl_direct() {
  env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u all_proxy -u ALL_PROXY -u no_proxy -u NO_PROXY \
    curl --silent --show-error --noproxy '*' "$@"
}

detect_proxy_listener() {
  local port="$1"
  lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
}

read_default_interface() {
  route -n get default 2>/dev/null | awk '/interface:/{print $2; exit}'
}

read_interface_ip() {
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

resolve_all_a() {
  local host="$1"
  dig +time=3 +tries=1 +short "${host}" A 2>/dev/null | awk 'NF'
}

resolve_first_a() {
  local host="$1"
  resolve_all_a "${host}" | head -n1
}

join_lines() {
  awk 'BEGIN{first=1} NF{if(!first) printf ","; printf "%s", $0; first=0} END{print ""}'
}

print_live_tunnel_state() {
  echo "live_xray_state"

  if detect_proxy_listener "${http_port}"; then
    echo "http_proxy_listener=up:${http_port}"
  else
    echo "http_proxy_listener=down:${http_port}"
  fi

  if detect_proxy_listener "${socks_port}"; then
    echo "socks_proxy_listener=up:${socks_port}"
  else
    echo "socks_proxy_listener=down:${socks_port}"
  fi

  if detect_proxy_listener "${http_port}"; then
    echo -n "proxy_exit_ip="
    curl --silent --show-error --max-time 10 --proxy "http://127.0.0.1:${http_port}" https://ifconfig.me/ip || true
    echo
  else
    echo "proxy_exit_ip=unknown"
  fi
}

print_lab_intro() {
  echo "dpi_lab"
  echo "safety_mode=do_not_touch_xray"
  echo "rule=This lab must not stop, restart, unload, or reconfigure local.xray."
  echo "what=This lab checks where the first failure appears: DNS resolution, TCP connect, TLS handshake, or HTTP response."
  echo "note=There is no single RFC for DPI itself. The lab measures standard protocol stages and uses the first divergence as a clue."
  echo "resources.dns=https://www.rfc-editor.org/rfc/rfc1035"
  echo "resources.tcp=https://www.rfc-editor.org/rfc/rfc9293"
  echo "resources.tls=https://www.rfc-editor.org/rfc/rfc8446"
  echo "resources.sni=https://www.rfc-editor.org/rfc/rfc6066"
  echo "resources.http=https://www.rfc-editor.org/rfc/rfc9110"
  echo "resources.telegram_datacenters=https://core.telegram.org/api/datacenter"
  echo "how_to_read.dns=If DNS fails, the resolver path is already broken before any connect attempt."
  echo "how_to_read.tcp=If TCP fails after DNS succeeds, the path to the IP or port is blocked, routed badly, or reset."
  echo "how_to_read.tls=If TCP succeeds but TLS fails, interference is likely happening during handshake metadata such as SNI or TLS fingerprint stage."
  echo "how_to_read.http=If TLS succeeds but HTTP fails, the application layer or endpoint policy is the first visible problem."
}

print_route_context() {
  local iface="$1"
  local source_ip="$2"

  echo
  echo "direct_route"
  echo "interface=${iface}"
  echo "local_ipv4=${source_ip}"
}

tcp_probe() {
  local ip="$1"
  local port="$2"
  local source_ip="${3:-}"

  TARGET_IP="${ip}" TARGET_PORT="${port}" SOURCE_IP="${source_ip}" python3 - <<'PY'
import os
import socket
import time

ip = os.environ["TARGET_IP"]
port = int(os.environ["TARGET_PORT"])
source_ip = os.environ.get("SOURCE_IP", "")

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.settimeout(5)

try:
    if source_ip:
        sock.bind((source_ip, 0))
    t0 = time.time()
    sock.connect((ip, port))
    print(f"ok|{time.time()-t0:.3f}|connected")
except Exception as exc:
    print(f"fail||{type(exc).__name__}:{exc}")
finally:
    sock.close()
PY
}

tls_probe() {
  local ip="$1"
  local sni="$2"
  local source_ip="${3:-}"

  TARGET_IP="${ip}" TARGET_SNI="${sni}" SOURCE_IP="${source_ip}" python3 - <<'PY'
import os
import socket
import ssl
import time

ip = os.environ["TARGET_IP"]
sni = os.environ["TARGET_SNI"]
source_ip = os.environ.get("SOURCE_IP", "")

raw = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
raw.settimeout(5)

try:
    if source_ip:
        raw.bind((source_ip, 0))
    t0 = time.time()
    raw.connect((ip, 443))
    tcp_dt = time.time() - t0
except Exception as exc:
    print(f"fail|||{type(exc).__name__}:{exc}")
    raw.close()
    raise SystemExit(0)

try:
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    wrapped = ctx.wrap_socket(raw, server_hostname=sni, do_handshake_on_connect=False)
    wrapped.settimeout(8)
    t1 = time.time()
    wrapped.do_handshake()
    tls_dt = time.time() - t1
    print(f"ok|{tcp_dt:.3f}|ok|{tls_dt:.3f}|{wrapped.version()}")
    wrapped.close()
except Exception as exc:
    print(f"ok|{tcp_dt:.3f}|fail||{type(exc).__name__}:{exc}")
    try:
        raw.close()
    except Exception:
        pass
PY
}

http_probe() {
  local name="$1"
  local url="$2"
  local max_time="${3:-12}"
  local extra=()

  if [[ -n "${test_interface}" ]]; then
    extra+=(--interface "${test_interface}")
  fi

  curl_direct \
    "${extra[@]}" \
    --output /dev/null \
    --location \
    --max-time "${max_time}" \
    --write-out "${name}|%{http_code}|%{remote_ip}|%{time_connect}|%{time_appconnect}|%{time_starttransfer}|%{time_total}|%{exitcode}:%{errormsg}\n" \
    "${url}" || true
}

run_direct_dns_layer() {
  echo
  echo "direct_dns_layer"
  echo "name|host|first_ip|all_ips|status"

  local name host url first_ip all_ips
  for item in "${TARGETS[@]}"; do
    IFS='|' read -r name host url <<<"${item}"
    all_ips="$(resolve_all_a "${host}" | join_lines)"
    first_ip="$(printf '%s\n' "${all_ips}" | awk -F',' 'NF{print $1}')"
    if [[ -n "${first_ip}" ]]; then
      echo "${name}|${host}|${first_ip}|${all_ips}|ok"
    else
      echo "${name}|${host}|||resolve_failed"
    fi
  done
}

run_direct_tcp_layer() {
  echo
  echo "direct_tcp_443_layer"
  echo "name|host|ip|tcp_status|connect_s|detail"

  local name host url ip result
  for item in "${TARGETS[@]}"; do
    IFS='|' read -r name host url <<<"${item}"
    ip="$(resolve_first_a "${host}")"
    if [[ -z "${ip}" ]]; then
      echo "${name}|${host}||fail||no_dns"
      continue
    fi
    result="$(tcp_probe "${ip}" 443 "${source_ip:-}")"
    echo "${name}|${host}|${ip}|${result}"
  done
}

run_direct_tls_layer() {
  echo
  echo "direct_tls_443_layer"
  echo "name|host|ip|tcp_status|tcp_connect_s|tls_status|tls_handshake_s|detail"

  local name host url ip result
  for item in "${TARGETS[@]}"; do
    IFS='|' read -r name host url <<<"${item}"
    ip="$(resolve_first_a "${host}")"
    if [[ -z "${ip}" ]]; then
      echo "${name}|${host}||fail|||no_dns"
      continue
    fi
    result="$(tls_probe "${ip}" "${host}" "${source_ip:-}")"
    echo "${name}|${host}|${ip}|${result}"
  done
}

run_direct_http_layer() {
  echo
  echo "direct_http_https_layer"
  echo "name|http_code|remote_ip|connect_s|tls_s|ttfb_s|total_s|detail"

  local name host url
  for item in "${TARGETS[@]}"; do
    IFS='|' read -r name host url <<<"${item}"
    http_probe "${name}" "${url}" "12"
  done
}

run_reference_layers() {
  if [[ "${compare_reference}" != "1" ]]; then
    echo
    echo "reference_compare=disabled"
    return 0
  fi

  echo
  echo "reference_compare"
  echo "reference_host=${reference_host}"

  if ! ssh "${SSH_OPTS[@]}" "${reference_host}" 'echo ssh_ok' >/dev/null 2>&1; then
    echo "status=ssh_failed"
    return 0
  fi

  ssh "${SSH_OPTS[@]}" "${reference_host}" 'bash -se' <<'EOF'
set -euo pipefail

require_command() {
  local name="$1"
  command -v "${name}" >/dev/null 2>&1 || {
    echo "Missing required command on reference host: ${name}" >&2
    exit 1
  }
}

resolve_all_a() {
  local host="$1"
  dig +time=3 +tries=1 +short "${host}" A 2>/dev/null | awk 'NF'
}

resolve_first_a() {
  local host="$1"
  resolve_all_a "${host}" | head -n1
}

join_lines() {
  awk 'BEGIN{first=1} NF{if(!first) printf ","; printf "%s", $0; first=0} END{print ""}'
}

tcp_probe() {
  local ip="$1"
  local port="$2"

  TARGET_IP="${ip}" TARGET_PORT="${port}" python3 - <<'PY'
import os
import socket
import time

ip = os.environ["TARGET_IP"]
port = int(os.environ["TARGET_PORT"])

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.settimeout(5)

try:
    t0 = time.time()
    sock.connect((ip, port))
    print(f"ok|{time.time()-t0:.3f}|connected")
except Exception as exc:
    print(f"fail||{type(exc).__name__}:{exc}")
finally:
    sock.close()
PY
}

tls_probe() {
  local ip="$1"
  local sni="$2"

  TARGET_IP="${ip}" TARGET_SNI="${sni}" python3 - <<'PY'
import os
import socket
import ssl
import time

ip = os.environ["TARGET_IP"]
sni = os.environ["TARGET_SNI"]

raw = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
raw.settimeout(5)

try:
    t0 = time.time()
    raw.connect((ip, 443))
    tcp_dt = time.time() - t0
except Exception as exc:
    print(f"fail|||{type(exc).__name__}:{exc}")
    raw.close()
    raise SystemExit(0)

try:
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    wrapped = ctx.wrap_socket(raw, server_hostname=sni, do_handshake_on_connect=False)
    wrapped.settimeout(8)
    t1 = time.time()
    wrapped.do_handshake()
    tls_dt = time.time() - t1
    print(f"ok|{tcp_dt:.3f}|ok|{tls_dt:.3f}|{wrapped.version()}")
    wrapped.close()
except Exception as exc:
    print(f"ok|{tcp_dt:.3f}|fail||{type(exc).__name__}:{exc}")
    try:
      raw.close()
    except Exception:
      pass
PY
}

http_probe() {
  local name="$1"
  local url="$2"
  curl \
    --silent \
    --show-error \
    --output /dev/null \
    --location \
    --max-time 12 \
    --write-out "${name}|%{http_code}|%{remote_ip}|%{time_connect}|%{time_appconnect}|%{time_starttransfer}|%{time_total}|%{exitcode}:%{errormsg}\n" \
    "${url}" || true
}

require_command curl
require_command dig
require_command python3

TARGETS=(
  "google|www.google.com|https://www.google.com/generate_204"
  "github|github.com|https://github.com/"
  "telegram_web|telegram.org|https://telegram.org/"
  "telegram_api|api.telegram.org|https://api.telegram.org/"
  "telegram_transport|pluto.web.telegram.org|https://pluto.web.telegram.org/api"
)

echo "status=ok"

echo
echo "reference_dns_layer"
echo "name|host|first_ip|all_ips|status"
for item in "${TARGETS[@]}"; do
  IFS='|' read -r name host url <<<"${item}"
  all_ips="$(resolve_all_a "${host}" | join_lines)"
  first_ip="$(printf '%s\n' "${all_ips}" | awk -F',' 'NF{print $1}')"
  if [[ -n "${first_ip}" ]]; then
    echo "${name}|${host}|${first_ip}|${all_ips}|ok"
  else
    echo "${name}|${host}|||resolve_failed"
  fi
done

echo
echo "reference_tcp_443_layer"
echo "name|host|ip|tcp_status|connect_s|detail"
for item in "${TARGETS[@]}"; do
  IFS='|' read -r name host url <<<"${item}"
  ip="$(resolve_first_a "${host}")"
  if [[ -z "${ip}" ]]; then
    echo "${name}|${host}||fail||no_dns"
    continue
  fi
  result="$(tcp_probe "${ip}" 443)"
  echo "${name}|${host}|${ip}|${result}"
done

echo
echo "reference_tls_443_layer"
echo "name|host|ip|tcp_status|tcp_connect_s|tls_status|tls_handshake_s|detail"
for item in "${TARGETS[@]}"; do
  IFS='|' read -r name host url <<<"${item}"
  ip="$(resolve_first_a "${host}")"
  if [[ -z "${ip}" ]]; then
    echo "${name}|${host}||fail|||no_dns"
    continue
  fi
  result="$(tls_probe "${ip}" "${host}")"
  echo "${name}|${host}|${ip}|${result}"
done

echo
echo "reference_http_https_layer"
echo "name|http_code|remote_ip|connect_s|tls_s|ttfb_s|total_s|detail"
for item in "${TARGETS[@]}"; do
  IFS='|' read -r name host url <<<"${item}"
  http_probe "${name}" "${url}"
done
EOF
}

require_command curl
require_command dig
require_command lsof
require_command route
require_command ipconfig
require_command ifconfig
require_command python3
require_command ssh

if [[ -z "${test_interface}" ]]; then
  test_interface="$(read_default_interface)"
fi

if [[ -z "${test_interface}" ]]; then
  echo "Could not determine the active interface." >&2
  exit 1
fi

source_ip="$(read_interface_ip "${test_interface}" || true)"
if [[ -z "${source_ip}" ]]; then
  echo "Interface ${test_interface} has no IPv4 address." >&2
  exit 1
fi

print_lab_intro
echo
print_live_tunnel_state
print_route_context "${test_interface}" "${source_ip}"
run_direct_dns_layer
run_direct_tcp_layer
run_direct_tls_layer
run_direct_http_layer
run_reference_layers
