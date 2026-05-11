package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.SkillData;
import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.domain.CombatPhase;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Hero;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

public class CombatController {

    public interface Listener {
        default void onActionResolved(Combatant attacker, Combatant target, SkillData skill, AttackResult result) {
        }

        default void onTurnAdvanced(Combatant nextActor) {
        }

        default void onRoundStarted(int roundNumber) {
        }

        default void onCombatEnded(CombatEncounter.Side winner) {
        }

        /** Sprint 9+ B2 — fired when a hero crosses stress 100 for the first time
         *  and rolls Affliction/Virtue. {@code traitId} is the chosen trait
         *  (already added to the hero); {@code affliction} distinguishes the
         *  side rolled (true=affliction, false=virtue) so the UI can pick the
         *  right popup style. */
        default void onAfflictionResolved(Hero hero, String traitId, boolean affliction) {
        }

        /** Sprint 9+ B2 — fired when a hero dies from a heart attack
         *  (stress reached {@link Hero#HEART_ATTACK_THRESHOLD}). Combat log
         *  shows a distinct entry; the dead animation still fires via the
         *  normal HP-zero path. */
        default void onHeroHeartAttack(Hero hero) {
        }
    }

    private final CombatEncounter encounter;
    private final GameData data;
    private final RandomGenerator rng;
    private final List<String> log = new ArrayList<>();
    private Listener listener = new Listener() {
    };
    /** Sprint 11 B1 — when > 0, enemy turn execution is gated. After advancing
     *  to an enemy actor, the UI must explicitly call {@link #processPendingNonPlayerTurn()}
     *  to fire the enemy action. The UI handles delay via libGDX Stage actions
     *  so the player can visually follow each action. Default 0 = legacy
     *  synchronous behavior (tests rely on this). */
    private float pacingDelaySec = 0f;
    /** True when control flow has advanced to an enemy actor and pacing is enabled
     *  — UI must call {@link #processPendingNonPlayerTurn()} to proceed. */
    private boolean awaitingNonPlayerProcess = false;

    public CombatController(CombatEncounter encounter, GameData data, RandomGenerator rng) {
        this.encounter = encounter;
        this.data = data;
        this.rng = rng;
    }

    public CombatEncounter encounter() {
        return encounter;
    }

    public List<String> log() {
        return List.copyOf(log);
    }

    public void setListener(Listener listener) {
        this.listener = listener != null ? listener : new Listener() {
        };
    }

    public void start() {
        encounter.startRound();
        listener.onRoundStarted(encounter.roundNumber());
        listener.onTurnAdvanced(encounter.currentActor());
        autoRunIfNotPlayer();
    }

    public List<SkillData> currentActorSkills() {
        Combatant actor = encounter.currentActor();
        if (!(actor instanceof Hero hero)) return List.of();
        List<SkillData> skills = new ArrayList<>();
        for (String id : hero.equippedSkills()) {
            SkillData s = data.skills().get(id);
            if (s != null) skills.add(s);
        }
        return skills;
    }

    public boolean executePlayerSkill(int skillIndex) {
        return executePlayerSkill(skillIndex, null);
    }

    public boolean executePlayerSkill(int skillIndex, Combatant pickedTarget) {
        if (encounter.isCombatOver()) return false;
        Combatant actor = encounter.currentActor();
        if (!(actor instanceof Hero hero)) return false;

        List<SkillData> skills = currentActorSkills();
        if (skillIndex < 0 || skillIndex >= skills.size()) return false;
        SkillData skill = skills.get(skillIndex);
        if (hero.isOnCooldown(skill.skillId())) return false;

        TargetType targetType = TargetType.parse(skill.targetType());
        if (!targetType.isSupported()) return false;

        List<Combatant> targets = targetType.requiresPlayerPick()
                ? TargetSelector.resolveWithPick(targetType, hero, encounter, pickedTarget, rng)
                : TargetSelector.autoResolve(targetType, hero, encounter, rng);

        if (targets.isEmpty()) return false;

        for (Combatant t : targets) {
            resolveAction(hero, t, skill);
        }
        if (skill.cooldown() > 0) hero.putOnCooldown(skill.skillId(), skill.cooldown());

        advanceTurn();
        return true;
    }

