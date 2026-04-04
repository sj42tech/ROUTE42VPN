#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SECRETS_FILE_DEFAULT="$ROOT_DIR/secrets/MIKROTIK/admin-192.168.0.1.txt"
SECRETS_FILE="${MIKROTIK_SECRET_FILE:-$SECRETS_FILE_DEFAULT}"

if [[ ! -f "$SECRETS_FILE" ]]; then
  echo "Missing MikroTik secrets file: $SECRETS_FILE" >&2
  exit 1
fi

HOST="${MIKROTIK_HOST:-$(sed -n 's/^HOST=//p' "$SECRETS_FILE" | head -n1)}"
USER_NAME="${MIKROTIK_USER:-$(sed -n 's/^USER=//p' "$SECRETS_FILE" | head -n1)}"
PASSWORD="${MIKROTIK_PASSWORD:-$(sed -n 's/^PASSWORD=//p' "$SECRETS_FILE" | head -n1)}"

INTERVAL="${INTERVAL:-1}"
PING_TARGET="${PING_TARGET:-10.12.109.5}"
PING_COUNT="${PING_COUNT:-1}"
WAN_INTERFACE="${WAN_INTERFACE:-ether1}"
MODE="${MODE:-dashboard}"
CONTROL_SOCKET_DIR="$(mktemp -d)"
CONTROL_SOCKET_PATH="$CONTROL_SOCKET_DIR/mikrotik-v6.sock"

if [[ -z "$HOST" || -z "$USER_NAME" || -z "$PASSWORD" ]]; then
  echo "Missing HOST/USER/PASSWORD in $SECRETS_FILE" >&2
  exit 1
fi

if ! command -v expect >/dev/null 2>&1; then
  echo "expect is required" >&2
  exit 1
fi

cleanup() {
  ssh -S "$CONTROL_SOCKET_PATH" -O exit -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null "${USER_NAME}@${HOST}" >/dev/null 2>&1 || true
  rm -rf "$CONTROL_SOCKET_DIR"
}

trap cleanup EXIT

ensure_master_connection() {
  if ssh -S "$CONTROL_SOCKET_PATH" -O check -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null "${USER_NAME}@${HOST}" >/dev/null 2>&1; then
    return 0
  fi

  EXPECT_HOST="$HOST" \
  EXPECT_USER="$USER_NAME" \
  EXPECT_PASS="$PASSWORD" \
  EXPECT_SOCKET="$CONTROL_SOCKET_PATH" \
  /usr/bin/expect <<'EOF'
set timeout 30
set host $env(EXPECT_HOST)
set user $env(EXPECT_USER)
set pass $env(EXPECT_PASS)
set sock $env(EXPECT_SOCKET)
spawn ssh -M -S $sock -o ControlMaster=yes -o ControlPersist=60 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -fnNT ${user}@${host}
expect {
  -re {[Pp]assword:} { send -- "$pass\r" }
}
expect eof
EOF
}

ros_run() {
  local cmd="$1"
  ensure_master_connection
  ssh -S "$CONTROL_SOCKET_PATH" \
    -o ControlMaster=auto \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null \
    "${USER_NAME}@${HOST}" \
    "$cmd"
}

parse_value() {
  local label="$1"
  local text="$2"
  printf '%s\n' "$text" | awk -F': ' -v key="$label" '
    $0 ~ key ":" {
      value=$2
      sub(/\r$/, "", value)
      gsub(/^[ \t]+|[ \t]+$/, "", value)
      print value
      exit
    }
  '
}

cleanup_output() {
  sed -e $'s/\r//g' \
      -e '/^spawn ssh /d' \
      -e '/^Warning: Permanently added/d' \
      -e '/^\\*\\* WARNING:/d' \
      -e '/^\\*\\* This session/d' \
      -e '/^\\*\\* The server/d' \
      -e '/^admin@.*password:/d'
}

