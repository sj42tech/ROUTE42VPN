#!/bin/bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SECRETS_FILE_DEFAULT="$ROOT_DIR/secrets/MIKROTIK/admin-192.168.88.1.txt"
SECRETS_FILE="${MIKROTIK_SECRET_FILE:-$SECRETS_FILE_DEFAULT}"
DOMAIN_LIST_PATH="${DOMAIN_LIST_PATH:-$ROOT_DIR/ops/tele2-domain-matrix.domains.tsv}"

secret_user=""
secret_password=""

if [[ -f "$SECRETS_FILE" ]]; then
  secret_user="$(sed -n 's/^USER=//p' "$SECRETS_FILE" | head -n 1)"
  secret_password="$(sed -n 's/^PASSWORD=//p' "$SECRETS_FILE" | head -n 1)"
fi

MIKROTIK_HOST="${MIKROTIK_HOST:-192.168.0.106}"
MIKROTIK_USER="${MIKROTIK_USER:-$secret_user}"
MIKROTIK_PASSWORD="${MIKROTIK_PASSWORD:-$secret_password}"
SOURCE_CLIENT_IP="${SOURCE_CLIENT_IP:-$(ipconfig getifaddr en0 2>/dev/null || true)}"
CONNECT_TIMEOUT="${CONNECT_TIMEOUT:-5}"
MAX_TIME="${MAX_TIME:-10}"
RUN_STAMP="$(date +%F-%H%M%S)"
REPORT_PATH="${REPORT_PATH:-$ROOT_DIR/docs/tele2-domain-matrix-$RUN_STAMP.md}"
LATEST_REPORT_PATH="${LATEST_REPORT_PATH:-$ROOT_DIR/docs/tele2-domain-matrix-latest.md}"
PREFIX="lab-tele2-matrix-$$"

if [[ -z "$MIKROTIK_USER" || -z "$MIKROTIK_PASSWORD" ]]; then
  echo "Missing MikroTik credentials. Set MIKROTIK_USER and MIKROTIK_PASSWORD or provide $SECRETS_FILE." >&2
  exit 1
fi

if [[ -z "$SOURCE_CLIENT_IP" ]]; then
  echo "Could not detect SOURCE_CLIENT_IP. Set SOURCE_CLIENT_IP explicitly." >&2
  exit 1
fi

if [[ ! -f "$DOMAIN_LIST_PATH" ]]; then
  echo "Domain list not found: $DOMAIN_LIST_PATH" >&2
  exit 1
fi

if ! command -v expect >/dev/null 2>&1; then
  echo "expect is required" >&2
  exit 1
fi

TMP_DIR="$(mktemp -d)"

ROS_LAST_OUTPUT=""

ros_run_file() {
  local cmd_file="$1"
  local timeout="${2:-60}"
  local output_file="$TMP_DIR/ros-output-$$.txt"

  EXPECT_HOST="$MIKROTIK_HOST" \
  EXPECT_USER="$MIKROTIK_USER" \
  EXPECT_PASS="$MIKROTIK_PASSWORD" \
  EXPECT_CMD_FILE="$cmd_file" \
  EXPECT_TIMEOUT="$timeout" \
  /usr/bin/expect <<'EOF' >"$output_file"
set timeout $env(EXPECT_TIMEOUT)
set host $env(EXPECT_HOST)
set user $env(EXPECT_USER)
set pass $env(EXPECT_PASS)
set cmdfile $env(EXPECT_CMD_FILE)
set fd [open $cmdfile r]
set cmd [read $fd]
close $fd
spawn ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${user}@${host} $cmd
expect {
  -re {[Pp]assword:} { send -- "$pass\r" }
}
expect eof
EOF

  ROS_LAST_OUTPUT="$(cat "$output_file")"
}

ros_run() {
  local cmd="$1"
  local timeout="${2:-60}"
  local cmd_file="$TMP_DIR/ros-command-$$.rsc"
  printf '%s\n' "$cmd" >"$cmd_file"
  ros_run_file "$cmd_file" "$timeout"
}

