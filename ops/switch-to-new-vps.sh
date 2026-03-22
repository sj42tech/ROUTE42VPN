#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

source_config="${repo_root}/secrets/PROXY/xray/config.hostkey.json"
target_config="${repo_root}/secrets/PROXY/xray/config.json"
backup_dir="${repo_root}/secrets/PROXY/xray/backups"
timestamp="$(date +%Y%m%d-%H%M%S)"
backup_config="${backup_dir}/config.json.before-switch-${timestamp}"
xray_bin="${XRAY_BIN:-$(command -v xray || true)}"
launchctl_label="${LAUNCHCTL_LABEL:-local.xray}"
canary_socks_port="${CANARY_SOCKS_PORT:-19080}"
canary_http_port="${CANARY_HTTP_PORT:-19081}"
allow_experimental_switch="${ALLOW_EXPERIMENTAL_SWITCH:-0}"
temp_config=""
temp_log=""
temp_pid=""
service_target=""

if [[ ! -f "${source_config}" ]]; then
  echo "Missing new-VPS config: ${source_config}" >&2
  exit 1
fi

mkdir -p "${backup_dir}"

if [[ -z "${xray_bin}" ]]; then
  echo "xray binary not found in PATH, cannot validate active config." >&2
  exit 1
fi

cleanup_canary() {
  if [[ -n "${temp_pid}" ]] && kill -0 "${temp_pid}" >/dev/null 2>&1; then
    kill "${temp_pid}" >/dev/null 2>&1 || true
    wait "${temp_pid}" >/dev/null 2>&1 || true
  fi
  if [[ -n "${temp_config}" && -f "${temp_config}" ]]; then
    rm -f "${temp_config}"
  fi
  if [[ -n "${temp_log}" && -f "${temp_log}" ]]; then
    rm -f "${temp_log}"
  fi
}

restore_previous_config() {
  if [[ -f "${backup_config}" ]]; then
    cp "${backup_config}" "${target_config}"
    echo "Restored previous config from ${backup_config}"
  fi
}

wait_for_local_proxy() {
  local http_port="$1"
  local attempt
  for attempt in {1..15}; do
    if curl --silent --max-time 2 --proxy "http://127.0.0.1:${http_port}" http://ifconfig.me/ip >/dev/null 2>&1; then
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

"${xray_bin}" run -test -config "${source_config}"
echo "New-VPS source config validated successfully."

temp_config="$(mktemp /tmp/xray-new-vps-canary.XXXXXX.json)"
temp_log="$(mktemp /tmp/xray-new-vps-canary.XXXXXX.log)"
perl -0pe "s/\"port\": 1080/\"port\": ${canary_socks_port}/; s/\"port\": 8080/\"port\": ${canary_http_port}/" "${source_config}" > "${temp_config}"
"${xray_bin}" run -test -config "${temp_config}" >/dev/null

trap cleanup_canary EXIT

echo "Starting canary Xray on temporary ports ${canary_socks_port}/${canary_http_port}..."
"${xray_bin}" run -config "${temp_config}" >"${temp_log}" 2>&1 &
temp_pid="$!"
sleep 2

if ! kill -0 "${temp_pid}" >/dev/null 2>&1; then
  echo "Canary Xray exited unexpectedly." >&2
  sed -n '1,120p' "${temp_log}" >&2 || true
  exit 1
fi

if ! run_proxy_health_checks "${canary_http_port}" "${canary_socks_port}"; then
  echo "Canary checks failed. Nothing was changed in ${target_config}." >&2
  exit 1
fi
echo "Canary checks passed."

if [[ "${allow_experimental_switch}" != "1" ]]; then
  cat <<EOF
Canary checks passed, but the live switch is blocked by default.

Reason:
  Real browser traffic already failed after switching to 108.61.171.121,
  even when basic proxy checks looked healthy.

Nothing was changed in:
  ${target_config}

If you still want to try a live cutover, rerun with:
  ALLOW_EXPERIMENTAL_SWITCH=1 ${repo_root}/ops/switch-to-new-vps.sh
EOF
  exit 3
fi

user_domain="gui/$(id -u)"
if launchctl print "${user_domain}/${launchctl_label}" >/dev/null 2>&1; then
  service_target="${user_domain}/${launchctl_label}"
elif launchctl print "system/${launchctl_label}" >/dev/null 2>&1; then
  service_target="system/${launchctl_label}"
else
  echo "Launchctl label not found: ${launchctl_label}" >&2
  exit 2
fi

if [[ -f "${target_config}" ]]; then
  cp "${target_config}" "${backup_config}"
  echo "Backed up current config to ${backup_config}"
fi

cp "${source_config}" "${target_config}"
echo "Switched active config to ${target_config}"

launchctl kickstart -k "${service_target}"
echo "Restarted ${service_target}"

if ! wait_for_local_proxy 8080; then
  echo "Live proxy did not become ready after restart, rolling back..." >&2
  restore_previous_config
  launchctl kickstart -k "${service_target}" >/dev/null 2>&1 || true
  exit 1
fi

if ! run_proxy_health_checks 8080 1080; then
  echo "Live health checks failed after switch, rolling back..." >&2
  restore_previous_config
  launchctl kickstart -k "${service_target}" >/dev/null 2>&1 || true
  exit 1
fi

cat <<EOF
Switch completed.

Active server:
  108.61.171.121

Rollback:
  LAUNCHCTL_LABEL=${launchctl_label} ${repo_root}/ops/rollback-to-baseline.sh
EOF
