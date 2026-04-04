#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

http_port="${HTTP_PORT:-8080}"
socks_port="${SOCKS_PORT:-1080}"
test_interface="${TEST_INTERFACE:-}"

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

direct_probe() {
  local name="$1"
  local url="$2"
  local max_time="${3:-15}"
  local curl_args=()

  if [[ -n "${test_interface}" ]]; then
    curl_args+=(--interface "${test_interface}")
  fi

  curl_direct \
    "${curl_args[@]}" \
    --output /dev/null \
    --max-time "${max_time}" \
    --write-out "${name}|%{http_code}|%{time_connect}|%{time_appconnect}|%{time_starttransfer}|%{time_total}|%{size_download}|%{speed_download}\n" \
    "${url}"
}

run_telegram_segment_probes() {
  local source_ip="${1:-}"

  echo
  echo "telegram_segment_tcp_443"
  echo "name|ip|port|tcp_status|connect_s|detail"

  TELEGRAM_SOURCE_IP="${source_ip}" python3 - <<'PY'
import os
import socket
import time

cases = [
    ("api_telegram_org", "149.154.166.110", 443),
    ("telegram_org", "149.154.167.99", 443),
    ("tg_91_108_4_1", "91.108.4.1", 443),
    ("tg_91_108_8_1", "91.108.8.1", 443),
    ("tg_91_108_16_1", "91.108.16.1", 443),
    ("tg_91_108_56_1", "91.108.56.1", 443),
    ("tg_95_161_64_1", "95.161.64.1", 443),
]

source_ip = os.environ.get("TELEGRAM_SOURCE_IP", "")

for name, ip, port in cases:
    t0 = time.time()
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(5)
    try:
        if source_ip:
            sock.bind((source_ip, 0))
        sock.connect((ip, port))
        print(f"{name}|{ip}|{port}|ok|{time.time()-t0:.3f}|connected")
    except Exception as exc:
        print(f"{name}|{ip}|{port}|fail||{type(exc).__name__}:{exc}")
    finally:
        sock.close()
PY

  echo
  echo "telegram_web_tls_443"
  echo "name|ip|tcp_status|tcp_connect_s|tls_status|tls_handshake_s|detail"

  TELEGRAM_SOURCE_IP="${source_ip}" python3 - <<'PY'
import os
import socket
import ssl
import time

cases = [
    ("telegram.org", "149.154.167.99"),
    ("api.telegram.org", "149.154.166.110"),
]

source_ip = os.environ.get("TELEGRAM_SOURCE_IP", "")

for sni, ip in cases:
    tcp_t0 = time.time()
    raw = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    raw.settimeout(5)
    try:
        if source_ip:
            raw.bind((source_ip, 0))
        raw.connect((ip, 443))
        tcp_dt = time.time() - tcp_t0
    except Exception as exc:
        print(f"{sni}|{ip}|fail|||{type(exc).__name__}:{exc}")
        raw.close()
        continue

    try:
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
        wrapped = ctx.wrap_socket(raw, server_hostname=sni, do_handshake_on_connect=False)
        wrapped.settimeout(8)
        tls_t0 = time.time()
        wrapped.do_handshake()
        tls_dt = time.time() - tls_t0
        print(f"{sni}|{ip}|ok|{tcp_dt:.3f}|ok|{tls_dt:.3f}|{wrapped.version()}")
        wrapped.close()
    except Exception as exc:
        print(f"{sni}|{ip}|ok|{tcp_dt:.3f}|fail||{type(exc).__name__}:{exc}")
        try:
            raw.close()
        except Exception:
            pass
PY

  echo
  echo "telegram_mtproto_ports"
  echo "name|ip|port|tcp_status|connect_s|detail"

  TELEGRAM_SOURCE_IP="${source_ip}" python3 - <<'PY'
import os
import socket
import time

cases = [
    ("api_dc", "149.154.166.110", 80),
    ("api_dc", "149.154.166.110", 443),
    ("api_dc", "149.154.166.110", 5222),
    ("web_dc", "149.154.167.99", 80),
    ("web_dc", "149.154.167.99", 443),
    ("web_dc", "149.154.167.99", 5222),
    ("alt_dc", "91.108.16.1", 80),
    ("alt_dc", "91.108.16.1", 443),
    ("alt_dc", "91.108.16.1", 5222),
]

source_ip = os.environ.get("TELEGRAM_SOURCE_IP", "")

for name, ip, port in cases:
    t0 = time.time()
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(5)
    try:
        if source_ip:
            sock.bind((source_ip, 0))
        sock.connect((ip, port))
        print(f"{name}|{ip}|{port}|ok|{time.time()-t0:.3f}|connected")
    except Exception as exc:
        print(f"{name}|{ip}|{port}|fail||{type(exc).__name__}:{exc}")
    finally:
        sock.close()
PY

  echo
  echo "telegram_mtproto_https_transport"
  echo "name|http_code|remote_ip|connect_s|tls_s|ttfb_s|total_s|detail"

  local transport_targets=(
    "pluto_https|https://pluto.web.telegram.org/api"
    "venus_https|https://venus.web.telegram.org/api"
    "aurora_https|https://aurora.web.telegram.org/api"
    "vesta_https|https://vesta.web.telegram.org/api"
    "flora_https|https://flora.web.telegram.org/api"
  )

  local curl_args=()
  if [[ -n "${test_interface}" ]]; then
    curl_args+=(--interface "${test_interface}")
  fi

  local name url
  for item in "${transport_targets[@]}"; do
    name="${item%%|*}"
    url="${item#*|}"
    curl_direct \
      "${curl_args[@]}" \
      --output /dev/null \
      --max-time 12 \
      --write-out "${name}|%{http_code}|%{remote_ip}|%{time_connect}|%{time_appconnect}|%{time_starttransfer}|%{time_total}|ok\n" \
      "${url}" || echo "${name}|000||| | | |curl_failed"
  done
}

