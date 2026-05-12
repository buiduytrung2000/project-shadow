package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 13 B3 — verify stage environmental modifiers applied by
 * {@link ConditionResolver#onCombatStart(CombatEncounter, String)}.
 *
 * <ul>
 *   <li>stage_2: global -5 accuracy to all combatants.</li>
 *   <li>stage_3: +1 stress/turn to all heroes on their turn.</li>
 *   <li>null stageId: no modifier applied.</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StageEnvModifierTest {

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
        Path[] candidates = {Path.of("../assets/data"), Path.of("assets/data")};
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) {
            dataDir = p;
            break;
        }
        gd = GameData.loadFromDirectory(dataDir);
    }

    @AfterEach
    void resetStageState() {
        // Reset static mutable env-mod fields after each test to avoid cross-test leakage.
        ConditionResolver.stageAccuracyMod = 0;
        ConditionResolver.stageStressPerTurn = 0;
    }

    private Hero freshHero() {
        return new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
    }

    private Enemy freshEnemy() {
        return new Enemy(gd.enemies().get("enemy_01"), Position.POS_3, gd.effects());
    }

    private CombatEncounter singleCombat(Hero hero, Enemy enemy) {
        return new CombatEncounter(List.of(hero), List.of(enemy));
    }

    // ── Stage 2: global -5 accuracy ──────────────────────────────────────

    @Test
    void stage2_setsGlobalAccuracyMod() {
        Hero hero = freshHero();
        Enemy enemy = freshEnemy();
        CombatEncounter enc = singleCombat(hero, enemy);

        ConditionResolver.onCombatStart(enc, "stage_2");

        assertThat(ConditionResolver.stageAccuracyMod).isEqualTo(-5);
    }

    @Test
    void stage2_heroEffectiveAccuracyReduced() {
        Hero hero = freshHero();
        Enemy enemy = freshEnemy();
        CombatEncounter enc = singleCombat(hero, enemy);

        int baseAcc = hero.effectiveAccuracy();

        ConditionResolver.onCombatStart(enc, "stage_2");

        // effectiveAccuracy() = accuracy() + activeEffects.sumFlatModifier + stageAccuracyMod.
        assertThat(hero.effectiveAccuracy()).isEqualTo(baseAcc - 5);
    }

    @Test
    void stage2_enemyEffectiveAccuracyAlsoReduced() {
        Hero hero = freshHero();
        Enemy enemy = freshEnemy();
        CombatEncounter enc = singleCombat(hero, enemy);

        int baseEnemyAcc = enemy.effectiveAccuracy();

        ConditionResolver.onCombatStart(enc, "stage_2");

        // The global mod applies to ALL combatants via Combatant.effectiveAccuracy().
        assertThat(enemy.effectiveAccuracy()).isEqualTo(baseEnemyAcc - 5);
    }

    // ── Stage 3: +1 stress per hero turn ─────────────────────────────────

    @Test
    void stage3_setsStressPerTurn() {
        Hero hero = freshHero();
        Enemy enemy = freshEnemy();
        CombatEncounter enc = singleCombat(hero, enemy);

        ConditionResolver.onCombatStart(enc, "stage_3");

        assertThat(ConditionResolver.stageStressPerTurn).isEqualTo(1);
    }

    @Test
    void stage3_heroStressIncreasesOnTurnStart() {
        Hero hero = freshHero();
        Enemy enemy = freshEnemy();
        CombatEncounter enc = singleCombat(hero, enemy);

        ConditionResolver.onCombatStart(enc, "stage_3");

        int stressBefore = hero.currentStress();
        RandomGenerator rng = java.util.random.RandomGeneratorFactory.getDefault().create(42L);

        // onTurnStart fires the stage3 stress increment for the hero.
        ConditionResolver.onTurnStart(hero, enc, rng);

        assertThat(hero.currentStress()).isEqualTo(stressBefore + ConditionResolver.STAGE_3_STRESS_PER_TURN);
    }

    // ── No stageId: no modifier ───────────────────────────────────────────

    @Test
    void nullStageId_clearsModifiers() {
        Hero hero = freshHero();
        Enemy enemy = freshEnemy();
        CombatEncounter enc = singleCombat(hero, enemy);

        // Pre-set mod as if a previous combat used stage_2.
        ConditionResolver.stageAccuracyMod = -5;
        ConditionResolver.stageStressPerTurn = 0;

        ConditionResolver.onCombatStart(enc, null);

        assertThat(ConditionResolver.stageAccuracyMod).isZero();
        assertThat(ConditionResolver.stageStressPerTurn).isZero();
    }

    @Test
    void stage1_noModifier() {
        Hero hero = freshHero();
        Enemy enemy = freshEnemy();
        CombatEncounter enc = singleCombat(hero, enemy);

        int baseAcc = hero.effectiveAccuracy();
        ConditionResolver.onCombatStart(enc, "stage_1");

        assertThat(hero.effectiveAccuracy()).isEqualTo(baseAcc);
        assertThat(ConditionResolver.stageAccuracyMod).isZero();
    }
}
