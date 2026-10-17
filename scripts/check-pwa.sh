#!/usr/bin/env bash
# Checks that a running copy of the app can be installed as a PWA: the page links a manifest,
# the manifest has what browsers ask for, its icons are images, and the service worker and its
# file list are served. Usage: scripts/check-pwa.sh BASE_URL
set -euo pipefail
base="${1:?usage: scripts/check-pwa.sh BASE_URL}"
fetch() { curl -fsS --max-time 10 "$base$1"; }
type_of() { curl -fsS -o /dev/null -w '%{content_type}' --max-time 10 "$base$1"; }
fail() { echo "FAIL $1" >&2; exit 1; }

page=$(fetch /) || fail "/: not served"
grep -q '<link rel="manifest" href="manifest.webmanifest"' <<<"$page" || fail "/: no manifest link"
manifest=$(fetch /manifest.webmanifest) || fail "/manifest.webmanifest: not served"
type=$(type_of /manifest.webmanifest)
[ "$type" = application/manifest+json ] || fail "/manifest.webmanifest: served as $type"
jq -e '.name and .short_name and .start_url and .display == "standalone"' <<<"$manifest" >/dev/null ||
  fail "manifest: needs name, short_name, start_url and display standalone"
# Browsers want a 192 and a 512 px icon to install, and Android a maskable one for its home screen.
for want in '192x192 any' '512x512 any' '512x512 maskable'; do
  read -r size purpose <<<"$want"
  src=$(jq -r --arg s "$size" --arg p "$purpose" \
    '[.icons[] | select(.sizes == $s and .purpose == $p)][0].src // empty' <<<"$manifest")
  [ -n "$src" ] || fail "manifest: no $size $purpose icon"
  type=$(type_of "/$src") || fail "/$src: not served"
  [ "$type" = image/png ] || fail "/$src: served as $type"
done
echo "ok   manifest and icons"

type_of /ngsw-worker.js | grep -q javascript || fail "/ngsw-worker.js: not served as JavaScript"
files=$(fetch /ngsw.json) || fail "/ngsw.json: not served"
jq -e '.index == "/index.html"' <<<"$files" >/dev/null || fail "/ngsw.json: wrong index"
echo "ok   service worker"
