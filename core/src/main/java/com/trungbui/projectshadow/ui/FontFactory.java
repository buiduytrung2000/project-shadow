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

    private static final String BOLD_TTF_PATH = "fonts/BeVietnamPro-Bold.ttf";

    private final FreeTypeFontGenerator generator;
    /** Lazy-loaded bold generator — null until first bold font is requested. */
    private FreeTypeFontGenerator boldGenerator;
    private final List<BitmapFont> generated = new ArrayList<>();

    public FontFactory(FileHandle ttf) {
        if (ttf == null || !ttf.exists()) {
            throw new IllegalArgumentException(
                    "Font TTF not found: " + (ttf == null ? "null" : ttf.path())
                            + ". Place BeVietnamPro-Regular.ttf into assets/fonts/.");
        }
        this.generator = new FreeTypeFontGenerator(ttf);
    }

    /**
     * Create a BitmapFont at the given pixel size using the bold variant if available,
     * otherwise fall back to the regular generator with synthetic bold (borderWidth=1).
     *
     * @param sizePx font size in pixels (must be &gt; 0)
     * @param bold   true to use bold weight; false for regular
     */
    public BitmapFont create(int sizePx, boolean bold) {
        if (sizePx <= 0) throw new IllegalArgumentException("sizePx must be > 0, got " + sizePx);
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = sizePx;
        p.characters = VietnameseCharset.ALL;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        FreeTypeFontGenerator gen;
        if (bold) {
            gen = boldGeneratorOrSynthetic(p);
        } else {
            gen = generator;
        }
        BitmapFont f = gen.generateFont(p);
        generated.add(f);
        return f;
    }

    /**
     * Create a BitmapFont using the bold weight (default).
     * Equivalent to {@code create(sizePx, true)}.
     */
    public BitmapFont create(int sizePx) {
        return create(sizePx, true);
    }

    /**
     * Returns the bold generator if the bold TTF is available, otherwise falls back to the
     * regular generator and sets synthetic bold via {@code borderWidth=1.0f} on the parameter.
     */
    private FreeTypeFontGenerator boldGeneratorOrSynthetic(FreeTypeFontParameter p) {
        if (boldGenerator == null) {
            com.badlogic.gdx.files.FileHandle boldFile =
                    com.badlogic.gdx.Gdx.files.internal(BOLD_TTF_PATH);
            if (boldFile.exists()) {
                boldGenerator = new FreeTypeFontGenerator(boldFile);
            }
        }
        if (boldGenerator != null) {
            return boldGenerator;
        }
        // Fallback: synthetic bold via FreeType border (renders as bold-ish outline).
        p.borderWidth = 1.0f;
        return generator;
    }

    @Override
    public void dispose() {
        for (BitmapFont f : generated) f.dispose();
        generated.clear();
        generator.dispose();
        if (boldGenerator != null) boldGenerator.dispose();
    }
}
