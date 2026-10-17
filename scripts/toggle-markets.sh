#!/usr/bin/env bash
# The kill switch: sets which markets are live on Cloud Run, with no build and no release.
# Cloud Run starts a revision of the same image with the new MARKETS_ENABLED; this then moves
# all traffic to it. A market left out answers 404 from then on, until it is switched back on.
# Usage: scripts/toggle-markets.sh PROJECT_ID MARKETS   (e.g. SE or SE,DK)
set -euo pipefail
project="${1:?usage: scripts/toggle-markets.sh PROJECT_ID MARKETS}"
markets="${2:?usage: scripts/toggle-markets.sh PROJECT_ID MARKETS}"

# Checked before touching anything: a malformed list would switch markets off by accident.
if [[ ! "$markets" =~ ^[A-Z]{2}(,[A-Z]{2})*$ ]]; then
  echo "Refusing: markets must look like SE or SE,DK, got '$markets'" >&2
  exit 1
fi

where=(--project="$project" --region=europe-north1 --quiet)

# ^@^ makes '@' the separator between variables, so the comma inside SE,DK survives.
gcloud run services update nordic-onboarding "${where[@]}" --update-env-vars="^@^MARKETS_ENABLED=$markets"
revision=$(gcloud run services describe nordic-onboarding "${where[@]}" \
  --format='value(status.latestCreatedRevisionName)')

# Releases pin traffic to one named revision, so point it at the new one explicitly.
gcloud run services update-traffic nordic-onboarding "${where[@]}" --to-revisions="$revision=100"
echo "$revision now serves 100% of traffic, with MARKETS_ENABLED=$markets"
