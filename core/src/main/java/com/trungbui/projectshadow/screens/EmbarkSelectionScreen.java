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
import com.trungbui.projectshadow.save.HeroState;
import com.trungbui.projectshadow.ui.FontFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Sprint 8 — pre-run roster picker. Player selects exactly 4 heroes from the roster
 * to embark with. Click hero → toggle selection. Embark enabled when 4 selected.
 */
public class EmbarkSelectionScreen implements Screen {

    private static final int VIRTUAL_WIDTH = 1920;
    private static final int VIRTUAL_HEIGHT = 1080;
    private static final int PARTY_SIZE = 4;

    private final ProjectShadowGame game;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final FontFactory fontFactory;
    private final Skin skin;
    private final Stage uiStage;
    private final Table root;
    private final BitmapFont titleFont;
    private final List<String> selected = new ArrayList<>();

    public EmbarkSelectionScreen(ProjectShadowGame game) {
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

        rebuildUi();
        uiStage.addActor(root);
        viewport.apply(true);
    }

    private void rebuildUi() {
        root.clear();
        Label title = new Label("Chọn đội (4 hero)", new Label.LabelStyle(titleFont, Color.GOLD));
        root.add(title).colspan(2).pad(20).row();

        Label status = new Label("Đã chọn: " + selected.size() + " / " + PARTY_SIZE, skin);
        status.setColor(selected.size() == PARTY_SIZE ? Color.LIME : Color.WHITE);
        root.add(status).colspan(2).pad(10).row();

        for (HeroState rs : game.meta().roster()) {
            HeroData data = game.gameData().heroes().get(rs.heroId());
            String name = data != null ? data.nameVn() : rs.heroId();
            String info = name + " Lv" + rs.level()
                    + " HP " + rs.currentHp() + "  Stress " + rs.currentStress();
            Label heroLabel = new Label(info, skin);

            boolean isSelected = selected.contains(rs.heroId());
            TextButton btn = new TextButton(isSelected ? "✓ Đã chọn" : "Chọn", skin);
            if (isSelected) btn.setColor(Color.LIME);
            final String idCaptured = rs.heroId();
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    if (selected.contains(idCaptured)) {
                        selected.remove(idCaptured);
                    } else if (selected.size() < PARTY_SIZE) {
                        selected.add(idCaptured);
                    }
                    rebuildUi();
                }
            });

            root.add(heroLabel).pad(8).left();
            root.add(btn).pad(8).width(220).height(50).row();
        }

        TextButton embark = new TextButton("EMBARK", skin);
        embark.setDisabled(selected.size() != PARTY_SIZE);
        if (embark.isDisabled()) embark.setColor(Color.GRAY);
        embark.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                if (selected.size() == PARTY_SIZE) game.startNewRun(List.copyOf(selected));
            }
        });

        TextButton back = new TextButton("Hủy", skin);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.returnToHamlet();
            }
        });

        root.add(embark).pad(20).width(280).height(70);
        root.add(back).pad(20).width(280).height(70);
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.10f, 1f);
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
