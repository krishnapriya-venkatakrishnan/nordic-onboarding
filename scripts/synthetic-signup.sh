#!/usr/bin/env bash
# A synthetic sign-up in every live market, the way a customer would do it: eID login, ID number
# check, application. Fails if any market can't open an account.
# It uses each market's own demo ID number and made-up details, and tags its requests so that
# dashboards and alerts leave them out. Usage: scripts/synthetic-signup.sh BASE_URL
set -euo pipefail
base="${1:?usage: scripts/synthetic-signup.sh BASE_URL}"
tag="X-Synthetic-Check: true"

# Reads can safely be retried; a submission can't (a retry would reuse a spent eID session).
get() { curl -fsS --retry 3 --retry-all-errors --max-time 15 -H "$tag" "$base$1"; }
post() { curl -fsS --max-time 15 -H "$tag" -H "Content-Type: application/json" -d "$2" "$base$1"; }

markets=$(get /api/markets | jq -r '.[].code')
[ -n "$markets" ] || { echo "FAIL: no live markets" >&2; exit 1; }

for code in $markets; do
  config=$(get "/api/markets/$code/onboarding")
  id=$(jq -r .nationalId.example <<<"$config")
  product=$(jq -r '.products[0].id' <<<"$config")
  confirmations=$(jq -c --arg p "$product" \
    '[.disclosures[] | select(.required and (.productId == null or .productId == $p)) | .id]' <<<"$config")

  session=$(post "/api/markets/$code/identity/sessions" '{}' | jq -r .sessionId)
  post "/api/markets/$code/national-id/check" "$(jq -n --arg v "$id" '{value: $v}')" | jq -e .valid >/dev/null ||
    { echo "FAIL $code: the demo ID number was rejected" >&2; exit 1; }

  request=$(jq -n --arg s "$session" --arg id "$id" --arg p "$product" --argjson c "$confirmations" \
    '{identitySessionId: $s, nationalId: $id, fullName: "Synthetic Check",
      email: "synthetic-check@example.com", productId: $p, acceptedDisclosures: $c}')
  result=$(post "/api/markets/$code/applications" "$request") ||
    { echo "FAIL $code: the application was not accepted" >&2; exit 1; }
  echo "ok   $code: $(jq -r .applicationId <<<"$result") opened ($product, $(jq -r .currency <<<"$result"))"
done
