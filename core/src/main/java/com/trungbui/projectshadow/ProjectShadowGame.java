package com.trungbui.projectshadow;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.trungbui.projectshadow.audio.AudioManager;
import com.trungbui.projectshadow.combat.CombatScenario;
import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.meta.HamletService;
import com.trungbui.projectshadow.meta.MetaState;
import com.trungbui.projectshadow.meta.MetaStateManager;
import com.trungbui.projectshadow.run.RunSession;
import com.trungbui.projectshadow.save.HeroState;
import com.trungbui.projectshadow.save.SaveManager;
import com.trungbui.projectshadow.screens.CaretakerScreen;
import com.trungbui.projectshadow.screens.CombatScreen;
import com.trungbui.projectshadow.screens.EmbarkSelectionScreen;
import com.trungbui.projectshadow.screens.GameOverScreen;
import com.trungbui.projectshadow.screens.GuildScreen;
import com.trungbui.projectshadow.screens.HamletScreen;
import com.trungbui.projectshadow.screens.NodeInfoScreen;
import com.trungbui.projectshadow.screens.StagecoachScreen;
import com.trungbui.projectshadow.screens.StageMapScreen;
import com.trungbui.projectshadow.screens.SurvivalistScreen;
import com.trungbui.projectshadow.screens.VictoryScreen;
import com.trungbui.projectshadow.stage.BossNode;
import com.trungbui.projectshadow.stage.CombatNode;
import com.trungbui.projectshadow.stage.EliteNode;
import com.trungbui.projectshadow.stage.MinibossNode;
import com.trungbui.projectshadow.stage.StageNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ProjectShadowGame extends Game {

    public static final int VIRTUAL_WIDTH = 1920;
    public static final int VIRTUAL_HEIGHT = 1080;

    private static final List<String> DEFAULT_ROSTER =
            List.of("hero_01", "hero_13", "hero_05", "hero_03");
    private static final String DEFAULT_STAGE = "stage_1";
    private static final long DEFAULT_SEED = 42L;

    private GameData gameData;
    private SaveManager saveManager;
    private MetaStateManager metaManager;
    private MetaState meta;
    private RunSession runSession;
    private AudioManager audio;

    @Override
    public void create() {
        gameData = loadGameData();
        Path savesDir = Gdx.files.local("saves").file().toPath();
        saveManager = new SaveManager(savesDir);
        metaManager = new MetaStateManager(savesDir);
        audio = new AudioManager();
        try {
            meta = metaManager.loadOrInit(gameData, DEFAULT_ROSTER);
            metaManager.save(meta);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load/save meta state", e);
        }
        setScreen(new HamletScreen(this));
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
            String runId = active.get(active.size() - 1); // most recent
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

    /** Start a new run with the chosen 4 heroes (must already be in the meta roster). */
    public void startNewRun(List<String> heroIds) {
        Screen prev = getScreen();
        // Use a snapshot of the heroes' meta state as their starting state for the run.
        // RunSession.startNew creates fresh Hero instances; to carry over level/HP/stress,
        // we restore each from its HeroState snapshot in meta.roster after creation.
        runSession = RunSession.startNew(gameData, saveManager, DEFAULT_STAGE, DEFAULT_SEED, heroIds);
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
            case CombatNode c -> startCombat(c.label(), c.enemies());
            case EliteNode e -> startCombat(e.label(), e.enemies());
            case MinibossNode mb -> startCombat(mb.label(), List.of(mb.minibossId()));
            case BossNode boss -> startCombat(boss.label(), List.of(boss.bossId()));
            default -> setScreen(new NodeInfoScreen(this, node));
        }
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    /** Called by {@link NodeInfoScreen} when the player presses Continue. */
    public void finishCurrentNonCombatNode(StageNode node) {
        Screen prev = getScreen();
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

    /**
     * Switch to a {@link CombatScreen} for the given node label and enemy roster.
     *
     * <p><strong>Caller owns the dispose lifecycle.</strong> This method only swaps the active
     * screen — it deliberately does NOT call {@code prev.dispose()}. Previously both this
     * method and {@link #enterNode(StageNode)} disposed the outgoing screen, which caused
     * a double-dispose crash on the {@link StageMapScreen}'s internal {@code SpriteBatch}
     * ({@code "buffer not allocated with newUnsafeByteBuffer or already disposed"}).</p>
     */
    private void startCombat(String nodeLabel, List<String> enemyIds) {
        CombatEncounter encounter = CombatScenario.buildWithHeroes(
                gameData, runSession.party(), enemyIds);
        Runnable onWin = () -> handleCombatWin(nodeLabel);
        Runnable onLose = () -> handleCombatLoss(nodeLabel);
        CombatScreen cs = new CombatScreen(gameData, encounter, onWin, onLose);
        cs.setAudio(audio); // must be before setScreen so show() picks it up
        setScreen(cs);
        // dispose is handled by enterNode() — single ownership
    }

    private void handleCombatWin(String nodeLabel) {
        Screen prev = getScreen();
        try {
            runSession.completeNode(nodeLabel);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save run after combat at " + nodeLabel, e);
        }
        if (runSession.isOnBossNode()) {
            applyOutcomeAndArchive(true);
            setScreen(new VictoryScreen(this));
        } else {
            setScreen(new StageMapScreen(this));
        }
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    private void handleCombatLoss(String nodeLabel) {
        Screen prev = getScreen();
        try {
            runSession.completeNode(nodeLabel);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save run after defeat at " + nodeLabel, e);
        }
        applyOutcomeAndArchive(false);
        setScreen(new GameOverScreen(this));
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
