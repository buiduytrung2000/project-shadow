# Audio Assets — Sourcing Guide

Project Shadow loads audio at runtime. Drop `.ogg` files into the right folders
and the game picks them up automatically. **Missing files are silent — no crash**,
just a one-line log warning per missing key.

## Required keys

### SFX — `assets/audio/sfx/<key>.ogg`

| Key | Triggered by |
|---|---|
| `button_click` | Any UI button press (Hamlet, building screens, combat skills) |
| `hit` | Successful non-crit attack |
| `crit` | Critical hit |
| `miss` | Whiff |
| `heal` | Heal skill (e.g. Cleric `sk_c2`) |
| `level_up` | Guild level-up purchase |
| `hire` | Stagecoach hire purchase (coin clink) |
| `victory` | Boss defeated |
| `defeat` | Party died |

### Music — `assets/audio/music/<key>.ogg`

| Key | Played in |
|---|---|
| `hamlet_theme` | HamletScreen + all building screens (calm medieval, ~2min loop) |
| `map_theme` | StageMapScreen (exploration, mysterious) |
| `combat_theme` | CombatScreen (tension, drums) |
| `victory` | VictoryScreen (one-shot triumph) |
| `gameover` | GameOverScreen (somber) |

## How to create SFX (free, ~10 minutes)

### Option A — BFXR (web-based, no install)

1. Open https://www.bfxr.net/
2. Click a preset that matches the SFX you want:
   - `Hit/Hurt` → for `hit`, `miss`
   - `Powerup` → for `level_up`, `victory`
   - `Pickup/Coin` → for `hire`, `button_click`
   - `Hurt/Damage` → for `defeat`, `crit`
3. Tweak sliders or click `Mutate`/`Randomize` until it sounds right.
4. Click **Export Wav** to download.
5. Convert `.wav` → `.ogg` using [Audacity](https://www.audacityteam.org/):
   - File > Open `<file>.wav`
   - File > Export > Export Audio... > Format: `Ogg Vorbis`, Quality: 5
   - Save as `assets/audio/sfx/<key>.ogg`

### Option B — Free SFX libraries

- [freesound.org](https://freesound.org/) — filter "License: Creative Commons 0"
- [kenney.nl/assets](https://kenney.nl/assets) — "Sound packs" (CC0)
- [opengameart.org](https://opengameart.org/) — tag `sfx`

## How to source Music (free)

- [Kevin MacLeod / incompetech.com](https://incompetech.com/) — copy the attribution line into `assets/audio/CREDITS.md`
- [opengameart.org](https://opengameart.org/) — tags `rpg` + `loop`
- [Free Music Archive](https://freemusicarchive.org/)

### Loop preparation in Audacity

1. Open the source file
2. Listen for a clean section that starts and ends on the same beat
3. Select that section → Edit > Trim Audio
4. Mark loop points: Edit > Selection > At Zero Crossings (silences crackles)
5. Export as `.ogg` Quality 4–5 (~1MB/file is plenty)

## File naming reminder

Path = `assets/audio/{music|sfx}/<key>.ogg` exactly. The `<key>` is what
{@code AudioManager.playMusic("hamlet_theme", true)} looks up. Anything else
(filename misspelt, wrong folder, `.mp3` instead of `.ogg`) → game logs a
warning and continues silent.

## Credits

When using attribution-required tracks, append to `assets/audio/CREDITS.md`:

```markdown
## hamlet_theme.ogg
- "Hidden Wonders" by Kevin MacLeod (incompetech.com)
- Licensed under Creative Commons: By Attribution 4.0
- https://creativecommons.org/licenses/by/4.0/
```
