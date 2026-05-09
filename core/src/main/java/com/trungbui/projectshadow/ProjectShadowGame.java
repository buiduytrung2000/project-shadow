package com.trungbui.projectshadow;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ProjectShadowGame extends ApplicationAdapter {

    public static final int VIRTUAL_WIDTH = 1920;
    public static final int VIRTUAL_HEIGHT = 1080;

    private SpriteBatch batch;
    private BitmapFont font;
    private OrthographicCamera camera;
    private Viewport viewport;
    private String firstHeroLine = "(loading...)";

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(2f);

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply(true);

        // ─── Verify pipeline đọc data ───
        FileHandle heroesCsv = Gdx.files.internal("data/heroes.csv");
        if (heroesCsv.exists()) {
            String[] lines = heroesCsv.readString("UTF-8").split("\n");
            if (lines.length >= 2) {
                firstHeroLine = "Loaded: " + lines[1].substring(0, Math.min(lines[1].length(), 80));
            } else {
                firstHeroLine = "CSV empty";
            }
        } else {
            firstHeroLine = "ERROR: heroes.csv NOT found at assets/data/";
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        font.draw(batch, "PROJECT SHADOW", 100, VIRTUAL_HEIGHT - 100);
        font.draw(batch, "libGDX 1.13 + JDK 21 + Pixel HD 1920x1080", 100, VIRTUAL_HEIGHT - 160);
        font.draw(batch, "Data pipeline test:", 100, VIRTUAL_HEIGHT - 280);
        font.draw(batch, firstHeroLine, 100, VIRTUAL_HEIGHT - 340);
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 100, 100);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
