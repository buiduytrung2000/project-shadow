package com.trungbui.projectshadow.domain;

import com.trungbui.projectshadow.effect.ActiveEffects;
import com.trungbui.projectshadow.effect.StatType;

public interface Combatant {

    String id();

    int currentHp();

    int maxHp();

    void setCurrentHp(int hp);

    Position position();

    void setPosition(Position pos);

    int speed();

    int accuracy();

    int dodge();

    double critChance();

    int dmgMin();

    int dmgMax();

    ActiveEffects activeEffects();

    default boolean isAlive() {
        return currentHp() > 0;
    }

    default void takeHpDamage(int amount) {
        if (amount <= 0) return;
        setCurrentHp(Math.max(0, currentHp() - amount));
    }

    default void heal(int amount) {
        if (amount <= 0) return;
        setCurrentHp(Math.min(maxHp(), currentHp() + amount));
    }

    default boolean isOnCooldown(String skillId) {
        return false;
    }

    default void putOnCooldown(String skillId, int turns) {
    }

    default void tickCooldowns() {
    }

    default int effectiveAccuracy() {
        // Sprint 13 B3: stage_2 env mod applies global accuracy penalty.
        return accuracy() + activeEffects().sumFlatModifier(StatType.ACCURACY)
                + com.trungbui.projectshadow.combat.ConditionResolver.stageAccuracyMod;
    }

    default int effectiveDodge() {
        int flat = activeEffects().sumFlatModifier(StatType.DODGE);
        double pct = activeEffects().sumPercentModifier(StatType.DODGE);
        return dodge() + flat + (int) Math.round(100 * pct);
    }

    default double effectiveCritChance() {
        return critChance() + activeEffects().sumPercentModifier(StatType.CRIT);
    }

    default int effectiveSpeed() {
        return speed() + activeEffects().sumFlatModifier(StatType.SPEED);
    }

    default int effectiveDmgMin() {
        double pct = activeEffects().sumPercentModifier(StatType.DAMAGE);
        return (int) Math.round(dmgMin() * (1d + pct));
    }

    default int effectiveDmgMax() {
        double pct = activeEffects().sumPercentModifier(StatType.DAMAGE);
        return (int) Math.round(dmgMax() * (1d + pct));
    }

    default double damageReceivedMultiplier() {
        return 1d + activeEffects().sumPercentModifier(StatType.DAMAGE_RECEIVED);
    }

    // ── Sprint 13 B1: absorb shield ──────────────────────────────────────────

    /** Current shield HP. Absorbs incoming damage before HP is reduced. */
    default int shieldHp() { return 0; }

    /** Set the shield HP. Implementations that support shields override this. */
    default void setShieldHp(int shield) {}

    /**
     * Take damage, consuming shield before HP. Returns the actual HP damage dealt
     * after shield absorption (may be 0 if shield fully absorbed).
     */
    default int takeDamageWithShield(int amount) {
        if (amount <= 0) return 0;
        int shield = shieldHp();
        if (shield > 0) {
            int absorbed = Math.min(shield, amount);
            setShieldHp(shield - absorbed);
            amount -= absorbed;
        }
        if (amount > 0) takeHpDamage(amount);
        return amount;
    }
}
