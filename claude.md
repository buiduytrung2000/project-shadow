# 🌑 Project Shadow — Game Design Hub

> **Tên game:** **Project Shadow** (final, không phải codename)
> **Genre:** Turn-based dungeon crawl roguelite (lấy cảm hứng Darkest Dungeon)
> **Engine:** libGDX + JDK 21 · **Target platforms:** Windows, macOS, Linux desktop
> **Solo dev project** · **Status:** Sprint 9 / 9 (Polish) đang chạy

---

## 📖 Tổng quan dự án

**Project Shadow** là một game RPG chiến thuật theo lượt nơi người chơi dẫn dắt 4 "linh hồn sa ngã" qua các ngục tối ngẫu nhiên, đối mặt với enemies, stress, bệnh tật và affliction. Lấy cảm hứng từ **Darkest Dungeon** nhưng *điều chỉnh để dễ tiếp cận hơn* — độ khó tăng dần theo lựa chọn của người chơi.

### Mục tiêu MVP
- **3 stage** với boss riêng (b01 Giant Zombie, b02 Whispering Shade, b03 Black Heart).
- **14 lớp nhân vật**, mỗi lớp 10 skill (chọn 4 / run).
- **15 enemies** (11 thường + 3 boss + 2 miniboss Stage 3).
- **50 items** (consumable + trinket + class-specific + relic + cursed).
- **Hamlet hub** với 4 công trình (Stagecoach, Guild, Survivalist, Caretaker).
- **Stress system** với Affliction (70%) / Virtue (30%) khi stress ≥ 100.
- **Pathway dạng cây** (layered_tree) — branching, weighted random.
- **Permadeath**, **save/load JSON**, **i18n VN/EN**.

### Ba trụ cột thiết kế
1. **Dễ tiếp cận, khó làm chủ** — Stage 1 nhẹ nhàng, Stage 3 đè áp lực tâm lý lẫn chiến thuật.
2. **Ngẫu nhiên bất định nhưng công bằng** — Mỗi run unique nhưng player có thể đọc và quyết định.
3. **Tâm lý đội nhóm là vũ khí &amp; kẻ thù** — Stress + disease + trait ảnh hưởng trực tiếp hành vi.

---

## 📚 Danh mục tài liệu

Tất cả docs nằm trong `docs/` (HTML có chung style `docs/style.css`):

| File | Nội dung |
|---|---|
| [docs/design_overview.html](docs/design_overview.html) | **Thiết kế tổng quan** — Concept, Cốt truyện, Core Loop (micro/mid/macro), Art Style, Âm thanh |
| [docs/game_systems.html](docs/game_systems.html) | **Hệ thống &amp; Cân bằng** — Bảng Heroes/Enemies/Items/Diseases/Traits + công thức damage |
| [docs/technical_data.html](docs/technical_data.html) | **Kỹ thuật &amp; Data** — JSON schemas (hero/skill/enemy/item/event/effect), API pseudocode, flowcharts |
| [docs/pathway_stage1.html](docs/pathway_stage1.html) | **Stage 1 pathway** — JSON đầy đủ, pools, rules, thuật toán sinh stage (pseudocode) |
| [docs/project_plan.html](docs/project_plan.html) | **Kế hoạch &amp; Scope** — Sprint plan 9 sprints, Roadmap 48 tuần, Feature List với status |
| [docs/notes_and_considerations.html](docs/notes_and_considerations.html) | **Notes** — Out-of-scope, có thể phát triển thêm (post-MVP), còn mơ hồ |
| [docs/test_plan.html](docs/test_plan.html) | **Test Plan** — Checklist test thủ công chi tiết cho 9 sprints + extras + regression suite |

### Source data (CSV/JSON)
- `assets/data/heroes.csv` — 14 heroes
- `assets/data/skills.csv` — 140 skills
- `assets/data/effects.csv` — 134 effects (data-driven)
- `assets/data/items.csv` — 50 items
- `assets/data/enemies.csv` — 15 enemies (incl. 3 boss + 2 miniboss)
- `assets/data/enemy_skills.csv` — 23 enemy skills
- `assets/data/events.csv` — 11 events
- `assets/data/diseases_traits.csv` — 14 traits (6 disease + 8 personality)
- `assets/data/stages/stage_1.json` — Stage 1 (layered_tree, fully implemented)
- `assets/data/stages/stage_2.json` — Stage 2 spec (boss enemy_b02)
- `assets/data/stages/stage_3.json` — Stage 3 spec (miniboss layer + final boss enemy_b03)

