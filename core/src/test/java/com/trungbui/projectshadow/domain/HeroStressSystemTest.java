package com.trungbui.projectshadow.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 9+ B2 stress system rework. Verifies:
 * <ul>
 *   <li>Crossing {@link Hero#AFFLICTION_THRESHOLD} (100) for the first time latches
 *       {@code pendingAfflictionRoll} until consumed.</li>
 *   <li>Re-crossing 100 (after dropping below + climbing again) does NOT re-roll
 *       because {@code afflictionResolved} stays latched for the rest of the run.</li>
 *   <li>Hitting {@link Hero#HEART_ATTACK_THRESHOLD} (200) instantly kills the hero
 *       (HP forced to 0).</li>
 *   <li>Stress clamps to {@code [0, STRESS_MAX]}.</li>
 * </ul>
 */
class HeroStressSystemTest {

    private static Hero freshHero() {
        // Use 0 stressResist so reductions don't soften the test numbers.
        var data = new com.trungbui.projectshadow.data.model.HeroData(
                "Common", "Test", "Test", "h1",
                "DPS", "Front",
                30, 5, 10, 80, 0.05, 5, 0.0, /* baseStressResist */
                3, 1, 0.01, 0.02,
                java.util.List.of("sk_test"), java.util.List.of("sk_test"),
                ""
        );
        return new Hero(data, 0, Position.POS_1, java.util.List.of("sk_test"));
    }

    @Test
    void crossing100_latchesPendingAfflictionRoll() {
        Hero h = freshHero();
        h.takeStressDamage(120); // crosses 100
        assertThat(h.consumePendingAfflictionRoll()).isTrue();
        // Consumed: subsequent reads must return false.
        assertThat(h.consumePendingAfflictionRoll()).isFalse();
        assertThat(h.afflictionResolved()).isTrue();
    }

    @Test
    void crossing100_doesNotLatchIfAlreadyResolved() {
        Hero h = freshHero();
        h.takeStressDamage(120);
        h.consumePendingAfflictionRoll(); // latches afflictionResolved
        h.reduceStress(80); // back below threshold
        h.takeStressDamage(120); // cross again
        assertThat(h.consumePendingAfflictionRoll()).isFalse();
    }

    @Test
    void stressAt200_triggersHeartAttack() {
        Hero h = freshHero();
        h.takeStressDamage(250); // overshoots, clamped to 200
        assertThat(h.currentStress()).isEqualTo(Hero.STRESS_MAX);
        assertThat(h.isHeartAttacked()).isTrue();
        assertThat(h.currentHp()).isZero();
        assertThat(h.isAlive()).isFalse();
    }

    @Test
    void belowAfflictionThreshold_doesNotLatchAnything() {
        Hero h = freshHero();
        h.takeStressDamage(50);
        assertThat(h.currentStress()).isEqualTo(50);
        assertThat(h.consumePendingAfflictionRoll()).isFalse();
        assertThat(h.afflictionResolved()).isFalse();
        assertThat(h.isHeartAttacked()).isFalse();
    }

    @Test
    void crossingExactly100_isInclusiveTrigger() {
        Hero h = freshHero();
        h.takeStressDamage(100); // exactly at threshold
        assertThat(h.currentStress()).isEqualTo(100);
        assertThat(h.consumePendingAfflictionRoll()).isTrue();
    }

    @Test
    void stressIsClampedToZero() {
        Hero h = freshHero();
        h.reduceStress(50); // already at 0
        assertThat(h.currentStress()).isZero();
    }
}