restore_router_state() {
  ros_run "
/ip firewall nat remove [find comment~\"$PREFIX\"];
/ip dhcp-client set [find comment=defconf] default-route-distance=1;
/ip dhcp-client set [find comment=tele2-hotspot] default-route-distance=10;
/ip route print detail where dst-address=0.0.0.0/0
" 40 || true
}

trap 'restore_router_state >/dev/null 2>&1 || true; rm -rf "$TMP_DIR"' EXIT

LIVE_PROXY_EXIT_IP="$(curl -sS --max-time 10 --proxy http://127.0.0.1:8080 https://ifconfig.me/ip 2>/dev/null || true)"

ros_run "
/ip dhcp-client set [find comment=defconf] default-route-distance=20;
/ip dhcp-client set [find comment=tele2-hotspot] default-route-distance=1;
/ip route print detail where dst-address=0.0.0.0/0
" 40

ROUTE_PROMOTION_OUTPUT="$ROS_LAST_OUTPUT"

REPORT_TMP="$TMP_DIR/report.md"
{
  echo "# Tele2 Domain Matrix $(date +%F)"
  echo
  echo "This report was generated through the isolated MikroTik v7 hotspot lab without touching the live desktop Xray tunnel."
  echo
  echo "## Safety"
  echo
  echo "- MacBook stayed on the working \`v6\` LAN"
  echo "- live desktop tunnel was not stopped"
  echo "- Tele2 was temporarily promoted to the primary WAN only on \`v7\` during the test run"
  echo "- \`v7\` was restored to \`ether1 distance=1\` and \`wifi2 distance=10\` after the run"
  echo
  echo "## Context"
  echo
  echo "- MikroTik test host: \`$MIKROTIK_HOST\`"
  echo "- Source client IP on working LAN: \`$SOURCE_CLIENT_IP\`"
  echo "- Live desktop proxy exit IP before run: \`${LIVE_PROXY_EXIT_IP:-unknown}\`"
  echo "- Connect timeout: \`${CONNECT_TIMEOUT}s\`"
  echo "- Curl max time: \`${MAX_TIME}s\`"
  echo "- Domain list: \`$(realpath "$DOMAIN_LIST_PATH")\`"
  echo
  echo "## Tele2 Route Promotion"
  echo
  echo '```text'
  printf '%s\n' "$ROUTE_PROMOTION_OUTPUT"
  echo '```'
  echo
  echo "## Domain Matrix"
  echo
  echo "| Category | Domain | IPv4 | TCP 443 | HTTP Code | Connect s | TLS s | TTFB s | Total s | Outcome |"
  echo "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |"
} >"$REPORT_TMP"

TOTAL=0
TCP_UP=0
HTTP_UP=0
PARTIAL=0
DOWN=0
WORKING_DOMAINS=""
PARTIAL_DOMAINS=""
DOWN_DOMAINS=""

PORT=56000

while IFS='|' read -r category domain note; do
  [[ -z "$category" ]] && continue
  [[ "$category" == \#* ]] && continue
  [[ -z "$domain" ]] && continue

  ip="$(dig +short "$domain" | awk '/^[0-9.]+$/{print; exit}')"
  if [[ -z "$ip" ]]; then
    printf '| %s | %s | %s | %s | %s | %s | %s | %s | %s | %s |\n' \
      "$category" "$domain" "NO_A" "skip" "-" "-" "-" "-" "-" "no_ipv4" >>"$REPORT_TMP"
    continue
  fi

  TOTAL=$((TOTAL + 1))
  comment="${PREFIX}-${PORT}"

  ros_run "
/ip firewall nat add chain=dstnat in-interface=ether1 protocol=tcp src-address=$SOURCE_CLIENT_IP dst-port=$PORT action=dst-nat to-addresses=$ip to-ports=443 comment=\"$comment\"
" 20

  sleep 1

  tcp_status="down"
  if nc -G "$CONNECT_TIMEOUT" -zvw"$CONNECT_TIMEOUT" "$MIKROTIK_HOST" "$PORT" >/dev/null 2>&1; then
    tcp_status="up"
    TCP_UP=$((TCP_UP + 1))
  fi

  curl_output="$(curl -k -sS --noproxy '*' \
    --connect-timeout "$CONNECT_TIMEOUT" \
    --max-time "$MAX_TIME" \
    --resolve "$domain:$PORT:$MIKROTIK_HOST" \
    -o /dev/null \
    -w 'code=%{http_code} connect=%{time_connect} tls=%{time_appconnect} ttfb=%{time_starttransfer} total=%{time_total}' \
    "https://$domain:$PORT/" 2>&1 || true)"

  ros_run "
/ip firewall nat remove [find comment=\"$comment\"]
" 20

  http_code="$(printf '%s\n' "$curl_output" | sed -n 's/.*code=\([0-9][0-9][0-9]\).*/\1/p' | tail -n 1)"
  connect_s="$(printf '%s\n' "$curl_output" | sed -n 's/.*connect=\([0-9.][0-9.]*\).*/\1/p' | tail -n 1)"
  tls_s="$(printf '%s\n' "$curl_output" | sed -n 's/.*tls=\([0-9.][0-9.]*\).*/\1/p' | tail -n 1)"
  ttfb_s="$(printf '%s\n' "$curl_output" | sed -n 's/.*ttfb=\([0-9.][0-9.]*\).*/\1/p' | tail -n 1)"
  total_s="$(printf '%s\n' "$curl_output" | sed -n 's/.*total=\([0-9.][0-9.]*\).*/\1/p' | tail -n 1)"

  http_code="${http_code:-000}"
  connect_s="${connect_s:-0}"
  tls_s="${tls_s:-0}"
  ttfb_s="${ttfb_s:-0}"
  total_s="${total_s:-0}"

  outcome="down"
  if [[ "$http_code" != "000" ]]; then
    outcome="http_up"
    HTTP_UP=$((HTTP_UP + 1))
    WORKING_DOMAINS="${WORKING_DOMAINS}- \`$domain\` -> \`$http_code\`\n"
  elif printf '%s\n' "$curl_output" | grep -qi 'Connection reset by peer\|SSL_ERROR_SYSCALL\|Recv failure'; then
    outcome="tls_reset"
    PARTIAL=$((PARTIAL + 1))
    PARTIAL_DOMAINS="${PARTIAL_DOMAINS}- \`$domain\` -> TLS reset / interrupted after connect\n"
  elif [[ "$tcp_status" == "up" ]]; then
    outcome="tcp_only"
    PARTIAL=$((PARTIAL + 1))
    PARTIAL_DOMAINS="${PARTIAL_DOMAINS}- \`$domain\` -> TCP up, HTTPS failed\n"
  else
    DOWN=$((DOWN + 1))
    DOWN_DOMAINS="${DOWN_DOMAINS}- \`$domain\`\n"
  fi

  printf '| %s | %s | %s | %s | %s | %s | %s | %s | %s | %s |\n' \
    "$category" "$domain" "$ip" "$tcp_status" "$http_code" "$connect_s" "$tls_s" "$ttfb_s" "$total_s" "$outcome" >>"$REPORT_TMP"

  PORT=$((PORT + 1))
done <"$DOMAIN_LIST_PATH"

restore_router_state

{
  echo
  echo "## Summary"
  echo
  echo "- tested IPv4-backed domains: \`$TOTAL\`"
  echo "- TCP 443 reachable: \`$TCP_UP\`"
  echo "- HTTP/TLS succeeded with non-zero HTTP code: \`$HTTP_UP\`"
  echo "- partial cases (TCP only or TLS reset): \`$PARTIAL\`"
  echo "- complete failures: \`$DOWN\`"
  echo
  echo "## Working Domains"
  echo
  if [[ -n "$WORKING_DOMAINS" ]]; then
    printf '%b' "$WORKING_DOMAINS"
  else
    echo "- none"
  fi
  echo
  echo "## Partial Domains"
  echo
  if [[ -n "$PARTIAL_DOMAINS" ]]; then
    printf '%b' "$PARTIAL_DOMAINS"
  else
    echo "- none"
  fi
  echo
  echo "## Down Domains"
  echo
  if [[ -n "$DOWN_DOMAINS" ]]; then
    printf '%b' "$DOWN_DOMAINS"
  else
    echo "- none"
  fi
} >>"$REPORT_TMP"

mv "$REPORT_TMP" "$REPORT_PATH"
cp "$REPORT_PATH" "$LATEST_REPORT_PATH"
echo "Report written to $REPORT_PATH"
echo "Latest report updated at $LATEST_REPORT_PATH"
