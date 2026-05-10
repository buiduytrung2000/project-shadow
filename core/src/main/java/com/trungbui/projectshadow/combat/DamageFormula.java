package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.model.SkillData;
import com.trungbui.projectshadow.domain.Combatant;

import java.util.random.RandomGenerator;

public final class DamageFormula {

    public static final double CRIT_MULTIPLIER = 1.5;

    private DamageFormula() {
    }

    public static AttackResult resolve(
            Combatant attacker,
            Combatant target,
            SkillData skill,
            RandomGenerator rng
    ) {
        int hitChance = clamp(
                attacker.effectiveAccuracy() + skill.accuracyModifier() - target.effectiveDodge(),
                0, 100
        );
        boolean hit = rng.nextInt(100) < hitChance;
        if (!hit) return AttackResult.miss();

        int rolled = rollInclusive(rng, attacker.effectiveDmgMin(), attacker.effectiveDmgMax());
        double base = rolled * skill.damageMultiplier();

        boolean crit = rng.nextDouble() < attacker.effectiveCritChance();
        double finalDmg = (crit ? base * CRIT_MULTIPLIER : base) * target.damageReceivedMultiplier();

        int hpDamage = (int) Math.round(finalDmg);
        return new AttackResult(true, crit, hpDamage, skill.stressDamage());
    }

    private static int rollInclusive(RandomGenerator rng, int min, int max) {
        if (max < min) return min;
        return min + rng.nextInt(max - min + 1);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
