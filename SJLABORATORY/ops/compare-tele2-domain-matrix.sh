#!/bin/bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
REPORT_GLOB="$ROOT_DIR/docs/tele2-domain-matrix-20*.md"
RUN_STAMP="$(date +%F-%H%M%S)"
REPORT_PATH_DEFAULT="$ROOT_DIR/docs/tele2-domain-matrix-compare-$RUN_STAMP.md"

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "$value"
}

pick_latest_two() {
  mapfile -t reports < <(ls -1 $REPORT_GLOB 2>/dev/null | sort)
  if (( ${#reports[@]} < 2 )); then
    echo "Need at least two historical Tele2 reports under $ROOT_DIR/docs" >&2
    exit 1
  fi
  BEFORE_REPORT="${reports[-2]}"
  AFTER_REPORT="${reports[-1]}"
}

if (( $# >= 2 )); then
  BEFORE_REPORT="$1"
  AFTER_REPORT="$2"
else
  pick_latest_two
fi

REPORT_PATH="${3:-$REPORT_PATH_DEFAULT}"

if [[ ! -f "$BEFORE_REPORT" ]]; then
  echo "Missing before-report: $BEFORE_REPORT" >&2
  exit 1
fi

if [[ ! -f "$AFTER_REPORT" ]]; then
  echo "Missing after-report: $AFTER_REPORT" >&2
  exit 1
fi

awk '
function trim(s) {
  gsub(/^[ \t]+|[ \t]+$/, "", s)
  return s
}

function score(outcome) {
  if (outcome == "http_up") return 3
  if (outcome == "tls_reset") return 2
  if (outcome == "tcp_only") return 1
  if (outcome == "no_ipv4") return -1
  return 0
}

function load(path, prefix,   line, parts, domain, category, outcome) {
  while ((getline line < path) > 0) {
    if (line !~ /^\|/) continue
    if (line ~ /^\| ---/) continue
    split(line, parts, "|")
    category = trim(parts[2])
    domain = trim(parts[3])
    outcome = trim(parts[11])
    if (category == "Category" || domain == "" || outcome == "") continue
    categories[domain] = category
    data[prefix, domain] = outcome
    seen[domain] = 1
  }
  close(path)
}

BEGIN {
  before_path = ARGV[1]
  after_path = ARGV[2]
  ARGV[1] = ""
  ARGV[2] = ""

  load(before_path, "before")
  load(after_path, "after")

  improved_count = 0
  regressed_count = 0
  unchanged_count = 0
  before_only_count = 0
  after_only_count = 0

  for (domain in seen) {
    before = data["before", domain]
    after = data["after", domain]

    if (before == "") {
      after_only[++after_only_count] = domain
      continue
    }
    if (after == "") {
      before_only[++before_only_count] = domain
      continue
    }

    if (score(after) > score(before)) {
      improved[++improved_count] = domain
      improved_from[domain] = before
      improved_to[domain] = after
    } else if (score(after) < score(before)) {
      regressed[++regressed_count] = domain
      regressed_from[domain] = before
      regressed_to[domain] = after
    } else {
      unchanged[++unchanged_count] = domain
      unchanged_state[domain] = after
    }
  }

  print "# Tele2 Domain Matrix Comparison"
  print ""
  print "- Before: `" before_path "`"
  print "- After: `" after_path "`"
  print ""
  print "## Summary"
  print ""
  print "- improved domains: `" improved_count "`"
  print "- regressed domains: `" regressed_count "`"
  print "- unchanged domains: `" unchanged_count "`"
  print "- before-only domains: `" before_only_count "`"
  print "- after-only domains: `" after_only_count "`"
  print ""

  print "## Improved"
  print ""
  if (improved_count == 0) {
    print "- none"
  } else {
    for (i = 1; i <= improved_count; i++) {
      domain = improved[i]
      print "- `" domain "`: `" improved_from[domain] "` -> `" improved_to[domain] "`"
    }
  }
  print ""

  print "## Regressed"
  print ""
  if (regressed_count == 0) {
    print "- none"
  } else {
    for (i = 1; i <= regressed_count; i++) {
      domain = regressed[i]
      print "- `" domain "`: `" regressed_from[domain] "` -> `" regressed_to[domain] "`"
    }
  }
  print ""

  print "## Unchanged"
  print ""
  if (unchanged_count == 0) {
    print "- none"
  } else {
    for (i = 1; i <= unchanged_count; i++) {
      domain = unchanged[i]
      print "- `" domain "`: `" unchanged_state[domain] "`"
    }
  }
  print ""

  print "## Before Only"
  print ""
  if (before_only_count == 0) {
    print "- none"
  } else {
    for (i = 1; i <= before_only_count; i++) {
      print "- `" before_only[i] "`"
    }
  }
  print ""

  print "## After Only"
  print ""
  if (after_only_count == 0) {
    print "- none"
  } else {
    for (i = 1; i <= after_only_count; i++) {
      print "- `" after_only[i] "`"
    }
  }
}
' "$BEFORE_REPORT" "$AFTER_REPORT" >"$REPORT_PATH"

echo "Comparison written to $REPORT_PATH"
