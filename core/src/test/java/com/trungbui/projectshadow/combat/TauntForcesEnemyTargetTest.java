package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Hero;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fix J (post-Sprint-13 pack #2) — enemy single-target attacks must be forced
 * to target a hero with the {@code eff_taunt} effect active.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TauntForcesEnemyTargetTest {

    private GameData gd;
    private CombatEncounter encounter;
    private Hero heroA;
    private Hero heroB;
    private Enemy enemy;

    @BeforeAll
    void loadAll() throws Exception {
        gd = GameData.loadFromDirectory(resolveDataDir());
    }

    @BeforeEach
    void setUp() {
        encounter = CombatScenario.buildDefault(gd);
        heroA = encounter.heroes().get(0);
        heroB = encounter.heroes().get(1);
        enemy = encounter.enemies().get(0);
    }

    private static Path resolveDataDir() {
        Path[] candidates = {
                Path.of("../assets/data"),
                Path.of("assets/data"),
                Path.of("../../assets/data")
        };
        for (Path p : candidates) {
            if (Files.isDirectory(p)) return p;
        }
        throw new IllegalStateException(
                "Cannot locate assets/data from cwd=" + Path.of("").toAbsolutePath());
    }

    @Test
    void frontFoe_forcedToTauntingHero() {
        // Apply taunt to heroB (not the front-most hero)
        heroB.activeEffects().apply("eff_taunt", heroB, heroB, new Random(1L));
        assertThat(heroB.activeEffects().isTaunting()).isTrue();

        List<Combatant> targets = TargetSelector.autoResolve(
                TargetType.FRONT_FOE, enemy, encounter, new Random(1L));

        assertThat(targets).hasSize(1);
        assertThat(targets.get(0))
                .as("enemy FRONT_FOE attack must target the taunting hero")
                .isEqualTo(heroB);
    }

    @Test
    void anyFoe_forcedToTauntingHero() {
        heroB.activeEffects().apply("eff_taunt", heroB, heroB, new Random(1L));

        List<Combatant> targets = TargetSelector.autoResolve(
                TargetType.ANY_FOE, enemy, encounter, new Random(1L));

        assertThat(targets).containsExactly(heroB);
    }

    @Test
    void randomFoe_forcedToTauntingHero() {
        heroA.activeEffects().apply("eff_taunt", heroA, heroA, new Random(1L));

        List<Combatant> targets = TargetSelector.autoResolve(
                TargetType.RANDOM_FOE, enemy, encounter, new Random(42L));

        assertThat(targets).containsExactly(heroA);
    }

    @Test
    void lowestHpFoe_forcedToTauntingHero_evenIfNotLowestHp() {
        // heroB has less damage, heroA has more HP loss
        heroA.takeHpDamage(20); // heroA lower HP
        heroB.activeEffects().apply("eff_taunt", heroB, heroB, new Random(1L));

        List<Combatant> targets = TargetSelector.autoResolve(
                TargetType.LOWEST_HP_FOE, enemy, encounter, new Random(1L));

        assertThat(targets).containsExactly(heroB);
    }

    @Test
    void aoe_allFoe_notAffectedByTaunt() {
        // AoE should NOT be redirected by taunt
        heroB.activeEffects().apply("eff_taunt", heroB, heroB, new Random(1L));

        List<Combatant> targets = TargetSelector.autoResolve(
                TargetType.ALL_FOE, enemy, encounter, new Random(1L));

        assertThat(targets)
                .as("ALL_FOE AoE must not be redirected by taunt")
                .hasSize(4);
    }

    @Test
    void noTaunter_normalTargetingApplies() {
        // No taunt active — normal front-hero targeting should apply
        assertThat(heroA.activeEffects().isTaunting()).isFalse();
        assertThat(heroB.activeEffects().isTaunting()).isFalse();

        List<Combatant> targets = TargetSelector.autoResolve(
                TargetType.FRONT_FOE, enemy, encounter, new Random(1L));

        assertThat(targets).hasSize(1);
        assertThat(targets.get(0))
                .as("with no taunt, FRONT_FOE should target the front-most hero")
                .isEqualTo(heroA);
    }
}
