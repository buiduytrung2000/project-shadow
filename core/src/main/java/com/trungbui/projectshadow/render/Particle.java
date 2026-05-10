package com.trungbui.projectshadow.render;

import com.badlogic.gdx.graphics.Color;

/**
 * Sprint 9 — single particle: position, velocity, life, color, size.
 *
 * <p>Mutated in place by {@link ParticleSystem#update}. Kept as a class (not a record)
 * because particles are pooled and frequently mutated for performance.</p>
 */
public final class Particle {

    public float x;
    public float y;
    public float vx;
    public float vy;
    public float life;
    public final float maxLife;
    public final Color color;
    public final float size;
    public final float gravity;

    public Particle(float x, float y, float vx, float vy,
                    float life, Color color, float size, float gravity) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
        this.color = new Color(color);
        this.size = size;
        this.gravity = gravity;
    }

    public void update(float delta) {
        x += vx * delta;
        y += vy * delta;
        vy -= gravity * delta;
        life -= delta;
    }

    public boolean isAlive() {
        return life > 0;
    }

    public float alpha() {
        return Math.max(0f, Math.min(1f, life / maxLife));
    }
}
