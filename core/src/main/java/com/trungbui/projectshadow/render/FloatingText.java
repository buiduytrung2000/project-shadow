package com.trungbui.projectshadow.render;

import com.badlogic.gdx.graphics.Color;

/**
 * Sprint 13 B4 — a single floating combat text entry.
 *
 * <p>Rendered over the target combatant and fades out as {@code age} approaches
 * {@code lifetime}. Crit entries additionally shake horizontally.</p>
 */
public final class FloatingText {

    /** Upward velocity in world units per second. */
    static final float DEFAULT_VY = 40f;
    /** Default lifetime in seconds before the entry is removed. */
    static final float DEFAULT_LIFETIME = 1.0f;
    /** Horizontal shake frequency (radians per second) for crit entries. */
    static final float SHAKE_FREQ = 30f;
    /** Peak horizontal shake amplitude in world units. */
    static final float SHAKE_AMPLITUDE = 2f;

    final String text;
    float x;
    float y;
    float vy;
    float age;
    float lifetime;
    Color color;
    boolean shake;

    public FloatingText(String text, float x, float y, Color color, boolean shake) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.vy = DEFAULT_VY;
        this.age = 0f;
        this.lifetime = DEFAULT_LIFETIME;
        this.color = color;
        this.shake = shake;
    }

    /** Alpha based on remaining life: 1.0 when fresh, 0.0 when expired. */
    public float alpha() {
        return Math.max(0f, 1f - (age / lifetime));
    }

    /** Whether this entry should still be rendered / updated. */
    public boolean isAlive() {
        return age < lifetime;
    }

    /** Current render X with optional crit shake applied. */
    public float renderX() {
        if (!shake || age <= 0f) return x;
        return x + (float) Math.sin(age * SHAKE_FREQ) * SHAKE_AMPLITUDE;
    }
}
