#!/usr/bin/env bash
# Download SFX from Freesound.org API v2 based on .sfx-manifest.json
# Usage: FREESOUND_API_KEY=<key> ./scripts/fetch_sfx.sh
# Or:    ./scripts/fetch_sfx.sh  (reads key from ~/.freesound_key)
# Get a free API key at: https://freesound.org/apiv2/apply/

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
MANIFEST="$ROOT_DIR/assets/audio/.sfx-manifest.json"
OUT_DIR="$ROOT_DIR/assets/audio/sfx"
IDS_FILE="$ROOT_DIR/assets/audio/.freesound-ids.json"
API_BASE="https://freesound.org/apiv2"

# Resolve API key
if [[ -z "${FREESOUND_API_KEY:-}" ]]; then
  KEY_FILE="$HOME/.freesound_key"
  if [[ ! -f "$KEY_FILE" ]]; then
    echo "ERROR: set FREESOUND_API_KEY env var or create $KEY_FILE with your key"
    echo "Get a free key at: https://freesound.org/apiv2/apply/"
    exit 1
  fi
  FREESOUND_API_KEY="$(cat "$KEY_FILE" | tr -d '[:space:]')"
fi

mkdir -p "$OUT_DIR"

# Load existing IDs file or start fresh
if [[ -f "$IDS_FILE" ]]; then
  IDS_JSON="$(cat "$IDS_FILE")"
else
  IDS_JSON="{}"
fi

echo "=== Project Shadow SFX Fetcher ==="
echo "Manifest: $MANIFEST"
echo "Output:   $OUT_DIR"
echo ""

# Parse manifest and process each SFX entry
mapfile -t SFX_IDS < <(jq -r '.sfx | keys[]' "$MANIFEST")

for SFX_ID in "${SFX_IDS[@]}"; do
  OUT_FILE="$OUT_DIR/$SFX_ID.ogg"

  if [[ -f "$OUT_FILE" ]]; then
    echo "[$SFX_ID] skipped (already exists)"
    continue
  fi

  QUERY="$(jq -r ".sfx[\"$SFX_ID\"].query" "$MANIFEST")"
  MIN_DUR="$(jq -r ".sfx[\"$SFX_ID\"].min_duration" "$MANIFEST")"
  MAX_DUR="$(jq -r ".sfx[\"$SFX_ID\"].max_duration" "$MANIFEST")"

  echo -n "[$SFX_ID] searching: \"$QUERY\" ... "

  # Search Freesound for top-rated CC0 or Attribution result within duration range
  ENCODED_QUERY="$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))" "$QUERY")"
  SEARCH_URL="${API_BASE}/search/text/?query=${ENCODED_QUERY}&filter=license:(\"Creative+Commons+0\"+OR+\"Attribution\")&fields=id,name,previews,duration&sort=rating_desc&page_size=10&token=${FREESOUND_API_KEY}"

  RESPONSE="$(curl -sf "$SEARCH_URL" 2>/dev/null || echo '{}')"
  COUNT="$(echo "$RESPONSE" | jq -r '.count // 0')"

  if [[ "$COUNT" == "0" ]] || [[ "$COUNT" == "null" ]]; then
    echo "WARN: no results found, skipping"
    continue
  fi

  # Pick first result within duration range
  SOUND_ID="$(echo "$RESPONSE" | jq -r \
    --argjson min "$MIN_DUR" --argjson max "$MAX_DUR" \
    '.results[] | select(.duration >= $min and .duration <= $max) | .id' | head -1)"

  if [[ -z "$SOUND_ID" ]]; then
    # Fall back to top result ignoring duration constraint
    SOUND_ID="$(echo "$RESPONSE" | jq -r '.results[0].id')"
    echo -n "(duration fallback) "
  fi

  PREVIEW_URL="$(echo "$RESPONSE" | jq -r \
    --arg id "$SOUND_ID" \
    '.results[] | select(.id == ($id | tonumber)) | .previews["preview-hq-ogg"]' 2>/dev/null || echo "")"

  if [[ -z "$PREVIEW_URL" ]]; then
    echo "WARN: no preview URL for sound $SOUND_ID, skipping"
    continue
  fi

  curl -sf -o "$OUT_FILE" "$PREVIEW_URL"
  echo "downloaded (freesound id: $SOUND_ID)"

  # Update IDs tracking
  IDS_JSON="$(echo "$IDS_JSON" | jq --arg k "$SFX_ID" --argjson v "$SOUND_ID" '. + {($k): $v}')"
done

# Write updated IDs file
echo "$IDS_JSON" | jq '.' > "$IDS_FILE"

echo ""
echo "=== Done ==="
echo "Files in $OUT_DIR:"
ls -1 "$OUT_DIR"/*.ogg 2>/dev/null | wc -l | xargs -I{} echo "  {} OGG files"
echo "IDs saved to $IDS_FILE"
