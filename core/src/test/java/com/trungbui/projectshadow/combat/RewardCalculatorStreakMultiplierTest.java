package com.trungbui.projectshadow.combat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Sprint 13 B2 — streak gold multiplier formula in {@link CombatRewardRoller}.
 */
class RewardCalculatorStreakMultiplierTest {

    @Test
    void streak0_multiplier_is1x() {
        assertThat(CombatRewardRoller.streakMultiplier(0)).isCloseTo(1.0, within(0.001));
    }

    @Test
    void streak1_multiplier_is1p1() {
        assertThat(CombatRewardRoller.streakMultiplier(1)).isCloseTo(1.1, within(0.001));
    }

    @Test
    void streak3_multiplier_is1p3() {
        assertThat(CombatRewardRoller.streakMultiplier(3)).isCloseTo(1.3, within(0.001));
    }

    @Test
    void streak5_multiplier_is1p5_cap() {
        assertThat(CombatRewardRoller.streakMultiplier(5)).isCloseTo(1.5, within(0.001));
    }

    @Test
    void streak10_multiplier_capped_at1p5() {
        assertThat(CombatRewardRoller.streakMultiplier(10)).isCloseTo(1.5, within(0.001));
    }

    @Test
    void streak100_multiplier_capped_at1p5() {
        assertThat(CombatRewardRoller.streakMultiplier(100)).isCloseTo(1.5, within(0.001));
    }

    @Test
    void negativeStreak_treatedAs0() {
        assertThat(CombatRewardRoller.streakMultiplier(-3)).isCloseTo(1.0, within(0.001));
    }
}
