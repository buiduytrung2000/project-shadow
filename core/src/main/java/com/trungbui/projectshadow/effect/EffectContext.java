package com.trungbui.projectshadow.effect;

import com.trungbui.projectshadow.domain.Combatant;

import java.util.random.RandomGenerator;

public record EffectContext(Combatant source, Combatant target, RandomGenerator rng) {

    public static EffectContext of(Combatant source, Combatant target, RandomGenerator rng) {
        return new EffectContext(source, target, rng);
    }
}
