package com.trungbui.projectshadow.combat;

public record AttackResult(
        boolean hit,
        boolean crit,
        int hpDamage,
        int stressDamage
) {
    public static AttackResult miss() {
        return new AttackResult(false, false, 0, 0);
    }

    public boolean isMiss() {
        return !hit;
    }
}
