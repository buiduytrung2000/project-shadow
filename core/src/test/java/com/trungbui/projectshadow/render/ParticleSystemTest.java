package com.trungbui.projectshadow.render;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class ParticleSystemTest {

    @Test
    void emitCritBurst_emits12Particles() {
        ParticleSystem ps = new ParticleSystem(new Random(42));
        assertThat(ps.activeCount()).isZero();
        ps.emitCritBurst(100, 200);
        assertThat(ps.activeCount()).isEqualTo(12);
    }

    @Test
    void emitHitSplash_emits6Particles() {
        ParticleSystem ps = new ParticleSystem(new Random(1));
        ps.emitHitSplash(50, 50);
        assertThat(ps.activeCount()).isEqualTo(6);
    }

    @Test
    void emitHealGlow_emits8Particles() {
        ParticleSystem ps = new ParticleSystem(new Random(1));
        ps.emitHealGlow(30, 30);
        assertThat(ps.activeCount()).isEqualTo(8);
    }

    @Test
    void update_decreasesLifeOnAllParticles() {
        ParticleSystem ps = new ParticleSystem(new Random(1));
        ps.emitCritBurst(0, 0);
        // crit lifetime = 0.5s; after 0.1s all still alive
        ps.update(0.1f);
        assertThat(ps.activeCount()).isEqualTo(12);
    }

    @Test
    void update_removesExpiredParticles() {
        ParticleSystem ps = new ParticleSystem(new Random(1));
        ps.emitHitSplash(0, 0); // life=0.3
        ps.update(0.4f); // exceed lifetime
        assertThat(ps.activeCount()).isZero();
    }

    @Test
    void update_appliesVelocity() {
        Particle p = new Particle(0, 0, 100, 50, 1f, Color.WHITE, 4f, 0f);
        p.update(0.5f);
        assertThat(p.x).isEqualTo(50f);
        assertThat(p.y).isEqualTo(25f);
    }

    @Test
    void update_appliesGravity() {
        Particle p = new Particle(0, 0, 0, 100, 1f, Color.WHITE, 4f, 200f);
        p.update(0.5f);
        // vy decreased by gravity*delta = 100
        assertThat(p.vy).isEqualTo(0f);
    }

    @Test
    void clear_removesAll() {
        ParticleSystem ps = new ParticleSystem(new Random(1));
        ps.emitCritBurst(0, 0);
        ps.clear();
        assertThat(ps.activeCount()).isZero();
    }

    @Test
    void alpha_proportionalToRemainingLife() {
        Particle p = new Particle(0, 0, 0, 0, 1f, Color.WHITE, 4f, 0f);
        assertThat(p.alpha()).isEqualTo(1f);
        p.update(0.5f);
        assertThat(p.alpha()).isCloseTo(0.5f, org.assertj.core.data.Offset.offset(0.01f));
        p.update(0.5f);
        assertThat(p.alpha()).isLessThanOrEqualTo(0.01f);
    }

    @Test
    void particle_isAlive_falseWhenExpired() {
        Particle p = new Particle(0, 0, 0, 0, 0.5f, Color.WHITE, 4f, 0f);
        assertThat(p.isAlive()).isTrue();
        p.update(1.0f);
        assertThat(p.isAlive()).isFalse();
    }
}
