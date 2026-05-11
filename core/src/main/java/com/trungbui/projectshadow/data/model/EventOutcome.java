package com.trungbui.projectshadow.data.model;

/**
 * Sprint 10 B3 — a single rollable outcome within an {@link EventChoice}.
 *
 * <p>Parsed from CSV DSL like: {@code "type=gold|value=300|chance=0.5"}.</p>
 *
 * @param type Effect type: {@code gold}, {@code damage}, {@code stress},
 *             {@code skill_cd_reset}, {@code trait_apply}, {@code disease},
 *             {@code item}, {@code none}, etc.
 * @param target {@code random_hero}, {@code party}, {@code self}, or null
 *               for outcomes without a target (e.g. {@code gold}).
 * @param value Raw value string. Numeric for gold/stress/damage (single
 *              "300" or range "8-15"); ID for trait_apply ("trait_08") /
 *              disease / item; "all" for skill_cd_reset → reset all skills.
 * @param chance Probability roll [0.0, 1.0]. {@code 1.0} = always fires.
 */
public record EventOutcome(String type, String target, String value, double chance) {
    public EventOutcome {
        if (chance < 0d) chance = 0d;
        if (chance > 1d) chance = 1d;
    }
}
