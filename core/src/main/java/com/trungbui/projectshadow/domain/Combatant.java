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
        return accuracy() + activeEffects().sumFlatModifier(StatType.ACCURACY);
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
}
