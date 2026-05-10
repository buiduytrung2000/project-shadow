package com.trungbui.projectshadow.render;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 9 — verifies state machine behavior + graceful fallback when atlas is missing.
 *
 * <p>Cannot test actual animation rendering (no GL context in headless tests), but
 * state transitions and missing-atlas behavior are pure logic.</p>
 */
class SpriteAnimatorTest {

    @Test
    void state_initIdle() {
        SpriteAnimator a = new SpriteAnimator(new SpriteAtlas(), "hero_warrior");
        assertThat(a.currentState()).isEqualTo(SpriteAnimator.State.IDLE);
    }

    @Test
    void hasAnimations_falseWhenAtlasMissing() {
        SpriteAnimator a = new SpriteAnimator(new SpriteAtlas(), "hero_warrior");
        assertThat(a.hasAnimations()).isFalse();
    }

    @Test
    void currentFrame_nullWhenAtlasMissing() {
        SpriteAnimator a = new SpriteAnimator(new SpriteAtlas(), "hero_warrior");
        assertThat(a.currentFrame()).isNull();
    }

    @Test
    void setState_changesCurrentState() {
        SpriteAnimator a = new SpriteAnimator(new SpriteAtlas(), "hero_warrior");
        a.setState(SpriteAnimator.State.ATTACKING);
        assertThat(a.currentState()).isEqualTo(SpriteAnimator.State.ATTACKING);
    }

    @Test
    void setState_nullIsNoOp() {
        SpriteAnimator a = new SpriteAnimator(new SpriteAtlas(), "hero_warrior");
        a.setState(null);
        assertThat(a.currentState()).isEqualTo(SpriteAnimator.State.IDLE);
    }

    @Test
    void update_doesNotCrashWhenNoAnimations() {
        SpriteAnimator a = new SpriteAnimator(new SpriteAtlas(), "hero_warrior");
        a.update(0.1f);
        a.update(0.5f);
        // ATTACKING/HURT auto-transition logic gated on animation existence;
        // missing atlas → no transition → state remains as-set
        a.setState(SpriteAnimator.State.ATTACKING);
        a.update(10f);
        assertThat(a.currentState()).isEqualTo(SpriteAnimator.State.ATTACKING);
    }

    @Test
    void deadState_holdsAcrossUpdates() {
        SpriteAnimator a = new SpriteAnimator(new SpriteAtlas(), "hero_warrior");
        a.setState(SpriteAnimator.State.DEAD);
        a.update(100f);
        assertThat(a.currentState()).isEqualTo(SpriteAnimator.State.DEAD);
    }

    @Test
    void atlas_constructorWithMissingPath_isUnloaded() {
        SpriteAtlas atlas = new SpriteAtlas("definitely/missing/path.atlas");
        assertThat(atlas.isLoaded()).isFalse();
        assertThat(atlas.findRegion("anything")).isNull();
        assertThat(atlas.findRegions("anything")).isEmpty();
    }
}
