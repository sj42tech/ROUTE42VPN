#!/usr/bin/env bash
set -euo pipefail

http_port="${HTTP_PORT:-8080}"
socks_port="${SOCKS_PORT:-1080}"
mode="${MODE:-http}"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

probe() {
  local name="$1"
  local url="$2"
  local max_time="${3:-20}"
  local proxy_flag=()

  if [[ "${mode}" == "http" ]]; then
    proxy_flag=(--proxy "http://127.0.0.1:${http_port}")
  else
    proxy_flag=(--socks5 "127.0.0.1:${socks_port}")
  fi

  curl --silent --show-error --output /dev/null \
    --max-time "${max_time}" \
    "${proxy_flag[@]}" \
    --write-out "${name}|%{http_code}|%{time_connect}|%{time_appconnect}|%{time_starttransfer}|%{time_total}|%{size_download}|%{speed_download}\n" \
    "$url"
}

require_cmd curl

echo "mode=${mode}"
echo "http_port=${http_port}"
echo "socks_port=${socks_port}"

if [[ "${mode}" == "http" ]]; then
  echo -n "exit_ip="
  curl --silent --show-error --max-time 10 --proxy "http://127.0.0.1:${http_port}" https://ifconfig.me/ip
  echo
else
  echo -n "exit_ip="
  curl --silent --show-error --max-time 10 --socks5 "127.0.0.1:${socks_port}" https://ifconfig.me/ip
  echo
fi

echo "name|http_code|connect_s|tls_s|ttfb_s|total_s|bytes|bytes_per_s"
probe "google_204" "https://www.google.com/generate_204"
probe "apple" "https://www.apple.com/"
probe "github" "https://github.com/"
probe "cloudflare" "https://www.cloudflare.com/"
probe "wikipedia" "https://www.wikipedia.org/"
probe "youtube" "https://www.youtube.com/"
probe "speed_cf_5mb" "https://speed.cloudflare.com/__down?bytes=5000000" "40"
