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
        if (encounter.isCombatOver()) return false;
        Combatant actor = encounter.currentActor();
        if (!(actor instanceof Hero hero)) return false;

        List<SkillData> skills = currentActorSkills();
        if (skillIndex < 0 || skillIndex >= skills.size()) return false;
        SkillData skill = skills.get(skillIndex);
        if (hero.isOnCooldown(skill.skillId())) return false;

        Combatant target = pickTargetForSkill(hero, skill);
        if (target == null) return false;

        resolveAction(hero, target, skill);
        if (skill.cooldown() > 0) hero.putOnCooldown(skill.skillId(), skill.cooldown());

        advanceTurn();
        return true;
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
        Combatant target = pickFirstAliveHero();
        if (target == null) {
            advanceTurn();
            return;
        }

        SkillData wrapped = wrapEnemySkill(skillData);
        resolveAction(enemy, target, wrapped);
        advanceTurn();
    }

    private void resolveAction(Combatant attacker, Combatant target, SkillData skill) {
        attacker.activeEffects().onTurnStart(attacker, rng);
        attacker.tickCooldowns();

        AttackResult result;
        if (skill.isOffensive()) {
            result = DamageFormula.resolve(attacker, target, skill, rng);
            if (result.hit()) {
                target.takeHpDamage(result.hpDamage());
                if (result.stressDamage() > 0 && target instanceof Hero h) {
                    h.takeStressDamage(result.stressDamage());
                }
            }
        } else {
            result = new AttackResult(true, false, 0, 0);
        }

        if (skill.primaryEffectId() != null && data.effects().containsKey(skill.primaryEffectId())) {
            Combatant effectTarget = skill.isOffensive() ? target : attacker;
            effectTarget.activeEffects().apply(skill.primaryEffectId(), attacker, effectTarget, rng);
        }

        log.add(formatLogEntry(attacker, target, skill, result));
        listener.onActionResolved(attacker, target, skill, result);
    }

    private Combatant pickTargetForSkill(Hero attacker, SkillData skill) {
        if (skill.isOffensive()) return pickFrontEnemy();
        return pickLowestHpAlly(attacker);
    }

    private Combatant pickFrontEnemy() {
        for (Enemy e : encounter.enemies()) {
            if (e.isAlive()) return e;
        }
        return null;
    }

    private Combatant pickFirstAliveHero() {
        for (Hero h : encounter.heroes()) {
            if (h.isAlive()) return h;
        }
        return null;
    }

    private Combatant pickLowestHpAlly(Hero self) {
        Hero lowest = null;
        for (Hero h : encounter.heroes()) {
            if (!h.isAlive()) continue;
            if (lowest == null || h.currentHp() < lowest.currentHp()) lowest = h;
        }
        return lowest != null ? lowest : self;
    }

    private void tickEndOfRound() {
        for (Hero h : encounter.heroes()) {
            if (h.isAlive()) {
                h.activeEffects().onTurnStart(h, rng);
                h.tickCooldowns();
            }
        }
        for (Enemy e : encounter.enemies()) {
            if (e.isAlive()) {
                e.activeEffects().onTurnStart(e, rng);
                e.tickCooldowns();
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