### Code (Java/libGDX)
- `core/src/main/java/com/trungbui/projectshadow/` — game logic library
- `lwjgl3/src/main/java/...` — desktop launcher
- `core/src/test/java/...` — JUnit 5 tests (247 tests, all passing ✅)

---

## 🛠️ Hướng dẫn sử dụng (solo dev)

### Khi cần **xem tổng quan**
→ Đọc theo thứ tự: [design_overview](docs/design_overview.html) → [game_systems](docs/game_systems.html) → [project_plan](docs/project_plan.html).

### Khi cần **implement feature mới**
1. Check [project_plan.html § Feature List](docs/project_plan.html) — feature đã có spec chưa?
2. Đọc [technical_data.html](docs/technical_data.html) cho JSON schema + API pseudocode.
3. Update `assets/data/*.csv` hoặc `*.json` nếu thay đổi data shape.
4. Update test ở `core/src/test/.../DataIntegrityTest.java` — sửa `loadCounts_match_expected` nếu thay số rows.
5. Chạy `./gradlew core:test` để verify FK.

### Khi cần **thêm nhân vật / enemy / item mới**
1. Thêm row vào CSV tương ứng.
2. Reference đến effects/skills phải tồn tại trong CSV gốc (FK check).
3. Nếu pseudo-id (e.g. `random_boss_pool`) → thêm vào `DataLoaderDemo.KNOWN_TODO_REFS`.
4. Update [game_systems.html](docs/game_systems.html) bảng tương ứng.
5. Queue PixelLab AI sprite qua MCP nếu cần visual.

### Khi cần **chạy game**
```bash
./gradlew lwjgl3:run         # Khởi động desktop
./gradlew core:test          # Chạy 247 unit tests
./gradlew core:runDemo       # CLI verify CSV/JSON load
./gradlew core:packAssets    # Pack PNG → combatants.atlas
```

### Khi sprite atlas thay đổi
1. PixelLab AI batch tạo sprite (xem [Notes & Considerations § PixelLab](docs/notes_and_considerations.html)).
2. Download ZIP qua script `scripts/process_pixellab_sprites.sh`.
3. Frames rename + copy vào `assets/sprites/raw/<id>_<tag>_<frame>.png`.
4. `./gradlew core:packAssets` rebuild atlas.

---

## 🏛️ Lịch sử các quyết định thiết kế quan trọng

### Tại sao giữ Stress system (mặc dù phức tạp)?
**Đặc trưng cốt lõi**. Cắt = mất identity, lẫn vào các roguelite turn-based khác. DD chứng minh stress là cơ chế hay nếu balance đúng — chúng ta giảm khắc nghiệt (Stage 1 ít stress) nhưng vẫn giữ Affliction/Virtue resolution để có "spike" cảm xúc.

### Tại sao 14 lớp thay vì 4 (như plan ban đầu)?
**Đa dạng team composition**. Mỗi lớp CSV ~50 dòng (10 skill × ~5 col) — không quá tốn. Người chơi muốn experiment với 4-hero combos → 14 lớp cho ~1000 team comps khả thi. Plan ban đầu 4 lớp quá ít.

### Tại sao chỉ 3 stage (không phải 5-10)?
**Scope solo dev**. Mỗi stage = pathway + pool + 1 boss + ~6 events + balance pass. 3 stage là sweet spot: đủ "vertical slice + 2 ext" cho roguelite. Stage 4-5 sẽ là DLC nếu game thành công.

### Tại sao libGDX (không Godot/Unity)?
**Java/JDK 21 dev đã biết**, type-safe domain model, atlas + freetype + audio mature. Godot có nhưng dùng GDScript chậm cho game lớn. Unity license + .NET overhead không cần thiết cho 2D pixel.

