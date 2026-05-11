package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.model.DiseaseTraitData;
import com.trungbui.projectshadow.domain.Hero;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Sprint 9+ B2 — Affliction/Virtue resolution roll.
 *
 * <p>Invoked by {@code CombatController} when a hero crosses
 * {@link Hero#AFFLICTION_THRESHOLD} for the first time. Rolls 70% Affliction /
 * 30% Virtue, then picks a random trait from the corresponding pool, excluding
 * traits the hero already has.</p>
 *
 * <p>The trait's stat effects are data-driven via {@code diseases_traits.csv}
 * but are not yet wired into combat (Sprint 10 follow-up). For now the trait
 * is added to {@link Hero#traits()} so the UI can display it and so a future
 * sprint can resolve the {@code Trigger} column into runtime behavior.</p>
 */
public final class AfflictionResolver {

    /** 70% Affliction / 30% Virtue per design lock (CLAUDE.md 2026-05-11). */
    public static final double AFFLICTION_CHANCE = 0.70;

    private AfflictionResolver() {
    }

    /**
     * Roll an Affliction or Virtue trait for the given hero.
     *
     * @param hero the hero who just crossed stress 100
     * @param traits the full disease/trait catalog (typically {@code gameData.diseasesTraits().values()})
     * @param rng deterministic RNG
     * @return the trait ID chosen (already added to {@code hero}), or {@code null}
     *         if no eligible trait exists in either pool (caller should treat
     *         this as a no-op).
     */
    public static String roll(Hero hero, Collection<DiseaseTraitData> traits, RandomGenerator rng) {
        if (hero == null || traits == null || traits.isEmpty()) return null;
        boolean pickAffliction = rng.nextDouble() < AFFLICTION_CHANCE;
        String picked = pickFromPool(hero, traits, pickAffliction, rng);
        if (picked == null) {
            // Fallback: opposite side if preferred side is exhausted.
            picked = pickFromPool(hero, traits, !pickAffliction, rng);
        }
        if (picked != null) {
            hero.addTrait(picked);
        }
        return picked;
    }

    private static String pickFromPool(
            Hero hero, Collection<DiseaseTraitData> traits, boolean affliction, RandomGenerator rng) {
        List<String> pool = new ArrayList<>();
        for (DiseaseTraitData t : traits) {
            boolean match = affliction ? t.isAffliction() : t.isVirtue();
            if (!match) continue;
            if (hero.traits().contains(t.id())) continue;
            pool.add(t.id());
        }
        if (pool.isEmpty()) return null;
        return pool.get(rng.nextInt(pool.size()));
    }
}