    public boolean skillRequiresTargetPick(int skillIndex) {
        List<SkillData> skills = currentActorSkills();
        if (skillIndex < 0 || skillIndex >= skills.size()) return false;
        return TargetType.parse(skills.get(skillIndex).targetType()).requiresPlayerPick();
    }

    public List<Combatant> skillCandidateTargets(int skillIndex) {
        Combatant actor = encounter.currentActor();
        if (!(actor instanceof Hero hero)) return List.of();
        List<SkillData> skills = currentActorSkills();
        if (skillIndex < 0 || skillIndex >= skills.size()) return List.of();
        SkillData skill = skills.get(skillIndex);
        TargetType type = TargetType.parse(skill.targetType());
        return TargetSelector.candidates(type, hero, encounter);
    }

    public boolean skillIsSupported(int skillIndex) {
        List<SkillData> skills = currentActorSkills();
        if (skillIndex < 0 || skillIndex >= skills.size()) return false;
        return TargetType.parse(skills.get(skillIndex).targetType()).isSupported();
    }

    private void advanceTurn() {
        if (checkCombatEnd()) return;
        boolean moved = encounter.advanceTurn();
        if (!moved) {
            tickEndOfRound();
            if (checkCombatEnd()) return;
            encounter.startRound();
            listener.onRoundStarted(encounter.roundNumber());
        }
        Combatant next = encounter.currentActor();
        listener.onTurnAdvanced(next);
        autoRunIfNotPlayer();
    }

    private void autoRunIfNotPlayer() {
        Combatant actor = encounter.currentActor();
        if (actor instanceof Enemy enemy && enemy.isAlive()) {
            // Sprint 11 B1: if pacing is enabled, defer the enemy turn until the UI
            // ticks through its delay. Otherwise (tests / legacy mode) run inline.
            if (pacingDelaySec > 0f) {
                awaitingNonPlayerProcess = true;
                return;
            }
            runEnemyTurn(enemy);
        }
    }

    /** Sprint 11 B1 — UI calls this after its 0.7s delay to actually execute the
     *  pending enemy turn. No-op if there's no pending non-player turn. Resets the
     *  flag before running so re-entrancy is safe. */
    public void processPendingNonPlayerTurn() {
        if (!awaitingNonPlayerProcess) return;
        awaitingNonPlayerProcess = false;
        Combatant actor = encounter.currentActor();
        if (actor instanceof Enemy enemy && enemy.isAlive()) {
            runEnemyTurn(enemy);
        }
    }

    /** Sprint 11 B1 — true if a non-player actor is the current actor and pacing
     *  is gating execution. UI polls this in its render loop. */
    public boolean isAwaitingNonPlayerProcess() {
        return awaitingNonPlayerProcess;
    }

    /** Sprint 11 B1 — set seconds of delay the UI should observe between
     *  combatant actions. Set to 0 to disable (synchronous mode). Default 0. */
    public void setPacingDelaySec(float seconds) {
        this.pacingDelaySec = Math.max(0f, seconds);
    }

    public float pacingDelaySec() {
        return pacingDelaySec;
    }

    private void runEnemyTurn(Enemy enemy) {
        var enemyData = enemy.data();
        String skillId = enemyData.skill1();
        var skillData = data.enemySkills().get(skillId);
        if (skillData == null) {
            advanceTurn();
            return;
        }

        SkillData wrapped = wrapEnemySkill(skillData);
        TargetType type = TargetType.parse(wrapped.targetType());
        List<Combatant> targets = type.isSupported()
                ? TargetSelector.autoResolve(type, enemy, encounter, rng)
                : List.of();

        if (targets.isEmpty()) {
            Combatant fallback = pickFirstAliveHero();
            if (fallback == null) {
                advanceTurn();
                return;
            }
            targets = List.of(fallback);
        }

        for (Combatant t : targets) {
            resolveAction(enemy, t, wrapped);
        }
        advanceTurn();
    }

