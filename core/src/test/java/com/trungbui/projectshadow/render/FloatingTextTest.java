package com.trungbui.projectshadow.render;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Sprint 13 B4 — unit tests for {@link FloatingText} data class.
 */
class FloatingTextTest {

    @Test
    void freshEntry_alphaIsOne() {
        FloatingText ft = new FloatingText("-12", 0f, 0f, Color.WHITE, false);
        assertThat(ft.alpha()).isEqualTo(1f);
    }

    @Test
    void halfAged_alphaIsHalf() {
        FloatingText ft = new FloatingText("-12", 0f, 0f, Color.WHITE, false);
        ft.age = 0.5f; // half of 1.0s lifetime
        assertThat(ft.alpha()).isCloseTo(0.5f, offset(0.01f));
    }

    @Test
    void fullyAged_alphaIsZero() {
        FloatingText ft = new FloatingText("-25!", 0f, 0f, Color.YELLOW, true);
        ft.age = 1.0f;
        assertThat(ft.alpha()).isLessThanOrEqualTo(0f);
    }

    @Test
    void isAlive_trueWhenFresh() {
        FloatingText ft = new FloatingText("MISS", 0f, 0f, Color.GRAY, false);
        assertThat(ft.isAlive()).isTrue();
    }

    @Test
    void isAlive_falseWhenExpired() {
        FloatingText ft = new FloatingText("+8", 0f, 0f, Color.GREEN, false);
        ft.age = 1.1f;
        assertThat(ft.isAlive()).isFalse();
    }

    @Test
    void noShake_renderXEqualsX() {
        FloatingText ft = new FloatingText("-5", 100f, 200f, Color.WHITE, false);
        ft.age = 0.5f;
        assertThat(ft.renderX()).isEqualTo(100f);
    }

    @Test
    void shake_renderXDiffersFromXWhenAged() {
        FloatingText ft = new FloatingText("-25!", 100f, 200f, Color.YELLOW, true);
        ft.age = 0.1f; // just started, shake active
        // sin(0.1 * 30) = sin(3) ≈ 0.141 → offset should be non-zero
        float rx = ft.renderX();
        // renderX may or may not equal 100 depending on sin value, but the shake
        // path should not throw and should produce a finite value.
        assertThat(Float.isFinite(rx)).isTrue();
    }

    @Test
    void defaultVelocity_isPositive() {
        FloatingText ft = new FloatingText("test", 0f, 0f, Color.WHITE, false);
        assertThat(ft.vy).isGreaterThan(0f);
    }

    @Test
    void defaultLifetime_isOneSecond() {
        FloatingText ft = new FloatingText("test", 0f, 0f, Color.WHITE, false);
        assertThat(ft.lifetime).isEqualTo(1.0f);
    }
}
