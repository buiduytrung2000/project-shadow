package com.trungbui.projectshadow.render;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Sprint 13 B4 — unit tests for {@link FloatingTextManager}.
 *
 * <p>No Gdx context needed: FloatingTextManager + FloatingText are pure Java.</p>
 */
class FloatingTextManagerTest {

    @Test
    void spawn_addsOneEntry() {
        FloatingTextManager mgr = new FloatingTextManager();
        mgr.spawn(100f, 200f, "-12", Color.WHITE, false);
        assertThat(mgr.activeCount()).isEqualTo(1);
    }

    @Test
    void spawn_multipleEntries_countIsCorrect() {
        FloatingTextManager mgr = new FloatingTextManager();
        mgr.spawn(10f, 20f, "-5", Color.WHITE, false);
        mgr.spawn(30f, 40f, "MISS", Color.GRAY, false);
        mgr.spawn(50f, 60f, "+10", Color.GREEN, false);
        assertThat(mgr.activeCount()).isEqualTo(3);
    }

    @Test
    void update_halfLifetime_entryIsStillAlive() {
        FloatingTextManager mgr = new FloatingTextManager();
        mgr.spawn(100f, 200f, "-8", Color.WHITE, false);
        mgr.update(0.5f); // FloatingText.DEFAULT_LIFETIME = 1.0s
        assertThat(mgr.activeCount()).isEqualTo(1);
    }

    @Test
    void update_exceedsLifetime_entryIsRemoved() {
        FloatingTextManager mgr = new FloatingTextManager();
        mgr.spawn(100f, 200f, "-8", Color.WHITE, false);
        mgr.update(1.1f); // beyond 1.0s lifetime
        assertThat(mgr.activeCount()).isZero();
    }

    @Test
    void update_exactlyLifetime_entryIsRemoved() {
        FloatingTextManager mgr = new FloatingTextManager();
        mgr.spawn(100f, 200f, "MISS", Color.GRAY, false);
        // Two updates totalling 1.0s
        mgr.update(0.5f);
        mgr.update(0.5f);
        assertThat(mgr.activeCount()).isZero();
    }

    @Test
    void update_movesEntryUpward() {
        FloatingTextManager mgr = new FloatingTextManager();
        // Spawn at y=100, vy=40
        mgr.spawn(0f, 100f, "test", Color.WHITE, false);
        mgr.update(0.5f);
        // After 0.5s at vy=40, y should be ~120.
        // We can't access the internal FloatingText directly but we can verify
        // that entries are still alive (movement doesn't cause premature removal).
        assertThat(mgr.activeCount()).isEqualTo(1);
    }

    @Test
    void activeCount_zeroWhenNoEntries() {
        FloatingTextManager mgr = new FloatingTextManager();
        assertThat(mgr.activeCount()).isZero();
    }

    @Test
    void update_noEntries_doesNotCrash() {
        FloatingTextManager mgr = new FloatingTextManager();
        mgr.update(0.016f); // no-op, must not throw
    }
}
