package com.trungbui.projectshadow.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
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
import java.util.List;
import java.util.Random;

public class CombatScreen implements Screen {

    private static final int LOG_LINES = 4;

    private final GameData gameData;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final SpriteBatch batch;
    private final Skin skin;
    private final Stage uiStage;

    private final CombatRenderer renderer;
    private final CombatController controller;

    private List<CombatantView> heroViews = List.of();
    private List<CombatantView> enemyViews = List.of();

    private final Table skillTable;
    private final Label statusLabel;
    private final Label logLabel;
    private final List<String> logBuffer = new ArrayList<>();
    private final List<TextButton> skillButtons = new ArrayList<>();
    private float autoAdvanceTimer = 0f;

    public CombatScreen(GameData gameData) {
        this.gameData = gameData;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(CombatRenderer.VIRTUAL_WIDTH, CombatRenderer.VIRTUAL_HEIGHT, camera);
        this.batch = new SpriteBatch();
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        this.uiStage = new Stage(viewport, batch);
        this.renderer = new CombatRenderer();

        CombatEncounter encounter = CombatScenario.buildDefault(gameData);
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
                refreshSkillButtons();
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
            btn.setDisabled(hero.isOnCooldown(skill.skillId()));
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    if (btn.isDisabled()) return;
                    controller.executePlayerSkill(index);
                }
            });
            skillTable.add(btn).pad(8).width(280).height(60);
            skillButtons.add(btn);
        }
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
        autoAdvanceTimer = Math.max(0f, autoAdvanceTimer - delta);
        for (CombatantView v : heroViews) v.update(delta);
        for (CombatantView v : enemyViews) v.update(delta);

        renderer.renderBackground();
        camera.update();

        renderer.renderCombatants(camera, heroViews, enemyViews, controller.currentActor().orElse(null));

        uiStage.act(delta);
        uiStage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(uiStage);
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
        batch.dispose();
    }
}
