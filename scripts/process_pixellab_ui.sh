#!/usr/bin/env bash
# Sprint 9+ — Download PixelLab UI component objects into atlas-ready PNG files.
#
# Usage: ./scripts/process_pixellab_ui.sh
#
# Reads assets/ui/.pixellab-ui-ids.json (asset_id → UUID map), downloads each
# object's single PNG via the MCP `/objects/{uuid}/download` endpoint, and saves
# to assets/ui/raw/<asset_id>.png. Then triggers `./gradlew packUIComponents`.
#
# Unlike characters (multi-anim ZIPs), UI objects are single-frame PNGs:
#  - Either the download endpoint returns the raw PNG directly, or
#  - Returns a ZIP with rotations/unknown.png inside.
# Script handles both cases.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

IDS_FILE="assets/ui/.pixellab-ui-ids.json"
RAW_DIR="assets/ui/raw"
TMP_DIR="$RAW_DIR/.tmp"
mkdir -p "$RAW_DIR" "$TMP_DIR"

# Reuse the same bearer token as the character script
TOKEN="d59bc446-5dab-4056-9360-fc6c77d51ebe"

# Need jq for JSON parsing; fall back to a simple grep+sed parser if absent.
parse_id() {
  local key="$1"
  if command -v jq >/dev/null 2>&1; then
    jq -r --arg k "$key" '
      .buttons[$k] // .panels[$k] // .icons[$k]
        // .node_icons[$k] // .status_icons[$k] // .frames[$k]
        // .effect_status_icons[$k]
        // empty
    ' "$IDS_FILE"
  else
    # Quick-and-dirty: grep "key": "value" — fine for flat string values.
    grep -E "\"$key\"[[:space:]]*:[[:space:]]*\"" "$IDS_FILE" \
      | head -1 \
      | sed -E 's/.*"([0-9a-f-]{36})".*/\1/'
  fi
}

download_object() {
  local asset_id="$1"
  local uuid
  uuid="$(parse_id "$asset_id")"

  if [[ -z "$uuid" || "$uuid" == "null" ]]; then
    echo "  ⚠️  $asset_id: no UUID in $IDS_FILE — skipping"
    return 0
  fi

  local out="$RAW_DIR/${asset_id}.png"
  if [[ -s "$out" ]]; then
    echo "  ✓ $asset_id: already downloaded ($out)"
    return 0
  fi

  local tmp_file="$TMP_DIR/${asset_id}.bin"
  echo "  ↓ $asset_id ($uuid)..."

  curl --fail --silent -L -o "$tmp_file" \
       -H "Authorization: Bearer $TOKEN" \
       "https://api.pixellab.ai/mcp/objects/$uuid/download" || {
    echo "  ❌ download failed for $asset_id"
    return 1
  }

  # Detect format: PNG starts with 0x89 0x50 0x4E 0x47; ZIP starts with PK
  local magic
  magic="$(head -c 2 "$tmp_file" | od -An -c | tr -d ' \n')"
  if [[ "$magic" == "PK" ]]; then
    # ZIP — extract rotations/unknown.png (or first PNG)
    local extract="$TMP_DIR/${asset_id}_extracted"
    rm -rf "$extract" && mkdir -p "$extract"
    unzip -q -o "$tmp_file" -d "$extract"
    local png
    png="$(find "$extract" -name '*.png' | head -1)"
    if [[ -z "$png" ]]; then
      echo "  ❌ no PNG found inside ZIP for $asset_id"
      return 1
    fi
    cp "$png" "$out"
  else
    # Assume PNG (or other image format) — copy directly
    cp "$tmp_file" "$out"
  fi
  echo "  ✓ $asset_id → $out"
}

ASSETS=(
  # Round 1 — base UI pack
  ui_btn_up ui_btn_down ui_btn_over ui_btn_disabled
  ui_panel_main ui_panel_tooltip ui_popup_bg
  ui_icon_stagecoach ui_icon_guild ui_icon_survivalist ui_icon_caretaker
  ui_icon_gold ui_icon_heirloom ui_icon_settings ui_icon_close
  # Round 2 — stage map node icons
  ui_node_combat ui_node_elite ui_node_miniboss ui_node_boss
  ui_node_event ui_node_rest ui_node_reward
  # Round 2 — combat status icons + decorative frames
  ui_icon_hp ui_icon_stress ui_icon_shield
  ui_frame_portrait ui_banner_title
  # B2 — effect status icons (12 icons, PixelLab UUIDs pending)
  status_heal status_burn status_taunt status_buff_dmg status_shield
  status_meditate status_bleed status_poison status_stun status_slow
  status_disease status_affliction
)

echo "=== PixelLab UI download ==="
for id in "${ASSETS[@]}"; do
  download_object "$id" || true
done

# Clean tmp
rm -rf "$TMP_DIR"

echo ""
echo "=== Final ==="
echo "Files in $RAW_DIR:"
ls "$RAW_DIR"/*.png 2>/dev/null || echo "  (none)"
count=$(ls "$RAW_DIR"/*.png 2>/dev/null | wc -l)
echo "Total: $count / ${#ASSETS[@]}"

echo ""
echo "Next: ./gradlew packUIComponents"
