#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

keystore_file="${script_dir}/route42-release.jks"
base64_file="${script_dir}/ROUTE42_KEYSTORE_BASE64.txt"
store_password_file="${script_dir}/ROUTE42_KEYSTORE_PASSWORD.txt"
key_alias_file="${script_dir}/ROUTE42_KEY_ALIAS.txt"
key_password_file="${script_dir}/ROUTE42_KEY_PASSWORD.txt"

read_single_line_file() {
  local file_path="$1"
  tr -d '\r\n' < "${file_path}"
}

require_file() {
  local file_path="$1"
  local label="$2"
  if [[ ! -f "${file_path}" ]]; then
    echo "Missing ${label}: ${file_path}" >&2
    exit 1
  fi
}

build_keystore_base64() {
  if [[ -f "${base64_file}" ]]; then
    read_single_line_file "${base64_file}"
    return
  fi

  require_file "${keystore_file}" "local keystore file"
  base64 < "${keystore_file}" | tr -d '\r\n'
}

require_file "${store_password_file}" "store password file"
require_file "${key_alias_file}" "key alias file"
require_file "${key_password_file}" "key password file"

keystore_base64="$(build_keystore_base64)"
store_password="$(read_single_line_file "${store_password_file}")"
key_alias="$(read_single_line_file "${key_alias_file}")"
key_password="$(read_single_line_file "${key_password_file}")"

cat <<EOF
1. ROUTE42_KEYSTORE_BASE64
${keystore_base64}

2. ROUTE42_KEYSTORE_PASSWORD
${store_password}

3. ROUTE42_KEY_ALIAS
${key_alias}

4. ROUTE42_KEY_PASSWORD
${key_password}
EOF
