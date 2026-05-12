package com.trungbui.projectshadow.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.trungbui.projectshadow.ProjectShadowGame;
import com.trungbui.projectshadow.i18n.I18n;
import com.trungbui.projectshadow.ui.FontFactory;
import com.trungbui.projectshadow.ui.SkinLoader;

/**
 * Sprint 12 B4 — splash screen shown at game launch. Auto-advances to the
 * main menu after {@link #SPLASH_SECONDS} OR on any key/click. Text-only
 * for MVP (no title sprite yet).
 */
public class SplashScreen implements Screen {

    public static final float SPLASH_SECONDS = 2.5f;

    private final ProjectShadowGame game;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final FontFactory fontFactory;
    private final Skin skin;
    private final Stage uiStage;
    private final BitmapFont titleFont;

    private float elapsed = 0f;
    private boolean advanced = false;

    public SplashScreen(ProjectShadowGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(1920, 1080, camera);
        this.fontFactory = new FontFactory(Gdx.files.internal("fonts/BeVietnamPro-Regular.ttf"));
        this.titleFont = fontFactory.create(96);
        var bodyFont = fontFactory.create(28);
        this.skin = SkinLoader.load();
        skin.get(Label.LabelStyle.class).font = bodyFont;
        SkinLoader.overrideFont(skin, bodyFont);
        this.uiStage = new Stage(viewport);

        Table root = new Table();
        root.setFillParent(true);
        root.center();

        Label title = new Label(I18n.t("mainmenu.title"),
                new Label.LabelStyle(titleFont, Color.GOLD));
        Label subtitle = new Label(I18n.t("splash.subtitle"), skin);
        subtitle.setColor(Color.LIGHT_GRAY);
        Label hint = new Label(I18n.t("splash.hint"), skin);
        hint.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));

        root.add(title).padBottom(40).row();
        root.add(subtitle).padBottom(20).row();
        root.add(hint).padTop(80);

        uiStage.addActor(root);
        viewport.apply(true);
    }

    @Override
    public void show() {
        // Any-input listener: advance on click or key.
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) { advance(); return true; }
            @Override
            public boolean touchDown(int x, int y, int pointer, int button) { advance(); return true; }
        });
    }

    private void advance() {
        if (advanced) return;
        advanced = true;
        game.openMainMenu();
    }

    @Override
    public void render(float delta) {
        elapsed += delta;
        if (elapsed >= SPLASH_SECONDS) advance();

        Gdx.gl.glClearColor(0.05f, 0.04f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        uiStage.act(delta);
        uiStage.draw();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void hide() { Gdx.input.setInputProcessor(null); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override
    public void dispose() {
        uiStage.dispose();
        skin.dispose();
        fontFactory.dispose();
    }
}
