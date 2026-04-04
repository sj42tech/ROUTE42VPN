#!/usr/bin/env bash
set -euo pipefail

TEMPLATE_PATH="${TEMPLATE_PATH:-/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/secrets/PROXY/xray/client.split.template.json}"
DEST_CONFIG="${1:-${DEST_CONFIG:-}}"
SERVER_ADDRESS="${SERVER_ADDRESS:-}"
SERVER_PORT="${SERVER_PORT:-443}"
UUID="${UUID:-775ed879-a162-45e3-b8af-c49f96eaede5}"
FLOW="${FLOW:-xtls-rprx-vision}"
SERVER_NAME="${SERVER_NAME:-www.microsoft.com}"
PUBLIC_KEY="${PUBLIC_KEY:-fivTvehL9FxvXGc9TmVPtOJa2baWSl8DkyAvoTLb0Q8}"
SHORT_ID="${SHORT_ID:-f3aa}"
SPIDER_X="${SPIDER_X:-/}"
XRAY_BIN="${XRAY_BIN:-/opt/homebrew/bin/xray}"

if [[ -z "$DEST_CONFIG" ]]; then
  cat >&2 <<'EOF'
Usage:
  SERVER_ADDRESS=5.39.219.74 render-client-xray-from-template.sh /absolute/path/to/config.json

Optional overrides:
  SERVER_PORT
  UUID
  FLOW
  SERVER_NAME
  PUBLIC_KEY
  SHORT_ID
  SPIDER_X
  TEMPLATE_PATH
EOF
  exit 64
fi

if [[ -z "$SERVER_ADDRESS" ]]; then
  echo "Missing required environment variable: SERVER_ADDRESS" >&2
  exit 1
fi

if [[ ! -f "$TEMPLATE_PATH" ]]; then
  echo "Template file not found: $TEMPLATE_PATH" >&2
  exit 1
fi

escape_sed() {
  printf '%s' "$1" | sed 's/[\/&]/\\&/g'
}

mkdir -p "$(dirname "$DEST_CONFIG")"

sed \
  -e "s/__SERVER_ADDRESS__/$(escape_sed "$SERVER_ADDRESS")/g" \
  -e "s/__SERVER_PORT__/$(escape_sed "$SERVER_PORT")/g" \
  -e "s/__UUID__/$(escape_sed "$UUID")/g" \
  -e "s/__FLOW__/$(escape_sed "$FLOW")/g" \
  -e "s/__SERVER_NAME__/$(escape_sed "$SERVER_NAME")/g" \
  -e "s/__PUBLIC_KEY__/$(escape_sed "$PUBLIC_KEY")/g" \
  -e "s/__SHORT_ID__/$(escape_sed "$SHORT_ID")/g" \
  -e "s/__SPIDER_X__/$(escape_sed "$SPIDER_X")/g" \
  "$TEMPLATE_PATH" > "$DEST_CONFIG"

if [[ -x "$XRAY_BIN" ]]; then
  "$XRAY_BIN" run -test -config "$DEST_CONFIG" >/dev/null
elif command -v xray >/dev/null 2>&1; then
  xray run -test -config "$DEST_CONFIG" >/dev/null
fi

cat <<EOF
Rendered client Xray config:
  template: $TEMPLATE_PATH
  dest:     $DEST_CONFIG
  address:  $SERVER_ADDRESS
  port:     $SERVER_PORT
EOF
