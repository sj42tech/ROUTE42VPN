#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 <new-host> [old-host]" >&2
  echo "Example: $0 root@203.0.113.10 5.39.219.74" >&2
  exit 64
fi

NEW_HOST="$1"
OLD_HOST="${2:-${OLD_HOST:-5.39.219.74}}"
XRAY_INSTALL_URL="${XRAY_INSTALL_URL:-https://raw.githubusercontent.com/XTLS/Xray-install/main/install-release.sh}"
SSH_OPTS=(-o BatchMode=yes -o ConnectTimeout=8)

echo "Installing Xray on ${NEW_HOST} if needed..."
ssh "${SSH_OPTS[@]}" "$NEW_HOST" 'bash -se' <<EOF
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y ca-certificates curl tar

if ! command -v xray >/dev/null 2>&1; then
  curl -fsSL "$XRAY_INSTALL_URL" -o /tmp/install-release.sh
  bash /tmp/install-release.sh install
fi

mkdir -p /usr/local/etc/xray /etc/systemd/system/xray.service.d
EOF

echo "Copying Xray config and systemd files from ${OLD_HOST} to ${NEW_HOST}..."
ssh "${SSH_OPTS[@]}" "$OLD_HOST" 'bash -se' <<'EOF' | ssh "${SSH_OPTS[@]}" "$NEW_HOST" 'tar xzf - -C /'
set -euo pipefail

paths=()
[[ -f /usr/local/etc/xray/config.json ]] && paths+=(/usr/local/etc/xray/config.json)
[[ -f /etc/systemd/system/xray.service ]] && paths+=(/etc/systemd/system/xray.service)
[[ -d /etc/systemd/system/xray.service.d ]] && paths+=(/etc/systemd/system/xray.service.d)

if [[ ${#paths[@]} -eq 0 ]]; then
  echo "No Xray files found on source host." >&2
  exit 1
fi

tar czf - "${paths[@]}"
EOF

echo "Reloading systemd and validating Xray on ${NEW_HOST}..."
ssh "${SSH_OPTS[@]}" "$NEW_HOST" 'bash -se' <<'EOF'
set -euo pipefail

systemctl daemon-reload
xray run -test -config /usr/local/etc/xray/config.json
systemctl enable xray
systemctl restart xray
sleep 1
systemctl --no-pager --full status xray | sed -n '1,40p'
ss -ltnp | grep ':443' || {
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
