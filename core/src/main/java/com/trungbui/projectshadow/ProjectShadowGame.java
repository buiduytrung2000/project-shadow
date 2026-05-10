package com.trungbui.projectshadow;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.trungbui.projectshadow.combat.CombatScenario;
import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.run.RunSession;
import com.trungbui.projectshadow.save.SaveManager;
import com.trungbui.projectshadow.screens.CombatScreen;
import com.trungbui.projectshadow.screens.GameOverScreen;
import com.trungbui.projectshadow.screens.NodeInfoScreen;
import com.trungbui.projectshadow.screens.StageMapScreen;
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

    private static final List<String> DEFAULT_PARTY =
            List.of("hero_01", "hero_13", "hero_05", "hero_03");
    private static final String DEFAULT_STAGE = "stage_1";
    private static final long DEFAULT_SEED = 42L;

    private GameData gameData;
    private SaveManager saveManager;
    private RunSession runSession;

    @Override
    public void create() {
        gameData = loadGameData();
        saveManager = new SaveManager(Gdx.files.local("saves").file().toPath());
        runSession = RunSession.startNew(
                gameData, saveManager, DEFAULT_STAGE, DEFAULT_SEED, DEFAULT_PARTY);
        setScreen(new StageMapScreen(this));
    }

    public GameData gameData() {
        return gameData;
    }

    public SaveManager saveManager() {
        return saveManager;
    }

    public RunSession runSession() {
        return runSession;
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
            handleDefeat();
        } else {
            setScreen(new StageMapScreen(this));
        }
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    private void startCombat(String nodeLabel, List<String> enemyIds) {
        CombatEncounter encounter = CombatScenario.buildWithHeroes(
                gameData, runSession.party(), enemyIds);
        Runnable onWin = () -> handleCombatWin(nodeLabel);
        Runnable onLose = () -> handleCombatLoss(nodeLabel);
        Screen prev = getScreen();
        setScreen(new CombatScreen(gameData, encounter, onWin, onLose));
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    private void handleCombatWin(String nodeLabel) {
        Screen prev = getScreen();
        try {
            runSession.completeNode(nodeLabel);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save run after combat at " + nodeLabel, e);
        }
        if (runSession.isOnBossNode()) {
            setScreen(new VictoryScreen(this));
        } else {
            setScreen(new StageMapScreen(this));
        }
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    private void handleCombatLoss(String nodeLabel) {
        // Party died inside combat — record the run on the death node, then archive.
        Screen prev = getScreen();
        try {
            runSession.completeNode(nodeLabel);
            runSession.archiveOnDeath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to archive run after defeat at " + nodeLabel, e);
        }
        setScreen(new GameOverScreen(this));
        if (prev != null && prev != getScreen()) prev.dispose();
    }

    private void handleDefeat() {
        try {
            runSession.archiveOnDeath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to archive run on defeat", e);
        }
        setScreen(new GameOverScreen(this));
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
