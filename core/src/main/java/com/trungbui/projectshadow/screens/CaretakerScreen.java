package com.trungbui.projectshadow.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
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
import com.trungbui.projectshadow.save.HeroState;
import com.trungbui.projectshadow.ui.FontFactory;
import com.trungbui.projectshadow.ui.SkinLoader;

public class CaretakerScreen implements Screen {

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

    public CaretakerScreen(ProjectShadowGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        this.fontFactory = new FontFactory(Gdx.files.internal("fonts/BeVietnamPro-Regular.ttf"));
        this.titleFont = fontFactory.create(48);
        var bodyFont = fontFactory.create(22);
        this.skin = SkinLoader.load();
        skin.get(Label.LabelStyle.class).font = bodyFont;
        skin.get(TextButton.TextButtonStyle.class).font = bodyFont;
        this.uiStage = new Stage(viewport);

        this.root = new Table();
        root.setFillParent(true);
        root.top().pad(40);

        this.feedbackLabel = new Label(I18n.t("caretaker.intro",
                HamletService.CARETAKER_DISEASE_CURE_COST,
                HamletService.CARETAKER_STRESS_RELIEF_BLOCK,
                HamletService.CARETAKER_STRESS_RELIEF_COST), skin);
        feedbackLabel.setColor(Color.WHITE);

        rebuildUi();
        uiStage.addActor(root);
        viewport.apply(true);
    }

    /** Sprint 10 B2 — Caretaker upgrade button. */
    private void addUpgradeButton() {
        int currentLevel = game.meta().buildingLevel(MetaState.B_CARETAKER);
        if (currentLevel >= 3) {
            Label maxed = new Label(I18n.t("hamlet.upgrade.maxed"), skin);
            maxed.setColor(Color.GOLD);
            root.add(maxed).colspan(3).pad(8).row();
            return;
        }
        int costGold = HamletService.upgradeGoldCost(MetaState.B_CARETAKER, currentLevel);
        int costHeir = HamletService.upgradeHeirloomCost(MetaState.B_CARETAKER, currentLevel);
        TextButton upgrade = new TextButton(
                I18n.t("hamlet.upgrade.button", currentLevel + 1, costGold, costHeir),
                skin);
        boolean affordable = game.meta().gold() >= costGold && game.meta().heirloom() >= costHeir;
        upgrade.setDisabled(!affordable);
        upgrade.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                try {
                    MetaState newMeta = HamletService.upgradeCaretaker(game.meta());
                    game.applyMeta(newMeta);
                    feedbackLabel.setText(I18n.t("hamlet.upgrade.success",
                            newMeta.buildingLevel(MetaState.B_CARETAKER)));
                    rebuildUi();
                } catch (HamletService.HamletException ex) {
                    feedbackLabel.setText(ex.getMessage());
                    rebuildUi();
                }
            }
        });
        root.add(upgrade).colspan(3).pad(10).width(420).height(60).row();
    }

    private void rebuildUi() {
        root.clear();
        Label title = new Label(I18n.t("caretaker.title"), new Label.LabelStyle(titleFont, Color.GOLD));
        root.add(title).colspan(3).pad(20).row();

        int caretakerLevel = game.meta().buildingLevel(MetaState.B_CARETAKER);
        int slotsUsed = game.meta().cureSlotsUsedThisVisit();
        int slotLimit = HamletService.CARETAKER_CURE_SLOTS_BY_LEVEL[caretakerLevel];
        Label gold = new Label(I18n.t("hamlet.gold", game.meta().gold())
                + "  |  " + I18n.t("hamlet.heirloom", game.meta().heirloom())
                + "  |  " + I18n.t("hamlet.buildingLevel", caretakerLevel)
                + "  |  " + I18n.t("caretaker.cureSlots", slotsUsed, slotLimit),
                skin);
        root.add(gold).colspan(3).pad(10).row();
        addUpgradeButton();

        root.add(feedbackLabel).colspan(3).pad(10).row();

        Table list = new Table();
        MetaState meta = game.meta();
        if (meta.roster().isEmpty()) {
            list.add(new Label(I18n.t("caretaker.empty"), skin)).pad(20).row();
        } else {
            for (HeroState rs : meta.roster()) {
                addHeroRow(list, rs);
            }
        }

        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        root.add(scroll).colspan(3).width(1500).height(600).pad(20).row();

        TextButton back = new TextButton(I18n.t("button.back"), skin);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.returnToHamlet();
            }
        });
        root.add(back).colspan(3).pad(20).width(280).height(70);
    }

    private void addHeroRow(Table list, HeroState rs) {
        HeroData data = game.gameData().heroes().get(rs.heroId());
        String name = data != null ? data.displayName() : rs.heroId();
        String diseases = rs.diseases().isEmpty()
                ? I18n.t("caretaker.noDisease")
                : String.join(",", rs.diseases());
        String info = I18n.t("caretaker.heroLine", name, rs.currentHp(), rs.currentStress(), diseases);
        Label heroLabel = new Label(info, skin);
        list.add(heroLabel).left().pad(8);

        // stress relief button
        TextButton stress = new TextButton(I18n.t("caretaker.stress",
                HamletService.CARETAKER_STRESS_RELIEF_BLOCK,
                HamletService.CARETAKER_STRESS_RELIEF_COST), skin);
        stress.setDisabled(rs.currentStress() == 0
                || game.meta().gold() < HamletService.CARETAKER_STRESS_RELIEF_COST);
        final String idCaptured = rs.heroId();
        stress.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                try {
                    game.applyMeta(HamletService.reduceStress(game.meta(), idCaptured, game.gameData()));
                    feedbackLabel.setText(I18n.t("caretaker.stressed", idCaptured));
                } catch (HamletService.HamletException ex) {
                    feedbackLabel.setText(ex.getMessage());
                }
                rebuildUi();
            }
        });
        list.add(stress).pad(8).width(260).height(50);

        // disease cure button (one button per disease)
        Table diseaseCol = new Table();
        for (String d : rs.diseases()) {
            TextButton cure = new TextButton(I18n.t("caretaker.cure",
                    d, HamletService.CARETAKER_DISEASE_CURE_COST), skin);
            cure.setDisabled(game.meta().gold() < HamletService.CARETAKER_DISEASE_CURE_COST);
            final String diseaseCaptured = d;
            cure.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    try {
                        game.applyMeta(HamletService.cureDisease(
                                game.meta(), idCaptured, diseaseCaptured, game.gameData()));
                        feedbackLabel.setText(I18n.t("caretaker.cured", diseaseCaptured, idCaptured));
                    } catch (HamletService.HamletException ex) {
                        feedbackLabel.setText(ex.getMessage());
                    }
                    rebuildUi();
                }
            });
            diseaseCol.add(cure).pad(4).width(280).height(40).row();
        }
        list.add(diseaseCol).pad(8);
        list.row();
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.06f, 0.08f, 0.07f, 1f);
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
