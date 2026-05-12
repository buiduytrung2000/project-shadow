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
import com.trungbui.projectshadow.ui.FontFactory;
import com.trungbui.projectshadow.ui.SkinLoader;

/**
 * Sprint 7 — terminal screen after the party dies. The save has already been archived
 * before this screen is shown. Quit returns to OS.
 */
public class GameOverScreen implements Screen {

    private static final int VIRTUAL_WIDTH = 1920;
    private static final int VIRTUAL_HEIGHT = 1080;

    private final ProjectShadowGame game;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final FontFactory fontFactory;
    private final Skin skin;
    private final Stage uiStage;

    public GameOverScreen(ProjectShadowGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        this.fontFactory = new FontFactory(Gdx.files.internal("fonts/BeVietnamPro-Regular.ttf"));
        var titleFont = fontFactory.create(72);
        var bodyFont = fontFactory.create(28);
        this.skin = SkinLoader.load();
        skin.get(Label.LabelStyle.class).font = bodyFont;
        skin.get(TextButton.TextButtonStyle.class).font = bodyFont;
        SkinLoader.overrideFont(skin, bodyFont);
        this.uiStage = new Stage(viewport);

        Table root = new Table();
        root.setFillParent(true);
        root.center();

        Label title = new Label(I18n.t("combat.defeat"), new Label.LabelStyle(titleFont, Color.SCARLET));
        Label body = new Label(I18n.t("gameover.body"), skin);
        body.setColor(Color.WHITE);
        body.setAlignment(com.badlogic.gdx.utils.Align.center);

        TextButton hamlet = new TextButton(I18n.t("button.hamlet"), skin);
        hamlet.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.returnToHamlet();
            }
        });
        TextButton quit = new TextButton(I18n.t("button.quit"), skin);
        quit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                Gdx.app.exit();
            }
        });

        Table buttons = new Table();
        buttons.add(hamlet).pad(20).width(280).height(70);
        buttons.add(quit).pad(20).width(280).height(70);

        root.add(title).pad(60).row();
        root.add(body).pad(40).row();
        root.add(buttons).pad(40);
        uiStage.addActor(root);
        viewport.apply(true);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.12f, 0.04f, 0.04f, 1f);
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
        if (game.audio() != null) game.audio().playMusic("gameover", false);
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