### Tại sao data-driven effect resolver?
**1 generic engine** đọc 134 effect rows từ CSV → switch behavior. Thay vì viết 134 class. Maintainability cao + designer có thể tinh chỉnh balance bằng spreadsheet.

### Tại sao Stage 1 dùng `layered_tree` (không "DAG ngẫu nhiên")?
- **3 layer × 2 node/layer + fully_connected**: 4 path/run khả thi.
- Đủ branching cho replay value, không quá phức tạp implementation.
- Player thấy được full tree → có thể plan trước (giảm frustration).

### Tại sao thêm `enemy_b02`, `enemy_b03`, `enemy_mb01/02` vào CSV?
**Stage 2/3 JSON đã reference** → cần data layer match. Stage 3 cần miniboss layer (L6) để tạo "test cuối trước final boss" — giống DD's "Champion fight" pattern.

### Tại sao Permadeath ON by default?
**Roguelite identity**. Người chơi muốn casual có thể save scum (manual file backup), nhưng default = permadeath để keep stake. Caretaker chữa **disease** ≠ revive hero.

### Tại sao Vietnamese primary + English secondary?
**Dev native VN**, viết content nhanh hơn. Market: VN gamer chưa được phục vụ với genre này. EN cho global reach. Hai ngôn ngữ qua `I18n.toggleLocale()` Sprint 9.

### Tại sao PixelLab AI cho sprite?
**Solo dev không có ~200h vẽ 30+ characters × 4 anims**. PixelLab AI cho consistent style + 5 credits/character (~$0.10 / char). Risk: server flake → đã có retry strategy (v2/v3/v4 nếu fail).

---

## ✅ Checklist hiện trạng dự án (2026-05-11)

### Sprint 1-8 (Data + Logic + UI + Save + Hamlet)
- [x] Data layer — 14 heroes, 140 skills, 134 effects, 50 items, 15 enemies, 23 enemy skills, 11 events, 14 traits, 3 stages
- [x] Domain models (Hero, Combatant, CombatEncounter, Effect, ActiveEffects)
- [x] Combat logic (turn order, damage formula, target selector)
- [x] Effect resolver (data-driven, 134 effect types)
- [x] Stage generator (`StageGenerator.generate(stage_1.json, seed)`)
- [x] StageMapScreen render + click navigation
- [x] Save/load run state (JSON `saves/run_*.json`)
- [x] Hamlet hub: 4 buildings (Stagecoach/Guild/Survivalist/Caretaker)
- [x] **247 unit tests passing** (DataIntegrityTest + 21 other test classes)

### Sprint 9 (Polish) — đang chạy
- [x] I18n VN + EN với toggle button
- [x] AudioManager (music + SFX skeleton)
- [x] Save/load Hamlet roster + meta state
- [x] **PixelLab AI sprite batch — COMPLETE** (2026-05-11):
  - [x] **14/14 heroes** ✅ base + 4 anims each (hero_02 missing dead anim only — minor, acceptable static fallback)
  - [x] **3/3 boss** ✅ all 4 anims: enemy_b01 Giant Zombie (size 96), enemy_b02 Whispering Shade (v2), enemy_b03 Black Heart (v2)
  - [x] **2/2 miniboss** ✅ all 4 anims: enemy_mb01 Echo Wraith (v2), enemy_mb02 Plague Bearer (v4)
  - [x] **11/11 enemies** ✅ all 4 anims each (after multiple retries — v2/v3/v4/v5/v6 for the difficult ones)
  - [x] **5/5 core items** queued (item_c01, c03, t02, t07, t10)
  - [x] **3/3 tilesets** queued (stage_1 Hầm Mộ, stage_2 Sương Mù, stage_3 Lò Đúc Linh Hồn)
  - [x] **Bugfix**: Double-dispose StageMapScreen → CombatScreen crash fixed (commit `1ae5e44`)

### Sprint 9+ Round 1 — Combat Polish trilogy (PR #3, 2026-05-11)
- [x] **B3 — Combat animations** (commit `82316a3`, `bugfix/combat-animations`):
  attacker now plays attack anim + dying combatants play dead anim. Before
  fix only `hurt` was firing. +5 unit tests.
