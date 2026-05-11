package com.trungbui.projectshadow.render;

import com.badlogic.gdx.graphics.Color;
import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Hero;

public class CombatantView {

    public static final float WIDTH = 150f;
    public static final float HEIGHT = 200f;
    private static final float SHAKE_DURATION = 0.4f;
    private static final float SHAKE_AMPLITUDE = 12f;

    private final Combatant combatant;
    private final float baseX;
    private final float baseY;
    private final Color color;

    private float shakeTimer = 0f;
    private float flashTimer = 0f;
    private SpriteAnimator animator; // null if no atlas — caller falls back to rectangle
    private boolean wasAlive = true; // tracks alive→dead transition so we trigger DEAD anim once

    public CombatantView(Combatant combatant, float baseX, float baseY) {
        this.combatant = combatant;
        this.baseX = baseX;
        this.baseY = baseY;
        this.color = colorFor(combatant);
        this.wasAlive = combatant.isAlive();
    }

    public void attachAnimator(SpriteAnimator animator) {
        this.animator = animator;
    }

    public SpriteAnimator animator() {
        return animator;
    }

    public Combatant combatant() {
        return combatant;
    }

    public float x() {
        if (shakeTimer <= 0f) return baseX;
        float t = shakeTimer / SHAKE_DURATION;
        double phase = t * Math.PI * 6;
        return baseX + (float) Math.sin(phase) * SHAKE_AMPLITUDE * t;
    }

    public float y() {
        return baseY;
    }

    public float width() {
        return WIDTH;
    }

    public float height() {
        return HEIGHT;
    }

    public Color color() {
        return color;
    }

    public boolean isFlashing() {
        return flashTimer > 0f;
    }

    public void triggerHit() {
        shakeTimer = SHAKE_DURATION;
        flashTimer = 0.2f;
        if (animator != null) animator.setState(SpriteAnimator.State.HURT);
    }

    public void triggerAttack() {
        if (animator != null) animator.setState(SpriteAnimator.State.ATTACKING);
    }

    public void update(float delta) {
        if (shakeTimer > 0f) shakeTimer = Math.max(0f, shakeTimer - delta);
        if (flashTimer > 0f) flashTimer = Math.max(0f, flashTimer - delta);

        // Detect alive→dead transition: trigger DEAD animation once. SpriteAnimator
        // holds the last frame (PlayMode.NORMAL on DEAD) so the corpse stays visible.
        boolean alive = combatant.isAlive();
        if (wasAlive && !alive) {
            if (animator != null) animator.setState(SpriteAnimator.State.DEAD);
        }
        wasAlive = alive;

        if (animator != null) animator.update(delta);
    }

    private static Color colorFor(Combatant c) {
        if (c instanceof Hero h) {
            String role = h.data().role().toLowerCase();
            if (role.contains("tank")) return new Color(0.75f, 0.30f, 0.25f, 1f);
            if (role.contains("heal")) return new Color(0.95f, 0.95f, 0.85f, 1f);
            if (role.contains("support")) return new Color(0.50f, 0.75f, 0.95f, 1f);
            if (role.contains("dps")) return new Color(0.95f, 0.65f, 0.20f, 1f);
            return new Color(0.70f, 0.70f, 0.70f, 1f);
        }
        if (c instanceof Enemy e) {
            String variant = e.data().variantType();
            return switch (variant == null ? "" : variant) {
                case "Tank" -> new Color(0.45f, 0.30f, 0.20f, 1f);
                case "Assassin" -> new Color(0.40f, 0.10f, 0.15f, 1f);
                case "Special" -> new Color(0.45f, 0.20f, 0.55f, 1f);
                case "Boss" -> new Color(0.60f, 0.10f, 0.10f, 1f);
                default -> new Color(0.30f, 0.45f, 0.25f, 1f);
            };
        }
        return Color.GRAY;
    }
}