detect_proxy_listener() {
  local port="$1"
  lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
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

run_tcp_tests() {
  local source_ip="$1"
  local nc_args=(-zvw5)

  if [[ -n "${source_ip}" ]]; then
    nc_args+=(-s "${source_ip}")
  fi

  echo
  echo "tcp_provider_ingress"

  while IFS='|' read -r name host port; do
    [[ -n "${name}" ]] || continue
    if nc "${nc_args[@]}" "${host}" "${port}"; then
      echo "${name}=reachable"
    else
      echo "${name}=blocked_or_failed"
    fi
  done <<'EOF'
hostkey_xray|5.39.219.74|443
exoscale_xray|194.182.174.240|443
EOF
}

run_direct_http_tests() {
  local curl_args=()

  if [[ -n "${test_interface}" ]]; then
    curl_args+=(--interface "${test_interface}")
  fi

  echo
  echo "direct_path"
  if [[ -n "${test_interface}" ]]; then
    echo "test_interface=${test_interface}"
  else
    echo "test_interface=default-route"
  fi

  echo -n "direct_exit_ip="
  curl_direct "${curl_args[@]}" --max-time 10 https://ifconfig.me/ip || true
  echo

  echo "name|http_code|connect_s|tls_s|ttfb_s|total_s|bytes|bytes_per_s"
  direct_probe "google_204" "https://www.google.com/generate_204" "12" || true
  direct_probe "github" "https://github.com/" "12" || true
  direct_probe "cloudflare" "https://www.cloudflare.com/" "12" || true
  direct_probe "telegram_org_site" "https://telegram.org/" "12" || true
  direct_probe "t_me_site" "https://t.me/" "12" || true
  direct_probe "web_telegram_site" "https://web.telegram.org/" "12" || true
  direct_probe "hostkey_site" "https://hostkey.com/" "15" || true
  direct_probe "exoscale_site" "https://www.exoscale.com/" "15" || true
  direct_probe "aws_lightsail_site" "https://aws.amazon.com/lightsail/" "15" || true
  direct_probe "tencent_lighthouse_site" "https://www.tencentcloud.com/products/lighthouse" "15" || true
  direct_probe "alibaba_sas_site" "https://www.alibabacloud.com/en/product/swas/pricing?_p_lc=1" "15" || true
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

require_command curl
require_command nc
require_command lsof
require_command python3

source_ip=""
if [[ -n "${test_interface}" ]]; then
  source_ip="$(read_interface_ip "${test_interface}" || true)"
  if [[ -z "${source_ip}" ]]; then
    echo "Interface ${test_interface} has no IPv4 address." >&2
    exit 1
  fi
fi

echo "safety_mode=do_not_touch_xray"
echo "rule=This script must not stop, restart, unload, or reconfigure local.xray."

print_live_tunnel_state
run_tcp_tests "${source_ip}"
run_direct_http_tests
run_telegram_segment_probes "${source_ip}"
