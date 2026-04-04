#!/usr/bin/env bash
set -euo pipefail

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

detect_proxy_listener() {
  local port="$1"
  lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
}

read_default_interface() {
  route -n get default 2>/dev/null | awk '/interface:/{print $2; exit}'
}

read_default_gateway() {
  route -n get default 2>/dev/null | awk '/gateway:/{print $2; exit}'
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

probe_http_meta() {
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
    --write-out "${name}|%{http_code}|%{remote_ip}|%{time_connect}|%{time_appconnect}|%{time_starttransfer}|%{time_total}|%{num_redirects}|%{size_download}|%{speed_download}\n" \
    "${url}"
}

probe_http_body_sample() {
  local name="$1"
  local url="$2"
  local max_time="${3:-12}"
  local extra=()

  if [[ -n "${test_interface}" ]]; then
    extra+=(--interface "${test_interface}")
  fi

  local sample
  sample="$(
    curl_direct \
      "${extra[@]}" \
      --location \
      --max-time "${max_time}" \
      "${url}" 2>/dev/null | tr '\r\n' ' ' | sed 's/[[:space:]]\+/ /g' | cut -c1-100
  )"

  echo "${name}|${sample}"
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

print_route_summary() {
  local iface="$1"
  local gateway="$2"
  local ip="$3"

  echo
  echo "direct_route"
  echo "interface=${iface}"
  echo "local_ipv4=${ip}"
  echo "gateway=${gateway}"
}

print_dns_summary() {
  echo
  echo "dns_resolvers"
  scutil --dns | awk '/nameserver\[[0-9]+\]/{print $3}' | awk '!seen[$0]++'
}

run_gateway_probes() {
  local gateway="$1"
  local source_ip="$2"

  echo
  echo "gateway_surface"
  echo "target=${gateway}"

  local nc_args=(-zvw3)
  if [[ -n "${source_ip}" ]]; then
    nc_args+=(-s "${source_ip}")
  fi

  for port in 53 80 443; do
    if nc "${nc_args[@]}" "${gateway}" "${port}" >/dev/null 2>&1; then
      echo "tcp_${port}=reachable"
    else
      echo "tcp_${port}=closed_or_filtered"
    fi
  done

  probe_http_meta "gateway_http" "http://${gateway}/" "6" || true
  probe_http_meta "gateway_https" "https://${gateway}/" "6" || true
}

run_captive_checks() {
  echo
  echo "captive_or_walled_garden_checks"
  echo "name|http_code|remote_ip|connect_s|tls_s|ttfb_s|total_s|redirects|bytes|bytes_per_s"
  probe_http_meta "gstatic_204" "http://connectivitycheck.gstatic.com/generate_204" "10" || true
  probe_http_meta "apple_hotspot_http" "http://captive.apple.com/hotspot-detect.html" "10" || true
  probe_http_meta "firefox_canonical" "http://detectportal.firefox.com/canonical.html" "10" || true
  probe_http_meta "neverssl_http" "http://neverssl.com/" "10" || true
  probe_http_meta "cloudflare_https_204" "https://cp.cloudflare.com/generate_204" "12" || true

  echo
  echo "captive_body_samples"
  probe_http_body_sample "apple_hotspot_http_body" "http://captive.apple.com/hotspot-detect.html" "10" || true
  probe_http_body_sample "firefox_canonical_body" "http://detectportal.firefox.com/canonical.html" "10" || true
  probe_http_body_sample "neverssl_http_body" "http://neverssl.com/" "10" || true
}

run_dns_tests() {
  echo
  echo "dns_probe"

  if command -v dig >/dev/null 2>&1; then
    while read -r resolver; do
      [[ -n "${resolver}" ]] || continue
      local_out="$(dig +time=3 +tries=1 @"${resolver}" example.com A +short 2>/dev/null | tr '\n' ',' | sed 's/,$//')"
      if [[ -n "${local_out}" ]]; then
        echo "resolver=${resolver}|example.com=${local_out}"
      else
        echo "resolver=${resolver}|example.com=failed"
      fi
    done < <(scutil --dns | awk '/nameserver\[[0-9]+\]/{print $3}' | awk '!seen[$0]++')
  else
    echo "dig=missing"
  fi
}

run_first_hops() {
  echo
  echo "first_hops"

  if ! command -v traceroute >/dev/null 2>&1; then
    echo "traceroute=missing"
    return 0
  fi

  local targets=(
    "1.1.1.1"
    "8.8.8.8"
    "5.39.219.74"
    "194.182.174.240"
  )

  local trace_args=(-n -m 4 -q 1 -w 2)
  if [[ -n "${test_interface}" ]]; then
    trace_args+=(-i "${test_interface}")
  fi

  for target in "${targets[@]}"; do
    echo "target=${target}"
    traceroute "${trace_args[@]}" "${target}" 2>/dev/null | sed 's/^[[:space:]]*//'
    echo
  done
}

run_direct_internet_probes() {
  echo
  echo "direct_internet"
  echo -n "direct_exit_ip="
  probe_http_body_sample "direct_ip" "https://ifconfig.me/ip" "10" | sed 's/^direct_ip|//'

  echo "name|http_code|remote_ip|connect_s|tls_s|ttfb_s|total_s|redirects|bytes|bytes_per_s"
  probe_http_meta "google_204_https" "https://www.google.com/generate_204" "12" || true
  probe_http_meta "github_https" "https://github.com/" "12" || true
  probe_http_meta "cloudflare_https" "https://www.cloudflare.com/" "12" || true
  probe_http_meta "telegram_org_https" "https://telegram.org/" "12" || true
  probe_http_meta "t_me_https" "https://t.me/" "12" || true
  probe_http_meta "web_telegram_https" "https://web.telegram.org/" "12" || true
}

require_command curl
require_command lsof
require_command route
require_command scutil
require_command sed
require_command awk
require_command python3

if [[ -z "${test_interface}" ]]; then
  test_interface="$(read_default_interface)"
fi

if [[ -z "${test_interface}" ]]; then
  echo "Could not determine the active interface." >&2
  exit 1
fi

source_ip="$(read_interface_ip "${test_interface}" || true)"
gateway="$(read_default_gateway)"

echo "safety_mode=do_not_touch_xray"
echo "rule=This script must not stop, restart, unload, or reconfigure local.xray."

print_live_tunnel_state
print_route_summary "${test_interface}" "${gateway}" "${source_ip:-unknown}"
print_dns_summary
run_gateway_probes "${gateway}" "${source_ip:-}"
run_captive_checks
run_dns_tests
run_first_hops
run_direct_internet_probes
run_telegram_segment_probes "${source_ip:-}"
