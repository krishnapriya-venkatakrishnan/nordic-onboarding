#!/usr/bin/env bash
# Safe release to Cloud Run: the new revision gets no traffic until its own tagged URL passes
# the smoke test. If it fails, customers stay on the current revision.
# CI runs this on every green main. Usage: scripts/deploy.sh PROJECT_ID
set -euo pipefail
project="${1:?usage: scripts/deploy.sh PROJECT_ID}"
region=europe-north1
service=nordic-onboarding
smoke_test="${SMOKE_TEST:-scripts/smoke-test.sh}" # swappable, so deploy.test.sh can check the logic
where=(--project="$project" --region="$region" --quiet)

# --min=1 keeps one warm instance for the service, on whichever revision serves traffic. The
#   per-revision minimum is cleared: it would keep every tagged candidate warm (and billed).
# --max-instances=1 per revision: eID sessions live in memory, so one instance must serve them.
#   Per revision, not per service, so a candidate can start next to the live one.
# MARKETS_ENABLED is not set here on purpose: a deploy keeps the service's current value, so a
#   market switched off by hand stays off. The default (SE,DK) comes from application.yml.
settings=(
  --source=.
  --build-service-account="projects/$project/serviceAccounts/nordic-build@$project.iam.gserviceaccount.com"
  --service-account="nordic-run@$project.iam.gserviceaccount.com"
  --min=1
  --min-instances=default
  --max-instances=1
)

# A safety net under .gcloudignore: never upload a credentials file with the source.
if gcloud meta list-files-for-upload . | grep -qE '(^|/)gha-creds-[^/]*\.json$'; then
  echo "Refusing to deploy: a gha-creds-*.json credentials file would be uploaded." >&2
  exit 1
fi

# --no-traffic can't create a service, so a first-ever deploy goes live straight away.
if ! gcloud run services describe "$service" "${where[@]}" >/dev/null 2>&1; then
  gcloud run deploy "$service" "${where[@]}" "${settings[@]}"
  exit 0
fi

# 1. Deploy the new revision with no traffic, reachable only at its own tagged URL.
gcloud run deploy "$service" "${where[@]}" "${settings[@]}" --no-traffic --tag=candidate
revision=$(gcloud run services describe "$service" "${where[@]}" \
  --format='value(status.latestCreatedRevisionName)')
url=$(gcloud run services describe "$service" "${where[@]}" --format=json |
  jq -r '.status.traffic[] | select(.tag == "candidate") | .url')
echo "Candidate $revision is at $url"

# 2. Smoke-test the candidate before any customer reaches it.
if ! "$smoke_test" "$url"; then
  echo "Smoke test failed: $revision gets no traffic; customers stay on the current revision." >&2
  exit 1
fi

# 3. Shift all traffic, and drop the tag so this revision isn't kept warm once it's replaced.
gcloud run services update-traffic "$service" "${where[@]}" \
  --to-revisions="$revision=100" --remove-tags=candidate
echo "$revision now serves 100% of traffic"
