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
            runEnemyTurn(enemy);
        }
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

        AttackResult result;
        Hero pendingAfflictionHero = null;
        Hero heartAttackHero = null;
        if (skill.isOffensive()) {
            result = DamageFormula.resolve(attacker, target, skill, rng);
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

        if (skill.primaryEffectId() != null && data.effects().containsKey(skill.primaryEffectId())) {
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