- [x] **B1 — Combat reward system** (commit `f257070`, `feature/combat-reward-system`):
  fixes boss-kill-grants-zero-gold bug (boss_node.rewards_on_kill was raw
  JsonNode), adds per-combat auto-drop (gold scaling by variant + item via
  CSV dropChance + -3 stress relief), wires reward node drops. +12 tests.
- [x] **B2 — Skill tooltip** (commit `9777fc8`, `feature/skill-description-tooltip`):
  hover skill button in combat → floating panel shows description, dmg
  multiplier, target type, cooldown, stress damage, effect. i18n VN/EN.
  +7 tests.

### Sprint 9+ Round 2 — Post-review hardening trilogy (PR #4/#5/#6, 2026-05-11)

A `/engineering:code-review` pass on Round 1 HEAD surfaced a build-blocker,
two gameplay-broken systems, and unhardened save data. User chose 3 sequential
branches, plan-mode with `AskUserQuestion` for ambiguous decisions.

- [x] **PR #4 — `hotfix/build-arraylist-import`** (commit `eedbe30`):
  missing `import java.util.ArrayList;` in `ProjectShadowGame.java` —
  `gradlew core:compileJava` was failing. Round 1's commit-message claim of
  "tests pass" was unverifiable until this landed.
- [x] **PR #5 — `fix/combat-correctness`** (commit `4a512e1`):
  - **Effect tick hybrid** — AoE skills were N-ticking DoTs because
    `resolveAction` looped per-target and end-of-round also re-ticked. Now:
    per-actor tick at action start, deduped via `tickedThisRound` set,
    cleared at `endRoundReset()`. New `EffectInstance.appliedRound` lets
    just-applied effects skip their first tick.
  - **Stress system rework** — `STRESS_MAX=200` was previously dormant. Now:
    crossing 100 latches `pendingAfflictionRoll` → new `AfflictionResolver`
    rolls 70% Affliction / 30% Virtue and picks a trait from
    `diseases_traits.csv`. Crossing 200 → instant heart-attack death
    (`setCurrentHp(0)`). Combat log + i18n popups for both events.
  - **Reward RNG seeding** — was `new Random()` (unseeded). Now derived
    from `runSession.state().stageSeed() ^ nodeLabel.hashCode()`.
    `StageGenerator.parseBossReward` uses sub-RNG so layout stays stable.
  - **Correctness nits**: `refreshDuration` uses `Math.max`,
    `applyImmediate` re-runs on refresh, `dropChance` clamped `[0,1]`,
    `item_random` resolves category→real item ID (was leaking
    `"trinket_common"` to inventory), tooltip exit listener on parent
    skill table, skill-tooltip parens gating.
  - **+4 new affliction traits** in `diseases_traits.csv` (Paranoid,
    Selfish, Fearful, Hopeless) + new `Resolution` column tags existing
    8 traits as Affliction/Virtue.
  - **+26 tests** across 7 new files.
- [x] **PR #6 — `chore/save-hardening`** (commit `882fdca`):
  - **Save schema versioning**: `saveVersion` field on `RunState` +
    `MetaState`. New `SaveMigration` utility refuses to load saves whose
    version exceeds the build (no silent data loss on downgrade). Legacy
    saves (no field) load as v1 transparently.
  - **Atomic write**: `.tmp` sibling + `Files.move(ATOMIC_MOVE,
    REPLACE_EXISTING)` in both `SaveManager` and `MetaStateManager`.
    Falls back to plain `REPLACE_EXISTING` on filesystems without
    `ATOMIC_MOVE`. A crash mid-write can no longer corrupt the final file.
  - **Path-traversal guard**: `runId` must match `[A-Za-z0-9_-]+`.
  - **i18n on HamletService errors** (was hard-coded VN). EN-locale
    players now see English exceptions.
  - **`RunState.withGoldDelta` throws on overdraw** (was silent floor-to-0).
  - **i18n polish**: `Locale.of(...)` over deprecated `new Locale(...)`;
    `volatile` on mutable static `bundle`/`current`.
  - **+28 tests** across 5 new files.

Test count: **247 → 271 (Round 1) → 325 (Round 2)**. Zero failures throughout.

### Sprint 9+ Round 3 — UI asset pack (PR #16/#20, 2026-05-11)

