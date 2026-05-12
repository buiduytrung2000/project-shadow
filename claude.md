# 🌑 Project Shadow — Game Design Hub

> **Tên game:** **Project Shadow** (final)
> **Genre:** Turn-based dungeon crawl roguelite (cảm hứng Darkest Dungeon)
> **Engine:** libGDX + JDK 21 · **Platforms:** Windows, macOS, Linux desktop
> **Solo dev** · **Status:** Sprints 1-12 complete (MVP feature-complete); Sprint 13+ post-MVP polish / Stages 2-3 content
> **Tests:** 509 passing ✅ · **Date:** 2026-05-12

> Behavioral guidelines (Think Before Coding / Simplicity / Surgical Changes / Goal-Driven) đã có ở `~/.claude/CLAUDE.md` — không lặp lại đây.

---

## 📖 Tổng quan

RPG chiến thuật theo lượt, dẫn 4 "linh hồn sa ngã" qua ngục tối ngẫu nhiên, đối mặt enemies + stress + bệnh tật + affliction. Cảm hứng Darkest Dungeon, _điều chỉnh để dễ tiếp cận hơn_.

### Mục tiêu MVP

- **3 stage** với boss riêng (b01 Giant Zombie, b02 Whispering Shade, b03 Black Heart)
- **14 lớp nhân vật**, mỗi lớp 10 skill (chọn 4 / run)
- **16 enemies** (11 thường + Poison Vine + 3 boss + 2 miniboss Stage 3)
- **50 items** (consumable + trinket + class-specific + relic + cursed)
- **Hamlet hub** với 4 công trình (Stagecoach, Guild, Survivalist, Caretaker)
- **Stress system** với Affliction (70%) / Virtue (30%) khi stress ≥ 100
- **Pathway dạng cây** (layered_tree) — branching, weighted random
- **Permadeath**, **save/load JSON**, **i18n VN/EN**

### Ba trụ cột thiết kế

1. **Dễ tiếp cận, khó làm chủ** — Stage 1 nhẹ, Stage 3 đè áp lực tâm lý lẫn chiến thuật.
2. **Ngẫu nhiên bất định nhưng công bằng** — Mỗi run unique nhưng player đọc được và quyết định.
3. **Tâm lý đội nhóm là vũ khí & kẻ thù** — Stress + disease + trait ảnh hưởng trực tiếp hành vi.

---

## 📚 Danh mục tài liệu

Docs ở `docs/` (chung `docs/style.css`):

| File | Nội dung |
| --- | --- |
| [design_overview.html](docs/design_overview.html) | Concept, Cốt truyện, Core Loop, Art Style, Âm thanh |
| [game_systems.html](docs/game_systems.html) | Bảng Heroes/Enemies/Items/Diseases/Traits + công thức damage |
| [technical_data.html](docs/technical_data.html) | JSON schemas, API pseudocode, flowcharts |
| [pathway_stage1.html](docs/pathway_stage1.html) | Stage 1 JSON đầy đủ, pools, rules |
| [project_plan.html](docs/project_plan.html) | Sprint plan, Roadmap, Feature List |
| [notes_and_considerations.html](docs/notes_and_considerations.html) | Out-of-scope, post-MVP, còn mơ hồ |
| [test_plan.html](docs/test_plan.html) | Checklist test thủ công + regression suite |

### Source data

- `assets/data/heroes.csv` — 14 heroes
- `assets/data/skills.csv` — 140 skills
- `assets/data/effects.csv` — 134 effects (data-driven)
- `assets/data/items.csv` — 50 items
- `assets/data/enemies.csv` — 16 enemies
- `assets/data/enemy_skills.csv` — 24 enemy skills
- `assets/data/events.csv` — 11 events
- `assets/data/diseases_traits.csv` — 18 rows (6 disease + 12 trait: 7 Affliction / 5 Virtue)
- `assets/data/stages/stage_{1,2,3}.json` — Stage specs (Stage 1 full impl, 2-3 partial pools)

### Code

- `core/src/main/java/com/trungbui/projectshadow/` — game logic library
- `lwjgl3/src/main/java/...` — desktop launcher
- `core/src/test/java/...` — JUnit 5 tests (509 passing)

---

## 🛠️ Workflows