    private void resolveAction(Combatant attacker, Combatant target, SkillData skill) {
        int round = encounter.roundNumber();
        // Sprint 9+ B2 (hybrid tick): per-actor tick at action start, deduped by round
        // so AoE skills calling resolveAction per-target don't double-tick the attacker's
        // own DoTs. Cooldown ticking moved to end-of-round (was also double-ticking on
        // AoE before).
        attacker.activeEffects().onTurnStart(attacker, rng, round);

        // Sprint 11 B1 — multi-hit support. Skills with eff_multi_hit roll damage
        // N times (N = parsed effectMagnitude). Each roll is independent (crit /
        // bleed / etc. computed per-hit). Total HP damage applied as one number;
        // listener.onActionResolved fires once with aggregate AttackResult so UI
        // damage popup shows the total but combat log lists every hit.
        int hitCount = multiHitCount(skill);

        AttackResult result;
        Hero pendingAfflictionHero = null;
        Hero heartAttackHero = null;
        if (skill.isOffensive()) {
            result = resolveOffensiveHits(attacker, target, skill, hitCount);
            if (result.hit()) {
                target.takeHpDamage(result.hpDamage());
                if (result.stressDamage() > 0 && target instanceof Hero h) {
                    boolean wasAlive = h.isAlive();
                    h.takeStressDamage(result.stressDamage());
                    if (h.consumePendingAfflictionRoll()) {
                        pendingAfflictionHero = h;
                    }
                    if (wasAlive && h.isHeartAttacked()) {
                        heartAttackHero = h;
                    }
                }
            }
        } else {
            result = new AttackResult(true, false, 0, 0);
        }

        // Sprint 11 B1: skip applying eff_multi_hit as a real effect — it's a
        // sentinel for the multi-hit mechanic only. Apply other effects normally.
        if (skill.primaryEffectId() != null
                && !skill.primaryEffectId().equals("eff_multi_hit")
                && data.effects().containsKey(skill.primaryEffectId())) {
            target.activeEffects().apply(
                    skill.primaryEffectId(), attacker, target, rng, skill.effectMagnitude(), round
            );
        }

        log.add(formatLogEntry(attacker, target, skill, result));
        listener.onActionResolved(attacker, target, skill, result);
        // Sprint 9+ B2: fire stress-resolution events AFTER the action-resolved
        // event so the UI can sequence: damage popup → affliction popup.
        if (pendingAfflictionHero != null) {
            String traitId = AfflictionResolver.roll(
                    pendingAfflictionHero, data.diseasesTraits().values(), rng);
            if (traitId != null) {
                boolean affliction = isAfflictionTrait(traitId);
                listener.onAfflictionResolved(pendingAfflictionHero, traitId, affliction);
            }
        }
        if (heartAttackHero != null) {
            log.add(heartAttackHero.id() + " — HEART ATTACK!");
            listener.onHeroHeartAttack(heartAttackHero);
        }
    }

    private boolean isAfflictionTrait(String traitId) {
        var t = data.diseasesTraits().get(traitId);
        return t != null && t.isAffliction();
    }

    private Combatant pickFirstAliveHero() {
        for (Hero h : encounter.heroes()) {
            if (h.isAlive()) return h;
        }
        return null;
    }

    /**
     * Sprint 9+ B2 (hybrid tick): end-of-round used to call {@code onTurnStart} on every
     * combatant — which double-ticked DoTs already ticked by per-actor {@code resolveAction}.
     * Now end-of-round (a) ticks cooldowns once per combatant and (b) clears the per-round
     * effect-tick dedup set so the next round's per-actor tick is allowed to fire.
     * Per-actor effect tick happens at action start in {@link #resolveAction}.
     */
    private void tickEndOfRound() {
        for (Hero h : encounter.heroes()) {
            if (h.isAlive()) {
                h.tickCooldowns();
                h.activeEffects().endRoundReset();
            }
        }
        for (Enemy e : encounter.enemies()) {
            if (e.isAlive()) {
                e.tickCooldowns();
                e.activeEffects().endRoundReset();
            }
        }
    }

