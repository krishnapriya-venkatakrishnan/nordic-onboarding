#!/usr/bin/env bash
# Creates or updates the project's monitoring setup from the files in infra/. Safe to re-run.
# Run by hand by an Owner: CI's deployer deliberately has no permission to change monitoring.
# Usage: infra/apply.sh PROJECT_ID
set -euo pipefail
project="${1:?usage: infra/apply.sh PROJECT_ID}"
cd "$(dirname "$0")"

# Log-based metrics: one file per metric, named after the metric.
for file in metrics/*.json; do
  name=$(basename "$file" .json)
  # A typo in a filter gives a metric that silently counts nothing, so check the syntax first:
  # Cloud Logging rejects a bad filter when reading with it.
  gcloud logging read "$(jq -r .filter "$file")" --project="$project" --freshness=1d --limit=1 >/dev/null
  if gcloud logging metrics describe "$name" --project="$project" >/dev/null 2>&1; then
    gcloud logging metrics update "$name" --project="$project" --config-from-file="$file"
  else
    gcloud logging metrics create "$name" --project="$project" --config-from-file="$file"
  fi
done
