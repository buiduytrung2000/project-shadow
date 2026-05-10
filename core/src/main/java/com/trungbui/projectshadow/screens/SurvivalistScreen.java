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
import com.trungbui.projectshadow.data.model.ItemData;
import com.trungbui.projectshadow.meta.HamletService;
import com.trungbui.projectshadow.ui.FontFactory;

import java.util.Random;

public class SurvivalistScreen implements Screen {

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

    public SurvivalistScreen(ProjectShadowGame game) {
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

        this.feedbackLabel = new Label("Chế tác trinket ngẫu nhiên: "
                + HamletService.SURVIVALIST_CRAFT_COST + " gold", skin);
        feedbackLabel.setColor(Color.WHITE);

        rebuildUi();
        uiStage.addActor(root);
        viewport.apply(true);
    }

    private void rebuildUi() {
        root.clear();
        Label title = new Label("Thợ Thủ Công", new Label.LabelStyle(titleFont, Color.GOLD));
        root.add(title).pad(20).row();

        Label gold = new Label("Gold: " + game.meta().gold(), skin);
        root.add(gold).pad(10).row();
        root.add(feedbackLabel).pad(10).row();

        Label invHeader = new Label("Kho trinket (" + game.meta().trinketInventory().size() + "):", skin);
        invHeader.setColor(Color.LIGHT_GRAY);
        root.add(invHeader).pad(20).row();

        StringBuilder invSb = new StringBuilder();
        if (game.meta().trinketInventory().isEmpty()) {
            invSb.append("(trống)");
        } else {
            boolean first = true;
            for (String itemId : game.meta().trinketInventory()) {
                if (!first) invSb.append("\n");
                first = false;
                ItemData it = game.gameData().items().get(itemId);
                invSb.append("  • ").append(it != null ? it.nameVn() : itemId)
                        .append(" (").append(itemId).append(")");
            }
        }
        Label invBody = new Label(invSb.toString(), skin);
        root.add(invBody).pad(10).row();

        TextButton craft = new TextButton(
                "Chế tác (" + HamletService.SURVIVALIST_CRAFT_COST + " gold)", skin);
        craft.setDisabled(game.meta().gold() < HamletService.SURVIVALIST_CRAFT_COST);
        craft.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                try {
                    var newMeta = HamletService.craftRandomTrinket(game.meta(), game.gameData(), new Random());
                    game.applyMeta(newMeta);
                    String latest = newMeta.trinketInventory().get(newMeta.trinketInventory().size() - 1);
                    ItemData it = game.gameData().items().get(latest);
                    feedbackLabel.setText("Đã chế tác: " + (it != null ? it.nameVn() : latest));
                    rebuildUi();
                } catch (HamletService.HamletException ex) {
                    feedbackLabel.setText(ex.getMessage());
                    rebuildUi();
                }
            }
        });
        root.add(craft).pad(20).width(360).height(70).row();

        TextButton back = new TextButton("Về Hamlet", skin);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.returnToHamlet();
            }
        });
        root.add(back).pad(20).width(280).height(70);
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.07f, 0.07f, 0.05f, 1f);
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