### Chạy game

```bash
./gradlew lwjgl3:run         # Khởi động desktop
./gradlew core:test          # Chạy 509 unit tests
./gradlew core:runDemo       # CLI verify CSV/JSON load
./gradlew core:packAssets    # Pack combatant PNG → combatants.atlas
./gradlew core:packUIComponents  # Pack UI PNG → components.atlas
```

### Implement feature mới

1. Check [project_plan.html § Feature List](docs/project_plan.html) — đã có spec chưa?
2. Đọc [technical_data.html](docs/technical_data.html) cho JSON schema + API pseudocode.
3. Update CSV/JSON nếu data shape thay đổi.
4. Update `DataIntegrityTest.loadCounts_match_expected` nếu thay số rows.
5. `./gradlew core:test` để verify FK.

### Thêm hero / enemy / item

1. Thêm row vào CSV tương ứng.
2. Mọi reference (effects/skills) phải tồn tại trong CSV gốc (FK check).
3. Pseudo-id (e.g. `random_boss_pool`) → thêm vào `DataLoaderDemo.KNOWN_TODO_REFS`.
4. Update bảng tương ứng trong [game_systems.html](docs/game_systems.html).
5. Queue PixelLab AI sprite qua MCP nếu cần visual.

### Sprite atlas thay đổi

1. PixelLab AI batch tạo sprite (xem [notes_and_considerations.html § PixelLab](docs/notes_and_considerations.html)).
2. Download ZIP qua `scripts/process_pixellab_sprites.sh` (combatants) hoặc `process_pixellab_ui.sh` (UI).
3. Frames rename + copy vào `assets/sprites/raw/<id>_<tag>_<frame>.png`.
4. `./gradlew core:packAssets` rebuild atlas.

### Kiểm tra game balance

→ `/game-balance` trong Claude Code. Output: DPS / survivability / threat ranking + outlier flags (>1.5 SD) + suggested adjustments.

### Download SFX

1. Đăng ký key tại https://freesound.org/apiv2/apply/ → lưu `~/.freesound_key` hoặc `FREESOUND_API_KEY=<key>`.
2. Edit `assets/audio/.sfx-manifest.json`.
3. `scripts/fetch_sfx.sh` → OGG vào `assets/audio/sfx/`. IDs lưu `.freesound-ids.json`.

### CI/CD

- GitHub Actions tự chạy `core:test` mỗi push/PR → main (`.github/workflows/ci.yml`).
- Fail → artifact "test results" trên job page có HTML report.
- Verify local `./gradlew core:test` trước khi push.

### Sprint history

Mọi sprint detail (per-PR commit hash, test delta per branch, code snippets) ở **`git log`** + **GitHub PR list**. Đừng viết lại vào file này.

---

## 🏛️ Lịch sử quyết định thiết kế

### Tại sao giữ Stress system?

Đặc trưng cốt lõi. Cắt = mất identity, lẫn vào roguelite turn-based khác. Chúng ta giảm khắc nghiệt (Stage 1 ít stress) nhưng giữ Affliction/Virtue resolution để có "spike" cảm xúc.

### Tại sao 14 lớp thay vì 4?

Đa dạng team comp. Mỗi lớp ~50 dòng CSV — rẻ. 14 lớp = ~1000 team combos khả thi. 4 lớp quá ít.

### Tại sao 3 stage?

Scope solo dev. Mỗi stage = pathway + pool + 1 boss + ~6 events + balance pass. 3 stage = vertical slice + 2 ext, sweet spot cho roguelite. Stage 4-5 = DLC nếu game thành công.

### Tại sao libGDX?

Java/JDK 21 dev đã biết, type-safe domain, atlas/freetype/audio mature. Godot GDScript chậm cho game lớn. Unity license + .NET overhead không cần cho 2D pixel.

### Tại sao data-driven effect resolver?

1 generic engine đọc 134 effect rows → switch behavior. Thay vì viết 134 class. Maintainability + designer chỉnh balance bằng spreadsheet.

### Tại sao Stage 1 `layered_tree` (không DAG ngẫu nhiên)?

3 layer × 2 node + fully_connected → 4 path/run. Đủ branching cho replay value, không quá phức tạp impl. Player thấy được full tree → plan trước (giảm frustration).

