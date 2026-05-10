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
import com.trungbui.projectshadow.i18n.I18n;
import com.trungbui.projectshadow.stage.EventNode;
import com.trungbui.projectshadow.stage.RestNode;
import com.trungbui.projectshadow.stage.RestOption;
import com.trungbui.projectshadow.stage.RewardNode;
import com.trungbui.projectshadow.stage.StageNode;
import com.trungbui.projectshadow.stage.DropEntry;
import com.trungbui.projectshadow.ui.FontFactory;

/**
 * Sprint 7 placeholder screen for non-combat nodes (event / rest / reward).
 * Shows node info + a Continue button. Mechanics for actually applying event/rest/reward
 * effects are deferred to Sprint 8 — for now Continue advances the run with no side effects.
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

    public NodeInfoScreen(ProjectShadowGame game, StageNode node) {
        this.game = game;
        this.node = node;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        this.fontFactory = new FontFactory(Gdx.files.internal("fonts/BeVietnamPro-Regular.ttf"));
        var vnFont = fontFactory.create(FONT_SIZE_PX);
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        skin.get(Label.LabelStyle.class).font = vnFont;
        skin.get(TextButton.TextButtonStyle.class).font = vnFont;
        this.uiStage = new Stage(viewport);
        buildUi();
        viewport.apply(true);
    }

    private void buildUi() {
        Table root = new Table();
        root.setFillParent(true);
        root.center();

        Label title = new Label(headerForType(), skin);
        title.setColor(Color.GOLD);

        Label subtitle = new Label(I18n.t("node.subtitle", node.label()), skin);
        subtitle.setColor(Color.LIGHT_GRAY);

        Label body = new Label(bodyText(), skin);
        body.setColor(Color.WHITE);
        body.setWrap(true);

        TextButton continueBtn = new TextButton(I18n.t("button.continue"), skin);
        continueBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.finishCurrentNonCombatNode(node);
            }
        });

        root.add(title).pad(20).row();
        root.add(subtitle).pad(10).row();
        root.add(body).width(1200).pad(40).row();
        root.add(continueBtn).width(280).height(70).pad(40);
        uiStage.addActor(root);
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
            case EventNode e -> I18n.t("node.body.event");
            case RestNode r -> {
                StringBuilder sb = new StringBuilder(I18n.t("node.body.restHeader")).append("\n");
                if (r.options().isEmpty()) sb.append(I18n.t("node.body.restEmpty")).append("\n");
                for (RestOption o : r.options()) {
                    sb.append(I18n.t("node.body.restOption",
                            o.label(), o.effect(), o.target(), o.valueMin(), o.valueMax())).append("\n");
                }
                sb.append(I18n.t("node.body.restFooter"));
                yield sb.toString();
            }
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
                sb.append(I18n.t("node.body.rewardFooter"));
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
        uiStage.dispose();
        skin.dispose();
        fontFactory.dispose();
    }
}
