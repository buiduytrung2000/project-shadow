package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.DiseaseTraitData;
import com.trungbui.projectshadow.data.model.SkillData;
import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.domain.Hero;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Sprint 11 B2 — runtime resolver for hero conditions (traits + diseases).
 *
 * <p>Data-driven dispatch on the {@code Trigger} column of
 * {@code diseases_traits.csv}. Each registered handler reads the hero's
 * traits + diseases and applies its effect when conditions match.</p>
 *
 * <h2>MVP scope (Sprint 11)</h2>
 * Wires 3 trait effects per design lock:
 * <ul>
 *   <li><strong>Cowardly</strong> (trait_02, Affliction) — on-turn-start with
 *       stress &gt; 50: 20% chance hero skips action. Sets {@code Hero.skipNextAction};
 *       {@link CombatController} consumes the flag at action start.</li>
 *   <li><strong>Masochist</strong> (trait_04, Affliction) — on self-damage:
 *       all allies receive +5 stress. (No HP-cost skills exist yet — handler
 *       is wired but won't trigger until Sprint 12+ adds them.)</li>
 *   <li><strong>Bloodthirsty</strong> (trait_07, Affliction reclass) — on
 *       turn-start: forces hero to auto-attack first alive enemy. On kill:
 *       adds a stack (caps at 5). Effective damage gains +5% per stack via
 *       {@link Hero#bloodthirstyStacks()} override.</li>
 * </ul>
 *
 * <p>Other affliction traits (Paranoid/Selfish/Fearful/Hopeless) + all 6
 * diseases defer to Sprint 12. Their CSV rows exist but the runtime is
 * a no-op via the default-pass case in the switch.</p>
 */
public final class ConditionResolver {

    public static final String TRAIT_COWARDLY = "trait_02";
    public static final String TRAIT_MASOCHIST = "trait_04";
    public static final String TRAIT_BLOODTHIRSTY = "trait_07";

    // Sprint 12 B1 — 4 more Affliction traits.
    public static final String TRAIT_PARANOID = "trait_a01";
    public static final String TRAIT_SELFISH = "trait_a02";
    public static final String TRAIT_FEARFUL = "trait_a03";
    public static final String TRAIT_HOPELESS = "trait_a04";

    // Sprint 12 B1 — 6 disease IDs.
    public static final String DISEASE_FEVER = "dis_01";
    public static final String DISEASE_BLINDNESS = "dis_02";
    public static final String DISEASE_NIGHTMARE = "dis_03";
    public static final String DISEASE_PARANOIA = "dis_04";
    public static final String DISEASE_PLAGUE = "dis_05";
    public static final String DISEASE_CURSED = "dis_06";

    public static final double COWARDLY_SKIP_CHANCE = 0.20;
    public static final int COWARDLY_STRESS_THRESHOLD = 50;
    public static final int MASOCHIST_STRESS_TO_ALLIES = 5;

    // Sprint 12 B1 — new effect magnitudes.
    public static final int PARANOID_ALLY_STRESS_PER_TURN = 1;
    public static final double FEARFUL_DMG_MULTIPLIER = 0.80; // -20% outgoing
    public static final int HOPELESS_ACC_DEBUFF = 10;
    public static final double FEVER_MAX_HP_MULT = 0.85;     // -15% max HP
    public static final int BLINDNESS_ACC_DEBUFF = 20;
    public static final int NIGHTMARE_STRESS_PER_TURN = 2;
    public static final double PARANOIA_FAIL_CHANCE = 0.15;
    public static final double PLAGUE_MAX_HP_MULT = 0.75;    // -25% max HP
    public static final double PLAGUE_SPREAD_CHANCE = 0.10;
    public static final int STRESS_ON_CRIT = 5;
    public static final int STRESS_ON_ALLY_DEATH = 10;

    // Sprint 13 B3 — stage environmental modifiers. Set at combat start via
    // onCombatStart(encounter, stageId); cleared (0) between combats.
    public static int stageAccuracyMod = 0;
    public static int stageStressPerTurn = 0;

    /** Stage 2 applies a global -5 accuracy penalty to ALL combatants. */
    public static final String STAGE_2_ID = "stage_2";
    /** Stage 3 applies +1 stress/turn to all heroes. */
    public static final String STAGE_3_ID = "stage_3";
    public static final int STAGE_2_ACCURACY_MOD = -5;
    public static final int STAGE_3_STRESS_PER_TURN = 1;

    private ConditionResolver() {
    }

    /**
     * Called by {@link CombatController} at the start of a hero's turn, before
     * action selection. Resolves "on-turn-start" triggers and sets transient
     * flags on the hero (e.g. {@code skipNextAction}, forced-attack flag).
     *
     * @param hero current actor (must be a Hero — caller pre-filters)
     * @param encounter ambient combat state (for reading party / enemies)
     * @param rng deterministic per-combat RNG
     */
    public static void onTurnStart(Hero hero, CombatEncounter encounter, RandomGenerator rng) {
        if (hero == null || !hero.isAlive()) return;

        // Cowardly: stress > 50 → 20% chance skip turn.
        if (hero.traits().contains(TRAIT_COWARDLY)
                && hero.currentStress() > COWARDLY_STRESS_THRESHOLD
                && rng.nextDouble() < COWARDLY_SKIP_CHANCE) {
            hero.setSkipNextAction(true);
        }

        // Bloodthirsty: forced attack — flag is handled by caller via
        // hasForcedAttack() check in this class. No persistent flag set here.

        // Sprint 12 B1 — Paranoid: all alive allies +1 stress.
        if (hero.traits().contains(TRAIT_PARANOID) && encounter != null) {
            for (Hero ally : encounter.heroes()) {
                if (ally == hero || !ally.isAlive()) continue;
                ally.takeStressDamage(PARANOID_ALLY_STRESS_PER_TURN);
            }
        }

        // Sprint 12 B1 — Nightmare: +2 stress/turn to self.
        if (hero.diseases().contains(DISEASE_NIGHTMARE)) {
            hero.takeStressDamage(NIGHTMARE_STRESS_PER_TURN);
        }

        // Sprint 12 B1 — Plague: 10% chance/turn to spread to a disease-free ally.
        if (hero.diseases().contains(DISEASE_PLAGUE) && encounter != null
                && rng.nextDouble() < PLAGUE_SPREAD_CHANCE) {
            java.util.List<Hero> candidates = new java.util.ArrayList<>();
            for (Hero ally : encounter.heroes()) {
                if (ally == hero || !ally.isAlive()) continue;
                if (!ally.diseases().contains(DISEASE_PLAGUE)) candidates.add(ally);
            }
            if (!candidates.isEmpty()) {
                Hero infected = candidates.get(rng.nextInt(candidates.size()));
                infected.addDisease(DISEASE_PLAGUE);
            }
        }

        // Sprint 13 B3 — Stage 3 env modifier: +1 stress/turn to all heroes.
        if (stageStressPerTurn > 0) {
            hero.takeStressDamage(stageStressPerTurn);
        }
    }

    /**
     * Called when a hero takes self-damage (e.g. HP-cost skills). Currently no
     * HP-cost skills exist in skills.csv, so this is a hook for Sprint 12+.
     *
     * @param self the hero who took the self-damage
     * @param encounter combat state (for stress propagation to allies)
     */
    public static void onSelfDamage(Hero self, CombatEncounter encounter) {
        if (self == null || encounter == null) return;
        if (!self.traits().contains(TRAIT_MASOCHIST)) return;
        for (Hero ally : encounter.heroes()) {
            if (ally == self) continue;
            if (ally.isAlive()) ally.takeStressDamage(MASOCHIST_STRESS_TO_ALLIES);
        }
    }

    /**
     * Called when a hero kills a target (HP reduced to 0 by hero's action).
     * Bloodthirsty: adds a stack (caps at 5).
     *
     * @param killer hero who landed the killing blow
     */
    public static void onKill(Hero killer) {
        if (killer == null) return;
        if (killer.traits().contains(TRAIT_BLOODTHIRSTY)) {
            killer.addBloodthirstyStack();
        }
    }

    /**
     * Called at the start of each combat — resets per-combat counters
     * (Bloodthirsty stacks, Cowardly flag, shields) and applies stage
     * environmental modifiers.
     *
     * @param encounter the combat encounter
     */
    public static void onCombatStart(CombatEncounter encounter) {
        onCombatStart(encounter, null);
    }

    /**
     * Sprint 13 B1/B3 — overload that also applies stage environmental modifiers.
     * Stage 2: -5 accuracy (global, all combatants). Stage 3: +1 stress/turn to heroes.
     * Null stageId → no env modifier applied (e.g. test combats without a stage).
     */
    public static void onCombatStart(CombatEncounter encounter, String stageId) {
        if (encounter == null) return;
        for (Hero h : encounter.heroes()) {
            h.resetBloodthirstyStacks();
            h.setSkipNextAction(false);
            // Sprint 13 B1: reset absorb shield at combat start.
            h.setShieldHp(0);
        }
        for (com.trungbui.projectshadow.domain.Enemy e : encounter.enemies()) {
            // Sprint 13 B1: reset enemy shields too.
            e.setShieldHp(0);
        }
        // Sprint 13 B3 — apply stage env modifier accuracy debuff.
        applyStageEnvModifier(encounter, stageId);
    }

    /**
     * Sprint 13 B3 — apply stage environmental modifiers for the given stageId.
     * Currently two stages are handled:
     * <ul>
     *   <li>stage_2: global -5 accuracy via {@link #stageAccuracyMod}</li>
     *   <li>stage_3: +1 stress/turn to heroes via {@link #stageStressPerTurn}</li>
     * </ul>
     * Calling with null or an unrecognized stageId resets both mods to 0.
     */
    static void applyStageEnvModifier(CombatEncounter encounter, String stageId) {
        stageAccuracyMod = 0;
        stageStressPerTurn = 0;
        if (STAGE_2_ID.equals(stageId)) {
            stageAccuracyMod = STAGE_2_ACCURACY_MOD;
        } else if (STAGE_3_ID.equals(stageId)) {
            stageStressPerTurn = STAGE_3_STRESS_PER_TURN;
        }
    }

    /**
     * True if the hero must auto-attack this turn (Bloodthirsty Affliction).
     * Caller (CombatController / CombatScreen) checks this before showing
     * the skill picker UI; if true, picks the first offensive skill targeting
     * the first alive enemy and executes immediately.
     */
    public static boolean hasForcedAttack(Hero hero) {
        return hero != null && hero.traits().contains(TRAIT_BLOODTHIRSTY);
    }

    /**
     * Pick the skill index to use for a forced-attack hero. Returns the first
     * offensive skill the hero has equipped, or -1 if none available (all
     * skills are non-offensive / on cooldown). Caller handles -1 by skipping turn.
     */
    public static int pickForcedAttackSkill(Hero hero, GameData gd) {
        if (hero == null || gd == null) return -1;
        List<String> equipped = hero.equippedSkills();
        for (int i = 0; i < equipped.size(); i++) {
            SkillData s = gd.skills().get(equipped.get(i));
            if (s == null) continue;
            if (!s.isOffensive()) continue;
            if (hero.isOnCooldown(s.skillId())) continue;
            return i;
        }
        return -1;
    }

    /**
     * For Sprint 12+ ConditionResolver scope expansion. Returns whether a
     * trait/disease is recognized by the current resolver runtime. Other
     * traits / all diseases in CSV are data-only until wired.
     */
    public static boolean isImplemented(String conditionId) {
        return TRAIT_COWARDLY.equals(conditionId)
                || TRAIT_MASOCHIST.equals(conditionId)
                || TRAIT_BLOODTHIRSTY.equals(conditionId)
                || TRAIT_PARANOID.equals(conditionId)
                || TRAIT_SELFISH.equals(conditionId)
                || TRAIT_FEARFUL.equals(conditionId)
                || TRAIT_HOPELESS.equals(conditionId)
                || DISEASE_FEVER.equals(conditionId)
                || DISEASE_BLINDNESS.equals(conditionId)
                || DISEASE_NIGHTMARE.equals(conditionId)
                || DISEASE_PARANOIA.equals(conditionId)
                || DISEASE_PLAGUE.equals(conditionId)
                || DISEASE_CURSED.equals(conditionId);
    }

    /** Sprint 12 B1 — Fearful outgoing damage multiplier. Called by
     *  {@link com.trungbui.projectshadow.combat.DamageFormula#resolve}
     *  after computing base damage. Returns multiplier (1.0 = no change). */
    public static double outgoingDamageMultiplier(Hero attacker) {
        if (attacker == null) return 1d;
        if (attacker.traits().contains(TRAIT_FEARFUL)) return FEARFUL_DMG_MULTIPLIER;
        return 1d;
    }

    /** Sprint 12 B1 — Hopeless + Blindness accuracy debuff. Returns total
     *  amount to subtract from base accuracy (>= 0). */
    public static int accuracyDebuff(Hero hero) {
        if (hero == null) return 0;
        int debuff = 0;
        if (hero.traits().contains(TRAIT_HOPELESS)) debuff += HOPELESS_ACC_DEBUFF;
        if (hero.diseases().contains(DISEASE_BLINDNESS)) debuff += BLINDNESS_ACC_DEBUFF;
        return debuff;
    }

    /** Sprint 12 B1 — Fever + Plague max-HP multiplier. Multiplicative if
     *  hero has both. Returns 1.0 if no disease applies. */
    public static double maxHpMultiplier(Hero hero) {
        if (hero == null) return 1d;
        double mult = 1d;
        if (hero.diseases().contains(DISEASE_FEVER)) mult *= FEVER_MAX_HP_MULT;
        if (hero.diseases().contains(DISEASE_PLAGUE)) mult *= PLAGUE_MAX_HP_MULT;
        return mult;
    }

    /** Sprint 12 B1 — Paranoia action-fail roll. Called by
     *  {@code CombatController.executePlayerSkill} BEFORE resolving the skill.
     *  Returns true if the action should fail (skip turn). */
    public static boolean rollParanoiaActionFail(Hero hero, RandomGenerator rng) {
        if (hero == null || rng == null) return false;
        if (!hero.diseases().contains(DISEASE_PARANOIA)) return false;
        return rng.nextDouble() < PARANOIA_FAIL_CHANCE;
    }

    /** Defensive lookup helper (not currently used in MVP — for Sprint 12). */
    static DiseaseTraitData lookup(GameData gd, String id) {
        return gd == null ? null : gd.diseasesTraits().get(id);
    }
}
