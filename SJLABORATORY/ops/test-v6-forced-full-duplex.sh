#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SECRETS_FILE="${MIKROTIK_SECRET_FILE:-$ROOT_DIR/secrets/MIKROTIK/admin-192.168.0.1.txt}"

if [[ ! -f "$SECRETS_FILE" ]]; then
  echo "Missing MikroTik secrets file: $SECRETS_FILE" >&2
  exit 1
fi

HOST="${MIKROTIK_HOST:-$(sed -n 's/^HOST=//p' "$SECRETS_FILE" | head -n1)}"
USER_NAME="${MIKROTIK_USER:-$(sed -n 's/^USER=//p' "$SECRETS_FILE" | head -n1)}"
PASSWORD="${MIKROTIK_PASSWORD:-$(sed -n 's/^PASSWORD=//p' "$SECRETS_FILE" | head -n1)}"

WAN_INTERFACE="${WAN_INTERFACE:-ether1}"
PING_TARGET="${PING_TARGET:-10.12.109.5}"
TEST_URL="${TEST_URL:-https://speed.cloudflare.com/__down?bytes=20000000}"
BASELINE_SECONDS="${BASELINE_SECONDS:-60}"
FORCED_SECONDS="${FORCED_SECONDS:-300}"
POST_SECONDS="${POST_SECONDS:-60}"
SAMPLE_MAX_TIME="${SAMPLE_MAX_TIME:-30}"
TMP_DIR="$(mktemp -d)"
forced_mode_applied=0

cleanup() {
  if [[ "$forced_mode_applied" == "1" ]]; then
    ros_cmd "/interface ethernet set ${WAN_INTERFACE} auto-negotiation=yes" >/dev/null 2>&1 || true
  fi
  rm -rf "$TMP_DIR"
}

trap cleanup EXIT

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd curl
require_cmd expect
require_cmd python3

ros_cmd() {
  local cmd="$1"
  EXPECT_HOST="$HOST" \
  EXPECT_USER="$USER_NAME" \
  EXPECT_PASS="$PASSWORD" \
  ROS_CMD="$cmd" \
  /usr/bin/expect <<'EOF'
log_user 1
set timeout 30
set host $env(EXPECT_HOST)
set user $env(EXPECT_USER)
set pass $env(EXPECT_PASS)
set cmd $env(ROS_CMD)
spawn ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${user}@${host} $cmd
expect {
  -re {[Pp]assword:} { send -- "$pass\r" }
}
expect eof
EOF
}

cleanup_output() {
  sed -e $'s/\r//g' \
      -e '/^spawn ssh /d' \
      -e '/^Warning: Permanently added/d' \
      -e '/^\*\* WARNING:/d' \
      -e '/^\*\* This session/d' \
      -e '/^\*\* The server/d' \
      -e '/^admin@.*password:/d'
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

router_snapshot() {
  local raw
  raw="$(ros_cmd "/interface ethernet monitor ${WAN_INTERFACE} once; /interface ethernet print stats where name=${WAN_INTERFACE}; /ping ${PING_TARGET} count=3" | cleanup_output)"
  local status rate duplex partner rx_align tx_collision tx_late ping_loss ping_avg ping_max
  status="$(parse_value "status" "$raw")"
  rate="$(parse_value "rate" "$raw")"
  duplex="$(parse_value "full-duplex" "$raw")"
  partner="$(parse_value "link-partner-advertising" "$raw")"
  rx_align="$(parse_value "rx-align-error" "$raw")"
  tx_collision="$(parse_value "tx-collision" "$raw")"
  tx_late="$(parse_value "tx-late-collision" "$raw")"
  IFS='|' read -r ping_loss ping_avg ping_max <<<"$(parse_ping_from_seq "$raw")"
  printf '%s|%s|%s|%s|%s|%s|%s|%s|%s|%s\n' \
    "${status:-unknown}" \
    "${rate:-unknown}" \
    "${duplex:-unknown}" \
    "${partner:-unknown}" \
    "${rx_align// /}" \
    "${tx_collision// /}" \
    "${tx_late// /}" \
    "$ping_loss" \
    "$ping_avg" \
    "$ping_max"
}

direct_sample() {
  local out_file err_file metrics rc note
  out_file="$(mktemp "$TMP_DIR/sample.out.XXXXXX")"
  err_file="$(mktemp "$TMP_DIR/sample.err.XXXXXX")"
  set +e
  curl --noproxy '*' -4 -L --silent --show-error --max-time "$SAMPLE_MAX_TIME" \
    -o /dev/null \
    -w '%{http_code}|%{time_connect}|%{time_appconnect}|%{time_starttransfer}|%{time_total}|%{size_download}|%{speed_download}' \
    "$TEST_URL" >"$out_file" 2>"$err_file"
  rc=$?
  set -e
  metrics="$(tr -d '\r' <"$out_file")"
  note="$(tr '\n' ' ' <"$err_file" | sed 's/[[:space:]]\+/ /g; s/^ //; s/ $//')"
  rm -f "$out_file" "$err_file"

  if [[ $rc -ne 0 ]]; then
    if [[ -z "$metrics" ]]; then
      metrics='000|0|0|0|0|0|0'
    fi
    printf '%s|curl_error=%s\n' "$metrics" "$note"
  else
    printf '%s|-\n' "$metrics"
  fi
}

run_phase() {
  local name="$1"
  local duration="$2"
  local phase_file="$TMP_DIR/${name}.tsv"
  local start_ts now sample_idx before after

  before="$(router_snapshot)"
  start_ts="$(python3 - <<'PY'
import time
print(time.time())
PY
)"

  printf '\n== %s (%ss) ==\n' "$name" "$duration"
  printf 'router_before=%s\n' "$before"
  printf 'timestamp|sample|http_code|connect_s|tls_s|ttfb_s|total_s|bytes|bytes_per_s|note\n'

  : > "$phase_file"
  sample_idx=0

  while true; do
    now="$(python3 - <<PY
import time
print(time.time())
PY
)"
    if python3 - <<PY
