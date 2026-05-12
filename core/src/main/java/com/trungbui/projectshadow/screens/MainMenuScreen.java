package com.trungbui.projectshadow.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
 * Sprint 12 B4 — main menu. 4 actions:
 * <ul>
 *   <li><strong>New game</strong> — fresh hamlet (preserves meta unless user opts otherwise — Sprint 13 wipe-confirm).</li>
 *   <li><strong>Continue</strong> — opens hamlet (which auto-resumes any saved run).</li>
 *   <li><strong>Settings</strong> — volume / fullscreen / locale.</li>
 *   <li><strong>Quit</strong> — exits the application.</li>
 * </ul>
 */
public class MainMenuScreen implements Screen {

    private final ProjectShadowGame game;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final FontFactory fontFactory;
    private final Skin skin;
    private final Stage uiStage;
    private final BitmapFont titleFont;

    public MainMenuScreen(ProjectShadowGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(1920, 1080, camera);
        this.fontFactory = new FontFactory(Gdx.files.internal("fonts/BeVietnamPro-Regular.ttf"));
        this.titleFont = fontFactory.create(72);
        var bodyFont = fontFactory.create(30);
        this.skin = SkinLoader.load();
        skin.get(Label.LabelStyle.class).font = bodyFont;
        skin.get(TextButton.TextButtonStyle.class).font = bodyFont;
        SkinLoader.overrideFont(skin, bodyFont);
        this.uiStage = new Stage(viewport);

        Table root = new Table();
        root.setFillParent(true);
        root.center();

        Label title = new Label(I18n.t("mainmenu.title"),
                new Label.LabelStyle(titleFont, Color.GOLD));
        root.add(title).padBottom(60).row();

        TextButton newGame = new TextButton(I18n.t("mainmenu.newGame"), skin);
        newGame.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.startFreshGame();
            }
        });

        TextButton cont = new TextButton(I18n.t("mainmenu.continue"), skin);
        cont.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.returnToHamlet(); // existing meta is auto-loaded by create()
            }
        });

        TextButton settings = new TextButton(I18n.t("mainmenu.settings"), skin);
        settings.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.openSettings();
            }
        });

        TextButton quit = new TextButton(I18n.t("mainmenu.quit"), skin);
        quit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                Gdx.app.exit();
            }
        });

        root.add(newGame).pad(12).width(420).height(72).row();
        root.add(cont).pad(12).width(420).height(72).row();
        root.add(settings).pad(12).width(420).height(72).row();
        root.add(quit).pad(12).width(420).height(72).row();

        uiStage.addActor(root);
        viewport.apply(true);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.06f, 0.05f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        uiStage.act(delta);
        uiStage.draw();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void show() { Gdx.input.setInputProcessor(uiStage); }
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
