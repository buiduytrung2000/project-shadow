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
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.trungbui.projectshadow.ProjectShadowGame;
import com.trungbui.projectshadow.i18n.I18n;
import com.trungbui.projectshadow.meta.SettingsManager;
import com.trungbui.projectshadow.ui.FontFactory;
import com.trungbui.projectshadow.ui.SkinLoader;

import java.io.IOException;

/**
 * Sprint 12 B4 — settings screen. Music volume + SFX volume sliders,
 * fullscreen toggle, locale toggle (VN/EN). Persists to {@code saves/settings.json}
 * via {@link SettingsManager}. Hooks into the game's {@code AudioManager}
 * so volume changes apply live.
 */
public class SettingsScreen implements Screen {

    private final ProjectShadowGame game;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final FontFactory fontFactory;
    private final Skin skin;
    private final Stage uiStage;
    private final BitmapFont titleFont;
    private final Label statusLabel;

    public SettingsScreen(ProjectShadowGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(1920, 1080, camera);
        this.fontFactory = new FontFactory(Gdx.files.internal("fonts/BeVietnamPro-Regular.ttf"));
        this.titleFont = fontFactory.create(56);
        var bodyFont = fontFactory.create(26);
        this.skin = SkinLoader.load();
        skin.get(Label.LabelStyle.class).font = bodyFont;
        skin.get(TextButton.TextButtonStyle.class).font = bodyFont;
        SkinLoader.overrideFont(skin, bodyFont);
        this.uiStage = new Stage(viewport);

        SettingsManager settings = game.settings();
        statusLabel = new Label("", skin);
        statusLabel.setColor(Color.LIGHT_GRAY);

        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(60);

        Label title = new Label(I18n.t("settings.title"),
                new Label.LabelStyle(titleFont, Color.GOLD));
        root.add(title).colspan(2).padBottom(40).row();

        // Music volume slider
        root.add(new Label(I18n.t("settings.musicVolume"), skin))
                .right().padRight(20);
        Slider musicSlider = new Slider(0f, 1f, 0.05f, false, skin);
        musicSlider.setValue(settings.musicVolume());
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                settings.setMusicVolume(musicSlider.getValue());
                if (game.audio() != null) game.audio().setMusicVolume(musicSlider.getValue());
                persist();
            }
        });
        root.add(musicSlider).width(500).pad(10).row();

        // SFX volume slider
        root.add(new Label(I18n.t("settings.sfxVolume"), skin))
                .right().padRight(20);
        Slider sfxSlider = new Slider(0f, 1f, 0.05f, false, skin);
        sfxSlider.setValue(settings.sfxVolume());
        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                settings.setSfxVolume(sfxSlider.getValue());
                if (game.audio() != null) game.audio().setSfxVolume(sfxSlider.getValue());
                persist();
            }
        });
        root.add(sfxSlider).width(500).pad(10).row();

        // Fullscreen toggle
        root.add(new Label(I18n.t("settings.fullscreen"), skin))
                .right().padRight(20);
        TextButton fullscreenBtn = new TextButton(
                settings.fullscreen() ? I18n.t("settings.on") : I18n.t("settings.off"), skin);
        fullscreenBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                boolean newVal = !settings.fullscreen();
                settings.setFullscreen(newVal);
                fullscreenBtn.setText(newVal ? I18n.t("settings.on") : I18n.t("settings.off"));
                applyFullscreen(newVal);
                persist();
            }
        });
        root.add(fullscreenBtn).width(200).height(60).pad(10).row();

        // Locale toggle
        root.add(new Label(I18n.t("settings.language"), skin))
                .right().padRight(20);
        TextButton localeBtn = new TextButton(displayLocale(settings.locale()), skin);
        localeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                I18n.toggleLocale();
                String code = I18n.currentLocale().getLanguage();
                settings.setLocale(code);
                localeBtn.setText(displayLocale(code));
                persist();
            }
        });
        root.add(localeBtn).width(200).height(60).pad(10).row();

        // Back
        TextButton back = new TextButton(I18n.t("button.back"), skin);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.openMainMenu();
            }
        });
        root.add(back).colspan(2).pad(40).width(280).height(70).row();

        root.add(statusLabel).colspan(2).pad(10);

        uiStage.addActor(root);
        viewport.apply(true);
    }

    private void applyFullscreen(boolean fullscreen) {
        if (fullscreen) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            Gdx.graphics.setWindowedMode(1920, 1080);
        }
    }

    private void persist() {
        try {
            game.settings().save();
            statusLabel.setText(I18n.t("settings.saved"));
        } catch (IOException e) {
            statusLabel.setText(I18n.t("settings.saveFailed", e.getMessage()));
        }
    }

    private static String displayLocale(String code) {
        return "vi".equalsIgnoreCase(code) ? "Tiếng Việt" : "English";
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.06f, 0.09f, 1f);
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
