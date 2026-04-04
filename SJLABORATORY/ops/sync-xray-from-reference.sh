#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  cat >&2 <<'EOF'
Usage: sync-xray-from-reference.sh <target-host> [target-host...]

Environment overrides:
  REF_HOST=5.39.219.74
  REF_HOST_SSH_KEY=/path/to/ref-key
  TARGET_HOST_SSH_KEY=/path/to/target-key
EOF
  exit 64
fi

REF_HOST="${REF_HOST:-5.39.219.74}"
REF_HOST_SSH_KEY="${REF_HOST_SSH_KEY:-}"
TARGET_HOST_SSH_KEY="${TARGET_HOST_SSH_KEY:-}"

for target in "$@"; do
  echo "Syncing Xray from $REF_HOST to $target..."
  OLD_HOST_SSH_KEY="$REF_HOST_SSH_KEY" \
  NEW_HOST_SSH_KEY="$TARGET_HOST_SSH_KEY" \
  /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/migrate-xray-from-old-vps.sh "$target" "$REF_HOST"
done

echo
echo "Fleet sync finished. Recommended follow-up:"
echo "  /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/audit-xray-fleet.sh $*"