    private boolean checkCombatEnd() {
        if (encounter.isCombatOver()) {
            CombatEncounter.Side winner = encounter.winningSide();
            encounter.setPhase(winner == CombatEncounter.Side.HEROES
                    ? CombatPhase.COMBAT_WIN : CombatPhase.COMBAT_LOSE);
            listener.onCombatEnded(winner);
            return true;
        }
        return false;
    }

    /** Sprint 11 B1 — parse the hit count for multi-hit skills. Returns 1 for
     *  normal skills (single hit). For skills with primaryEffectId="eff_multi_hit",
     *  parses the effectMagnitude as an int (e.g. "2" → 2 hits, "3" → 3 hits).
     *  Falls back to 1 if magnitude is missing/unparseable.
     *
     *  <p>Used by sk_ar6 (Bắn Kép, 2 hits) and sk_mk2 (Liên Hoàn Quyền, 3 hits).
     *  Pre-Sprint-11 these skills fired only 1 hit at low multiplier — effectively
     *  half/third strength. This is the bug user reported.</p>
     */
    static int multiHitCount(SkillData skill) {
        if (skill == null || skill.primaryEffectId() == null) return 1;
        if (!"eff_multi_hit".equals(skill.primaryEffectId())) return 1;
        String mag = skill.effectMagnitude();
        if (mag == null || mag.isBlank()) return 1;
        try {
            int n = Integer.parseInt(mag.trim());
            return Math.max(1, Math.min(n, 10)); // sanity cap at 10 to avoid runaway
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** Sprint 11 B1 — roll {@code hitCount} independent damage attempts. Each roll
     *  uses its own hit chance + crit + variance via {@link DamageFormula#resolve}.
     *  HP damages are summed; final result reports {@code crit=true} if any hit
     *  was crit, and {@code hit=true} if any hit landed. Stress damage applied
     *  once (skill-level, not per-hit). */
    private AttackResult resolveOffensiveHits(Combatant attacker, Combatant target,
                                              SkillData skill, int hitCount) {
        if (hitCount <= 1) {
            return DamageFormula.resolve(attacker, target, skill, rng);
        }
        int totalHpDamage = 0;
        boolean anyHit = false;
        boolean anyCrit = false;
        for (int i = 0; i < hitCount; i++) {
            AttackResult r = DamageFormula.resolve(attacker, target, skill, rng);
            if (r.hit()) {
                anyHit = true;
                if (r.crit()) anyCrit = true;
                totalHpDamage += r.hpDamage();
            }
        }
        return new AttackResult(anyHit, anyCrit, totalHpDamage, skill.stressDamage());
    }

    private static SkillData wrapEnemySkill(com.trungbui.projectshadow.data.model.EnemySkillData es) {
        return new SkillData(
                es.skillId(), es.nameVn(), es.nameEn(),
                es.userType(), es.userType(),
                es.isOffensive(), es.targetType(),
                es.damageMultiplier(), es.accuracyModifier(), es.cooldown(),
                es.effectId(), es.effectValue(), null, null,
                es.stressDamage(), "Common", es.descriptionVn()
        );
    }

    private static String formatLogEntry(Combatant attacker, Combatant target, SkillData skill, AttackResult r) {
        if (!r.hit() && skill.isOffensive()) {
            return attacker.id() + " used " + skill.nameVn() + " on " + target.id() + " — MISS";
        }
        if (skill.isOffensive()) {
            String crit = r.crit() ? " CRIT!" : "";
            return attacker.id() + " hit " + target.id()
                    + " for " + r.hpDamage() + " HP" + crit
                    + " (" + skill.nameVn() + ")";
        }
        return attacker.id() + " used " + skill.nameVn() + " on " + target.id();
    }

    public Optional<Combatant> currentActor() {
        return Optional.ofNullable(encounter.currentActor());
    }
}
