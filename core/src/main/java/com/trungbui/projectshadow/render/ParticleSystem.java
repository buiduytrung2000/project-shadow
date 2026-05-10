package com.trungbui.projectshadow.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * Sprint 9 — programmatic particle effects (no asset files required).
 *
 * <p>Effect catalog:</p>
 * <ul>
 *   <li>{@link #emitCritBurst}: 12 yellow stars exploding outward from impact</li>
 *   <li>{@link #emitHitSplash}: 6 red dots radial</li>
 *   <li>{@link #emitHealGlow}: 8 green sparkles rising upward</li>
 * </ul>
 */
public class ParticleSystem {

    private static final float CRIT_GRAVITY = 200f;
    private static final float HIT_GRAVITY = 100f;
    private static final float HEAL_GRAVITY = -50f; // negative → particles rise

    private final List<Particle> particles = new ArrayList<>();
    private final RandomGenerator rng;

    public ParticleSystem() {
        this(new Random());
    }

    public ParticleSystem(RandomGenerator rng) {
        this.rng = rng;
    }

    public void emitCritBurst(float x, float y) {
        for (int i = 0; i < 12; i++) {
            float angle = rng.nextFloat() * MathUtils.PI2;
            float speed = 150f + rng.nextFloat() * 100f;
            particles.add(new Particle(x, y,
                    MathUtils.cos(angle) * speed,
                    MathUtils.sin(angle) * speed,
                    0.5f,
                    Color.YELLOW,
                    5f,
                    CRIT_GRAVITY));
        }
    }

    public void emitHitSplash(float x, float y) {
        for (int i = 0; i < 6; i++) {
            float angle = rng.nextFloat() * MathUtils.PI2;
            float speed = 80f + rng.nextFloat() * 60f;
            particles.add(new Particle(x, y,
                    MathUtils.cos(angle) * speed,
                    MathUtils.sin(angle) * speed,
                    0.3f,
                    new Color(0.95f, 0.20f, 0.20f, 1f),
                    4f,
                    HIT_GRAVITY));
        }
    }

    public void emitHealGlow(float x, float y) {
        for (int i = 0; i < 8; i++) {
            float vx = (rng.nextFloat() - 0.5f) * 50f;
            float vy = 60f + rng.nextFloat() * 40f;
            particles.add(new Particle(x + (rng.nextFloat() - 0.5f) * 80f, y,
                    vx, vy,
                    0.7f,
                    new Color(0.40f, 0.95f, 0.40f, 1f),
                    4f,
                    HEAL_GRAVITY));
        }
    }

    public void update(float delta) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(delta);
            if (!p.isAlive()) it.remove();
        }
    }

    public void render(ShapeRenderer shapes) {
        if (particles.isEmpty()) return;
        // Caller manages begin/end if sharing the renderer; we manage our own here.
        boolean wasDrawing = shapes.isDrawing();
        if (wasDrawing) shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Particle p : particles) {
            shapes.setColor(p.color.r, p.color.g, p.color.b, p.alpha());
            shapes.circle(p.x, p.y, p.size);
        }
        shapes.end();
        if (wasDrawing) shapes.begin(ShapeRenderer.ShapeType.Filled);
    }

    public int activeCount() {
        return particles.size();
    }

    public void clear() {
        particles.clear();
    }
}
