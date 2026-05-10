package com.trungbui.projectshadow.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * Sprint 9 — wraps a libGDX {@link TextureAtlas} with graceful fallback when the
 * atlas file is missing. The game must remain playable without sprite assets.
 *
 * <p>Loaded atlases come from {@code assets/sprites/combatants.atlas} (produced by
 * the {@code :core:packAssets} gradle task or TexturePacker GUI).</p>
 */
public class SpriteAtlas implements Disposable {

    private final TextureAtlas atlas;
    private final boolean loaded;

    public SpriteAtlas(String atlasPath) {
        TextureAtlas a = null;
        boolean ok = false;
        try {
            FileHandle f = Gdx.files != null ? Gdx.files.internal(atlasPath) : null;
            if (f != null && f.exists()) {
                a = new TextureAtlas(f);
                ok = true;
            } else {
                log("Atlas missing, fallback to rectangles: " + atlasPath);
            }
        } catch (Exception e) {
            log("Failed to load atlas " + atlasPath + ": " + e.getMessage());
        }
        this.atlas = a;
        this.loaded = ok;
    }

    /** Test-only constructor for headless tests with no atlas file. */
    public SpriteAtlas() {
        this.atlas = null;
        this.loaded = false;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public Array<TextureAtlas.AtlasRegion> findRegions(String name) {
        return loaded ? atlas.findRegions(name) : new Array<>();
    }

    public TextureAtlas.AtlasRegion findRegion(String name) {
        return loaded ? atlas.findRegion(name) : null;
    }

    private static void log(String msg) {
        if (Gdx.app != null) {
            Gdx.app.log("SpriteAtlas", msg);
        } else {
            System.err.println("[SpriteAtlas] " + msg);
        }
    }

    @Override
    public void dispose() {
        if (atlas != null) atlas.dispose();
    }
}
