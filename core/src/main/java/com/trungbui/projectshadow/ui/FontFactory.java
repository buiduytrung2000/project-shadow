package com.trungbui.projectshadow.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.List;

public final class FontFactory implements Disposable {

    private final FreeTypeFontGenerator generator;
    private final List<BitmapFont> generated = new ArrayList<>();

    public FontFactory(FileHandle ttf) {
        if (ttf == null || !ttf.exists()) {
            throw new IllegalArgumentException(
                    "Font TTF not found: " + (ttf == null ? "null" : ttf.path())
                            + ". Place BeVietnamPro-Regular.ttf into assets/fonts/.");
        }
        this.generator = new FreeTypeFontGenerator(ttf);
    }

    public BitmapFont create(int sizePx) {
        if (sizePx <= 0) throw new IllegalArgumentException("sizePx must be > 0, got " + sizePx);
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = sizePx;
        p.characters = VietnameseCharset.ALL;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        BitmapFont f = generator.generateFont(p);
        generated.add(f);
        return f;
    }

    @Override
    public void dispose() {
        for (BitmapFont f : generated) f.dispose();
        generated.clear();
        generator.dispose();
    }
}
