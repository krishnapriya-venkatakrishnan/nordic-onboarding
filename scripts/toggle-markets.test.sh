#!/usr/bin/env bash
# Tests the kill switch's logic without Google Cloud: a fake gcloud records every call.
# Usage: scripts/toggle-markets.test.sh
set -euo pipefail
cd "$(dirname "$0")/.."
fake=$(mktemp -d)
trap 'rm -rf "$fake"' EXIT

cat >"$fake/gcloud" <<'EOF'
#!/usr/bin/env bash
echo "gcloud $*" >>"$CALLS"
case "$*" in
  *"services describe"*"latestCreatedRevisionName"*) echo svc-00007 ;;
esac
EOF
chmod +x "$fake/gcloud"

toggle() {
  : >"$fake/calls"
  CALLS="$fake/calls" PATH="$fake:$PATH" scripts/toggle-markets.sh test-project "$1" >/dev/null 2>&1
}
called() { grep -q -- "$1" "$fake/calls"; }
fail() { echo "FAIL: $1"; cat "$fake/calls"; exit 1; }

toggle SE || fail "switching to SE only should work"
called "run services update nordic-onboarding .*--update-env-vars=^@^MARKETS_ENABLED=SE$" ||
  fail "sets MARKETS_ENABLED=SE"
called "update-traffic nordic-onboarding .*--to-revisions=svc-00007=100" || fail "moves traffic to the new revision"
echo "ok   switching Denmark off sets MARKETS_ENABLED=SE and moves all traffic to the new revision"

toggle SE,DK || fail "switching both on should work"
called "--update-env-vars=^@^MARKETS_ENABLED=SE,DK$" || fail "keeps the comma inside SE,DK"
echo "ok   both markets on: the comma in SE,DK survives gcloud's parsing"

for bad in "" "se" "SE DK" "SE,,DK" "SE;rm" "SWE"; do
  if toggle "$bad"; then fail "should refuse '$bad'"; fi
  [ -s "$fake/calls" ] && fail "touched Cloud Run for bad input '$bad'"
done
echo "ok   badly formed markets are refused before anything changes"