First batch of UI visual assets cho game — buttons, panels, icons generated qua
PixelLab AI, mỗi component 1 asset riêng, **4 button states share chung
template + viền + rivets** (1 base + 3 `vary_object`). Tích hợp non-breaking
vào libGDX skin pipeline qua `SkinLoader` mới.

- [x] **PR #16 — `claude/gallant-meninsky-1e146f`** (commit `193bf2f`):
  - **15 PixelLab assets** generated + packed thành
    `assets/ui/components.atlas` (512×256):
    - **4 button states** (`ui_btn_up/down/over/disabled`) 96×96 stone slab,
      4 iron rivets ở góc. NinePatch margin 16 → 1 source scale tới Hamlet
      280×120, CTA 600×80, lang toggle 120×50.
    - **3 NinePatch panels** (`ui_panel_main`, `ui_panel_tooltip`,
      `ui_popup_bg`) — ornate manuscript, gold tooltip, gothic rose-thorn
      popup.
    - **8 static icons** (stagecoach, guild, survivalist, caretaker, gold,
      heirloom, settings, close) — 32-48px TextureRegionDrawables.
  - **`SkinLoader` utility** (`core/src/main/.../ui/SkinLoader.java`) —
    overlay components atlas lên uiskin, đăng ký NinePatchDrawables +
    `"primary"` ImageTextButtonStyle + `"panel"/"popup"` WindowStyle +
    `"panel"` TextTooltipStyle. Graceful fallback nếu atlas chưa pack.
  - **11 screens migrate** `new Skin(...)` → `SkinLoader.load()` — đồng
    nhất loading, zero behavior change ngoài Hamlet.
  - **HamletScreen 4 building buttons** → ImageTextButton có icon prefix
    + stone-slab background.
  - **New Gradle task** `packUIComponents` pack `assets/ui/raw` →
    `components.atlas` (riêng khỏi `combatants.atlas`, rebuild độc lập).
  - **New script** `scripts/process_pixellab_ui.sh` — download object PNGs
    từ PixelLab MCP endpoint (auto-detect ZIP vs raw PNG).
  - **Tracking file** `assets/ui/.pixellab-ui-ids.json` — 15 UUIDs cho
    regenerate / vary tương lai.

- [x] **PR #20 — `fix/ui-skin-loader-drawable-and-font`** (commits
  `58c46b7`, `69501f4`) — 2 bug visual phát hiện sau khi merge PR #16:
  - **Bug 1 — `Skin.has()` exact-type match**: `HamletScreen.buildingButton()`
    check `skin.has(iconDrawable, Drawable.class)` luôn false vì
    `SkinLoader` register icons dưới `TextureRegionDrawable.class`. libGDX
    `Skin.has()` không tự fallback theo superclass → 4 button fallback về
    plain TextButton (default dark-rect, không icon).
    **Fix:** SkinLoader register mỗi drawable dưới **cả** type cụ thể
    (`NinePatchDrawable` / `TextureRegionDrawable`) **và** `Drawable.class`.
  - **Bug 2 — Stale font reference**: `SkinLoader.load()` chạy trước
    `fontFactory.create(24)`, copy font từ default style — đó là
    `font.fnt` (ASCII-only). HamletScreen set VN font CHỈ cho default
    style → `"primary"` style giữ font ASCII cũ → ký tự VN missing
    (box / glyph sai).
    **Fix:** thêm `SkinLoader.overrideFont(skin, font)` utility. HamletScreen
    gọi nó sau khi VN bodyFont sẵn sàng.

PixelLab API workflow note: 1 lần generate `ui_btn_up` timeout server →
queue đầu thất bại, retry 2 lần (10 jobs concurrent limit). 4 button states
giữ template nhất quán bằng cách generate 1 base rồi `vary_object` cho 3
state — KHÔNG generate 4 lần độc lập (sẽ khác border style).

UI mới CHỈ wire ở HamletScreen ở 2 PR này. Atlas + SkinLoader sẵn sàng;
các screen khác (Stagecoach/Guild/CombatScreen/...) opt-in sau bằng
`skin.get("primary", ...)`.

