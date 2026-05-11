package com.trungbui.projectshadow.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.trungbui.projectshadow.ProjectShadowGame;
import com.trungbui.projectshadow.data.model.EventChoice;
import com.trungbui.projectshadow.data.model.EventData;
import com.trungbui.projectshadow.i18n.I18n;
import com.trungbui.projectshadow.run.EventOutcomeApplier;
import com.trungbui.projectshadow.run.RestOptionApplier;
import com.trungbui.projectshadow.stage.DropEntry;
import com.trungbui.projectshadow.stage.EventNode;
import com.trungbui.projectshadow.stage.RestNode;
import com.trungbui.projectshadow.stage.RestOption;
import com.trungbui.projectshadow.stage.RewardNode;
import com.trungbui.projectshadow.stage.StageNode;
import com.trungbui.projectshadow.ui.FontFactory;
import com.trungbui.projectshadow.ui.SkinLoader;

import java.util.Random;

/**
 * Sprint 10 B3 — non-combat node screen with interactive choices.
 *
 * <p>Previously a "Sprint 7 placeholder" (Continue only). Now:</p>
 * <ul>
 *   <li><strong>Event nodes</strong>: render 2-3 choice buttons (no outcome preview —
 *       mystery mode per design lock). Click → {@code EventOutcomeApplier} →
 *       feedback line + Continue.</li>
 *   <li><strong>Rest nodes</strong>: render each option as a button. Click →
 *       {@code RestOptionApplier} → feedback + Continue.</li>
 *   <li><strong>Reward nodes</strong>: unchanged — drops are auto-applied in
 *       {@code ProjectShadowGame.finishCurrentNonCombatNode}; Continue advances.</li>
 * </ul>
 */
public class NodeInfoScreen implements Screen {

    private static final int VIRTUAL_WIDTH = 1920;
    private static final int VIRTUAL_HEIGHT = 1080;
    private static final int FONT_SIZE_PX = 26;

    private final ProjectShadowGame game;
    private final StageNode node;

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final FontFactory fontFactory;
    private final Skin skin;
    private final Stage uiStage;

    /** Sprint 10 B3 — feedback label that mutates after a choice is picked. */
    private final Label feedbackLabel;
    /** Tracks whether the player has interacted (for event/rest). Continue is
     *  always enabled to keep flow simple — but if a choice was already picked,
     *  buttons disable so player can't double-apply. */
    private boolean interactionDone = false;
    private Table choicesContainer;

    public NodeInfoScreen(ProjectShadowGame game, StageNode node) {
        this.game = game;
        this.node = node;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        this.fontFactory = new FontFactory(Gdx.files.internal("fonts/BeVietnamPro-Regular.ttf"));
        var vnFont = fontFactory.create(FONT_SIZE_PX);
        this.skin = SkinLoader.load();
        skin.get(Label.LabelStyle.class).font = vnFont;
        skin.get(TextButton.TextButtonStyle.class).font = vnFont;
        this.uiStage = new Stage(viewport);
        this.feedbackLabel = new Label("", skin);
        this.feedbackLabel.setColor(Color.LIGHT_GRAY);
        this.feedbackLabel.setWrap(true);
        buildUi();
        viewport.apply(true);
    }