start=float("$start_ts")
now=float("$now")
duration=float("$duration")
import sys
sys.exit(0 if now-start < duration else 1)
PY
    then
      sample_idx=$((sample_idx + 1))
      local ts sample
      ts="$(date '+%F %T')"
      sample="$(direct_sample)"
      printf '%s|%s|%s\n' "$ts" "$sample_idx" "$sample" | tee -a "$phase_file"
    else
      break
    fi
  done

  after="$(router_snapshot)"
  printf 'router_after=%s\n' "$after"
  summarize_phase "$name" "$before" "$after" "$phase_file"
}

summarize_phase() {
  local name="$1"
  local before="$2"
  local after="$3"
  local phase_file="$4"

  python3 - <<PY
from statistics import mean
name = ${name@Q}
before = ${before@Q}.split("|")
after = ${after@Q}.split("|")
phase_file = ${phase_file@Q}

samples = []
with open(phase_file, "r", encoding="utf-8") as fh:
    for line in fh:
        line = line.strip()
        if not line:
            continue
        ts, idx, http_code, connect_s, tls_s, ttfb_s, total_s, size_download, speed_download, note = line.split("|", 9)
        samples.append({
            "http_code": http_code,
            "total_s": float(total_s),
            "bytes": int(size_download),
            "speed": float(speed_download),
            "note": note,
        })

ok = [s for s in samples if s["http_code"] == "200" and s["bytes"] > 0]
all_speeds = [s["speed"] for s in ok]
avg_speed = mean(all_speeds) if all_speeds else 0.0
best_speed = max(all_speeds) if all_speeds else 0.0
total_bytes = sum(s["bytes"] for s in ok)
failures = len(samples) - len(ok)

def as_int(v):
    try:
        return int(v)
    except Exception:
        return 0

rx_delta = as_int(after[4]) - as_int(before[4])
tx_coll_delta = as_int(after[5]) - as_int(before[5])
tx_late_delta = as_int(after[6]) - as_int(before[6])

print(f"summary[{name}] samples={len(samples)} ok={len(ok)} failures={failures} avg_Bps={avg_speed:.0f} avg_Mbps={avg_speed*8/1_000_000:.2f} best_Mbps={best_speed*8/1_000_000:.2f} total_MB={total_bytes/1_000_000:.2f}")
print(f"router[{name}] before_status={before[0]} before_duplex={before[2]} before_partner={before[3]} before_ping={before[7]}/{before[8]}")
print(f"router[{name}] after_status={after[0]} after_duplex={after[2]} after_partner={after[3]} after_ping={after[7]}/{after[8]}")
print(f"router_delta[{name}] rx_align={rx_delta} tx_collision={tx_coll_delta} tx_late={tx_late_delta}")
PY
}

apply_forced_mode() {
  printf '\nApplying forced 100M/full on %s...\n' "$WAN_INTERFACE"
  ros_cmd "/interface ethernet set ${WAN_INTERFACE} auto-negotiation=no speed=100Mbps full-duplex=yes" >/dev/null
  forced_mode_applied=1
  sleep 2
  printf 'forced_monitor=%s\n' "$(router_snapshot)"
}

revert_autoneg() {
  printf '\nReverting %s to auto-negotiation...\n' "$WAN_INTERFACE"
  ros_cmd "/interface ethernet set ${WAN_INTERFACE} auto-negotiation=yes" >/dev/null
  forced_mode_applied=0
  sleep 3
  printf 'reverted_monitor=%s\n' "$(router_snapshot)"
}

printf 'Test URL           : %s\n' "$TEST_URL"
printf 'Baseline Seconds   : %s\n' "$BASELINE_SECONDS"
printf 'Forced Seconds     : %s\n' "$FORCED_SECONDS"
printf 'Post Seconds       : %s\n' "$POST_SECONDS"
printf 'WAN Interface      : %s\n' "$WAN_INTERFACE"
printf 'Ping Target        : %s\n' "$PING_TARGET"

run_phase "baseline_autoneg" "$BASELINE_SECONDS"
apply_forced_mode
run_phase "forced_full_duplex" "$FORCED_SECONDS"
revert_autoneg
run_phase "post_autoneg" "$POST_SECONDS"
