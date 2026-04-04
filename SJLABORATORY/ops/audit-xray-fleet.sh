#!/usr/bin/env bash
set -euo pipefail

DEFAULT_HOSTS=(
  "5.39.219.74"
  "debian@194.182.174.240"
)

SSH_OPTS=(-o BatchMode=yes -o ConnectTimeout=8)
XRAY_FLEET_SSH_KEY="${XRAY_FLEET_SSH_KEY:-}"

if [[ -n "$XRAY_FLEET_SSH_KEY" ]]; then
  SSH_OPTS+=(-i "$XRAY_FLEET_SSH_KEY")
fi

if [[ $# -gt 0 ]]; then
  HOSTS=("$@")
elif [[ -n "${XRAY_FLEET_HOSTS:-}" ]]; then
  read -r -a HOSTS <<<"${XRAY_FLEET_HOSTS}"
else
  HOSTS=("${DEFAULT_HOSTS[@]}")
fi

printf '%-24s %-10s %-10s %s\n' "host" "service" "listen443" "version"
printf '%-24s %-10s %-10s %s\n' "------------------------" "----------" "----------" "----------------------------------------"

for host in "${HOSTS[@]}"; do
  RESULT="$(
    ssh "${SSH_OPTS[@]}" "$host" 'bash -se' <<'EOF' 2>/dev/null || true
set -euo pipefail

SUDO=""
if [[ "$(id -u)" -ne 0 ]]; then
  SUDO=sudo
fi

SERVICE="$($SUDO systemctl is-active xray 2>/dev/null || echo unknown)"
LISTEN443="down"
if $SUDO ss -ltn 2>/dev/null | grep -q ':443'; then
  LISTEN443="up"
fi

VERSION="$($SUDO /usr/local/bin/xray version 2>/dev/null | head -n1 || true)"
if [[ -z "$VERSION" ]]; then
  VERSION="$($SUDO xray version 2>/dev/null | head -n1 || true)"
fi
if [[ -z "$VERSION" ]]; then
  VERSION="unavailable"
fi

printf '%s|%s|%s\n' "$SERVICE" "$LISTEN443" "$VERSION"
EOF
  )"

  if [[ -z "$RESULT" ]]; then
    printf '%-24s %-10s %-10s %s\n' "$host" "ssh-fail" "unknown" "unavailable"
    continue
  fi

  IFS='|' read -r SERVICE LISTEN443 VERSION <<<"$RESULT"
  printf '%-24s %-10s %-10s %s\n' "$host" "$SERVICE" "$LISTEN443" "$VERSION"
done