    private void buildUi() {
        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(40);

        Label title = new Label(headerForType(), skin);
        title.setColor(Color.GOLD);
        Label subtitle = new Label(I18n.t("node.subtitle", node.label()), skin);
        subtitle.setColor(Color.LIGHT_GRAY);

        root.add(title).pad(20).row();
        root.add(subtitle).pad(10).row();

        // Body text (description for event/rest, drop list for reward).
        Label body = new Label(bodyText(), skin);
        body.setColor(Color.WHITE);
        body.setWrap(true);
        root.add(body).width(1200).pad(20).row();

        // Sprint 10 B3 — interactive choice buttons per node type.
        choicesContainer = new Table();
        buildChoicesUi(choicesContainer);
        root.add(choicesContainer).pad(20).row();

        // Feedback after pick.
        root.add(feedbackLabel).width(1200).pad(10).row();

        TextButton continueBtn = new TextButton(I18n.t("button.continue"), skin);
        continueBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.finishCurrentNonCombatNode(node);
            }
        });
        root.add(continueBtn).width(280).height(70).pad(40);
        uiStage.addActor(root);
    }

    /** Sprint 10 B3 — choice buttons for event / rest nodes. Reward nodes have
     *  no interactive choices (drops auto-applied on Continue). */
    private void buildChoicesUi(Table container) {
        container.clear();
        switch (node) {
            case EventNode en -> {
                EventData ed = game.gameData().events().get(en.eventId());
                if (ed == null) {
                    container.add(new Label("(event " + en.eventId() + " not found)", skin)).row();
                    return;
                }
                for (EventChoice choice : ed.choices()) {
                    TextButton btn = new TextButton(choice.text(), skin);
                    btn.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                            if (interactionDone) return;
                            EventOutcomeApplier.AppliedSummary summary =
                                    EventOutcomeApplier.apply(choice, game.runSession(), new Random());
                            feedbackLabel.setText(summarizeEvent(summary));
                            interactionDone = true;
                            buildChoicesUi(container); // re-render to disable buttons
                        }
                    });
                    btn.setDisabled(interactionDone);
                    container.add(btn).pad(10).width(400).height(60).row();
                }
            }
            case RestNode rn -> {
                for (RestOption opt : rn.options()) {
                    String labelText = opt.label() + " (" + opt.effect() + ")";
                    TextButton btn = new TextButton(labelText, skin);
                    btn.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                            if (interactionDone) return;
                            RestOptionApplier.AppliedSummary summary =
                                    RestOptionApplier.apply(opt, game.runSession(), new Random());
                            feedbackLabel.setText(summarizeRest(summary));
                            interactionDone = true;
                            buildChoicesUi(container);
                        }
                    });
                    btn.setDisabled(interactionDone);
                    container.add(btn).pad(10).width(500).height(60).row();
                }
            }
            default -> {
                // Reward + generic: no interactive choices.
            }
        }
    }

    private static String summarizeEvent(EventOutcomeApplier.AppliedSummary s) {
        StringBuilder sb = new StringBuilder();
        if (s.goldDelta != 0) sb.append("Gold ").append(s.goldDelta > 0 ? "+" : "").append(s.goldDelta).append(" · ");
        if (s.partyStressAdded > 0) sb.append("Party stress +").append(s.partyStressAdded).append(" · ");
        if (s.singleHeroStressAdded > 0) sb.append("Stress +").append(s.singleHeroStressAdded).append(" · ");
        if (s.partyDamageDealt > 0) sb.append("Party HP -").append(s.partyDamageDealt).append(" · ");
        if (s.singleHeroDamageDealt > 0) sb.append("HP -").append(s.singleHeroDamageDealt).append(" · ");
        if (s.skillCdReset) sb.append("Skill CD reset · ");
        if (!s.traitsApplied.isEmpty()) sb.append("Trait: ").append(s.traitsApplied).append(" · ");
        if (!s.diseasesApplied.isEmpty()) sb.append("Disease: ").append(s.diseasesApplied).append(" · ");
        if (!s.itemsAdded.isEmpty()) sb.append("Item: ").append(s.itemsAdded).append(" · ");
        return sb.length() == 0 ? "(không có gì xảy ra)" : sb.toString();
    }

    private static String summarizeRest(RestOptionApplier.AppliedSummary s) {
        StringBuilder sb = new StringBuilder();
        if (s.totalHealed > 0) sb.append("Hồi +").append(s.totalHealed).append(" HP · ");
        if (s.totalStressReduced > 0) sb.append("Stress -").append(s.totalStressReduced).append(" · ");
        if (s.diseaseRemovedFrom != null) sb.append("Khỏi bệnh: ").append(s.diseaseRemovedId)
                .append(" (").append(s.diseaseRemovedFrom).append(") · ");
        if (s.removeDiseaseNoOp) sb.append("(không có bệnh nào để chữa) · ");
        if (s.buffSkipped) sb.append("(buff chưa hỗ trợ Sprint 10) · ");
        if (s.skillSwapNeedsUi) sb.append("(skill swap cần UI Sprint 11+) · ");
        return sb.length() == 0 ? "(không có gì xảy ra)" : sb.toString();
    }

    private String headerForType() {
        return switch (node) {
            case EventNode e -> I18n.t("node.title.event", e.eventId());
            case RestNode r -> I18n.t("node.title.rest");
            case RewardNode r -> I18n.t("node.title.reward");
            default -> I18n.t("node.title.generic", node.type());
        };
    }

    private String bodyText() {
        return switch (node) {
            case EventNode e -> {
                EventData ed = game.gameData().events().get(e.eventId());
                yield ed != null && ed.descriptionVn() != null
                        ? ed.descriptionVn()
                        : I18n.t("node.body.event");
            }
            case RestNode r -> I18n.t("node.body.restHeader");
            case RewardNode r -> {
                StringBuilder sb = new StringBuilder(I18n.t("node.body.rewardHeader")).append("\n");
                if (r.drops().isEmpty()) sb.append(I18n.t("node.body.rewardEmpty")).append("\n");
                for (DropEntry d : r.drops()) {
                    StringBuilder line = new StringBuilder(d.type());
                    if (d.itemId() != null) line.append(" — ").append(d.itemId());
                    if (d.category() != null) line.append(" — ").append(d.category());
                    if (d.valueMin() != null && d.valueMax() != null) {
                        line.append(" (").append(d.valueMin()).append("..").append(d.valueMax()).append(")");
                    }
                    sb.append(I18n.t("node.body.rewardEntry", line.toString())).append("\n");
                }
                yield sb.toString();
            }
            default -> I18n.t("node.body.generic", node.type());
        };
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.06f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        uiStage.act(delta);
        uiStage.draw();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void show() { Gdx.input.setInputProcessor(uiStage); }
    @Override public void hide() { Gdx.input.setInputProcessor(null); }
    @Override public void pause() { }
    @Override public void resume() { }

    @Override
    public void dispose() {
        uiStage.dispose();
        skin.dispose();
        fontFactory.dispose();
    }
}
