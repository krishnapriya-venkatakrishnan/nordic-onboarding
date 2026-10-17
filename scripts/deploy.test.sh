#!/usr/bin/env bash
# Tests deploy.sh's release logic without Google Cloud: a fake gcloud records every call, and a
# fake smoke test passes or fails on demand. Usage: scripts/deploy.test.sh
set -euo pipefail
cd "$(dirname "$0")/.."
fake=$(mktemp -d)
trap 'rm -rf "$fake"' EXIT

cat >"$fake/gcloud" <<'EOF'
#!/usr/bin/env bash
echo "gcloud $*" >>"$CALLS"
case "$*" in
  *"meta list-files-for-upload"*)
    printf 'Dockerfile\nbackend/pom.xml\n'
    [ "${UPLOAD_CREDS:-no}" = yes ] && echo gha-creds-1234abcd.json
    true ;;
  *"services describe"*"--format=json"*)
    echo '{"status":{"traffic":[{"percent":100,"revisionName":"svc-00001"},
           {"tag":"candidate","revisionName":"svc-00002","url":"https://candidate---svc.run.app"}]}}' ;;
  *"services describe"*"latestCreatedRevisionName"*) echo svc-00002 ;;
  *"services describe"*) [ "$SERVICE_EXISTS" = yes ] ;;
esac
EOF
printf '#!/usr/bin/env bash\necho "smoke $1" >>"$CALLS"\n' >"$fake/smoke-pass"
printf '#!/usr/bin/env bash\necho "smoke $1" >>"$CALLS"\nexit 1\n' >"$fake/smoke-fail"
chmod +x "$fake"/*

# Runs deploy.sh with the fakes; returns its exit code.
release() {
  : >"$fake/calls"
  CALLS="$fake/calls" SERVICE_EXISTS="$1" SMOKE_TEST="$fake/$2" PATH="$fake:$PATH" \
    scripts/deploy.sh test-project >/dev/null 2>&1
}
called() { grep -q -- "$1" "$fake/calls"; }
fail() { echo "FAIL: $1"; cat "$fake/calls"; exit 1; }

release yes smoke-pass || fail "a passing candidate should release"
called "run deploy nordic-onboarding .*--no-traffic --tag=candidate" || fail "deploys with no traffic"
called "smoke https://candidate---svc.run.app" || fail "smoke-tests the candidate's own URL"
called "update-traffic nordic-onboarding .*--to-revisions=svc-00002=100" || fail "promotes the candidate"
echo "ok   a passing candidate is tested at its tagged URL, then gets 100% of traffic"

if release yes smoke-fail; then fail "a failing candidate should stop the release"; fi
called "update-traffic" && fail "a failing candidate must get no traffic"
echo "ok   a failing candidate gets no traffic, and the release fails"

release no smoke-pass || fail "a first deploy should succeed"
called "--no-traffic" && fail "a first deploy can't use --no-traffic"
echo "ok   a first-ever deploy creates the service directly"

if UPLOAD_CREDS=yes release yes smoke-pass; then fail "credentials in the upload should stop the release"; fi
called "run deploy" && fail "nothing may be deployed when credentials would be uploaded"
echo "ok   a release is refused if a credentials file would be uploaded"

# The real ignore rules: a credentials file in the checkout is neither committed nor uploaded.
git check-ignore -q gha-creds-1234abcd.json || fail ".gitignore must cover gha-creds-*.json"
grep -qx '#!include:.gitignore' .gcloudignore || fail ".gcloudignore must include .gitignore"
echo "ok   gha-creds-*.json is ignored by git and, through .gitignore, by the deploy upload"
