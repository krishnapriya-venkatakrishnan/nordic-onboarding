#!/usr/bin/env bash
# Checks that the app serves exactly the given markets: each is listed by the API, and each
# market named as dark answers 404. Retries for a minute while a change takes effect.
# Usage: scripts/check-markets.sh BASE_URL LIVE [DARK]   (e.g. ... SE DK)
set -euo pipefail
base="${1:?usage: scripts/check-markets.sh BASE_URL LIVE [DARK]}"
live="${2:?usage: scripts/check-markets.sh BASE_URL LIVE [DARK]}"
dark="${3:-}"
sorted() { tr ',' '\n' <<<"$1" | sed '/^$/d' | sort | paste -sd, -; }
expected=$(sorted "$live")

for attempt in $(seq 1 12); do
  listed=$(curl -fsS --max-time 10 "$base/api/markets" | jq -r '[.[].code] | sort | join(",")') || listed=""
  [ "$listed" = "$expected" ] && break
  [ "$attempt" = 12 ] && { echo "FAIL: live markets are '$listed', expected '$expected'" >&2; exit 1; }
  sleep 5
done
echo "ok   live: $listed"

for code in $(tr ',' ' ' <<<"$dark"); do
  status=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "$base/api/markets/$code/onboarding")
  [ "$status" = 404 ] || { echo "FAIL: $code should be dark (404) but answered $status" >&2; exit 1; }
  echo "ok   dark: $code answers 404"
done
