package com.trungbui.projectshadow.render;

import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.Position;
import com.trungbui.projectshadow.effect.ActiveEffects;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the animation triggering behaviour added in bugfix/combat-animations:
 * <ul>
 *   <li>{@code triggerAttack()} switches the animator to {@code ATTACKING} state.</li>
 *   <li>An alive→dead transition automatically switches to {@code DEAD} on the next
 *       {@link CombatantView#update(float)} tick.</li>
 *   <li>The DEAD state is set exactly once (no repeated re-triggers).</li>
 * </ul>
 */
class CombatantViewTest {

    /** Minimal Combatant that lets the test control HP directly. */
    private static final class TestCombatant implements Combatant {
        private int hp = 10;
        private final ActiveEffects effects = new ActiveEffects();
        @Override public String id() { return "test_unit"; }
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

    @Test
    void triggerAttack_setsAttackingState() {
        TestCombatant c = new TestCombatant();
        CombatantView view = new CombatantView(c, 0f, 0f);
        SpriteAnimator animator = new SpriteAnimator(new SpriteAtlas(), c.id());
        view.attachAnimator(animator);

        view.triggerAttack();

        assertThat(animator.currentState()).isEqualTo(SpriteAnimator.State.ATTACKING);
    }

    @Test
    void aliveToDeadTransition_triggersDeadStateOnNextUpdate() {
        TestCombatant c = new TestCombatant();
        CombatantView view = new CombatantView(c, 0f, 0f);
        SpriteAnimator animator = new SpriteAnimator(new SpriteAtlas(), c.id());
        view.attachAnimator(animator);

        // Initial state is IDLE
        assertThat(animator.currentState()).isEqualTo(SpriteAnimator.State.IDLE);

        // Kill the combatant
        c.setCurrentHp(0);
        assertThat(c.isAlive()).isFalse();

        // Tick → CombatantView detects alive→dead transition and sets DEAD
        view.update(0.016f);

        assertThat(animator.currentState()).isEqualTo(SpriteAnimator.State.DEAD);
    }

    @Test
    void deadState_notReTriggeredOnSubsequentUpdates() {
        TestCombatant c = new TestCombatant();
        CombatantView view = new CombatantView(c, 0f, 0f);
        SpriteAnimator animator = new SpriteAnimator(new SpriteAtlas(), c.id());
        view.attachAnimator(animator);

        c.setCurrentHp(0);
        view.update(0.016f);
        // Manually move to a different state to ensure the next updates don't re-set DEAD
        animator.setState(SpriteAnimator.State.IDLE);

        view.update(0.016f);
        view.update(1.0f);

        // wasAlive should already be false after the first transition; further updates
        // must not re-trigger DEAD (otherwise we'd be stuck in DEAD forever).
        assertThat(animator.currentState()).isEqualTo(SpriteAnimator.State.IDLE);
    }

    @Test
    void aliveCombatant_keepsIdleState() {
        TestCombatant c = new TestCombatant();
        CombatantView view = new CombatantView(c, 0f, 0f);
        SpriteAnimator animator = new SpriteAnimator(new SpriteAtlas(), c.id());
        view.attachAnimator(animator);

        view.update(0.016f);
        view.update(0.5f);

        assertThat(animator.currentState()).isEqualTo(SpriteAnimator.State.IDLE);
    }

    @Test
    void update_doesNotCrashWithoutAnimator() {
        TestCombatant c = new TestCombatant();
        CombatantView view = new CombatantView(c, 0f, 0f);
        // No animator attached → null-safe

        c.setCurrentHp(0);
        view.update(0.016f); // must not throw
        view.update(1.0f);
    }
}
