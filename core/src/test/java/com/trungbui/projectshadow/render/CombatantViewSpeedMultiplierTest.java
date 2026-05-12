package com.trungbui.projectshadow.render;

import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.Position;
import com.trungbui.projectshadow.effect.ActiveEffects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 13 B3 — verify that {@link CombatantView#globalSpeedMultiplier} scales
 * animation timers so x2 mode advances animations twice as fast.
 */
class CombatantViewSpeedMultiplierTest {

    /** Minimal Combatant for tests. */
    private static final class TestCombatant implements Combatant {
        private int hp = 10;
        private final ActiveEffects effects = new ActiveEffects();
        @Override public String id() { return "test"; }
        @Override public int currentHp() { return hp; }
        @Override public int maxHp() { return 10; }
        @Override public void setCurrentHp(int v) { hp = v; }
        @Override public Position position() { return Position.POS_1; }
        @Override public void setPosition(Position pos) {}
        @Override public int speed() { return 5; }
        @Override public int accuracy() { return 80; }
        @Override public int dodge() { return 0; }
        @Override public double critChance() { return 0; }
        @Override public int dmgMin() { return 1; }
        @Override public int dmgMax() { return 2; }
        @Override public ActiveEffects activeEffects() { return effects; }
    }

    @BeforeEach
    void resetMultiplier() {
        CombatantView.globalSpeedMultiplier = 1f;
    }

    @AfterEach
    void restoreMultiplier() {
        CombatantView.globalSpeedMultiplier = 1f;
    }

    @Test
    void defaultMultiplier_isOne() {
        assertThat(CombatantView.globalSpeedMultiplier).isEqualTo(1f);
    }

    @Test
    void speedMultiplier2x_flashTimerExpiresInHalfTime() {
        // At 1x speed, flash (0.2s) expires after 0.2s of real delta.
        // At 2x speed, it should expire after 0.1s of real delta.
        TestCombatant c = new TestCombatant();
        CombatantView view = new CombatantView(c, 0f, 0f);
        view.triggerHit(); // sets flashTimer = 0.2s, shakeTimer = 0.4s

        CombatantView.globalSpeedMultiplier = 2f;

        // After 0.05s real delta at 2x = 0.10s effective → flash still active
        view.update(0.05f);
        assertThat(view.isFlashing()).isTrue();

        // After another 0.06s real = 0.12s effective → total 0.22s effective > 0.2s → expired
        view.update(0.06f);
        assertThat(view.isFlashing()).isFalse();
    }

    @Test
    void speedMultiplier1x_flashTimerExpiresAtNormalTime() {
        TestCombatant c = new TestCombatant();
        CombatantView view = new CombatantView(c, 0f, 0f);
        view.triggerHit(); // flashTimer = 0.2s

        CombatantView.globalSpeedMultiplier = 1f;

        // 0.15s elapsed at 1x — still flashing
        view.update(0.15f);
        assertThat(view.isFlashing()).isTrue();

        // 0.06s more = 0.21s total > 0.2s → expired
        view.update(0.06f);
        assertThat(view.isFlashing()).isFalse();
    }

    @Test
    void speedMultiplier2x_setAndReset() {
        CombatantView.globalSpeedMultiplier = 2f;
        assertThat(CombatantView.globalSpeedMultiplier).isEqualTo(2f);
        CombatantView.globalSpeedMultiplier = 1f;
        assertThat(CombatantView.globalSpeedMultiplier).isEqualTo(1f);
    }

    @Test
    void update_withoutAnimator_doesNotThrow() {
        TestCombatant c = new TestCombatant();
        CombatantView view = new CombatantView(c, 0f, 0f);
        CombatantView.globalSpeedMultiplier = 2f;
        // Should not throw
        view.update(0.016f);
    }
}
