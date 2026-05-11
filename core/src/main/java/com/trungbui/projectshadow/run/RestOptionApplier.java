package com.trungbui.projectshadow.run;

import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.stage.RestOption;

import java.util.List;
import java.util.Locale;
import java.util.random.RandomGenerator;

/**
 * Sprint 10 B3 — applies a {@link RestOption} choice to a {@link RunSession}.
 *
 * <p>Supported effect types (per Sprint 10 design lock 2026-05-11):</p>
 * <ul>
 *   <li>{@code heal} — restore HP. target: {@code random_hero} | {@code party}.
 *       value: amount (uniform roll between {@code valueMin} and {@code valueMax}).</li>
 *   <li>{@code reduce_stress} — decrease stress. target: same options.</li>
 *   <li>{@code buff} — placeholder for status buffs (Sprint 11 will wire to
 *       ActiveEffects); today logs but no-ops.</li>
 *   <li>{@code remove_disease} — strip a random disease from a random alive
 *       hero. (Sprint 10 new effect type.)</li>
 *   <li>{@code skill_swap} — placeholder for hero-picks-skill UI flow.
 *       Today no-ops with a flag; UI may extend later. (Sprint 10 new effect.)</li>
 * </ul>
 */
public final class RestOptionApplier {

    private RestOptionApplier() {
    }

    /** Apply a single rest option to the run. Returns a brief summary. */
    public static AppliedSummary apply(RestOption option, RunSession run, RandomGenerator rng) {
        AppliedSummary out = new AppliedSummary();
        if (option == null || option.effect() == null) return out;
        String effect = option.effect().toLowerCase(Locale.ROOT);
        int amount = rollAmount(option.valueMin(), option.valueMax(), rng);
        switch (effect) {
            case "heal" -> applyHeal(option, run, amount, rng, out);
            case "reduce_stress" -> applyStressRelief(option, run, amount, rng, out);
            case "remove_disease" -> applyRemoveDisease(run, rng, out);
            case "buff" -> {
                // Sprint 11 follow-up: wire via ActiveEffects.apply once a buff
                // effect_id mapping is in RestOption schema. Today: no-op + flag.
                out.buffSkipped = true;
            }
            case "skill_swap" -> {
                // UI flow not landed; flag for caller to optionally prompt.
                out.skillSwapNeedsUi = true;
            }
            default -> {
                // Unknown effect — forward-compat, skip silently.
            }
        }
        return out;
    }

    private static void applyHeal(RestOption opt, RunSession run, int amount,
                                  RandomGenerator rng, AppliedSummary out) {
        if (amount <= 0) return;
        if ("party".equalsIgnoreCase(opt.target())) {
            for (Hero h : run.party()) {
                if (h.isAlive()) {
                    h.heal(amount);
                    out.totalHealed += amount;
                }
            }
        } else {
            Hero h = pickAliveRandom(run, rng);
            if (h != null) {
                h.heal(amount);
                out.totalHealed += amount;
                out.targetedHeroId = h.id();
            }
        }
    }

    private static void applyStressRelief(RestOption opt, RunSession run, int amount,
                                          RandomGenerator rng, AppliedSummary out) {
        if (amount <= 0) return;
        if ("party".equalsIgnoreCase(opt.target())) {
            for (Hero h : run.party()) {
                if (h.isAlive()) {
                    h.reduceStress(amount);
                    out.totalStressReduced += amount;
                }
            }
        } else {
            Hero h = pickAliveRandom(run, rng);
            if (h != null) {
                h.reduceStress(amount);
                out.totalStressReduced += amount;
                out.targetedHeroId = h.id();
            }
        }
    }

    private static void applyRemoveDisease(RunSession run, RandomGenerator rng, AppliedSummary out) {
        // Pick a random alive hero who has at least one disease, then remove a
        // random disease from that hero.
        List<Hero> diseased = run.party().stream()
                .filter(Hero::isAlive)
                .filter(h -> !h.diseases().isEmpty())
                .toList();
        if (diseased.isEmpty()) {
            // No-op — there's nothing to cure. Flag so UI can show "Không có
            // bệnh nào để chữa" instead of pretending success.
            out.removeDiseaseNoOp = true;
            return;
        }
        Hero target = diseased.get(rng.nextInt(diseased.size()));
        List<String> diseases = List.copyOf(target.diseases());
        String picked = diseases.get(rng.nextInt(diseases.size()));
        target.removeDisease(picked);
        out.diseaseRemovedFrom = target.id();
        out.diseaseRemovedId = picked;
    }

    private static int rollAmount(int min, int max, RandomGenerator rng) {
        if (max < min) max = min;
        if (max == min) return min;
        return min + rng.nextInt(max - min + 1);
    }

    private static Hero pickAliveRandom(RunSession run, RandomGenerator rng) {
        List<Hero> alive = run.party().stream().filter(Hero::isAlive).toList();
        if (alive.isEmpty()) return null;
        return alive.get(rng.nextInt(alive.size()));
    }

    /** What an apply() pass actually did, for UI feedback. */
    public static final class AppliedSummary {
        public int totalHealed = 0;
        public int totalStressReduced = 0;
        public String targetedHeroId = null;
        public String diseaseRemovedFrom = null;
        public String diseaseRemovedId = null;
        public boolean removeDiseaseNoOp = false;
        public boolean buffSkipped = false;
        public boolean skillSwapNeedsUi = false;
    }
}
