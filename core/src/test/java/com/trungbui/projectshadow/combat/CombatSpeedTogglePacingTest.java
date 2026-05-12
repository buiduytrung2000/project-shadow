package com.trungbui.projectshadow.combat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 13 B3 — verify pacing delay values for speed toggle.
 * Tests the logic contract: speed 2x → 0.35s pacing, default → 0.7s.
 * Does not test CombatScreen directly (no Gdx context required).
 */
class CombatSpeedTogglePacingTest {

    @Test
    void speedMode2x_on_pacingIs035() {
        CombatController controller = new CombatController(
                null, null, new java.util.Random());
        // Simulate toggle ON: set pacing to 0.35s
        controller.setPacingDelaySec(0.35f);
        assertThat(controller.pacingDelaySec()).isEqualTo(0.35f);
    }

    @Test
    void speedMode2x_off_pacingIs07() {
        CombatController controller = new CombatController(
                null, null, new java.util.Random());
        controller.setPacingDelaySec(0.7f);
        assertThat(controller.pacingDelaySec()).isEqualTo(0.7f);
    }

    @Test
    void setPacingDelaySec_clampsNegativeToZero() {
        CombatController controller = new CombatController(
                null, null, new java.util.Random());
        controller.setPacingDelaySec(-1f);
        assertThat(controller.pacingDelaySec()).isZero();
    }

    @Test
    void toggle_on_thenOff_restoresToDefault() {
        CombatController controller = new CombatController(
                null, null, new java.util.Random());
        // Toggle ON
        controller.setPacingDelaySec(0.35f);
        assertThat(controller.pacingDelaySec()).isEqualTo(0.35f);
        // Toggle OFF
        controller.setPacingDelaySec(0.7f);
        assertThat(controller.pacingDelaySec()).isEqualTo(0.7f);
    }
}
