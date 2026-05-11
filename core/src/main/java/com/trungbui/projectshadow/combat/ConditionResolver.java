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

    public static final double COWARDLY_SKIP_CHANCE = 0.20;
    public static final int COWARDLY_STRESS_THRESHOLD = 50;
    public static final int MASOCHIST_STRESS_TO_ALLIES = 5;

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
     * (Bloodthirsty stacks, Cowardly flag if stale).
     */
    public static void onCombatStart(CombatEncounter encounter) {
        if (encounter == null) return;
        for (Hero h : encounter.heroes()) {
            h.resetBloodthirstyStacks();
            h.setSkipNextAction(false);
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
                || TRAIT_BLOODTHIRSTY.equals(conditionId);
    }

    /** Defensive lookup helper (not currently used in MVP — for Sprint 12). */
    static DiseaseTraitData lookup(GameData gd, String id) {
        return gd == null ? null : gd.diseasesTraits().get(id);
    }
}
