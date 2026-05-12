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
import com.trungbui.projectshadow.ui.SkinLoader;
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
import java.util.function.Consumer;

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

    /** Sprint 13 B2 — supplies the 3 item cards for the RewardCardPickerPopup shown
     *  after Elite / Miniboss / Boss victories. Null = use CombatRewardPopup instead. */
    private java.util.function.Supplier<List<com.trungbui.projectshadow.data.model.ItemData>> cardPickerSupplier;
    /** Sprint 13 B2 — invoked with the item the player chooses in RewardCardPickerPopup.
     *  Fires before the onWin transition (null item = empty pool, player dismissed). */
    private Consumer<com.trungbui.projectshadow.data.model.ItemData> cardPickerOnPick;

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

    /** Sprint 12 B3 — when non-null, the skill row is replaced with item buttons.
     *  Cleared when the player presses "Back" or after a successful item use. */
    private boolean inItemMode = false;
    /** Sprint 12 B3 — when the player clicks a heal item, we enter target mode
     *  for that item id (similar to skill target mode). */
    private String pendingItemId = null;
    /** Sprint 12 B3 — the run session backing the inventory. Null in standalone
     *  combat (no items tab in that mode). */
    private com.trungbui.projectshadow.run.RunSession runSession;

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
        this.skin = SkinLoader.load();
        skin.get(Label.LabelStyle.class).font = vnFont;
        skin.get(TextButton.TextButtonStyle.class).font = vnFont;
        this.uiStage = new Stage(viewport, batch);
        this.renderer = new CombatRenderer();

        this.controller = new CombatController(encounter, gameData, new Random());
        // Sprint 11 B1 — pacing: 0.7s between actions so player can follow.
        // Env var SHADOW_ACTION_DELAY (seconds, float) overrides for tuning;
        // 0 disables (legacy synchronous mode, used by tests).
        controller.setPacingDelaySec(actionDelayFromEnv(0.7f));

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

            @Override
            public void onBossDeath(Combatant boss) {
                // Sprint 11 B3 — boss death epic moment. Add a dramatic log line
                // and trigger zoom-in on the boss view. The combat-ended flow
                // (showContinueButton + reward popup) follows the existing path;
                // we just amplify the visual moment here.
                pushLog(I18n.t("combat.log.bossDeath", boss.id()));
                triggerBossDeathZoom(boss);
            }
        });

        layoutCombatants();
        // Sprint 13 B3: controller.start() moved to show() so setStageId/setRunSession
        // callers (ProjectShadowGame) can configure the controller before first tick.
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

        // Sprint 12 B3 — item mode replaces the skill row with item buttons.
        if (inItemMode) {
            buildItemRow();
            return;
        }

        // Sprint 12 B3 — Bloodthirsty forced-attack: all skills gray + indicator
        // label; controller already auto-picks the skill on click, but here we
        // schedule auto-fire after 0.5s so the player sees the indicator first.
        boolean forced = com.trungbui.projectshadow.combat.ConditionResolver.hasForcedAttack(hero);

        List<SkillData> skills = controller.currentActorSkills();
        for (int i = 0; i < skills.size(); i++) {
            SkillData skill = skills.get(i);
            final int index = i;
            final SkillData skillForTooltip = skill;
            String label = I18n.t("combat.skillButton", i + 1, skill.displayName());
            TextButton btn = new TextButton(label, skin);
            boolean disabled = forced
                    || hero.isOnCooldown(skill.skillId())
                    || !controller.skillIsSupported(index);
            btn.setDisabled(disabled);
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    if (btn.isDisabled()) return;
                    onSkillButtonClicked(index);
                }
            });
            // Sprint 9 (feature/skill-description-tooltip) — show panel on enter, hide on exit.
            // Sprint 11 B1: pass actor's effective damage range so tooltip shows actual
            // "Dmg X..Y" numbers instead of the abstract "Dmg ×1.2" multiplier.
            final int actorDmgMin = hero.effectiveDmgMin();
            final int actorDmgMax = hero.effectiveDmgMax();
            btn.addListener(new InputListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer,
                                  com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    if (skillTooltip != null) {
                        skillTooltip.showFor(skillForTooltip, actorDmgMin, actorDmgMax);
                    }
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

        // Sprint 12 B3 — Bloodthirsty indicator label + 0.5s auto-fire.
        if (forced) {
            Label forcedLabel = new Label(I18n.t("combat.bloodthirsty.indicator"), skin);
            forcedLabel.setColor(Color.SALMON);
            skillTable.add(forcedLabel).pad(8);
            scheduleForcedAttack();
            return; // skip Items tab — no choice this turn
        }

        // Sprint 12 B3 — Items tab button at end of skill row. Only shown when
        // a run session is attached AND the inventory has at least one consumable.
        if (runSession != null) {
            int consumableCount = countConsumablesInInventory();
            TextButton itemsBtn = new TextButton(
                    I18n.t("combat.items.tab", consumableCount), skin);
            itemsBtn.setDisabled(consumableCount == 0);
            itemsBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    if (itemsBtn.isDisabled()) return;
                    inItemMode = true;
                    refreshSkillButtons();
                }
            });
            skillTable.add(itemsBtn).pad(8).width(220).height(60);
        }
    }

    /** Sprint 12 B3 — schedule an auto-fire for the Bloodthirsty forced action.
     *  Adds a stage Action that fires after 0.5s on the UI thread. Idempotent:
     *  the controller short-circuits if the actor changed or the encounter is
     *  over by the time the action fires. */
    private void scheduleForcedAttack() {
        // Use Stage's act-based Actions to run after a delay without blocking.
        com.badlogic.gdx.scenes.scene2d.actions.DelayAction delay =
                com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(0.5f);
        com.badlogic.gdx.scenes.scene2d.actions.RunnableAction fire =
                com.badlogic.gdx.scenes.scene2d.actions.Actions.run(() -> {
                    // Re-check: only fire if the current actor is still the Bloodthirsty hero.
                    Combatant cur = controller.currentActor().orElse(null);
                    if (!(cur instanceof Hero h)) return;
                    if (!com.trungbui.projectshadow.combat.ConditionResolver.hasForcedAttack(h)) return;
                    if (controller.encounter().isCombatOver()) return;
                    // Skill 0 is overridden inside CombatController.executePlayerSkill
                    // when hasForcedAttack is true (auto-pick first offensive).
                    controller.executePlayerSkill(0);
                });
        uiStage.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(delay, fire));
    }

    /** Sprint 12 B3 — count consumable items currently in the run inventory. */
    private int countConsumablesInInventory() {
        if (runSession == null) return 0;
        int n = 0;
        for (String id : runSession.state().inventory()) {
            var it = gameData.items().get(id);
            if (it != null && "consumable".equalsIgnoreCase(it.category())) n++;
        }
        return n;
    }

    /** Sprint 12 B3 — render one button per consumable in inventory plus a
     *  "Back" button. Click handler decides whether to enter target mode (heal)
     *  or fire immediately (party-wide stress reduce). */
    private void buildItemRow() {
        skillTable.clear();
        skillButtons.clear();
        if (runSession == null) {
            inItemMode = false;
            return;
        }
        // Group identical item IDs into stacks so duplicates show as "Bình Máu Nhỏ x2".
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (String id : runSession.state().inventory()) {
            counts.merge(id, 1, Integer::sum);
        }
        int rendered = 0;
        for (var entry : counts.entrySet()) {
            String itemId = entry.getKey();
            int qty = entry.getValue();
            var it = gameData.items().get(itemId);
            if (it == null || !"consumable".equalsIgnoreCase(it.category())) continue;
            String label = it.nameVn() + (qty > 1 ? " x" + qty : "");
            TextButton btn = new TextButton(label, skin);
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    onItemButtonClicked(itemId);
                }
            });
            skillTable.add(btn).pad(8).width(280).height(60);
            rendered++;
            if (rendered >= 5) break; // sanity cap to avoid overflow
        }
        TextButton back = new TextButton(I18n.t("combat.items.back"), skin);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                inItemMode = false;
                pendingItemId = null;
                refreshSkillButtons();
            }
        });
        skillTable.add(back).pad(8).width(180).height(60);
    }

    /** Sprint 12 B3 — handle a click on a consumable item button. Heal items
     *  enter target-pick mode; non-target items fire immediately. */
    private void onItemButtonClicked(String itemId) {
        if (controller.isAwaitingNonPlayerProcess()) return;
        var it = gameData.items().get(itemId);
        if (it == null) return;
        // Heal items need a target hero pick.
        if ("eff_heal".equals(it.effectId())) {
            pendingItemId = itemId;
            uiState = UiState.AWAITING_TARGET;
            highlightedTargets.clear();
            for (Hero h : controller.encounter().heroes()) {
                if (h.isAlive()) highlightedTargets.add(h);
            }
            statusLabel.setText(I18n.t("combat.items.pickTarget", it.nameVn()));
            return;
        }
        // Stress-reduce (party-wide) and any future no-target items fire immediately.
        controller.useItem(itemId, null);
        inItemMode = false;
        pendingItemId = null;
    }

    private void onSkillButtonClicked(int skillIndex) {
        // Sprint 11 B1 — ignore clicks while pacing delay is gating the enemy turn.
        // Prevents the player from queueing actions while watching enemy animation.
        if (controller.isAwaitingNonPlayerProcess()) return;
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
        // Sprint 12 B3 — if an item is pending, resolve item use instead of skill.
        if (pendingItemId != null) {
            String itemId = pendingItemId;
            pendingItemId = null;
            exitTargetMode();
            inItemMode = false;
            controller.useItem(itemId, target);
            return;
        }
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

    /** Sprint 13 B2 — wire the reward card picker for Elite / Miniboss / Boss wins.
     *  When set, {@link RewardCardPickerPopup} replaces {@link CombatRewardPopup} at
     *  victory. The player must pick one card before the transition fires.
     *
     * @param supplier produces the list of up to 3 items to offer; called once at end
     * @param onPick   receives the chosen item (or null if pool was empty) before onWin */
    public void setCardPickerProvider(
            java.util.function.Supplier<List<com.trungbui.projectshadow.data.model.ItemData>> supplier,
            Consumer<com.trungbui.projectshadow.data.model.ItemData> onPick) {
        this.cardPickerSupplier = supplier;
        this.cardPickerOnPick = onPick;
    }

    /** Sprint 12 B3 — attach the run session so the Items tab can read the
     *  inventory and {@link CombatController#useItem} can consume it. */
    public void setRunSession(com.trungbui.projectshadow.run.RunSession runSession) {
        this.runSession = runSession;
        controller.setRunSession(runSession);
        refreshSkillButtons(); // redraw to show Items tab if available
    }

    /** Sprint 13 B3 — pass the active stage ID to the controller so environmental
     *  modifiers (stage_2: -5 acc, stage_3: +1 stress/turn) are applied. Must be
     *  called before {@link CombatController#start()}, i.e. before the screen
     *  constructor finishes. Use the setter pattern (like setRunSession) so callers
     *  that don't have a stage can simply omit the call. */
    public void setStageId(String stageId) {
        controller.setStageId(stageId);
    }

    private void showContinueButton(CombatEncounter.Side winner) {
        if (onWin == null && onLose == null) return; // standalone mode — no callback wiring
        skillTable.clear();
        skillButtons.clear();

        // Sprint 13 B2 — Elite / Miniboss / Boss: show reward card picker.
        // Player must pick one item before the transition fires. Takes priority over
        // CombatRewardPopup when wired (card picker nodes still show gold via the
        // existing reward path in handleCombatWin; the picker is the item bonus only).
        if (winner == CombatEncounter.Side.HEROES && cardPickerSupplier != null) {
            List<com.trungbui.projectshadow.data.model.ItemData> cards = cardPickerSupplier.get();
            Consumer<com.trungbui.projectshadow.data.model.ItemData> pick = item -> {
                if (cardPickerOnPick != null) cardPickerOnPick.accept(item);
                if (!transitioned) {
                    transitioned = true;
                    onWin.run();
                }
            };
            RewardCardPickerPopup popup = new RewardCardPickerPopup(skin, cards == null ? List.of() : cards, pick);
            popup.setPosition(
                    (CombatRenderer.VIRTUAL_WIDTH - popup.getWidth()) / 2f,
                    (CombatRenderer.VIRTUAL_HEIGHT - popup.getHeight()) / 2f);
            uiStage.addActor(popup);
            return; // popup owns the transition trigger; skip legacy button.
        }

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

    /** Sprint 11 B3 — boss epic-death moment. Scales the boss view up over 0.5s
     *  to draw attention while the death anim plays, then lets the normal combat-end
     *  flow take over. Uses Scene2D Actions on the CombatantView's stage actor —
     *  if the view isn't a stage actor (current pure-renderer design), we just
     *  flag it for the renderer (no-op stub for now; CombatRenderer can pick up
     *  a "zoom factor" field per view in a follow-up). */
    private void triggerBossDeathZoom(Combatant boss) {
        if (boss == null) return;
        for (CombatantView v : enemyViews) {
            if (v.combatant() == boss) {
                v.triggerEpicDeath();
            }
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

    /** Sprint 11 B1 — accumulates time since current actor became "pending" so the
     *  UI can wait {@code controller.pacingDelaySec()} before invoking the enemy turn. */
    private float pacingTimer = 0f;

    /** Read SHADOW_ACTION_DELAY env var (seconds, float). Returns the given default
     *  if missing or unparseable. 0 disables pacing. */
    private static float actionDelayFromEnv(float defaultSec) {
        String env = System.getenv("SHADOW_ACTION_DELAY");
        if (env != null && !env.isBlank()) {
            try { return Float.parseFloat(env.trim()); } catch (NumberFormatException ignored) {}
        }
        return defaultSec;
    }

    @Override
    public void render(float delta) {
        // Sprint 11 B1 — drive pending enemy turns after the pacing delay elapses.
        if (controller.isAwaitingNonPlayerProcess()) {
            pacingTimer += delta;
            if (pacingTimer >= controller.pacingDelaySec()) {
                pacingTimer = 0f;
                controller.processPendingNonPlayerTurn();
            }
        } else {
            pacingTimer = 0f;
        }

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
        // Sprint 13 B3: start controller here (after setStageId/setRunSession are called).
        controller.start();
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
