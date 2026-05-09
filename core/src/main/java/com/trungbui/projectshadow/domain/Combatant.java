package com.trungbui.projectshadow.domain;

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
}
