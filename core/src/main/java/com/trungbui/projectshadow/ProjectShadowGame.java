package com.trungbui.projectshadow;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.trungbui.projectshadow.audio.AudioManager;
import com.trungbui.projectshadow.combat.CombatReward;
import com.trungbui.projectshadow.combat.CombatRewardRoller;
import com.trungbui.projectshadow.combat.CombatScenario;
import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.meta.HamletService;
import com.trungbui.projectshadow.meta.MetaState;
import com.trungbui.projectshadow.meta.MetaStateManager;
import com.trungbui.projectshadow.meta.SettingsManager;
import com.trungbui.projectshadow.run.RunSession;
import com.trungbui.projectshadow.save.HeroState;
import com.trungbui.projectshadow.save.SaveManager;
import com.trungbui.projectshadow.screens.CaretakerScreen;
import com.trungbui.projectshadow.screens.CombatScreen;
import com.trungbui.projectshadow.screens.EmbarkSelectionScreen;
import com.trungbui.projectshadow.screens.GameOverScreen;
import com.trungbui.projectshadow.screens.GuildScreen;
import com.trungbui.projectshadow.screens.HamletScreen;
import com.trungbui.projectshadow.screens.MainMenuScreen;
import com.trungbui.projectshadow.screens.NodeInfoScreen;
import com.trungbui.projectshadow.screens.SettingsScreen;
import com.trungbui.projectshadow.screens.SplashScreen;
import com.trungbui.projectshadow.screens.StagecoachScreen;
import com.trungbui.projectshadow.screens.StageMapScreen;
import com.trungbui.projectshadow.screens.SurvivalistScreen;
import com.trungbui.projectshadow.combat.RewardCardSampler;
import com.trungbui.projectshadow.screens.RewardCardPickerPopup;
import com.trungbui.projectshadow.screens.RunSummaryScreen;
import com.trungbui.projectshadow.screens.VictoryScreen;
import com.trungbui.projectshadow.stage.BossNode;
import com.trungbui.projectshadow.stage.CombatNode;
import com.trungbui.projectshadow.stage.EliteNode;
import com.trungbui.projectshadow.stage.MinibossNode;
import com.trungbui.projectshadow.stage.StageNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProjectShadowGame extends Game {

    public static final int VIRTUAL_WIDTH = 1920;
    public static final int VIRTUAL_HEIGHT = 1080;

    private static final List<String> DEFAULT_ROSTER =
            List.of("hero_01", "hero_13", "hero_05", "hero_03");
    private static final String DEFAULT_STAGE = "stage_1";

    /** Sprint 10 B2 — extract stage act (1/2/3) from a stageId like "stage_1". */
    static int stageActFromId(String stageId) {
        if (stageId == null) return 0;
        int us = stageId.lastIndexOf('_');
        if (us < 0 || us == stageId.length() - 1) return 0;
        try {
            return Integer.parseInt(stageId.substring(us + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Sprint 10 B1 — formerly hardcoded {@code 42L}. Now generated per-run by
     *  {@link #generateRunSeed()} unless the env var {@code SHADOW_FIXED_SEED}
     *  is set (dev/test override). Random seed → each run has a unique stage map. */
    private static long generateRunSeed() {
        String fixed = System.getenv("SHADOW_FIXED_SEED");
        if (fixed != null && !fixed.isBlank()) {
            try {
                return Long.parseLong(fixed.trim());
            } catch (NumberFormatException ignored) {
                // Fall through to random.
            }
        }
        return new java.util.Random().nextLong();
    }

    private GameData gameData;
    private SaveManager saveManager;
    private MetaStateManager metaManager;
    private MetaState meta;
    private RunSession runSession;
    private AudioManager audio;
    /** Sprint 12 B4 — persistent user settings (audio volume, fullscreen, locale). */
    private SettingsManager settings;

    @Override
    public void create() {
        gameData = loadGameData();
        Path savesDir = Gdx.files.local("saves").file().toPath();
        saveManager = new SaveManager(savesDir);
        metaManager = new MetaStateManager(savesDir);
        audio = new AudioManager();
        // Sprint 12 B4 — load settings + apply audio volumes + locale.
        settings = new SettingsManager(savesDir);
        settings.load();
        audio.setMusicVolume(settings.musicVolume());
        audio.setSfxVolume(settings.sfxVolume());
        com.trungbui.projectshadow.i18n.I18n.setLocale(
                java.util.Locale.of(settings.locale()));
        try {
            meta = metaManager.loadOrInit(gameData, DEFAULT_ROSTER);
            metaManager.save(meta);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load/save meta state", e);
        }
        // Sprint 12 B4 — boot into SplashScreen → MainMenu → HamletScreen.
        setScreen(new SplashScreen(this));
    }

    public SettingsManager settings() {
        return settings;
    }

    public void openMainMenu() {
        Screen prev = getScreen();
        setScreen(new MainMenuScreen(this));
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    public void openSettings() {
        Screen prev = getScreen();
        setScreen(new SettingsScreen(this));
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (audio != null) audio.dispose();
    }

    public AudioManager audio() {
        return audio;
    }

    public GameData gameData() {
        return gameData;
    }

    public SaveManager saveManager() {
        return saveManager;
    }

    public MetaStateManager metaManager() {
        return metaManager;
    }

    public MetaState meta() {
        return meta;
    }

    public RunSession runSession() {
        return runSession;
    }

    /** Persist a new {@link MetaState}. Called after every Hamlet action. */
    public void applyMeta(MetaState newMeta) {
        this.meta = newMeta;
        try {
            metaManager.save(newMeta);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save meta state", e);
        }
    }

    // ---------- Hamlet hub navigation ----------

    public void returnToHamlet() {
        Screen prev = getScreen();
        runSession = null; // any active run is finalized when we land here
        setScreen(new HamletScreen(this));
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    public void openStagecoach() {
        Screen prev = getScreen();
        setScreen(new StagecoachScreen(this));
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    public void openGuild() {
        Screen prev = getScreen();
        setScreen(new GuildScreen(this));
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    public void openSurvivalist() {
        Screen prev = getScreen();
        setScreen(new SurvivalistScreen(this));
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    public void openCaretaker() {
        Screen prev = getScreen();
        setScreen(new CaretakerScreen(this));
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    public void openEmbarkSelection() {
        Screen prev = getScreen();
        setScreen(new EmbarkSelectionScreen(this));
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    // ---------- Run lifecycle ----------

    /** Resume the most-recently-saved active run if any exists. */
    public void resumeLatestRun() {
        try {
            List<String> active = saveManager.listActiveSaves();
            if (active.isEmpty()) return;
            String runId = active.getLast(); // most recent
            runSession = RunSession.resume(gameData, saveManager, runId);
            Screen prev = getScreen();
            setScreen(new StageMapScreen(this));
            if (prev != null && prev != getScreen()) prev.dispose();
        } catch (IOException e) {
            throw new RuntimeException("Failed to resume run", e);
        }
    }

    public boolean hasActiveRun() {
        try {
            return !saveManager.listActiveSaves().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    /** Start a new run with the chosen 4 heroes (must already be in the meta roster).
     *  <p>Sprint 10 B2: deducts supplies tax (Option C) before run starts. If player
     *  can't afford, throws {@code HamletException} — caller (EmbarkSelectionScreen)
     *  should pre-validate via {@code HamletService.suppliesTax(stageAct)}.</p> */
    public void startNewRun(List<String> heroIds) {
        Screen prev = getScreen();
        // Sprint 10 B2 — supplies tax up-front. Non-refundable.
        int stageAct = stageActFromId(DEFAULT_STAGE);
        MetaState afterTax = HamletService.paySuppliesTax(meta, stageAct);
        applyMeta(afterTax);
        // Sprint 10 B1: if roster exceeds SOFT_ROSTER_CAP, auto-cull random excess heroes
        // (none in the picked party). This is destructive — heroes are permanently dropped.
        // Player was warned by HamletScreen UI when cap was exceeded.
        if (meta.isRosterOverCap()) {
            MetaState culled = HamletService.autoCullRosterToCap(meta, heroIds);
            applyMeta(culled);
        }
        // Use a snapshot of the heroes' meta state as their starting state for the run.
        // RunSession.startNew creates fresh Hero instances; to carry over level/HP/stress,
        // we restore each from its HeroState snapshot in meta.roster after creation.
        // Sprint 10 B1: seed is now per-run (was hardcoded 42L). Stage map varies
        // between runs unless SHADOW_FIXED_SEED env var pins it (dev/test).
        long runSeed = generateRunSeed();
        runSession = RunSession.startNew(gameData, saveManager, DEFAULT_STAGE, runSeed, heroIds);
        // overlay roster snapshot onto party (level/hp/stress/diseases/cooldowns)
        for (int i = 0; i < heroIds.size(); i++) {
            HeroState rs = meta.heroInRoster(heroIds.get(i)).orElseThrow();
            var live = runSession.party().get(i);
            live.setLevel(rs.level());
            live.setCurrentHp(rs.currentHp());
            live.setCurrentStress(rs.currentStress());
            for (String t : rs.traits()) live.addTrait(t);
            for (String d : rs.diseases()) live.addDisease(d);
            for (var e : rs.skillCooldowns().entrySet()) live.putOnCooldown(e.getKey(), e.getValue());
        }
        setScreen(new StageMapScreen(this));
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    /** Called by {@link StageMapScreen} when a clickable node is picked. */
    public void enterNode(StageNode node) {
        Screen prev = getScreen();
        switch (node) {
            case CombatNode c -> startCombat(c.label(), c.enemies(), false);
            case EliteNode e -> startCombat(e.label(), e.enemies(), true);
            case MinibossNode mb -> startCombat(mb.label(), List.of(mb.minibossId()), true);
            case BossNode boss -> startCombat(boss.label(), List.of(boss.bossId()), true);
            default -> setScreen(new NodeInfoScreen(this, node));
        }
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    /** Called by {@link NodeInfoScreen} when the player presses Continue. */
    public void finishCurrentNonCombatNode(StageNode node) {
        Screen prev = getScreen();

        // Sprint 9 (combat-reward-system) — reward nodes previously displayed drops
        // but never granted them. Apply the pre-rolled drop list to the run state.
        if (node instanceof com.trungbui.projectshadow.stage.RewardNode rn) {
            applyRewardNodeDrops(rn);
        }

        try {
            runSession.completeNode(node.label());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save run after node " + node.label(), e);
        }
        if (runSession.isPartyDead()) {
            applyOutcomeAndArchive(false);
            setScreen(new GameOverScreen(this));
        } else {
            setScreen(new StageMapScreen(this));
        }
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    /** Apply each {@code DropEntry} on a reward node to the run state.
     *  Sprint 9+ B2: RNG is now seeded from {@code stageSeed ^ nodeLabel.hashCode()}
     *  so the same run on the same seed produces deterministic reward drops. */
    private void applyRewardNodeDrops(com.trungbui.projectshadow.stage.RewardNode rn) {
        long seed = runSession.state().stageSeed() ^ rn.label().hashCode();
        java.util.Random rng = new java.util.Random(seed);
        int goldDelta = 0;
        List<String> items = new ArrayList<>();
        for (var drop : rn.drops()) {
            switch (drop.type() == null ? "" : drop.type()) {
                case "gold" -> {
                    int lo = drop.valueMin() != null ? drop.valueMin() : 0;
                    int hi = drop.valueMax() != null ? drop.valueMax() : lo;
                    goldDelta += (hi > lo) ? lo + rng.nextInt(hi - lo + 1) : lo;
                }
                case "item" -> {
                    if (drop.itemId() != null) items.add(drop.itemId());
                }
                case "item_random" -> {
                    // Sprint 9+ B2: resolve category to an actual item ID via the shared
                    // helper in CombatRewardRoller. Previously this added the raw category
                    // string ("trinket_common") to inventory.
                    String resolved = CombatRewardRoller.resolveRandomItem(drop.category(), gameData, rng);
                    if (resolved != null) items.add(resolved);
                }
                default -> {
                    // unknown drop type, ignore
                }
            }
        }
        runSession.applyCombatReward(new CombatReward(goldDelta, items, null, 0));
    }

    /**
     * Switch to a {@link CombatScreen} for the given node label and enemy roster.
     *
     * <p><strong>Caller owns the dispose lifecycle.</strong> This method only swaps the active
     * screen — it deliberately does NOT call {@code prev.dispose()}. Previously both this
     * method and {@link #enterNode(StageNode)} disposed the outgoing screen, which caused
     * a double-dispose crash on the {@link StageMapScreen}'s internal {@code SpriteBatch}
     * ({@code "buffer not allocated with newUnsafeByteBuffer or already disposed"}).</p>
     */
    private void startCombat(String nodeLabel, List<String> enemyIds, boolean showCardPicker) {
        CombatEncounter encounter = CombatScenario.buildWithHeroes(
                gameData, runSession.party(), enemyIds);
        Runnable onWin = () -> handleCombatWin(nodeLabel);
        Runnable onLose = () -> handleCombatLoss(nodeLabel);
        CombatScreen cs = new CombatScreen(gameData, encounter, onWin, onLose);
        cs.setAudio(audio); // must be before setScreen so show() picks it up
        // Sprint 12 B3 — wire run session so Items tab can read inventory.
        cs.setRunSession(runSession);
        // Sprint 13 B3 — wire stage ID for environmental modifiers.
        cs.setStageId(runSession.state().stageId());

        if (showCardPicker) {
            // Sprint 13 B2 — Elite / Miniboss / Boss: offer 3 rarity-weighted item cards.
            // Seeded separately from the gold reward so the two rolls are independent.
            long cardSeed = runSession.state().stageSeed() ^ nodeLabel.hashCode() ^ 0xC0DEL;
            cs.setCardPickerProvider(
                    () -> RewardCardSampler.sample3(
                            gameData.items().values(), new java.util.Random(cardSeed)),
                    pickedItem -> {
                        if (pickedItem != null) {
                            // Add the chosen item to the run inventory before handleCombatWin runs.
                            runSession.setStateForItemUse(
                                    runSession.state().withInventoryAdd(pickedItem.itemId()));
                        }
                    });
        } else {
            // Sprint 10 B3 — supply reward provider for CombatRewardPopup. The popup
            // displays this reward; the actual apply runs in handleCombatWin after
            // onWin fires. Both calls produce identical results (deterministic
            // RNG seeding from Sprint 9+ B2), so the display can't drift from reality.
            cs.setRewardProvider(() -> {
                StageNode resolvedNode = runSession.stageTree().getNode(nodeLabel).orElse(null);
                return rollRewardForNode(resolvedNode);
            });
        }
        setScreen(cs);
        // dispose is handled by enterNode() — single ownership
    }

    private void handleCombatWin(String nodeLabel) {
        Screen prev = getScreen();

        // Roll combat rewards BEFORE completeNode so the gold/items get persisted in the
        // single save call below. Sprint 9 (combat-reward-system) — fixes the bug where
        // boss kills granted no gold and per-combat drops never fired.
        StageNode resolvedNode = runSession.stageTree().getNode(nodeLabel).orElse(null);
        CombatReward reward = rollRewardForNode(resolvedNode);
        runSession.applyCombatReward(reward);

        // Sprint 13 B2 — record enemies killed in this combat.
        if (resolvedNode != null) {
            int defeatedCount = switch (resolvedNode) {
                case CombatNode c -> c.enemies().size();
                case EliteNode e -> e.enemies().size();
                case MinibossNode mb -> 1;
                case BossNode b -> 1;
                default -> 0;
            };
            if (defeatedCount > 0) runSession.recordEnemiesKilled(defeatedCount);
        }

        // Sprint 10 B2 — boss kill grants Heirloom currency into meta (was reserved
        // Sprint 8.5 followup; now wired). 1/2/4 per Stage 1/2/3 boss.
        if (resolvedNode instanceof BossNode) {
            int stageAct = stageActFromId(runSession.state().stageId());
            int heirloomDrop = HamletService.heirloomFromBoss(stageAct);
            if (heirloomDrop > 0) {
                applyMeta(meta.withHeirloomDelta(heirloomDrop));
                // Sprint 13 B2 — also record heirloom earned in run state for summary.
                runSession.recordHeirloomEarned(heirloomDrop);
            }
            // Sprint 12 B2 — end-of-stage XP bonus. Each alive hero earns
            // +END_OF_STAGE_XP (default 50) on boss kill. Dead heroes miss it.
            for (com.trungbui.projectshadow.domain.Hero h : runSession.party()) {
                if (h.isAlive()) {
                    h.addXp(HamletService.END_OF_STAGE_XP);
                }
            }
        } else {
            // Sprint 12 B2 — post-combat disease roll. 30% per alive hero per
            // non-boss combat win. Pool = dis_01..dis_05. Skip heroes who
            // already have the rolled disease.
            applyPostCombatDiseaseRoll(nodeLabel);
        }

        try {
            runSession.completeNode(nodeLabel);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save run after combat at " + nodeLabel, e);
        }
        if (runSession.isOnBossNode()) {
            applyOutcomeAndArchive(true);
            // Sprint 13 B2 — route through RunSummaryScreen before returning to Hamlet.
            var summaryState = runSession.state();
            var summaryParty = new ArrayList<>(runSession.party());
            setScreen(new RunSummaryScreen(this, summaryState, summaryParty, true));
        } else {
            setScreen(new StageMapScreen(this));
        }
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    /**
     * Sprint 12 B2 — roll 30% chance per alive hero to catch a random disease
     * after a non-boss combat win. Delegates to {@link HamletService#rollPostCombatDisease}
     * for the actual roll so logic is unit-testable.
     *
     * <p>RNG is seeded deterministically from
     * {@code stageSeed ^ nodeLabel.hashCode() ^ 0xDEADBEEF} so the same
     * (run, node) always rolls the same outcome.</p>
     */
    private void applyPostCombatDiseaseRoll(String nodeLabel) {
        long seed = runSession.state().stageSeed() ^ nodeLabel.hashCode() ^ 0xDEADBEEFL;
        java.util.Random rng = new java.util.Random(seed);
        for (com.trungbui.projectshadow.domain.Hero h : runSession.party()) {
            HamletService.rollPostCombatDisease(h, rng);
        }
    }

    /** Compute the reward for a freshly-defeated combat node. Returns empty if node is null.
     *  Sprint 9+ B2: seeds the RNG from {@code stageSeed ^ nodeLabel.hashCode()} so the
     *  same (run, node) always rolls the same reward — fixes the bug where a crash
     *  mid-combat could produce different rewards on replay.
     *  Sprint 13 B2: passes the current combo streak for gold multiplier. */
    private CombatReward rollRewardForNode(StageNode node) {
        if (node == null) return CombatReward.empty();
        // Defeated enemy IDs (works for Combat/Elite/Miniboss/Boss nodes)
        List<String> defeated = switch (node) {
            case CombatNode c -> c.enemies();
            case EliteNode e -> e.enemies();
            case MinibossNode mb -> List.of(mb.minibossId());
            case BossNode boss -> List.of(boss.bossId());
            default -> List.of();
        };
        // Live (alive) heroes for stress-relief targeting
        List<com.trungbui.projectshadow.domain.Hero> alive = runSession.party().stream()
                .filter(h -> h.currentHp() > 0).toList();
        long seed = runSession.state().stageSeed() ^ node.label().hashCode();
        int streak = runSession.state().consecutiveNodesCleared();
        return CombatRewardRoller.roll(node, defeated, alive, gameData, new java.util.Random(seed), streak);
    }

    private void handleCombatLoss(String nodeLabel) {
        Screen prev = getScreen();
        try {
            runSession.completeNode(nodeLabel);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save run after defeat at " + nodeLabel, e);
        }
        applyOutcomeAndArchive(false);
        // Sprint 13 B2 — route through RunSummaryScreen before Hamlet.
        var summaryState = runSession.state();
        var summaryParty = new ArrayList<>(runSession.party());
        setScreen(new RunSummaryScreen(this, summaryState, summaryParty, false));
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    /**
     * Sync the run result into the meta roster + wallet, then archive the run save.
     * On victory, surviving heroes' states overwrite their roster snapshot and the run gold
     * is added to the wallet. On defeat, dead heroes are removed from the roster.
     */
    private void applyOutcomeAndArchive(boolean victory) {
        if (runSession == null) return;
        MetaState newMeta = HamletService.applyRunOutcome(meta, runSession, victory);
        applyMeta(newMeta);
        try {
            runSession.archiveOnDeath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to archive run save", e);
        }
    }

    private static GameData loadGameData() {
        try {
            Path explicitDir = Path.of("assets", "data");
            if (java.nio.file.Files.isDirectory(explicitDir)) {
                return GameData.loadFromDirectory(explicitDir);
            }
            Path internal = Path.of(Gdx.files.internal("data").file().getAbsolutePath());
            return GameData.loadFromDirectory(internal);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load game data", e);
        }
    }

}
