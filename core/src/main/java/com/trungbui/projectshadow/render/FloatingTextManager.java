package com.trungbui.projectshadow.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Sprint 13 B4 — manages floating combat text entries (damage numbers, MISS, +heal).
 *
 * <p>Call {@link #spawn} to add a new entry, {@link #update} each frame to age entries
 * and remove expired ones, and {@link #render} inside an active SpriteBatch pass to
 * draw them.</p>
 */
public class FloatingTextManager {

    private final List<FloatingText> active = new ArrayList<>();

    /**
     * Spawns a new floating text at the given world coordinates.
     *
     * @param x      world X (typically the combatant's sprite centre-X)
     * @param y      world Y (typically the top of the combatant's sprite)
     * @param text   the string to render (e.g. "-12", "+8", "MISS", "-25!")
     * @param color  fill colour for the text
     * @param shake  when true, the entry shakes horizontally (used for crits)
     */
    public void spawn(float x, float y, String text, Color color, boolean shake) {
        active.add(new FloatingText(text, x, y, new Color(color), shake));
    }

    /**
     * Ages all active entries and removes those that have exceeded their lifetime.
     *
     * @param delta seconds since last frame
     */
    public void update(float delta) {
        Iterator<FloatingText> it = active.iterator();
        while (it.hasNext()) {
            FloatingText ft = it.next();
            ft.age += delta;
            ft.y += ft.vy * delta;
            if (!ft.isAlive()) it.remove();
        }
    }

    /**
     * Renders all live entries. Must be called inside {@code batch.begin()} / {@code batch.end()}.
     * Restores font color to WHITE after drawing.
     *
     * @param batch active SpriteBatch
     * @param font  BitmapFont to draw with (alpha channel set per entry)
     */
    public void render(SpriteBatch batch, BitmapFont font) {
        if (active.isEmpty()) return;
        Color savedColor = new Color(font.getColor());
        for (FloatingText ft : active) {
            float alpha = ft.alpha();
            if (alpha <= 0f) continue;
            font.setColor(ft.color.r, ft.color.g, ft.color.b, alpha);
            font.draw(batch, ft.text, ft.renderX(), ft.y);
        }
        font.setColor(savedColor);
    }

    /** Number of currently live entries (for testing). */
    public int activeCount() {
        return active.size();
    }
}
