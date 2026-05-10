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
import com.trungbui.projectshadow.data.model.HeroData;
import com.trungbui.projectshadow.meta.HamletService;
import com.trungbui.projectshadow.meta.MetaState;
import com.trungbui.projectshadow.save.HeroState;
import com.trungbui.projectshadow.ui.FontFactory;

public class GuildScreen implements Screen {

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

    public GuildScreen(ProjectShadowGame game) {
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

        this.feedbackLabel = new Label("Click 'Level up' để tăng cấp hero (max lv " + HamletService.GUILD_MAX_LEVEL + ")", skin);
        feedbackLabel.setColor(Color.WHITE);

        rebuildUi();
        uiStage.addActor(root);
        viewport.apply(true);
    }

    private void rebuildUi() {
        root.clear();
        Label title = new Label("Phường Hội", new Label.LabelStyle(titleFont, Color.GOLD));
        root.add(title).colspan(2).pad(20).row();

        Label gold = new Label("Gold: " + game.meta().gold(), skin);
        root.add(gold).colspan(2).pad(10).row();

        root.add(feedbackLabel).colspan(2).pad(10).row();

        MetaState meta = game.meta();
        if (meta.roster().isEmpty()) {
            Label none = new Label("Roster trống.", skin);
            none.setColor(Color.SALMON);
            root.add(none).colspan(2).pad(20).row();
        } else {
            for (HeroState rs : meta.roster()) {
                HeroData data = game.gameData().heroes().get(rs.heroId());
                String name = data != null ? data.nameVn() : rs.heroId();
                String info = name + " (Lv " + rs.level() + ")";
                Label heroLabel = new Label(info, skin);

                int cost = HamletService.levelUpCost(rs.level());
                boolean atMax = rs.level() >= HamletService.GUILD_MAX_LEVEL;
                String btnText = atMax ? "Đã max" : "Level up (" + cost + " gold)";
                TextButton btn = new TextButton(btnText, skin);
                btn.setDisabled(atMax || meta.gold() < cost);
                final String idCaptured = rs.heroId();
                btn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                        try {
                            game.applyMeta(HamletService.levelUpHero(game.meta(), idCaptured, game.gameData()));
                            feedbackLabel.setText("Đã tăng cấp " + idCaptured);
                            rebuildUi();
                        } catch (HamletService.HamletException ex) {
                            feedbackLabel.setText(ex.getMessage());
                            rebuildUi();
                        }
                    }
                });

                root.add(heroLabel).pad(10).left();
                root.add(btn).pad(10).width(280).height(60);
                root.row();
            }
        }

        TextButton back = new TextButton("Về Hamlet", skin);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.returnToHamlet();
            }
        });
        root.add(back).colspan(2).pad(40).width(280).height(70);
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.07f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        uiStage.act(delta);
        uiStage.draw();
    }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void show() { Gdx.input.setInputProcessor(uiStage); }
    @Override public void hide() { Gdx.input.setInputProcessor(null); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() {
        uiStage.dispose(); skin.dispose(); fontFactory.dispose();
    }
}
