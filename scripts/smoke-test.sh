#!/usr/bin/env bash
# Checks that a running copy of the app answers: health, the page, and the API.
# Works for a local container and for a deployed URL. Usage: scripts/smoke-test.sh BASE_URL
set -euo pipefail
base="${1:?usage: scripts/smoke-test.sh BASE_URL}"

check() {
  local path=$1 expect=$2 body
  # Retries cover a server that is still starting up.
  if ! body=$(curl -fsS --retry 20 --retry-all-errors --retry-delay 2 --max-time 10 "$base$path"); then
    echo "FAIL $path: no successful answer" >&2
    exit 1
  fi
  if ! grep -q "$expect" <<<"$body"; then
    echo "FAIL $path: expected to find $expect" >&2
    exit 1
  fi
  echo "ok   $path"
}

check /actuator/health '"status":"UP"'
check / '<app-root>'
# Market-agnostic on purpose: whichever markets are enabled, at least one must be listed.
check /api/markets '"code":'