parse_ping_from_seq() {
  local text="$1"
  printf '%s\n' "$text" | awk -v target="$PING_TARGET" '
    $1 ~ /^[0-9]+$/ && $2 == target {
      sent++
      last=$NF
      gsub(/\r/, "", last)
      if (last ~ /ms$/) {
        sub(/ms$/, "", last)
        value=last + 0
        recv++
        sum += value
        if (best == "" || value < best) best = value
        if (maxv == "" || value > maxv) maxv = value
      }
    }
    END {
      if (sent == 0) {
        print "unknown|unknown|unknown"
        exit
      }
      loss = ((sent - recv) * 100) / sent
      if (recv == 0) {
        print loss "%|timeout|timeout"
        exit
      }
      avg = sum / recv
      printf "%.0f%%|%.1fms|%.1fms\n", loss, avg, maxv
    }
  '
}

render_dashboard() {
  local status="$1"
  local rate="$2"
  local duplex="$3"
  local partner="$4"
  local ping_loss="$5"
  local ping_avg="$6"
  local ping_max="$7"
  local delta_rx_align="$8"
  local delta_tx_collision="$9"
  local delta_tx_late_collision="${10}"
  local rx_align="${11}"
  local tx_collision="${12}"
  local tx_late_collision="${13}"
  local cycle_s="${14}"

  if [[ -t 1 ]]; then
    printf '\033[H\033[2J'
  fi

  printf 'MikroTik v6 WAN Watch\n'
  printf '\n'
  printf 'Time               : %s\n' "$(date '+%F %T')"
  printf 'Router             : %s\n' "$HOST"
  printf 'Interface          : %s\n' "$WAN_INTERFACE"
  printf 'Ping Target        : %s\n' "$PING_TARGET"
  printf 'Refresh Interval   : %ss\n' "$INTERVAL"
  printf 'Cycle Time         : %ss\n' "$cycle_s"
  printf '\n'
  printf 'Status             : %s\n' "${status:-unknown}"
  printf 'Rate               : %s\n' "${rate:-unknown}"
  printf 'Full Duplex        : %s\n' "${duplex:-unknown}"
  printf 'Link Partner       : %s\n' "${partner:-unknown}"
  printf '\n'
  printf 'Ping Loss          : %s\n' "${ping_loss:-unknown}"
  printf 'Ping Avg           : %s\n' "${ping_avg:-unknown}"
  printf 'Ping Max           : %s\n' "${ping_max:-unknown}"
  printf '\n'
  printf 'd_rx_align_error   : %s\n' "$delta_rx_align"
  printf 'd_tx_collision     : %s\n' "$delta_tx_collision"
  printf 'd_tx_late_collision: %s\n' "$delta_tx_late_collision"
  printf '\n'
  printf 'rx_align_error     : %s\n' "$rx_align"
  printf 'tx_collision       : %s\n' "$tx_collision"
  printf 'tx_late_collision  : %s\n' "$tx_late_collision"
  printf '\n'

  local warnings=()
  if [[ "${duplex:-unknown}" != "yes" ]]; then
    warnings+=("full duplex is not enabled")
  fi
  if [[ "${partner:-unknown}" == *"half"* ]]; then
    warnings+=("link partner is advertising half duplex")
  fi
  if [[ "$delta_rx_align" -gt 0 || "$delta_tx_collision" -gt 0 || "$delta_tx_late_collision" -gt 0 ]]; then
    warnings+=("error counters are increasing")
  fi
  if [[ "${ping_loss:-unknown}" != "0%" ]]; then
    warnings+=("ping loss is non-zero")
  fi

  printf 'Health Summary     : '
  if (( ${#warnings[@]} == 0 )); then
    printf 'looks stable\n'
  else
    printf '\n'
    local warning
    for warning in "${warnings[@]}"; do
      printf '  - %s\n' "$warning"
    done
  fi
  printf '\n'
  printf 'Press Ctrl+C to stop.\n'
}

render_line() {
  local status="$1"
  local rate="$2"
  local duplex="$3"
  local partner="$4"
  local ping_loss="$5"
  local ping_avg="$6"
  local ping_max="$7"
  local delta_rx_align="$8"
  local delta_tx_collision="$9"
  local delta_tx_late_collision="${10}"
  local rx_align="${11}"
  local tx_collision="${12}"
  local tx_late_collision="${13}"
  local cycle_s="${14}"

  printf '%s cycle=%ss status=%s rate=%s duplex=%s partner=%s ping_loss=%s ping_avg=%s ping_max=%s d_rx_align=%s d_tx_collision=%s d_tx_late=%s total_rx_align=%s total_tx_collision=%s total_tx_late=%s\n' \
    "$(date '+%F %T')" \
    "$cycle_s" \
    "${status:-unknown}" \
    "${rate:-unknown}" \
    "${duplex:-unknown}" \
    "${partner:-unknown}" \
    "$ping_loss" \
    "$ping_avg" \
    "$ping_max" \
    "$delta_rx_align" \
    "$delta_tx_collision" \
    "$delta_tx_late_collision" \
    "$rx_align" \
    "$tx_collision" \
    "$tx_late_collision"
}

prev_rx_align=""
prev_tx_collision=""
prev_tx_late_collision=""

ensure_master_connection

while true; do
  cycle_start="$(python3 - <<'PY'
import time
print(time.time())
PY
)"

  raw_output="$(
    ros_run "/interface ethernet monitor ${WAN_INTERFACE} once; /interface ethernet print stats where name=${WAN_INTERFACE}; /ping ${PING_TARGET} count=${PING_COUNT}" \
      | cleanup_output
  )"

  status="$(parse_value "status" "$raw_output")"
  rate="$(parse_value "rate" "$raw_output")"
  duplex="$(parse_value "full-duplex" "$raw_output")"
  partner="$(parse_value "link-partner-advertising" "$raw_output")"

  rx_align="$(parse_value "rx-align-error" "$raw_output")"
  tx_collision="$(parse_value "tx-collision" "$raw_output")"
  tx_late_collision="$(parse_value "tx-late-collision" "$raw_output")"

  rx_align="${rx_align// /}"
  tx_collision="${tx_collision// /}"
  tx_late_collision="${tx_late_collision// /}"

  if [[ -n "$prev_rx_align" ]]; then
    delta_rx_align=$((rx_align - prev_rx_align))
    delta_tx_collision=$((tx_collision - prev_tx_collision))
    delta_tx_late_collision=$((tx_late_collision - prev_tx_late_collision))
  else
    delta_rx_align=0
    delta_tx_collision=0
    delta_tx_late_collision=0
  fi

  prev_rx_align="$rx_align"
  prev_tx_collision="$tx_collision"
  prev_tx_late_collision="$tx_late_collision"

  IFS='|' read -r ping_loss ping_avg ping_max <<<"$(parse_ping_from_seq "$raw_output")"

  cycle_end="$(python3 - <<'PY'
import time
print(time.time())
PY
)"
  cycle_s="$(python3 - <<PY
start = float("$cycle_start")
end = float("$cycle_end")
print(f"{end-start:.2f}")
PY
)"

  if [[ "$MODE" == "line" ]]; then
    render_line \
      "$status" \
      "$rate" \
      "$duplex" \
      "$partner" \
      "$ping_loss" \
      "$ping_avg" \
      "$ping_max" \
      "$delta_rx_align" \
      "$delta_tx_collision" \
      "$delta_tx_late_collision" \
      "$rx_align" \
      "$tx_collision" \
      "$tx_late_collision" \
      "$cycle_s"
  else
    render_dashboard \
      "$status" \
      "$rate" \
      "$duplex" \
      "$partner" \
      "$ping_loss" \
      "$ping_avg" \
      "$ping_max" \
      "$delta_rx_align" \
      "$delta_tx_collision" \
      "$delta_tx_late_collision" \
      "$rx_align" \
      "$tx_collision" \
      "$tx_late_collision" \
      "$cycle_s"
  fi

  sleep_for="$(python3 - <<PY
interval = float("$INTERVAL")
cycle = float("$cycle_s")
remaining = interval - cycle
print(f"{remaining:.2f}" if remaining > 0 else "0")
PY
)"
  sleep "$sleep_for"
done