### Tại sao Permadeath ON?

Roguelite identity. Caretaker chữa **disease** ≠ revive hero. Casual có thể manual save scum.

### Tại sao VN primary + EN secondary?

Dev native VN, viết content nhanh hơn. VN gamer chưa được phục vụ với genre này. EN cho global. Toggle qua `I18n.toggleLocale()`.

### Tại sao PixelLab AI?

Solo dev không có ~200h vẽ 30+ characters × 4 anims. PixelLab consistent style + ~$0.10/char. Risk: server flake → retry strategy v2/v3/v4.

---

## ✅ Đã chốt — Design locks

- **Affliction/Virtue 70/30** — Pool 7 Affliction / 5 Virtue (Bloodthirsty đã reclass Affliction Sprint 11 B2)
- **Boss HP**: b01=100, b02=130, b03=170 · **Boss dmg**: b01=13-19, b02=14-20, b03=15-23
- **Hit-rate**: normal acc=80, miniboss=85, boss=90
- **Heart-attack**: stress=200 → hero chết tức thì
- **Cure slot reset**: end-of-run (Caretaker Lv1/2/3 = 1/2/4 slots, cost 30/25/20g; Cursed không chữa được)
- **Stagecoach refresh**: 50g/lần
- **Roster cap**: soft-20, vượt → embark auto-cull random (party protected)
- **Run seed**: per-run `nextLong()`, env `SHADOW_FIXED_SEED` cho dev
- **Debt model**: gold âm cho phép trên survival paths (hire/refresh/supplies tax); reward pay down. Strict-throw chỉ cho upgrade/cure/craft.
- **Action pacing**: 0.7s enemy turn delay, env `SHADOW_ACTION_DELAY` override
- **Hybrid XP**: per-kill grant + end-of-stage bonus. Guild level-up = gold AND xp `{100,200,350,500,750}`
- **Recruit**: 2 random Virtues mỗi hire (never afflictions)
- **Turn order**: per-action re-sort tail theo `effectiveSpeed`, 1 action/actor/round invariant giữ
- **Crit**: 1.5×
- **Disease chance**: 30%/hero/stage
- **Hire cost**: 50g Common / 80g Rare / 150g Legendary
- **Hamlet upgrade 3 tiers**: cost gold + Heirloom, full = 3,280g + 14 Heirloom (~4-5 boss kills)
- **Heirloom**: boss drop 1/2/4 (Stage 1/2/3), chỉ dùng upgrade
- **Supplies Tax**: 100/200/400g up-front, non-refundable
- **Boss gold reward**: 200/400/1000g (Stage 1/2/3) · **No gold cap**

---

## ❌ Out of scope MVP

- Multiplayer / co-op / PvP
- Cinematic cutscenes + voice acting
- Procedural skill generation
- Mobile / console port
- Achievement system in-game
- Controller native support
- Adaptive music
- Dynamic lighting
- Crafting / equipment forging

---

## 📋 Deferred (post-MVP / Sprint 13+)

- **Reward "combo 3"**: streak bonus (+10% gold compounding, cap 1.5×, reset on rest) + Slay-the-Spire-style 1-of-3 reward cards cho elite/miniboss/boss. Plan trong [notes_and_considerations.html](docs/notes_and_considerations.html).
- **descriptionEn CSV column**: skill tooltips fall back VN trong EN locale until skills.csv thêm cột EN.
- **Stages 2/3 playable content**: JSON specs exist nhưng pools chưa đầy đủ.
- **Item use Phase 2**: `eff_burn`, `eff_dmg_buff`, `eff_absorb`, etc. còn log "not yet supported".
- **UI Round 4 wiring**: atlas + SkinLoader có sẵn cho node icons / status icons / frames; chưa wire vào StageMapScreen / CombatRenderer / HamletScreen banner. Mỗi cái là PR riêng.
- **Freesound SFX integration**: script + manifest ready (Round 5); cần API key + wire vào AudioManager.
- Particle effects khi crit/affliction
- Aseprite-style anim refinement (tween + 2-3 frame)

---

## ❓ Còn cần quyết định

- Random damage variance ±5% vs ±10%
- Stress max uniform 100 vs differentiated by rarity
