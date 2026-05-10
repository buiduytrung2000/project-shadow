# Sprite Sheet Authoring Guide for Project Shadow

This guide walks you through creating placeholder pixel-art combatant sprites in
**Aseprite** (or its free fork **LibreSprite**), exporting them, and packing them
into a `combatants.atlas` file the game loads at runtime.

The game runs **without sprites** (falls back to colored rectangles) — sprites are
purely visual polish. Drop them in any time.

---

## Tools

### Aseprite (recommended, paid)
- $19.99 on [Steam](https://store.steampowered.com/app/431730/Aseprite/) or [itch.io](https://aseprite.itch.io/aseprite)
- Or **build from source for free**: https://github.com/aseprite/aseprite (Visual Studio walkthrough in repo)

### LibreSprite (free fork, ~95% feature parity)
- https://libresprite.github.io/

### TexturePacker (combine PNG → atlas)
- Built-in libGDX task — see "Pack into atlas" below.
- Or GUI: https://www.codeandweb.com/texturepacker (free tier OK)

---

## Sprite specs

### Hero sprites — file naming = `<heroId>.aseprite`

The game looks up sprites by hero ID (the IDs in `assets/data/heroes.csv`). Recommended starter set (matches the default party in `ProjectShadowGame`):
- `hero_01.aseprite` (Chiến Binh / Warrior)
- `hero_03.aseprite` (Tăng Lữ / Cleric)
- `hero_05.aseprite` (your DPS pick)
- `hero_13.aseprite` (Võ Thánh / Monk)

You can add more hero sprites later (`hero_02`, `hero_04`, ...) — the game falls back to a colored rectangle for any hero whose sprite is missing.

**Canvas**: 96×96 px, RGBA, transparent background

**Animations** (all 4 sprites need these tags):

| Tag    | Frames | FPS         | Loop | Purpose                              |
|--------|--------|-------------|------|--------------------------------------|
| idle   | 4      | 8 (125 ms)  | Yes  | Standing pose, breathing animation   |
| attack | 6      | 12 (83 ms)  | No   | Windup → strike → recovery           |
| hurt   | 2      | 8 (125 ms)  | No   | Flinch when hit                      |
| dead   | 1      | hold        | -    | Collapsed pose                       |

Total frames per file: **13** → exported PNG width = `13 × 96 = 1248 px`.

### Enemy sprites — file naming = `<enemyId>.aseprite`

Recommended starter set (matches the default combat encounter):
- `enemy_01.aseprite` (basic)
- `enemy_01_tank.aseprite` (tanky variant)
- `enemy_01_assassin.aseprite` (fast variant)
- `enemy_01_special.aseprite` (caster variant)

Same canvas (96×96) and animation tags as heroes.

### Boss sprite (optional, can defer)
- `boss_stage1.aseprite` — canvas 144×144 (1.5x size for visual impact)

---

## Step-by-step authoring (per sprite file)

### Step 1: Create canvas
- **File > New** (Ctrl+N)
- Width: `96`, Height: `96`
- Color Mode: `RGBA`
- Background: `Transparent`
- OK

### Step 2: Set up layers
- Open the Layers panel (**Window > Layers** or `F11`)
- Click `+` to add layers in this order (bottom to top):
  1. `fill` — flat color base
  2. `outline` — black 1px border
  3. `shading` — darker shadow tones (optional)

### Step 3: Draw frame 1 (idle pose)

1. Use the **Pencil tool** (`B`) at 1px brush size.
2. On `outline` layer, draw the silhouette in pure black `#000000`.
3. On `fill` layer, use **Bucket Fill** (`G`) with the class color:

   | Class    | Hex       |
   |----------|-----------|
   | Warrior  | `#A52A2A` (red armor)   |
   | Cleric   | `#FFD700` (gold robe)   |
   | Bard     | `#228B22` (green tunic) |
   | Monk     | `#8B4513` (brown garb)  |
   | Enemy    | `#444444` darker grays  |

4. On `shading` layer, add a darker shade (e.g. `#000000` at 30% opacity) where the body curves away from light.

### Step 4: Build the idle animation (4 frames)

- Open the Timeline (**Window > Timeline** or `Tab`)
- Right-click frame 1 → **New Frame** (or `Alt+N`) — repeat to make 4 frames total
- Modify each frame slightly to suggest "breathing":
  - Frame 1: pose at rest
  - Frame 2: shift outline +1 px Y (slight rise)
  - Frame 3: pose at rest
  - Frame 4: shift outline -1 px Y (slight dip)

### Step 5: Tag idle frames

- In Timeline, drag-select frames 1–4 (or shift-click first and last)
- Right-click selection → **New Tag**
- Set:
  - Name: `idle` (lowercase exactly — TexturePacker uses this)
  - Direction: `Forward`
  - Repeats: `0` (infinite)
- OK

### Step 6: Build attack animation (6 frames, after idle)

Add 6 more frames (frames 5–10):

| Frame | Pose                                                                  |
|-------|-----------------------------------------------------------------------|
| 5     | Windup: weapon pulled back behind                                    |
| 6     | Weapon raised high                                                   |
| 7     | Weapon thrust forward                                                |
| 8     | Impact: weapon at peak forward; flash overlay layer with white       |
| 9     | Weapon retracting                                                    |
| 10    | Almost back to idle pose                                             |

Tag frames 5–10 → name=`attack`, repeats=`1` (plays once).

### Step 7: Build hurt animation (2 frames)

Frames 11–12:
- Frame 11: body tilted backward, eyes narrowed; add red overlay at low opacity
- Frame 12: returning to upright

Tag → name=`hurt`, repeats=`1`.

### Step 8: Build dead frame (1 frame)

Frame 13:
- Body collapsed on ground, eyes X marks

Tag → name=`dead`, repeats=`0` (holds last frame).

### Step 9: Set per-tag frame durations

For each tag, right-click any frame in that tag → **Frame Properties**:
- idle: `125` ms (8 fps)
- attack: `83` ms (12 fps)
- hurt: `125` ms (8 fps)
- dead: `1000` ms (held)

### Step 10: Export sprite sheet

- **File > Export Sprite Sheet** (or `Ctrl+E`)
- **Layout** tab:
  - Sheet type: `Horizontal Strip`
  - Constraints: None
  - Padding: `0`
- **Sprite** tab:
  - Layers: `Visible only`
  - Frames: `All frames`
  - Tags: keep ticked
- **Output** tab:
  - Output File: `assets/sprites/raw/hero_01.png` (relative to project root)
  - JSON Data: tick, format = `Hash`, output `hero_01.json`
  - Item Filename: `{tag}_{tagframe}` (this is what the atlas packer will use as region names)
- Click **Export**

You should now have:
- `assets/sprites/raw/hero_01.png` (1248×96 px)
- `assets/sprites/raw/hero_01.json` (frame metadata)

### Step 11: Repeat for the other sprites

Same process for `hero_03`, `hero_05`, `hero_13`, and the 4 enemy variants.

---

## Pack PNGs into a TextureAtlas

After all 8 PNG files are in `assets/sprites/raw/`, combine them into a single
atlas (more efficient — one GPU texture bind for all combatants).

### Option A: Gradle task (recommended — already set up)

Run from the project root:

```bash
./gradlew :core:packAssets
```

Output:
- `assets/sprites/combatants.atlas` (region descriptor)
- `assets/sprites/combatants.png` (combined sprite sheet)

The game loads `combatants.atlas` at runtime; if missing, falls back to colored
rectangles (no error).

### Option B: TexturePacker GUI

1. Open https://www.codeandweb.com/texturepacker
2. **File > New Project**
3. Drag all PNG files from `assets/sprites/raw/` into the canvas
4. Settings:
   - Data Format: `libgdx`
   - Texture Format: `PNG`
   - Output File: `assets/sprites/combatants.atlas`
5. **Publish**

---

## Color palette tip

For pixel art consistency, use the [**PICO-8 16-color palette**](https://lospec.com/palette-list/pico-8):

In Aseprite:
- Click the palette dropdown (left sidebar) → **Open Palette**
- Download `pico-8.gpl` from lospec.com → load it
- Now you can only paint with these 16 colors → enforces consistency

---

## Free sprite alternatives (if you don't want to draw)

- [Kenney.nl "Tiny Dungeon" pack](https://kenney.nl/assets/tiny-dungeon) — CC0
- [Kenney.nl "RPG Urban Pack"](https://kenney.nl/assets/rpg-urban-pack)
- https://opengameart.org/ — tags `pixel` + `rpg`
- https://itch.io/game-assets/free — search "pixel art rpg"

You'll need to:
1. Download the asset pack
2. Cut out individual hero/enemy sprites
3. Standardize to 96×96 canvas
4. Re-export with the same tag names (`idle`, `attack`, `hurt`, `dead`)

---

## Troubleshooting

**Sprite shows as a black rectangle**
→ Check the canvas Color Mode is `RGBA` (not Indexed) and the layer below isn't accidentally filled.

**Animation never plays**
→ Tag names must be lowercase `idle` / `attack` / `hurt` / `dead`. Check spelling in Aseprite Timeline.

**Frame durations look wrong**
→ Right-click frame in Timeline → Frame Properties → set ms. Re-export.

**Atlas not loading**
→ Run `./gradlew :core:packAssets` and confirm `assets/sprites/combatants.atlas` exists.

**Wrong sprite shows for a hero**
→ Check the file naming matches the heroId in `assets/data/heroes.csv`. E.g. `hero_01` (Warrior), `hero_03` (Cleric), `hero_13` (Monk). The atlas regions are named `<heroId>_<tag>_<frame>` after packing.
