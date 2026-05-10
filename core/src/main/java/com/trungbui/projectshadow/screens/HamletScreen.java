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
import com.trungbui.projectshadow.meta.MetaState;
import com.trungbui.projectshadow.save.HeroState;
import com.trungbui.projectshadow.ui.FontFactory;

/**
 * Sprint 8 — Hamlet hub. Shows 4 building buttons + roster summary + Embark CTA.
 */
public class HamletScreen implements Screen {

    private static final int VIRTUAL_WIDTH = 1920;
    private static final int VIRTUAL_HEIGHT = 1080;
    private static final int FONT_SIZE_PX = 26;

    private final ProjectShadowGame game;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final FontFactory fontFactory;
    private final Skin skin;
    private final Stage uiStage;

    public HamletScreen(ProjectShadowGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        this.fontFactory = new FontFactory(Gdx.files.internal("fonts/BeVietnamPro-Regular.ttf"));
        var titleFont = fontFactory.create(56);
        var bodyFont = fontFactory.create(FONT_SIZE_PX);
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        skin.get(Label.LabelStyle.class).font = bodyFont;
        skin.get(TextButton.TextButtonStyle.class).font = bodyFont;
        this.uiStage = new Stage(viewport);

        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(40);

        Label title = new Label("HAMLET", new Label.LabelStyle(titleFont, Color.GOLD));
        Label gold = new Label("Gold: " + game.meta().gold(), skin);

        root.add(title).colspan(4).pad(20).row();
        root.add(gold).colspan(4).pad(10).row();
        root.add(rosterLabel()).colspan(4).pad(20).row();

        TextButton stagecoach = button("Trạm Xe Ngựa\n(Stagecoach)", () -> game.openStagecoach());
        TextButton guild = button("Phường Hội\n(Guild)", () -> game.openGuild());
        TextButton survivalist = button("Thợ Thủ Công\n(Survivalist)", () -> game.openSurvivalist());
        TextButton caretaker = button("Người Chăm Sóc\n(Caretaker)", () -> game.openCaretaker());

        root.add(stagecoach).pad(10).width(280).height(120);
        root.add(guild).pad(10).width(280).height(120);
        root.add(survivalist).pad(10).width(280).height(120);
        root.add(caretaker).pad(10).width(280).height(120);
        root.row();

        TextButton embark = new TextButton("EMBARK — Bắt đầu chuyến đi", skin);
        embark.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.openEmbarkSelection();
            }
        });
        embark.setDisabled(game.meta().roster().size() < 4);
        if (embark.isDisabled()) embark.setColor(Color.GRAY);
        root.add(embark).colspan(4).pad(40).width(600).height(80);

        if (game.meta().roster().size() < 4) {
            root.row();
            Label warn = new Label(
                    "Cần ít nhất 4 hero trong roster để embark (hiện có "
                            + game.meta().roster().size() + ").",
                    skin);
            warn.setColor(Color.SALMON);
            root.add(warn).colspan(4).pad(10);
        }

        uiStage.addActor(root);
        viewport.apply(true);
    }

    private TextButton button(String text, Runnable onClick) {
        TextButton b = new TextButton(text, skin);
        b.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                onClick.run();
            }
        });
        return b;
    }

    private Label rosterLabel() {
        MetaState meta = game.meta();
        StringBuilder sb = new StringBuilder("Roster (" + meta.roster().size() + "):  ");
        boolean first = true;
        for (HeroState h : meta.roster()) {
            if (!first) sb.append("   |   ");
            first = false;
            var data = game.gameData().heroes().get(h.heroId());
            String name = data != null ? data.nameVn() : h.heroId();
            sb.append(name).append(" Lv").append(h.level())
                    .append(" HP ").append(h.currentHp())
                    .append(" Stress ").append(h.currentStress());
        }
        Label l = new Label(sb.toString(), skin);
        l.setColor(Color.LIGHT_GRAY);
        return l;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.10f, 0.08f, 0.06f, 1f);
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
