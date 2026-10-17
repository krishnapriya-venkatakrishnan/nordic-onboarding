#!/usr/bin/env bash
# Creates or updates the project's monitoring setup from the files in infra/. Safe to re-run.
# Run by hand by an Owner: CI's deployer deliberately has no permission to change monitoring.
# The alert email is an argument rather than a file, because the repo is public.
# Usage: infra/apply.sh PROJECT_ID ALERT_EMAIL
set -euo pipefail
project="${1:?usage: infra/apply.sh PROJECT_ID ALERT_EMAIL}"
email="${2:?usage: infra/apply.sh PROJECT_ID ALERT_EMAIL}"
cd "$(dirname "$0")"
tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT

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

# Dashboards: one file per dashboard, matched to the live one by its title. The repo's version
# always wins: an update must carry the live etag, so take it just before replacing.
for file in dashboards/*.json; do
  title=$(jq -r .displayName "$file")
  existing=$(gcloud monitoring dashboards list --project="$project" \
    --filter="displayName=\"$title\"" --format="value(name)")
  if [ -n "$existing" ]; then
    etag=$(gcloud monitoring dashboards describe "$existing" --project="$project" --format="value(etag)")
    jq --arg etag "$etag" '. + {etag: $etag}' "$file" >"$tmp"
    gcloud monitoring dashboards update "$existing" --project="$project" --config-from-file="$tmp" >/dev/null
    echo "Updated dashboard: $title"
  else
    gcloud monitoring dashboards create --project="$project" --config-from-file="$file" >/dev/null
    echo "Created dashboard: $title"
  fi
done

# Email channels and uptime checks have no GA gcloud commands, so these use the Monitoring API.
api=https://monitoring.googleapis.com/v3
token=$(gcloud auth print-access-token)
call() {
  curl -sS --fail-with-body -H "Authorization: Bearer $token" -H "x-goog-user-project: $project" \
    -H "Content-Type: application/json" "$@"
}

# The email channel alerts are sent to, found by its address.
channel=$(call "$api/projects/$project/notificationChannels" | jq -r --arg email "$email" \
  '[.notificationChannels[]? | select(.type == "email" and .labels.email_address == $email)][0].name // empty')
if [ -z "$channel" ]; then
  channel=$(jq -n --arg email "$email" \
    '{type: "email", displayName: "Nordic Onboarding alerts", labels: {email_address: $email}}' |
    call -X POST "$api/projects/$project/notificationChannels" -d @- | jq -r .name)
fi
echo "Alerts go to channel $channel"

# The uptime check, pointed at the live service's own address.
host=$(gcloud run services describe nordic-onboarding --project="$project" --region=europe-north1 \
  --format='value(status.url)' | sed 's|^https://||')
title=$(jq -r .displayName uptime/health.json)
check=$(call "$api/projects/$project/uptimeCheckConfigs" | jq -r --arg title "$title" \
  '[.uptimeCheckConfigs[]? | select(.displayName == $title)][0].name // empty')
if [ -n "$check" ]; then
  call -X PATCH "$api/$check?updateMask=displayName,httpCheck,contentMatchers,period,timeout,selectedRegions" \
    -d @uptime/health.json >/dev/null
  echo "Updated uptime check $check"
else
  check=$(jq --arg project "$project" --arg host "$host" \
    '. + {monitoredResource: {type: "uptime_url", labels: {project_id: $project, host: $host}}}' \
    uptime/health.json | call -X POST "$api/projects/$project/uptimeCheckConfigs" -d @- | jq -r .name)
  echo "Created uptime check $check for $host"
fi

# Alert policies, matched by title. Each one notifies the email channel; __CHECK_ID__ in a
# filter becomes the uptime check above.
for file in alerts/*.json; do
  title=$(jq -r .displayName "$file")
  jq --arg channel "$channel" --arg id "${check##*/}" \
    '.notificationChannels = [$channel] | (.. | strings) |= gsub("__CHECK_ID__"; $id)' "$file" >"$tmp"
  existing=$(gcloud monitoring policies list --project="$project" \
    --filter="displayName=\"$title\"" --format="value(name)")
  if [ -n "$existing" ]; then
    gcloud monitoring policies update "$existing" --project="$project" --policy-from-file="$tmp" >/dev/null
    echo "Updated alert policy: $title"
  else
    gcloud monitoring policies create --project="$project" --policy-from-file="$tmp" >/dev/null 2>&1
    echo "Created alert policy: $title"
  fi
done
