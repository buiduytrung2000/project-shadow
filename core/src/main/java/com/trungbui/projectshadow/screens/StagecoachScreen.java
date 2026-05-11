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
import com.trungbui.projectshadow.data.model.HeroData;
import com.trungbui.projectshadow.i18n.I18n;
import com.trungbui.projectshadow.meta.HamletService;
import com.trungbui.projectshadow.meta.MetaState;
import com.trungbui.projectshadow.ui.FontFactory;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import java.util.List;
import java.util.Random;

public class StagecoachScreen implements Screen {

    private static final int VIRTUAL_WIDTH = 1920;
    private static final int VIRTUAL_HEIGHT = 1080;

    private final ProjectShadowGame game;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final FontFactory fontFactory;
    private final Skin skin;
    private final Stage uiStage;
    private final Table root;
    private final Label feedbackLabel;
    private final BitmapFont titleFont;
    private List<String> currentOffers;

    public StagecoachScreen(ProjectShadowGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        this.fontFactory = new FontFactory(Gdx.files.internal("fonts/BeVietnamPro-Regular.ttf"));
        this.titleFont = fontFactory.create(48);
        var bodyFont = fontFactory.create(24);
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        skin.get(Label.LabelStyle.class).font = bodyFont;
        skin.get(TextButton.TextButtonStyle.class).font = bodyFont;
        this.uiStage = new Stage(viewport);

        this.root = new Table();
        root.setFillParent(true);
        root.top().pad(40);

        this.feedbackLabel = new Label("", skin);
        feedbackLabel.setColor(Color.WHITE);

        rerollOffers(true);

        uiStage.addActor(root);
        viewport.apply(true);
    }

    private void rerollOffers(boolean firstTime) {
        currentOffers = HamletService.rollStagecoachOffers(game.meta(), game.gameData(), new Random());
        rebuildUi(firstTime
                ? I18n.t("stagecoach.cost", HamletService.STAGECOACH_HIRE_COST)
                : I18n.t("stagecoach.refreshed"));
    }

    private void rebuildUi(String feedback) {
        root.clear();
        Label title = new Label(I18n.t("stagecoach.title"), new Label.LabelStyle(titleFont, Color.GOLD));
        root.add(title).colspan(3).pad(20).row();

        Label gold = new Label(I18n.t("hamlet.gold", game.meta().gold()), skin);
        root.add(gold).colspan(3).pad(10).row();

        feedbackLabel.setText(feedback);
        root.add(feedbackLabel).colspan(3).pad(10).row();

        if (currentOffers.isEmpty()) {
            Label none = new Label(I18n.t("stagecoach.empty"), skin);
            none.setColor(Color.SALMON);
            root.add(none).colspan(3).pad(20).row();
        } else {
            for (String heroId : currentOffers) {
                HeroData data = game.gameData().heroes().get(heroId);
                String label = data != null
                        ? data.displayName() + "\n(" + heroId + ")"
                        : heroId;
                TextButton hire = new TextButton(I18n.t("stagecoach.hire", label), skin);
                final String idCaptured = heroId;
                hire.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                        try {
                            MetaState newMeta = HamletService.hireHero(game.meta(), idCaptured, game.gameData());
                            game.applyMeta(newMeta);
                            currentOffers = currentOffers.stream()
                                    .filter(s -> !s.equals(idCaptured)).toList();
                            rebuildUi(I18n.t("stagecoach.hired", idCaptured));
                        } catch (HamletService.HamletException ex) {
                            rebuildUi(ex.getMessage());
                        }
                    }
                });
                hire.setDisabled(game.meta().gold() < HamletService.STAGECOACH_HIRE_COST);
                root.add(hire).pad(10).width(380).height(110);
            }
            root.row();
        }

        // Sprint 10 B1: refresh now costs gold (was free → save-scum loophole).
        TextButton refresh = new TextButton(
                I18n.t("stagecoach.refresh", HamletService.STAGECOACH_REFRESH_COST), skin);
        refresh.setDisabled(game.meta().gold() < HamletService.STAGECOACH_REFRESH_COST);
        refresh.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                try {
                    MetaState newMeta = HamletService.payStagecoachRefresh(game.meta());
                    game.applyMeta(newMeta);
                    rerollOffers(false);
                } catch (HamletService.HamletException ex) {
                    rebuildUi(ex.getMessage());
                }
            }
        });
        TextButton back = new TextButton(I18n.t("button.back"), skin);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.returnToHamlet();
            }
        });
        root.add(refresh).pad(20).width(280).height(70);
        root.add(back).pad(20).width(280).height(70);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.06f, 0.04f, 1f);
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
