package com.trungbui.projectshadow.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.trungbui.projectshadow.ui.FontFactory;
import com.trungbui.projectshadow.combat.AttackResult;
import com.trungbui.projectshadow.combat.CombatController;
import com.trungbui.projectshadow.combat.CombatScenario;
import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.SkillData;
import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.render.CombatRenderer;
import com.trungbui.projectshadow.render.CombatantView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class CombatScreen implements Screen {

    private enum UiState { IDLE, AWAITING_TARGET }

    private static final int LOG_LINES = 4;

    private static final int FONT_SIZE_PX = 24;

    private final GameData gameData;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final SpriteBatch batch;
    private final FontFactory fontFactory;
    private final Skin skin;
    private final Stage uiStage;

    private final CombatRenderer renderer;
    private final CombatController controller;
    private final Runnable onWin;
    private final Runnable onLose;

    private List<CombatantView> heroViews = List.of();
    private List<CombatantView> enemyViews = List.of();

    private final Table skillTable;
    private final Label statusLabel;
    private final Label logLabel;
    private final List<String> logBuffer = new ArrayList<>();
    private final List<TextButton> skillButtons = new ArrayList<>();

    private UiState uiState = UiState.IDLE;
    private int pendingSkillIndex = -1;
    private final Set<Combatant> highlightedTargets = new HashSet<>();
    private boolean transitioned = false;

    /** Standalone combat (no run loop wiring). Used for the Sprint 5 demo. */
    public CombatScreen(GameData gameData) {
        this(gameData, CombatScenario.buildDefault(gameData), null, null);
    }

    /**
     * Run-loop combat. {@code onWin}/{@code onLose} fire once after the combat ends and
     * the player presses Continue (currently auto-fires on combat end with a short delay
     * so the result label is visible).
     */
    public CombatScreen(GameData gameData, CombatEncounter encounter, Runnable onWin, Runnable onLose) {
        this.gameData = gameData;
        this.onWin = onWin;
        this.onLose = onLose;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(CombatRenderer.VIRTUAL_WIDTH, CombatRenderer.VIRTUAL_HEIGHT, camera);
        this.batch = new SpriteBatch();
        this.fontFactory = new FontFactory(Gdx.files.internal("fonts/BeVietnamPro-Regular.ttf"));
        BitmapFont vnFont = fontFactory.create(FONT_SIZE_PX);
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        skin.get(Label.LabelStyle.class).font = vnFont;
        skin.get(TextButton.TextButtonStyle.class).font = vnFont;
        this.uiStage = new Stage(viewport, batch);
        this.renderer = new CombatRenderer();

        this.controller = new CombatController(encounter, gameData, new Random());

        this.skillTable = new Table();
        this.statusLabel = new Label("", skin);
        this.logLabel = new Label("", skin);

        buildUi();
        viewport.apply(true);

        controller.setListener(new CombatController.Listener() {
            @Override
            public void onActionResolved(Combatant attacker, Combatant target, SkillData skill, AttackResult result) {
                triggerHitView(target);
                pushLog(formatActionLog(attacker, target, skill, result));
            }

            @Override
            public void onTurnAdvanced(Combatant nextActor) {
                exitTargetMode();
                refreshSkillButtons();
                refreshStatus(nextActor);
            }

            @Override
            public void onRoundStarted(int roundNumber) {
                pushLog("--- Round " + roundNumber + " ---");
            }

            @Override
            public void onCombatEnded(CombatEncounter.Side winner) {
                pushLog("=== " + (winner == CombatEncounter.Side.HEROES ? "VICTORY" : "DEFEAT") + " ===");
                exitTargetMode();
                refreshSkillButtons();
                showContinueButton(winner);
            }
        });

        layoutCombatants();
        controller.start();
    }

    private void buildUi() {
        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(20);

        statusLabel.setColor(Color.WHITE);
        root.add(statusLabel).expandX().left().pad(20).row();

        Table center = new Table();
        center.add().expand();
        root.add(center).expand().fill().row();

        logLabel.setColor(Color.LIGHT_GRAY);
        root.add(logLabel).expandX().left().pad(20).row();

        skillTable.bottom().pad(20);
        root.add(skillTable).expandX().bottom().padBottom(40);

        uiStage.addActor(root);
    }

    private void layoutCombatants() {
        heroViews = renderer.layoutHeroes(controller.encounter().heroes());
        enemyViews = renderer.layoutEnemies(controller.encounter().enemies());
    }

    private void refreshSkillButtons() {
        skillTable.clear();
        skillButtons.clear();

        Combatant actor = controller.currentActor().orElse(null);
        if (!(actor instanceof Hero hero)) return;
        if (controller.encounter().isCombatOver()) return;

        List<SkillData> skills = controller.currentActorSkills();
        for (int i = 0; i < skills.size(); i++) {
            SkillData skill = skills.get(i);
            final int index = i;
            String label = (i + 1) + ". " + skill.nameVn();
            TextButton btn = new TextButton(label, skin);
            boolean disabled = hero.isOnCooldown(skill.skillId()) || !controller.skillIsSupported(index);
            btn.setDisabled(disabled);
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    if (btn.isDisabled()) return;
                    onSkillButtonClicked(index);
                }
            });
            skillTable.add(btn).pad(8).width(280).height(60);
            skillButtons.add(btn);
        }
    }

    private void onSkillButtonClicked(int skillIndex) {
        if (controller.skillRequiresTargetPick(skillIndex)) {
            enterTargetMode(skillIndex);
        } else {
            controller.executePlayerSkill(skillIndex);
        }
    }

    private void enterTargetMode(int skillIndex) {
        uiState = UiState.AWAITING_TARGET;
        pendingSkillIndex = skillIndex;
        highlightedTargets.clear();
        highlightedTargets.addAll(controller.skillCandidateTargets(skillIndex));
        statusLabel.setText("Pick a target (ESC to cancel) — " +
                controller.currentActorSkills().get(skillIndex).nameVn());
    }

    private void exitTargetMode() {
        uiState = UiState.IDLE;
        pendingSkillIndex = -1;
        highlightedTargets.clear();
    }

    private void confirmTarget(Combatant target) {
        if (uiState != UiState.AWAITING_TARGET) return;
        if (!highlightedTargets.contains(target)) return;
        int idx = pendingSkillIndex;
        exitTargetMode();
        controller.executePlayerSkill(idx, target);
    }

    private void showContinueButton(CombatEncounter.Side winner) {
        if (onWin == null && onLose == null) return; // standalone mode — no callback wiring
        skillTable.clear();
        skillButtons.clear();
        TextButton btn = new TextButton(
                winner == CombatEncounter.Side.HEROES ? "Tiếp tục" : "Kết thúc",
                skin);
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                if (transitioned) return;
                transitioned = true;
                if (winner == CombatEncounter.Side.HEROES && onWin != null) onWin.run();
                else if (onLose != null) onLose.run();
            }
        });
        skillTable.add(btn).pad(8).width(280).height(60);
    }

    private void refreshStatus(Combatant actor) {
        if (controller.encounter().isCombatOver()) {
            CombatEncounter.Side winner = controller.encounter().winningSide();
            statusLabel.setText("Combat over — " + (winner == CombatEncounter.Side.HEROES ? "VICTORY" : "DEFEAT"));
            return;
        }
        if (actor == null) {
            statusLabel.setText("");
            return;
        }
        String name = actor instanceof Hero h ? h.data().nameVn() : actor instanceof Enemy e ? e.data().name() : actor.id();
        String side = actor instanceof Hero ? "PLAYER_TURN" : "ENEMY_TURN";
        statusLabel.setText("Round " + controller.encounter().roundNumber()
                + " — " + side + " — " + name + " (" + actor.id() + ")");
    }

    private void triggerHitView(Combatant target) {
        for (CombatantView v : heroViews) {
            if (v.combatant() == target) v.triggerHit();
        }
        for (CombatantView v : enemyViews) {
            if (v.combatant() == target) v.triggerHit();
        }
    }

    private void pushLog(String line) {
        logBuffer.add(line);
        while (logBuffer.size() > LOG_LINES) logBuffer.remove(0);
        logLabel.setText(String.join("\n", logBuffer));
    }

    private static String formatActionLog(Combatant attacker, Combatant target, SkillData skill, AttackResult r) {
        if (skill.isOffensive() && !r.hit()) {
            return attacker.id() + " used " + skill.nameVn() + " on " + target.id() + " — MISS";
        }
        if (skill.isOffensive()) {
            String crit = r.crit() ? " CRIT!" : "";
            return attacker.id() + " hit " + target.id()
                    + " for " + r.hpDamage() + " HP" + crit
                    + " (" + skill.nameVn() + ")";
        }
        return attacker.id() + " used " + skill.nameVn() + " on " + target.id();
    }

    @Override
    public void render(float delta) {
        for (CombatantView v : heroViews) v.update(delta);
        for (CombatantView v : enemyViews) v.update(delta);

        renderer.renderBackground();
        camera.update();

        renderer.renderCombatants(camera, heroViews, enemyViews,
                controller.currentActor().orElse(null), highlightedTargets);

        uiStage.act(delta);
        uiStage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void show() {
        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(uiStage);
        mux.addProcessor(new CombatInputAdapter());
        Gdx.input.setInputProcessor(mux);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        renderer.dispose();
        uiStage.dispose();
        skin.dispose();
        fontFactory.dispose();
        batch.dispose();
    }

    private class CombatInputAdapter extends InputAdapter {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (uiState != UiState.AWAITING_TARGET) return false;
            Vector3 world = new Vector3(screenX, screenY, 0);
            viewport.unproject(world);
            Combatant clicked = combatantAt(world.x, world.y);
            if (clicked != null && highlightedTargets.contains(clicked)) {
                confirmTarget(clicked);
                return true;
            }
            return false;
        }

        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE && uiState == UiState.AWAITING_TARGET) {
                exitTargetMode();
                refreshStatus(controller.currentActor().orElse(null));
                return true;
            }
            return false;
        }
    }

    private Combatant combatantAt(float worldX, float worldY) {
        for (CombatantView v : heroViews) {
            if (within(v, worldX, worldY)) return v.combatant();
        }
        for (CombatantView v : enemyViews) {
            if (within(v, worldX, worldY)) return v.combatant();
        }
        return null;
    }

    private static boolean within(CombatantView v, float x, float y) {
        return x >= v.x() && x <= v.x() + v.width()
                && y >= v.y() && y <= v.y() + v.height();
    }
}
