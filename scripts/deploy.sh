#!/usr/bin/env bash
# Builds the image from source with Cloud Build and deploys it to Cloud Run.
# CI runs this on every green main; it was also used by hand for the very first deploy.
# Usage: scripts/deploy.sh PROJECT_ID
set -euo pipefail
project="${1:?usage: scripts/deploy.sh PROJECT_ID}"
region=europe-north1
service=nordic-onboarding

# min-instances=1: always warm, so nobody waits for Spring Boot to start.
# max-instances=1: eID sessions live in memory; a second instance wouldn't know them. Also caps cost.
# MARKETS_ENABLED is not set here on purpose: a deploy keeps the service's current value, so a
# market switched off by hand stays off. The default (SE,DK) comes from application.yml.
gcloud run deploy "$service" \
  --project="$project" \
  --region="$region" \
  --source=. \
  --build-service-account="projects/$project/serviceAccounts/nordic-build@$project.iam.gserviceaccount.com" \
  --service-account="nordic-run@$project.iam.gserviceaccount.com" \
  --min-instances=1 \
  --max-instances=1 \
  --quiet
