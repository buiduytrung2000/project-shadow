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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.trungbui.projectshadow.ui.FontFactory;
import com.trungbui.projectshadow.audio.AudioManager;
import com.trungbui.projectshadow.i18n.I18n;
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
import com.trungbui.projectshadow.render.ParticleSystem;

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
    private final ParticleSystem particles = new ParticleSystem();
    private final Runnable onWin;
    private final Runnable onLose;
    /** Sprint 10 B3 — supplies the reward to show in CombatRewardPopup at end of
     *  a victorious combat. Null in standalone mode (no popup shown). */
    private java.util.function.Supplier<com.trungbui.projectshadow.combat.CombatReward> rewardProvider;

    private List<CombatantView> heroViews = List.of();
    private List<CombatantView> enemyViews = List.of();

    private final Table skillTable;
    private final Label statusLabel;
    private final Label logLabel;
    private final List<String> logBuffer = new ArrayList<>();
    private final List<TextButton> skillButtons = new ArrayList<>();
    private SkillDescriptionPanel skillTooltip;

    private UiState uiState = UiState.IDLE;
    private int pendingSkillIndex = -1;
    private final Set<Combatant> highlightedTargets = new HashSet<>();
    private boolean transitioned = false;
    private AudioManager audio;

    /** Standalone combat (no run loop wiring). Used for the Sprint 5 demo. */
    public CombatScreen(GameData gameData) {
        this(gameData, CombatScenario.buildDefault(gameData), null, null);
    }

    /** Wire optional audio after construction (Sprint 9). */
    public void setAudio(AudioManager audio) {
        this.audio = audio;
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
                triggerAttackView(attacker);
                triggerHitView(target);
                pushLog(formatActionLog(attacker, target, skill, result));
                playActionSfx(skill, result);
                emitActionParticles(target, skill, result);
            }

            @Override
            public void onTurnAdvanced(Combatant nextActor) {
                exitTargetMode();
                refreshSkillButtons();
                refreshStatus(nextActor);
            }

            @Override
            public void onRoundStarted(int roundNumber) {
                pushLog(I18n.t("combat.log.round", roundNumber));
            }

            @Override
            public void onCombatEnded(CombatEncounter.Side winner) {
                pushLog(winner == CombatEncounter.Side.HEROES
                        ? I18n.t("combat.log.victory")
                        : I18n.t("combat.log.defeat"));
                exitTargetMode();
                refreshSkillButtons();
                showContinueButton(winner);
            }

            @Override
            public void onAfflictionResolved(Hero hero, String traitId, boolean affliction) {
                // Sprint 9+ B2: trait name lookup via GameData (best-effort — fall back
                // to traitId if not in catalog). Push to combat log as a notable event.
                String traitName = traitId;
                var t = gameData.diseasesTraits().get(traitId);
                if (t != null) traitName = t.name();
                String key = affliction ? "combat.affliction.resolved" : "combat.virtue.resolved";
                pushLog(I18n.t(key, hero.data().displayName(), traitName));
            }

            @Override
            public void onHeroHeartAttack(Hero hero) {
                pushLog(I18n.t("combat.log.heartAttack", hero.data().displayName()));
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

        // Sprint 9 (feature/skill-description-tooltip) — floating tooltip in the
        // top-right corner. Shown on skill-button hover.
        skillTooltip = new SkillDescriptionPanel(skin);
        // Position: top-right with 20px margin from the right edge.
        skillTooltip.setPosition(
                CombatRenderer.VIRTUAL_WIDTH - skillTooltip.panelWidth() - 20f,
                CombatRenderer.VIRTUAL_HEIGHT - 360f
        );
        uiStage.addActor(skillTooltip);

        // Sprint 9+ B2 fix: attach a hide-tooltip listener on the parent skill row so
        // cursor leaving the row (in any direction, onto any non-button actor) hides
        // the panel. Previously per-button "exit" only hid when toActor == null,
        // leaving the tooltip stuck when the cursor moved sideways onto e.g. statusLabel.
        skillTable.addListener(new InputListener() {
            @Override
            public void exit(InputEvent event, float x, float y, int pointer,
                             com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                // Only hide if pointer truly left the skill table (not just moved onto a
                // child actor of it).
                if (skillTooltip == null) return;
                if (toActor == null || !isDescendantOf(toActor, skillTable)) {
                    skillTooltip.hide();
                }
            }
        });
    }

    /** Returns true if {@code actor} is the same as {@code ancestor} or a descendant of it. */
    private static boolean isDescendantOf(com.badlogic.gdx.scenes.scene2d.Actor actor,
                                          com.badlogic.gdx.scenes.scene2d.Actor ancestor) {
        for (com.badlogic.gdx.scenes.scene2d.Actor cur = actor; cur != null; cur = cur.getParent()) {
            if (cur == ancestor) return true;
        }
        return false;
    }

    private void layoutCombatants() {
        heroViews = renderer.layoutHeroes(controller.encounter().heroes());
        enemyViews = renderer.layoutEnemies(controller.encounter().enemies());
    }

    private void refreshSkillButtons() {
        skillTable.clear();
        skillButtons.clear();
        if (skillTooltip != null) skillTooltip.hide();

        Combatant actor = controller.currentActor().orElse(null);
        if (!(actor instanceof Hero hero)) return;
        if (controller.encounter().isCombatOver()) return;

        List<SkillData> skills = controller.currentActorSkills();
        for (int i = 0; i < skills.size(); i++) {
            SkillData skill = skills.get(i);
            final int index = i;
            final SkillData skillForTooltip = skill;
            String label = I18n.t("combat.skillButton", i + 1, skill.displayName());
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
            // Sprint 9 (feature/skill-description-tooltip) — show panel on enter, hide on exit.
            btn.addListener(new InputListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer,
                                  com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    if (skillTooltip != null) skillTooltip.showFor(skillForTooltip);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer,
                                 com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                    // Only hide if the cursor isn't entering another skill button. Stage's
                    // enter/exit fires per actor, so a simple "hide on exit" with the next
                    // button's enter immediately showing the new tooltip is the cleanest UX.
                    if (skillTooltip != null && toActor == null) skillTooltip.hide();
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
        statusLabel.setText(I18n.t("combat.targetHint",
                controller.currentActorSkills().get(skillIndex).displayName()));
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

    /** Sprint 10 B3 — supplies a function that produces the post-victory reward.
     *  Called once at combat end (HEROES win) to fetch the reward to display in
     *  {@link CombatRewardPopup}. Null = no popup (legacy / standalone mode). */
    public void setRewardProvider(java.util.function.Supplier<com.trungbui.projectshadow.combat.CombatReward> p) {
        this.rewardProvider = p;
    }

    private void showContinueButton(CombatEncounter.Side winner) {
        if (onWin == null && onLose == null) return; // standalone mode — no callback wiring
        skillTable.clear();
        skillButtons.clear();

        // Sprint 10 B3 — show reward popup on victory if a provider is wired.
        // Popup auto-dismisses after 3s and runs the onWin transition; manual
        // Continue click bypasses the timer. Defeat path skips popup.
        if (winner == CombatEncounter.Side.HEROES && rewardProvider != null) {
            com.trungbui.projectshadow.combat.CombatReward reward = rewardProvider.get();
            if (reward != null && !reward.isEmpty()) {
                Runnable transition = () -> {
                    if (transitioned) return;
                    transitioned = true;
                    onWin.run();
                };
                CombatRewardPopup popup = new CombatRewardPopup(skin, reward, transition);
                // Center the popup on screen.
                popup.setPosition(
                        (CombatRenderer.VIRTUAL_WIDTH - popup.getWidth()) / 2f,
                        (CombatRenderer.VIRTUAL_HEIGHT - popup.getHeight()) / 2f);
                uiStage.addActor(popup);
                return; // popup owns the transition trigger; skip the legacy button.
            }
        }

        TextButton btn = new TextButton(
                winner == CombatEncounter.Side.HEROES ? I18n.t("combat.continue") : I18n.t("combat.end"),
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
            String result = winner == CombatEncounter.Side.HEROES
                    ? I18n.t("combat.victory")
                    : I18n.t("combat.defeat");
            statusLabel.setText(I18n.t("combat.over", result));
            return;
        }
        if (actor == null) {
            statusLabel.setText("");
            return;
        }
        String name = actor instanceof Hero h ? h.data().displayName()
                : actor instanceof Enemy e ? e.data().name()
                : actor.id();
        String side = actor instanceof Hero ? I18n.t("combat.player") : I18n.t("combat.enemy");
        statusLabel.setText(I18n.t("combat.round",
                controller.encounter().roundNumber(), side, name, actor.id()));
    }

    private void triggerAttackView(Combatant attacker) {
        if (attacker == null) return;
        for (CombatantView v : heroViews) {
            if (v.combatant() == attacker) v.triggerAttack();
        }
        for (CombatantView v : enemyViews) {
            if (v.combatant() == attacker) v.triggerAttack();
        }
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

    private void playActionSfx(SkillData skill, AttackResult r) {
        if (audio == null) return;
        if (!skill.isOffensive()) {
            audio.playSfx("heal");
        } else if (!r.hit()) {
            audio.playSfx("miss");
        } else if (r.crit()) {
            audio.playSfx("crit");
        } else {
            audio.playSfx("hit");
        }
    }

    private void emitActionParticles(Combatant target, SkillData skill, AttackResult r) {
        CombatantView v = findView(target);
        if (v == null) return;
        float cx = v.x() + v.width() / 2f;
        float cy = v.y() + v.height() / 2f;
        if (!skill.isOffensive()) {
            particles.emitHealGlow(cx, cy);
        } else if (r.hit() && r.crit()) {
            particles.emitCritBurst(cx, cy);
        } else if (r.hit()) {
            particles.emitHitSplash(cx, cy);
        }
    }

    private CombatantView findView(Combatant c) {
        for (CombatantView v : heroViews) if (v.combatant() == c) return v;
        for (CombatantView v : enemyViews) if (v.combatant() == c) return v;
        return null;
    }

    private static String formatActionLog(Combatant attacker, Combatant target, SkillData skill, AttackResult r) {
        if (skill.isOffensive() && !r.hit()) {
            return I18n.t("combat.log.miss", attacker.id(), skill.displayName(), target.id());
        }
        if (skill.isOffensive()) {
            return r.crit()
                    ? I18n.t("combat.log.crit", attacker.id(), target.id(), r.hpDamage())
                    : I18n.t("combat.log.hit", attacker.id(), target.id(), r.hpDamage());
        }
        return I18n.t("combat.log.support", attacker.id(), skill.displayName(), target.id());
    }

    @Override
    public void render(float delta) {
        for (CombatantView v : heroViews) v.update(delta);
        for (CombatantView v : enemyViews) v.update(delta);
        particles.update(delta);

        renderer.renderBackground();
        camera.update();

        renderer.renderCombatants(camera, heroViews, enemyViews,
                controller.currentActor().orElse(null), highlightedTargets);

        // Particles drawn on top of combatants but under UI
        renderer.shapes().setProjectionMatrix(camera.combined);
        particles.render(renderer.shapes());

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
        if (audio != null) audio.playMusic("combat_theme", true);
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
