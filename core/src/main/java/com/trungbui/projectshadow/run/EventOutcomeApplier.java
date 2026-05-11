package com.trungbui.projectshadow.run;

import com.trungbui.projectshadow.data.model.EventChoice;
import com.trungbui.projectshadow.data.model.EventOutcome;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.save.RunState;

import java.util.List;
import java.util.Locale;
import java.util.random.RandomGenerator;

/**
 * Sprint 10 B3 — applies the outcomes of an {@link EventChoice} to a
 * {@link RunSession}. Each outcome rolls its {@code chance} independently;
 * if it passes, the effect is dispatched by {@code type}.
 *
 * <p>Supported outcome types (from {@code events.csv}):</p>
 * <ul>
 *   <li>{@code gold} — value: int (single or "min-max" range). Adds to run gold.</li>
 *   <li>{@code stress} — value: int (single or "min-max" range), target:
 *       {@code party} | {@code random_hero}. Increases stress.</li>
 *   <li>{@code damage} — value: int (single or "min-max" range), target:
 *       {@code random_hero} | {@code party}. Reduces HP.</li>
 *   <li>{@code skill_cd_reset} — target: {@code random_hero}. Value {@code all} clears all skill cooldowns.</li>
 *   <li>{@code trait_apply} — target: {@code random_hero}. Value: trait_id (e.g. {@code trait_08}).</li>
 *   <li>{@code disease} — target: {@code random_hero}. Value: disease_id.</li>
 *   <li>{@code item} — Value: item_id. Adds to run inventory.</li>
 *   <li>{@code none} — no-op (used as fallback in chance distributions).</li>
 * </ul>
 *
 * <p>Unknown types are silently skipped (forward-compat for designer adding
 * future outcome kinds).</p>
 */
public final class EventOutcomeApplier {

    private EventOutcomeApplier() {
    }

    /** Apply all outcomes of a single choice to the given run session.
     *  Each outcome rolls its chance independently. Returns a summary of what
     *  was applied (used by the UI for player feedback). */
    public static AppliedSummary apply(EventChoice choice, RunSession runSession, RandomGenerator rng) {
        AppliedSummary summary = new AppliedSummary();
        if (choice == null) return summary;
        for (EventOutcome outcome : choice.outcomes()) {
            if (rng.nextDouble() > outcome.chance()) continue; // failed roll
            applyOne(outcome, runSession, rng, summary);
        }
        return summary;
    }

    private static void applyOne(EventOutcome o, RunSession run, RandomGenerator rng, AppliedSummary out) {
        String type = o.type() == null ? "" : o.type().toLowerCase(Locale.ROOT);
        switch (type) {
            case "gold" -> {
                int amount = parseValueOrRange(o.value(), rng);
                if (amount != 0) {
                    try {
                        run.applyCombatReward(new com.trungbui.projectshadow.combat.CombatReward(
                                amount, List.of(), null, 0));
                        out.goldDelta += amount;
                    } catch (IllegalStateException ignored) {
                        // overdraw protection; gold can't go negative
                    }
                }
            }
            case "stress" -> {
                int amount = parseValueOrRange(o.value(), rng);
                if (amount <= 0) return;
                if ("party".equalsIgnoreCase(o.target())) {
                    for (Hero h : run.party()) {
                        if (h.isAlive()) h.takeStressDamage(amount);
                    }
                    out.partyStressAdded += amount;
                } else { // random_hero or default
                    Hero h = pickAliveRandom(run, rng);
                    if (h != null) {
                        h.takeStressDamage(amount);
                        out.singleHeroStressAdded += amount;
                    }
                }
            }
            case "damage" -> {
                int amount = parseValueOrRange(o.value(), rng);
                if (amount <= 0) return;
                if ("party".equalsIgnoreCase(o.target())) {
                    for (Hero h : run.party()) {
                        if (h.isAlive()) h.takeHpDamage(amount);
                    }
                    out.partyDamageDealt += amount;
                } else {
                    Hero h = pickAliveRandom(run, rng);
                    if (h != null) {
                        h.takeHpDamage(amount);
                        out.singleHeroDamageDealt += amount;
                    }
                }
            }
            case "skill_cd_reset" -> {
                Hero h = pickAliveRandom(run, rng);
                if (h != null) {
                    // Reset all skill cooldowns on the target. Today
                    // tickCooldowns() decrements by 1; we want zero.
                    for (String skillId : h.equippedSkills()) {
                        // Tick repeatedly until zero — simple and bounded by typical 3-5 turn CD.
                        for (int i = 0; i < 10 && h.isOnCooldown(skillId); i++) h.tickCooldowns();
                    }
                    out.skillCdReset = true;
                }
            }
            case "trait_apply" -> {
                Hero h = pickAliveRandom(run, rng);
                if (h != null && o.value() != null && !o.value().isBlank()) {
                    h.addTrait(o.value().trim());
                    out.traitsApplied.add(o.value().trim());
                }
            }
            case "disease" -> {
                Hero h = pickAliveRandom(run, rng);
                if (h != null && o.value() != null && !o.value().isBlank()) {
                    h.addDisease(o.value().trim());
                    out.diseasesApplied.add(o.value().trim());
                }
            }
            case "item" -> {
                if (o.value() != null && !o.value().isBlank()) {
                    run.applyCombatReward(new com.trungbui.projectshadow.combat.CombatReward(
                            0, List.of(o.value().trim()), null, 0));
                    out.itemsAdded.add(o.value().trim());
                }
            }
            case "none" -> { /* explicit no-op */ }
            default -> {
                // Forward-compat: log+skip unknown types (no logger wired today).
            }
        }
    }

    /** Parse {@code "N"} or {@code "min-max"} as an int. Range returns uniform random
     *  in [min, max]. Returns 0 if blank/unparseable. */
    static int parseValueOrRange(String raw, RandomGenerator rng) {
        if (raw == null || raw.isBlank()) return 0;
        String s = raw.trim();
        int dash = s.indexOf('-');
        // Be careful: range "8-15" has dash at index >0 not at 0. A leading "-" is
        // a negative number, not a range.
        if (dash > 0) {
            try {
                int lo = Integer.parseInt(s.substring(0, dash).trim());
                int hi = Integer.parseInt(s.substring(dash + 1).trim());
                int min = Math.min(lo, hi);
                int max = Math.max(lo, hi);
                return min + rng.nextInt(max - min + 1);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static Hero pickAliveRandom(RunSession run, RandomGenerator rng) {
        List<Hero> alive = run.party().stream().filter(Hero::isAlive).toList();
        if (alive.isEmpty()) return null;
        return alive.get(rng.nextInt(alive.size()));
    }

    /** Summary of what an apply pass actually did. Used by UI for feedback line. */
    public static final class AppliedSummary {
        public int goldDelta = 0;
        public int partyStressAdded = 0;
        public int singleHeroStressAdded = 0;
        public int partyDamageDealt = 0;
        public int singleHeroDamageDealt = 0;
        public boolean skillCdReset = false;
        public final java.util.List<String> traitsApplied = new java.util.ArrayList<>();
        public final java.util.List<String> diseasesApplied = new java.util.ArrayList<>();
        public final java.util.List<String> itemsAdded = new java.util.ArrayList<>();
    }
}
