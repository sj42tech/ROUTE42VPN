#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

launchctl_label="${LAUNCHCTL_LABEL:-local.xray}"
launchctl_plist="${LAUNCHCTL_PLIST:-$HOME/Library/LaunchAgents/xray.plist}"
network_service="${NETWORK_SERVICE:-}"
backup_dir="${repo_root}/secrets/PROXY/xray/backups"
timestamp="$(date +%Y%m%d-%H%M%S)"
backup_file="${backup_dir}/wifi-proxy-before-stop-${timestamp}.txt"
service_target=""

require_command() {
  local name="$1"
  if ! command -v "${name}" >/dev/null 2>&1; then
    echo "Missing required command: ${name}" >&2
    exit 1
  fi
}

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

discover_wifi_device() {
  networksetup -listallhardwareports | awk '
    $0 == "Hardware Port: Wi-Fi" {
      getline
      if ($1 == "Device:") {
        print $2
        exit
      }
    }
  '
}

discover_wifi_service() {
  local wifi_device="$1"
  networksetup -listnetworkserviceorder | awk -v device="${wifi_device}" '
    /^\([0-9]+\)/ {
      current = $0
      sub(/^\([0-9]+\) /, "", current)
    }
    $0 ~ "\\(Hardware Port: .*Device: " device "\\)" {
      print current
      exit
    }
  '
}

write_proxy_backup() {
  local path="$1"
  local service_name="$2"
  {
    echo "timestamp=${timestamp}"
    echo "network_service=${service_name}"
    echo "launchctl_label=${launchctl_label}"
    echo "launchctl_plist=${launchctl_plist}"
    echo
    echo "### web proxy"
    networksetup -getwebproxy "${service_name}" || true
    echo
    echo "### secure web proxy"
    networksetup -getsecurewebproxy "${service_name}" || true
    echo
    echo "### socks proxy"
    networksetup -getsocksfirewallproxy "${service_name}" || true
    echo
    echo "### auto proxy url"
    networksetup -getautoproxyurl "${service_name}" || true
    echo
    echo "### bypass domains"
    networksetup -getproxybypassdomains "${service_name}" || true
    echo
    echo "### restart commands"
    echo "launchctl bootstrap gui/$(id -u) \"${launchctl_plist}\""
    echo "launchctl kickstart -k gui/$(id -u)/${launchctl_label}"
  } > "${path}"
}

disable_wifi_proxies() {
  local service_name="$1"
  networksetup -setwebproxystate "${service_name}" off
  networksetup -setsecurewebproxystate "${service_name}" off
  networksetup -setsocksfirewallproxystate "${service_name}" off
  networksetup -setautoproxystate "${service_name}" off
  networksetup -setproxyautodiscovery "${service_name}" off
}

stop_launch_agent() {
  if ! resolve_service_target; then
    echo "Launchctl label not found: ${launchctl_label}"
    echo "Wi-Fi proxies were still disabled."
    return 0
  fi

  if launchctl bootout "${service_target}" >/dev/null 2>&1; then
    echo "Stopped launchctl service: ${service_target}"
    return 0
  fi

  echo "Failed to stop ${service_target} via launchctl bootout." >&2
  exit 1
}

require_command networksetup
require_command launchctl
require_command awk

mkdir -p "${backup_dir}"

if [[ -z "${network_service}" ]]; then
  wifi_device="$(discover_wifi_device)"
  if [[ -z "${wifi_device}" ]]; then
    echo "Could not find Wi-Fi hardware port." >&2
    exit 1
  fi

  network_service="$(discover_wifi_service "${wifi_device}")"
fi

if [[ -z "${network_service}" ]]; then
  echo "Could not resolve the Wi-Fi network service name." >&2
  echo "Set NETWORK_SERVICE explicitly, for example: NETWORK_SERVICE='Wi-Fi' $0" >&2
  exit 1
fi

write_proxy_backup "${backup_file}" "${network_service}"
echo "Saved current Wi-Fi proxy settings to ${backup_file}"

echo "Disabling system proxies for network service: ${network_service}"
disable_wifi_proxies "${network_service}"

echo "Stopping local tunnel service..."
stop_launch_agent

cat <<EOF
Tunnel stop completed.

What changed:
  - Wi-Fi system web proxy disabled
  - Wi-Fi system secure web proxy disabled
  - Wi-Fi system SOCKS proxy disabled
  - Wi-Fi auto proxy disabled
  - local Xray launch agent stopped if it was loaded

Manual restart later:
  launchctl bootstrap gui/$(id -u) "${launchctl_plist}"
  launchctl kickstart -k gui/$(id -u)/${launchctl_label}

Proxy backup:
  ${backup_file}
EOF
