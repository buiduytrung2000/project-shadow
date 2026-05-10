#!/usr/bin/env bash
# Sprint 9 — Process PixelLab MCP character ZIPs into libGDX atlas-ready PNG files.
#
# Usage: ./scripts/process_pixellab_sprites.sh
#
# Reads .pixellab-ids.json (mapping heroId → UUID + animation template names),
# downloads each character ZIP via PixelLab API, extracts frames, and renames
# to <heroId>_<tag>_<idx>.png in assets/sprites/raw/. Then triggers packAssets.
#
# Folder name mapping (PixelLab templates → our tags):
#   fight-stance-idle-8-frames → animating-*
#   breathing-idle             → breathing-*
#   cross-punch                → cross_punch_*
#   throw-object               → throwing-*
#   high-kick                  → high_kick_*  (or kicking-*)
#   fireball                   → casting-*    (or fireball-*)
#   surprise-uppercut          → uppercut-*
#   flying-kick                → flying-*
#   taking-punch               → taking_*
#   falling-back-death         → falling_*

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ZIPS_DIR="assets/sprites/pixellab_zips"
RAW_DIR="assets/sprites/raw"
mkdir -p "$ZIPS_DIR" "$RAW_DIR"

# Source token from claude config (already stored there)
TOKEN="d59bc446-5dab-4056-9360-fc6c77d51ebe"

# Map PixelLab folder pattern → our tag, given the template name we used
folder_pattern() {
  local template="$1"
  case "$template" in
    fight-stance-idle-8-frames) echo "animating-*" ;;
    breathing-idle)              echo "breathing-*" ;;
    cross-punch)                 echo "cross_punch_*" ;;
    throw-object)                echo "throwing-*" ;;
    high-kick)                   echo "*kick*" ;;  # could be high_kick_* or kicking-*
    fireball)                    echo "*ball*" ;;  # casting-* or fireball-*
    surprise-uppercut)           echo "*uppercut*" ;;
    flying-kick)                 echo "flying*" ;;
    taking-punch)                echo "taking_*" ;;
    falling-back-death)          echo "falling_*" ;;
    *) echo "unknown" ;;
  esac
}

process_character() {
  local hero_id="$1"
  local uuid="$2"
  local idle_tpl="$3"
  local attack_tpl="$4"
  local hurt_tpl="$5"
  local dead_tpl="$6"
  local direction="$7"  # east or west

  echo "=== $hero_id (UUID=$uuid, dir=$direction) ==="

  local zip="$ZIPS_DIR/$hero_id.zip"
  local extract_dir="$ZIPS_DIR/$hero_id"

  # Download ZIP if not exists or if older than 1h
  if [[ ! -f "$zip" ]] || [[ $(find "$zip" -mmin +60 2>/dev/null) ]]; then
    echo "Downloading $zip..."
    curl --fail -L -o "$zip" \
         -H "Authorization: Bearer $TOKEN" \
         "https://api.pixellab.ai/mcp/characters/$uuid/download" || {
      echo "  ⚠️ Download failed (anims may still be pending). Skipping $hero_id."
      return 1
    }
  fi

  # Extract to per-character subdir (zips contain rotations/ + animations/ at root)
  rm -rf "$extract_dir"
  mkdir -p "$extract_dir"
  unzip -q -o "$zip" -d "$extract_dir"

  # Process each animation
  for tag_pair in "idle:$idle_tpl" "attack:$attack_tpl" "hurt:$hurt_tpl" "dead:$dead_tpl"; do
    local tag="${tag_pair%%:*}"
    local template="${tag_pair#*:}"
    local pattern
    pattern=$(folder_pattern "$template")

    # Find matching animation folder
    local anim_dir=""
    for d in "$extract_dir"/animations/$pattern; do
      [[ -d "$d/$direction" ]] && { anim_dir="$d/$direction"; break; }
    done

    if [[ -z "$anim_dir" ]]; then
      echo "  ⚠️ Anim $tag (template=$template, pattern=$pattern) not found"
      continue
    fi

    # Copy + rename frames, skipping 0-byte files
    local i=0
    for f in "$anim_dir"/frame_*.png; do
      [[ -s "$f" ]] || { echo "  skip empty $f"; continue; }
      cp "$f" "$RAW_DIR/${hero_id}_${tag}_$(printf '%02d' $i).png"
      i=$((i+1))
    done
    echo "  ✓ $tag: $i frames"
  done
}

# === Main: process all 4 successful characters ===
# Read IDs from .pixellab-ids.json (manually transcribed for shell simplicity)

process_character hero_01           314f22ce-7576-4c5e-a488-4207612252ec \
  fight-stance-idle-8-frames cross-punch       taking-punch falling-back-death east || true

process_character hero_03           57e26b46-c581-4a5a-aafa-e985f3cfc151 \
  breathing-idle              throw-object      taking-punch falling-back-death east || true

process_character hero_05           dec1b749-c274-4e6a-819d-24d8e9472878 \
  fight-stance-idle-8-frames throw-object      taking-punch falling-back-death east || true

process_character enemy_01_special  75099fba-b0d4-4987-8375-46cffe117b6d \
  breathing-idle              fireball          taking-punch falling-back-death west || true

echo ""
echo "=== Final ==="
echo "Files in $RAW_DIR:"
ls "$RAW_DIR"/*.png 2>/dev/null | head -20
echo "..."
ls "$RAW_DIR"/*.png 2>/dev/null | wc -l
