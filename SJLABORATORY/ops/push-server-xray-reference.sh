#!/usr/bin/env bash
set -euo pipefail

REFERENCE_CONFIG="${REFERENCE_CONFIG:-/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/secrets/PROXY/xray/server.reference.json}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/sergei-macbook-vps}"
SSH_OPTS=(-o BatchMode=yes -o ConnectTimeout=8 -i "$SSH_KEY")

if [[ $# -lt 1 ]]; then
  cat >&2 <<'EOF'
Usage:
  push-server-xray-reference.sh root@5.39.219.74 debian@194.182.174.240

Optional overrides:
  REFERENCE_CONFIG
  SSH_KEY
EOF
  exit 64
fi

if [[ ! -f "$REFERENCE_CONFIG" ]]; then
  echo "Reference config not found: $REFERENCE_CONFIG" >&2
  exit 1
fi

validate_local_reference() {
  local xray_bin="${XRAY_BIN:-/opt/homebrew/bin/xray}"
  if [[ -x "$xray_bin" ]]; then
    "$xray_bin" run -test -config "$REFERENCE_CONFIG" >/dev/null
  elif command -v xray >/dev/null 2>&1; then
    xray run -test -config "$REFERENCE_CONFIG" >/dev/null
  fi
}

validate_local_reference

for host in "$@"; do
  remote_tmp="/tmp/xray-config.reference.json"
  timestamp="$(date +%Y%m%d-%H%M%S)"

  scp "${SSH_OPTS[@]}" "$REFERENCE_CONFIG" "$host:$remote_tmp" >/dev/null

  ssh "${SSH_OPTS[@]}" "$host" "bash -se" <<EOF
set -euo pipefail
SUDO=""
if [[ "\$(id -u)" -ne 0 ]]; then
  SUDO=sudo
fi

BACKUP="/usr/local/etc/xray/config.json.backup-$timestamp"

\$SUDO test -f /usr/local/etc/xray/config.json
\$SUDO cp /usr/local/etc/xray/config.json "\$BACKUP"
\$SUDO install -o root -g root -m 644 "$remote_tmp" /usr/local/etc/xray/config.json

if ! \$SUDO xray run -test -config /usr/local/etc/xray/config.json >/dev/null 2>&1; then
  \$SUDO install -o root -g root -m 644 "\$BACKUP" /usr/local/etc/xray/config.json
  echo "Validation failed on $host; restored backup \$BACKUP." >&2
  exit 1
fi

if ! \$SUDO systemctl restart xray; then
  \$SUDO install -o root -g root -m 644 "\$BACKUP" /usr/local/etc/xray/config.json
  \$SUDO systemctl restart xray
  echo "Restart failed on $host; restored backup \$BACKUP." >&2
  exit 1
fi

sleep 1

if ! \$SUDO ss -ltn | grep -q ':443'; then
  \$SUDO install -o root -g root -m 644 "\$BACKUP" /usr/local/etc/xray/config.json
  \$SUDO systemctl restart xray
  echo "Port 443 did not come back on $host; restored backup \$BACKUP." >&2
  exit 1
fi

VERSION="\$(\$SUDO /usr/local/bin/xray version 2>/dev/null | head -n1 || true)"
if [[ -z "\$VERSION" ]]; then
  VERSION="\$(\$SUDO xray version 2>/dev/null | head -n1 || true)"
fi

rm -f "$remote_tmp"
printf '%s|%s\n' "$host" "\$VERSION"
EOF
done