Test count: 325 (Round 2) → **~451 (Round 3 baseline)** sau khi rebase lên
main mới nhất (PR #7-18 đã thêm test). Round 3 không thêm test (visual UI
work). 1 pre-existing flake (`EnemyHitRateTest.bossB01...`) trên main từ
trước, không liên quan.

### Sprint 9+ Round 4 — UI asset pack Phase 2 (PR #24, 2026-05-12)

Continuation của Round 3 — thêm 12 components mới cho gameplay UI elements
chưa có asset (node icons, combat HUD, decorative frames). **Atlas grows
15 → 27 regions** (512×256 → 1024×256). Chưa wire vào screen ở PR này;
SkinLoader expose constants sẵn cho future opt-in.

- [x] **PR #24 — `feat/ui-asset-pack-round2`** (commit `dae344c`):
  - **7 stage map node icons** (48×48, 1-1 mapping với `NodeType`):
    - `ui_node_combat` — crossed swords + target dial
    - `ui_node_elite` — crowned sword + golden star
    - `ui_node_miniboss` — cracked horned skull
    - `ui_node_boss` — fanged demonic skull, curved horns
    - `ui_node_event` — open scroll with `?`
    - `ui_node_rest` — campfire + firewood
    - `ui_node_reward` — wooden chest + spilled coins
  - **3 combat HUD status icons** (32×32):
    - `ui_icon_hp` — anatomical red heart
    - `ui_icon_stress` — cracked skull + purple psychic aura
    - `ui_icon_shield` — heater shield + iron rivets
  - **2 NinePatch decorative frames** (stretchable):
    - `ui_frame_portrait` (96×96, margin 16) — hero portrait frame
    - `ui_banner_title` (128×128, margin 20) — screen title banner
  - **SkinLoader** thêm 12 public constant (`NODE_*`, `ICON_HP/STRESS/SHIELD`,
    `FRAME_PORTRAIT`, `BANNER_TITLE`). Status + node icons là
    `TextureRegionDrawable`, frames là `NinePatchDrawable`. Mọi drawable
    register dưới cả type cụ thể + `Drawable.class` (consistent với PR #20).
  - **Process script** (`scripts/process_pixellab_ui.sh`) — jq parser
    handle JSON sections mới (`node_icons` / `status_icons` / `frames`).
    `ASSETS` array expand đầy đủ 27 components.
  - **Wave generation**: 8 jobs + 4 jobs (rate limit 8 concurrent), tổng
    ~3 phút. Không có job nào fail timeout lần này (Round 3 có 1).

**Wiring vẫn pending** (deferred to future sprint, mỗi cái là PR riêng):
- A. StageMapScreen `buildNodeButtons()` — đính `NODE_*` icon vào node
  button thay vì chỉ TextButton + color tint
- B. `CombatRenderer.drawHpBar()` / `drawStressBar()` — đính HP/Stress
  icon đầu mỗi bar
- C. HamletScreen title — wrap "HAMLET" label bằng `ui_banner_title`
- D. EmbarkSelectionScreen + GuildScreen hero list — wrap portrait/icon
  bằng `ui_frame_portrait`

Test count: 451 (Round 3 baseline) → **488 (Round 4 baseline)** sau khi
rebase lên main (PRs upstream giữa Round 3 và Round 4 thêm test). Round 4
không thêm test — visual-only. Pre-existing `EnemyHitRateTest` flake
hết flake (RNG seed thay đổi do upstream commit).

### Sprint mở rộng (deferred — documented for future sprint)
- **Reward "combo 3" full feature**: streak bonus (compounding +10% gold,
  capped at 1.5×, reset on rest) + Pick 1 of 3 reward cards (Slay-the-Spire
  style) for elite/miniboss/boss nodes. Plan committed in
  [docs/notes_and_considerations.html](docs/notes_and_considerations.html).
- **CombatRewardPopup UI**: reward currently applied silently — popup with
  2-3s display + "Continue" button planned for Sprint 10.
- **descriptionEn CSV column**: skill tooltips fall back to VN description
  in EN locale until skills.csv adds an English column.
- **Trait stat effects runtime resolver**: Sprint 9+ Round 2 wired
  Affliction/Virtue *rolls* + UI display, but the trait effects themselves
  (Cowardly skip-turn, Masochist stress-to-all, Paranoid stress aura, etc.)
  are still data-only — `Trigger` column in `diseases_traits.csv` is not
  consumed at runtime yet. Needs a `TraitEffectResolver` akin to the
  existing data-driven `ActiveEffects`. Sprint 10.
- **Supplies Tax + Caretaker cure slots wiring**: constants exist in
  `HamletService` (`SUPPLIES_TAX_STAGE_*`, `CARETAKER_CURE_SLOTS_BY_LEVEL`)
  but are never consumed. Wire in Sprint 10 alongside Hamlet upgrade UI.
- [ ] Particle effects khi crit/affliction
- [ ] Splash screen, main menu, settings UI
- [ ] Aseprite-style animation refinement (tween + 2-3 frame)

### Out of scope MVP (đã quyết định cắt)
- ❌ Multiplayer / co-op / PvP
- ❌ Cinematic cutscenes + voice acting
- ❌ Procedural skill generation
- ❌ Mobile / console port
- ❌ Achievement system in-game
- ❌ Controller native support
- ❌ Adaptive music
- ❌ Dynamic lighting
- ❌ Crafting / equipment forging

### Đã chốt — design decisions (2026-05-11)
- ✅ **Affliction/Virtue ratio 70/30** — giữ độ khó cao, skills/items đa dạng bổ trợ
- ✅ **Boss HP scaling**: b01=80, b02=110, b03=150 (+30 HP/stage)
- ✅ **Crit multiplier 1.5×**
- ✅ **Disease chance 30%/hero/stage**
- ✅ **Final game name: "Project Shadow"**
- ✅ **Hire cost by rarity**: 50g Common / 80g Rare / 150g Legendary
- ✅ **3-tier Hamlet upgrade system**: Stagecoach/Guild/Survivalist/Caretaker × 3 levels. Costs gold + Heirloom (boss drop). Full upgrade = 3,280g + 14 Heirloom (~4-5 boss kills). Xem [project_plan.html § 5](docs/project_plan.html)
- ✅ **Caretaker cure slots**: Lv1=1, Lv2=2, Lv3=4 heroes/visit. Cost 30g→25g→20g. Cursed NOT cured
- ✅ **Heirloom currency #2**: boss drop 1/2/4 (Stage 1/2/3), chỉ dùng upgrade
- ✅ **Gold persistence Option C — Supplies Tax**: 100g/200g/400g up-front, non-refundable
- ✅ **Boss gold rewards**: 200g/400g/1000g (Stage 1/2/3)
- ✅ **No gold cap**

### Còn cần quyết định
- ❓ Random damage variance ±5% vs ±10%
- ❓ Stress max uniform 100 vs differentiated by rarity

---

## 📂 Files đã thay đổi trong session này

| File | Change |
|---|---|
| `assets/data/enemies.csv` | +4 rows: enemy_b02, enemy_b03, enemy_mb01, enemy_mb02 |
| `assets/data/enemy_skills.csv` | +14 rows: skills cho 4 boss/miniboss mới |
| `core/src/test/.../DataIntegrityTest.java` | Update expected counts (11→15 enemies, 9→23 skills) |
| `core/src/main/.../DataLoaderDemo.java` | Update KNOWN_TODO_REFS (remove b02/b03/mb01/mb02 sau khi chính thức có data) |
| `.pixellab-ids.json` | Đầy đủ UUIDs cho 14 heroes + 15 enemies + 5 items + 3 tilesets |
| `docs/style.css` | Shared CSS cho 6 HTML docs |
| `docs/design_overview.html` | NEW |
| `docs/game_systems.html` | NEW |
| `docs/technical_data.html` | NEW |
| `docs/pathway_stage1.html` | NEW |
| `docs/project_plan.html` | NEW |
| `docs/notes_and_considerations.html` | NEW |
| `claude.md` (file này) | NEW |

---

*Last update: 2026-05-12 · Tests: **488 passing** ✅ · PixelLab assets: 33 chars/enemies/tiles + **27 UI components** (Round 3+4) · Sprint 9+ Round 4 (UI asset pack Phase 2) merged via PR #24*
