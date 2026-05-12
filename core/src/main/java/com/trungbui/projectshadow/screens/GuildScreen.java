package com.trungbui.projectshadow.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.trungbui.projectshadow.ProjectShadowGame;
import com.trungbui.projectshadow.data.model.HeroData;
import com.trungbui.projectshadow.i18n.I18n;
import com.trungbui.projectshadow.meta.HamletService;
import com.trungbui.projectshadow.meta.MetaState;
import com.trungbui.projectshadow.save.HeroState;
import com.trungbui.projectshadow.ui.FontFactory;
import com.trungbui.projectshadow.ui.SkinLoader;

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
        this.skin = SkinLoader.load();
        skin.get(Label.LabelStyle.class).font = bodyFont;
        skin.get(TextButton.TextButtonStyle.class).font = bodyFont;
        SkinLoader.overrideFont(skin, bodyFont);
        this.uiStage = new Stage(viewport);

        this.root = new Table();
        root.setFillParent(true);
        root.top().pad(40);

        this.feedbackLabel = new Label(I18n.t("guild.intro", HamletService.GUILD_MAX_LEVEL), skin);
        feedbackLabel.setColor(Color.WHITE);

        rebuildUi();
        uiStage.addActor(root);
        viewport.apply(true);
    }

    /** Sprint 10 B2 — Guild upgrade button (Lv1→Lv2→Lv3). */
    private void addUpgradeButton() {
        int currentLevel = game.meta().buildingLevel(MetaState.B_GUILD);
        if (currentLevel >= 3) {
            Label maxed = new Label(I18n.t("hamlet.upgrade.maxed"), skin);
            maxed.setColor(Color.GOLD);
            root.add(maxed).colspan(2).pad(8).row();
            return;
        }
        int costGold = HamletService.upgradeGoldCost(MetaState.B_GUILD, currentLevel);
        int costHeir = HamletService.upgradeHeirloomCost(MetaState.B_GUILD, currentLevel);
        TextButton upgrade = new TextButton(
                I18n.t("hamlet.upgrade.button", currentLevel + 1, costGold, costHeir),
                skin);
        boolean affordable = game.meta().gold() >= costGold && game.meta().heirloom() >= costHeir;
        upgrade.setDisabled(!affordable);
        upgrade.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                try {
                    MetaState newMeta = HamletService.upgradeGuild(game.meta());
                    game.applyMeta(newMeta);
                    feedbackLabel.setText(I18n.t("hamlet.upgrade.success",
                            newMeta.buildingLevel(MetaState.B_GUILD)));
                    rebuildUi();
                } catch (HamletService.HamletException ex) {
                    feedbackLabel.setText(ex.getMessage());
                    rebuildUi();
                }
            }
        });
        root.add(upgrade).colspan(2).pad(10).width(420).height(60).row();
    }

    private void rebuildUi() {
        root.clear();
        Label title = new Label(I18n.t("guild.title"), new Label.LabelStyle(titleFont, Color.GOLD));
        root.add(title).colspan(2).pad(20).row();

        Label gold = new Label(I18n.t("hamlet.gold", game.meta().gold())
                + "  |  " + I18n.t("hamlet.heirloom", game.meta().heirloom())
                + "  |  " + I18n.t("hamlet.buildingLevel",
                        game.meta().buildingLevel(MetaState.B_GUILD)),
                skin);
        root.add(gold).colspan(2).pad(10).row();
        addUpgradeButton();

        root.add(feedbackLabel).colspan(2).pad(10).row();

        MetaState meta = game.meta();
        if (meta.roster().isEmpty()) {
            Label none = new Label(I18n.t("guild.empty"), skin);
            none.setColor(Color.SALMON);
            root.add(none).colspan(2).pad(20).row();
        } else {
            for (HeroState rs : meta.roster()) {
                HeroData data = game.gameData().heroes().get(rs.heroId());
                String name = data != null ? data.displayName() : rs.heroId();
                // Sprint 12 B2 — show XP progress inline with the level. At max
                // level the XP requirement is 0; show "MAX" instead.
                int xpReq = HamletService.xpRequiredForNextLevel(rs.level());
                String heroText = xpReq > 0
                        ? I18n.t("guild.heroLine", name, rs.level())
                            + "  XP " + rs.currentXp() + "/" + xpReq
                        : I18n.t("guild.heroLine", name, rs.level()) + "  XP MAX";
                Label heroLabel = new Label(heroText, skin);

                int cost = HamletService.levelUpCost(rs.level());
                boolean atMax = rs.level() >= HamletService.GUILD_MAX_LEVEL;
                boolean enoughXp = rs.currentXp() >= xpReq;
                boolean enoughGold = meta.gold() >= cost;
                String btnText = atMax ? I18n.t("guild.maxed") : I18n.t("guild.levelup", cost);
                TextButton btn = new TextButton(btnText, skin);
                btn.setDisabled(atMax || !enoughGold || !enoughXp);
                final String idCaptured = rs.heroId();
                btn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                        try {
                            game.applyMeta(HamletService.levelUpHero(game.meta(), idCaptured, game.gameData()));
                            feedbackLabel.setText(I18n.t("guild.success", idCaptured));
                            rebuildUi();
                        } catch (HamletService.HamletException ex) {
                            feedbackLabel.setText(ex.getMessage());
                            rebuildUi();
                        }
                    }
                });

                // Sprint 13 B3 — wrap hero label in FRAME_PORTRAIT NinePatch if atlas loaded.
                if (SkinLoader.hasComponents(skin) && skin.has(SkinLoader.FRAME_PORTRAIT, Drawable.class)) {
                    Container<Label> framed = new Container<>(heroLabel);
                    framed.background(skin.getDrawable(SkinLoader.FRAME_PORTRAIT));
                    framed.setColor(1f, 1f, 1f, 0.7f);
                    framed.pad(8f);
                    root.add(framed).pad(10).left();
                } else {
                    root.add(heroLabel).pad(10).left();
                }
                root.add(btn).pad(10).width(280).height(60);
                root.row();
            }
        }

        TextButton back = new TextButton(I18n.t("button.back"), skin);
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
