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
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.i18n.I18n;
import com.trungbui.projectshadow.save.RunState;
import com.trungbui.projectshadow.ui.FontFactory;
import com.trungbui.projectshadow.ui.SkinLoader;

import java.util.Comparator;
import java.util.List;

/**
 * Sprint 13 B2 — run summary screen shown after party-wipe or stage boss kill.
 *
 * <p>Displays 5 statistics from the completed run: total gold earned, heirloom
 * earned, enemies killed, and MVP hero (most damage dealt + total). Routes to
 * Hamlet via {@link ProjectShadowGame#returnToHamlet()} after the player clicks
 * "Return to Hamlet".</p>
 */
public class RunSummaryScreen implements Screen {

    private static final int VIRTUAL_WIDTH = 1920;
    private static final int VIRTUAL_HEIGHT = 1080;

    private final ProjectShadowGame game;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final FontFactory fontFactory;
    private final Skin skin;
    private final Stage uiStage;

    /**
     * @param game     game instance (for navigation)
     * @param state    final {@link RunState} with accumulated stats
     * @param party    final party list — used to find the MVP hero by damage dealt
     * @param victory  true if the run ended in a boss kill, false on party wipe
     */
    public RunSummaryScreen(ProjectShadowGame game, RunState state, List<Hero> party, boolean victory) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        this.fontFactory = new FontFactory(Gdx.files.internal("fonts/BeVietnamPro-Regular.ttf"));
        var titleFont = fontFactory.create(60);
        var bodyFont = fontFactory.create(28);
        this.skin = SkinLoader.load();
        skin.get(Label.LabelStyle.class).font = bodyFont;
        skin.get(TextButton.TextButtonStyle.class).font = bodyFont;
        this.uiStage = new Stage(viewport);

        Table root = new Table();
        root.setFillParent(true);
        root.center();

        Label title = new Label(I18n.t("run_summary.title"), new Label.LabelStyle(titleFont,
                victory ? Color.GOLD : Color.SCARLET));
        root.add(title).pad(48).row();

        // 5 stat rows
        addStatRow(root, skin, I18n.t("run_summary.gold", state.gold()));
        addStatRow(root, skin, I18n.t("run_summary.heirloom", state.heirloomEarned()));
        addStatRow(root, skin, I18n.t("run_summary.enemies_killed", state.enemiesKilled()));

        // MVP hero
        Hero mvp = findMvp(party);
        if (mvp != null) {
            addStatRow(root, skin, I18n.t("run_summary.mvp",
                    mvp.data().displayName(), mvp.currentRunDamage()));
        }

        TextButton hamletBtn = new TextButton(I18n.t("run_summary.return_to_hamlet"), skin);
        hamletBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.returnToHamlet();
            }
        });
        root.add(hamletBtn).pad(32).width(320).height(70);

        uiStage.addActor(root);
        viewport.apply(true);
    }

    private static void addStatRow(Table root, Skin skin, String text) {
        Label lbl = new Label(text, skin);
        lbl.setColor(Color.WHITE);
        root.add(lbl).pad(12).row();
    }

    /** Find the hero with the highest {@link Hero#currentRunDamage()} in {@code party}.
     *  Returns null if party is empty or all heroes dealt 0 damage. */
    static Hero findMvp(List<Hero> party) {
        if (party == null || party.isEmpty()) return null;
        Hero mvp = party.stream()
                .max(Comparator.comparingInt(Hero::currentRunDamage))
                .orElse(null);
        return (mvp != null && mvp.currentRunDamage() > 0) ? mvp : null;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.10f, 0.10f, 0.14f, 1f);
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
