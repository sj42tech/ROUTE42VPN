#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 <new-host> [old-host]" >&2
  echo "Example: $0 debian@203.0.113.10 5.39.219.74" >&2
  exit 64
fi

NEW_HOST="$1"
OLD_HOST="${2:-${OLD_HOST:-5.39.219.74}}"
XRAY_INSTALL_URL="${XRAY_INSTALL_URL:-https://raw.githubusercontent.com/XTLS/Xray-install/main/install-release.sh}"
SSH_OPTS=(-o BatchMode=yes -o ConnectTimeout=8)
NEW_HOST_SSH_KEY="${NEW_HOST_SSH_KEY:-}"
OLD_HOST_SSH_KEY="${OLD_HOST_SSH_KEY:-}"
NEW_SSH_OPTS=("${SSH_OPTS[@]}")
OLD_SSH_OPTS=("${SSH_OPTS[@]}")

if [[ -n "$NEW_HOST_SSH_KEY" ]]; then
  NEW_SSH_OPTS+=(-i "$NEW_HOST_SSH_KEY")
fi

if [[ -n "$OLD_HOST_SSH_KEY" ]]; then
  OLD_SSH_OPTS+=(-i "$OLD_HOST_SSH_KEY")
fi

echo "Installing Xray on ${NEW_HOST} if needed..."
ssh "${NEW_SSH_OPTS[@]}" "$NEW_HOST" 'bash -se' <<EOF
set -euo pipefail
SUDO=""
if [[ "\$(id -u)" -ne 0 ]]; then
  SUDO=sudo
fi
export DEBIAN_FRONTEND=noninteractive
\$SUDO apt-get update
\$SUDO apt-get install -y ca-certificates curl tar
\$SUDO mkdir -p /usr/local/bin /usr/local/share/xray /usr/local/etc/xray /etc/systemd/system/xray.service.d
EOF

echo "Copying Xray binary, data, config, and systemd files from ${OLD_HOST} to ${NEW_HOST}..."
ssh "${OLD_SSH_OPTS[@]}" "$OLD_HOST" 'bash -se' <<'EOF' | ssh "${NEW_SSH_OPTS[@]}" "$NEW_HOST" 'if [ "$(id -u)" -ne 0 ]; then sudo tar xzf - -C /; else tar xzf - -C /; fi'
set -euo pipefail
SUDO=""
if [[ "$(id -u)" -ne 0 ]]; then
  SUDO=sudo
fi

paths=()
$SUDO test -f /usr/local/bin/xray && paths+=(/usr/local/bin/xray)
$SUDO test -d /usr/local/share/xray && paths+=(/usr/local/share/xray)
$SUDO test -f /usr/local/etc/xray/config.json && paths+=(/usr/local/etc/xray/config.json)
$SUDO test -f /etc/systemd/system/xray.service && paths+=(/etc/systemd/system/xray.service)
$SUDO test -d /etc/systemd/system/xray.service.d && paths+=(/etc/systemd/system/xray.service.d)

if [[ ${#paths[@]} -eq 0 ]]; then
  echo "No Xray files found on source host." >&2
  exit 1
fi

$SUDO tar czf - "${paths[@]}"
EOF

echo "Reloading systemd and validating Xray on ${NEW_HOST}..."
ssh "${NEW_SSH_OPTS[@]}" "$NEW_HOST" 'bash -se' <<'EOF'
set -euo pipefail
SUDO=""
if [[ "$(id -u)" -ne 0 ]]; then
  SUDO=sudo
fi

$SUDO systemctl daemon-reload
$SUDO xray run -test -config /usr/local/etc/xray/config.json
$SUDO systemctl enable xray
$SUDO systemctl restart xray
sleep 1
$SUDO systemctl --no-pager --full status xray | sed -n '1,40p'
$SUDO ss -ltnp | grep ':443' || {
  echo "Xray is not listening on :443 after migration." >&2
  exit 1
}
EOF

cat <<EOF
Migration finished.

Old host: $OLD_HOST
New host: $NEW_HOST

Next:
  1. Update your VLESS link to point at the new server IP.
  2. Test the profile over mobile and home internet.
  3. Keep the old VPS for a short overlap window, then shut it down.
EOF
