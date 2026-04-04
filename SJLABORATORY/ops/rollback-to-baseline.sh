#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

source_config="${repo_root}/secrets/PROXY/xray/config.working_baseline.json"
target_config="${repo_root}/secrets/PROXY/xray/config.json"
backup_dir="${repo_root}/secrets/PROXY/xray/backups"
timestamp="$(date +%Y%m%d-%H%M%S)"
backup_config="${backup_dir}/config.json.before-rollback-${timestamp}"
xray_bin="${XRAY_BIN:-$(command -v xray || true)}"
launchctl_label="${LAUNCHCTL_LABEL:-local.xray}"
launchctl_plist="${LAUNCHCTL_PLIST:-$HOME/Library/LaunchAgents/xray.plist}"
service_target=""

if [[ ! -f "${source_config}" ]]; then
  echo "Missing baseline config: ${source_config}" >&2
  exit 1
fi

mkdir -p "${backup_dir}"

if [[ -f "${target_config}" ]]; then
  cp "${target_config}" "${backup_config}"
  echo "Backed up current config to ${backup_config}"
else
  echo "No existing target config found. A fresh baseline file will be created."
fi

cp "${source_config}" "${target_config}"
echo "Restored baseline config to ${target_config}"

if [[ -z "${xray_bin}" ]]; then
  echo "xray binary not found in PATH, cannot validate restored config." >&2
  exit 1
fi

"${xray_bin}" run -test -config "${target_config}"
echo "Baseline config validated successfully."

resolve_service_target() {
  local user_domain="gui/$(id -u)"
  if launchctl print "${user_domain}/${launchctl_label}" >/dev/null 2>&1; then
    service_target="${user_domain}/${launchctl_label}"
    return 0
  fi
  if launchctl print "system/${launchctl_label}" >/dev/null 2>&1; then
    service_target="system/${launchctl_label}"
    return 0
  fi
  return 1
}

wait_for_listener() {
  local port="$1"
  local attempt
  for attempt in {1..20}; do
    if lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

run_proxy_health_checks() {
  local http_port="$1"
  local socks_port="$2"
  local failed=0

  echo "Checking HTTP reachability via port ${http_port}..."
  if ! curl --silent --show-error --max-time 8 --proxy "http://127.0.0.1:${http_port}" http://ifconfig.me/ip >/dev/null; then
    failed=1
  fi

  echo "Checking HTTPS to Google via port ${http_port}..."
  if ! curl --silent --show-error --max-time 10 --proxy "http://127.0.0.1:${http_port}" -I https://www.google.com >/dev/null; then
    failed=1
  fi

  echo "Checking HTTPS to Apple via port ${http_port}..."
  if ! curl --silent --show-error --max-time 10 --proxy "http://127.0.0.1:${http_port}" -I https://www.apple.com >/dev/null; then
    failed=1
  fi

  echo "Checking SOCKS reachability via port ${socks_port}..."
  if ! curl --silent --show-error --max-time 10 --socks5 "127.0.0.1:${socks_port}" -I https://www.google.com >/dev/null; then
    failed=1
  fi

  return "${failed}"
}

restart_launchctl_service() {
  launchctl kickstart -k "${service_target}" >/dev/null 2>&1 || return 1

  wait_for_listener 1080 || return 1
  wait_for_listener 8080 || return 1
  run_proxy_health_checks 8080 1080 || return 1
  return 0
}

hard_restart_launchctl_service() {
  local gui_domain="gui/$(id -u)"
  if [[ "${service_target}" == ${gui_domain}/* && -f "${launchctl_plist}" ]]; then
    launchctl bootout "${service_target}" >/dev/null 2>&1 || true
    launchctl bootstrap "${gui_domain}" "${launchctl_plist}" >/dev/null 2>&1 || true
    launchctl kickstart -k "${service_target}" >/dev/null 2>&1 || true
  else
    launchctl kickstart -k "${service_target}" >/dev/null 2>&1 || true
  fi

  wait_for_listener 1080 || return 1
  wait_for_listener 8080 || return 1
  run_proxy_health_checks 8080 1080 || return 1
  return 0
}

if ! resolve_service_target; then
  echo "Launchctl label not found: ${launchctl_label}" >&2
  echo "Config rollback completed, but service restart was skipped." >&2
  exit 2
fi

echo "Restarting ${service_target}..."
if restart_launchctl_service; then
  cat <<EOF
Rollback completed.

Active server:
  5.39.219.74
EOF
  exit 0
fi

echo "Soft restart failed, trying a harder restart path..." >&2
if hard_restart_launchctl_service; then
  cat <<EOF
Rollback completed after hard restart.

Active server:
  5.39.219.74
EOF
  exit 0
fi

echo "Rollback restored the config file, but Xray did not recover automatically." >&2
exit 1
