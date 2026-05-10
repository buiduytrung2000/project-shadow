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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TargetSelectorTest {

    private GameData gd;
    private CombatEncounter encounter;
    private Hero attackerHero;
    private Enemy attackerEnemy;

    @BeforeAll
    void loadAll() throws Exception {
        gd = GameData.loadFromDirectory(resolveDataDir());
    }

    @BeforeEach
    void freshEncounter() {
        encounter = CombatScenario.buildDefault(gd);
        attackerHero = encounter.heroes().get(0);
        attackerEnemy = encounter.enemies().get(0);
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
    void parse_handlesAllCsvVariants() {
        assertThat(TargetType.parse("Self")).isEqualTo(TargetType.SELF);
        assertThat(TargetType.parse("Front Enemy")).isEqualTo(TargetType.FRONT_FOE);
        assertThat(TargetType.parse("Front Hero")).isEqualTo(TargetType.FRONT_FOE);
        assertThat(TargetType.parse("Back Enemy")).isEqualTo(TargetType.BACK_FOE);
        assertThat(TargetType.parse("Back Hero")).isEqualTo(TargetType.BACK_FOE);
        assertThat(TargetType.parse("Any Enemy")).isEqualTo(TargetType.ANY_FOE);
        assertThat(TargetType.parse("Single Enemy")).isEqualTo(TargetType.ANY_FOE);
        assertThat(TargetType.parse("Single Hero")).isEqualTo(TargetType.ANY_FOE);
        assertThat(TargetType.parse("Single Ally")).isEqualTo(TargetType.ANY_ALLY);
        assertThat(TargetType.parse("All Enemies")).isEqualTo(TargetType.ALL_FOE);
        assertThat(TargetType.parse("All Heroes")).isEqualTo(TargetType.ALL_FOE);
        assertThat(TargetType.parse("All Allies")).isEqualTo(TargetType.ALL_ALLY);
        assertThat(TargetType.parse("Random")).isEqualTo(TargetType.RANDOM_FOE);
        assertThat(TargetType.parse("Random Enemy")).isEqualTo(TargetType.RANDOM_FOE);
        assertThat(TargetType.parse("Random Hero")).isEqualTo(TargetType.RANDOM_FOE);
        assertThat(TargetType.parse("2 Front Enemies")).isEqualTo(TargetType.FRONT_2_FOE);
        assertThat(TargetType.parse("Front 2 Heroes")).isEqualTo(TargetType.FRONT_2_FOE);
        assertThat(TargetType.parse("2 Random Enemies")).isEqualTo(TargetType.RANDOM_FOE_2);
        assertThat(TargetType.parse("Lowest HP Enemy")).isEqualTo(TargetType.LOWEST_HP_FOE);
        assertThat(TargetType.parse("Front Row (AoE)")).isEqualTo(TargetType.FRONT_ROW_FOE);
        assertThat(TargetType.parse("Back Row (AoE)")).isEqualTo(TargetType.BACK_ROW_FOE);
    }

    @Test
    void parse_returnsUnsupportedForUnknownAndSpecial() {
        assertThat(TargetType.parse("Ground")).isEqualTo(TargetType.UNSUPPORTED);
        assertThat(TargetType.parse("Dead Ally")).isEqualTo(TargetType.UNSUPPORTED);
        assertThat(TargetType.parse("Dead Enemy")).isEqualTo(TargetType.UNSUPPORTED);
        assertThat(TargetType.parse("Summons")).isEqualTo(TargetType.UNSUPPORTED);
        assertThat(TargetType.parse(null)).isEqualTo(TargetType.UNSUPPORTED);
        assertThat(TargetType.parse("Some new thing")).isEqualTo(TargetType.UNSUPPORTED);
    }

    @Test
    void requiresPlayerPick_correctForPickTypes() {
        assertThat(TargetType.ANY_FOE.requiresPlayerPick()).isTrue();
        assertThat(TargetType.ANY_ALLY.requiresPlayerPick()).isTrue();
        assertThat(TargetType.BACK_FOE.requiresPlayerPick()).isTrue();
        assertThat(TargetType.SELF.requiresPlayerPick()).isFalse();
        assertThat(TargetType.FRONT_FOE.requiresPlayerPick()).isFalse();
        assertThat(TargetType.ALL_FOE.requiresPlayerPick()).isFalse();
    }

    @Test
    void self_returnsAttacker() {
        List<Combatant> r = TargetSelector.autoResolve(
                TargetType.SELF, attackerHero, encounter, new Random(1L));
        assertThat(r).containsExactly(attackerHero);
    }

    @Test
    void frontFoe_picksLowestPositionAliveEnemy() {
        List<Combatant> r = TargetSelector.autoResolve(
                TargetType.FRONT_FOE, attackerHero, encounter, new Random(1L));
        assertThat(r).hasSize(1);
        assertThat(r.get(0)).isEqualTo(encounter.enemies().get(0));
    }

    @Test
    void frontFoe_skipsDeadEnemies() {
        encounter.enemies().get(0).takeHpDamage(9999);
        List<Combatant> r = TargetSelector.autoResolve(
                TargetType.FRONT_FOE, attackerHero, encounter, new Random(1L));
        assertThat(r).hasSize(1);
        assertThat(r.get(0)).isEqualTo(encounter.enemies().get(1));
    }

    @Test
    void backFoe_picksHighestPositionAliveEnemy() {
        List<Combatant> r = TargetSelector.autoResolve(
                TargetType.BACK_FOE, attackerHero, encounter, new Random(1L));
        assertThat(r).hasSize(1);
        assertThat(r.get(0)).isEqualTo(encounter.enemies().get(3));
    }

    @Test
    void allFoe_returnsAllAliveEnemies() {
        List<Combatant> r = TargetSelector.autoResolve(
                TargetType.ALL_FOE, attackerHero, encounter, new Random(1L));
        assertThat(r).hasSize(4);
    }

    @Test
    void allFoe_excludesDeadEnemies() {
        encounter.enemies().get(0).takeHpDamage(9999);
        encounter.enemies().get(2).takeHpDamage(9999);
        List<Combatant> r = TargetSelector.autoResolve(
                TargetType.ALL_FOE, attackerHero, encounter, new Random(1L));
        assertThat(r).hasSize(2);
    }

    @Test
    void front2Foe_returnsTwoFrontmost() {
        List<Combatant> r = TargetSelector.autoResolve(
                TargetType.FRONT_2_FOE, attackerHero, encounter, new Random(1L));
        assertThat(r).hasSize(2);
        assertThat(r).containsExactly(encounter.enemies().get(0), encounter.enemies().get(1));
    }

    @Test
    void frontRowFoe_returnsAliveInFrontPositions() {
        List<Combatant> r = TargetSelector.autoResolve(
                TargetType.FRONT_ROW_FOE, attackerHero, encounter, new Random(1L));
        assertThat(r).hasSize(2);
        assertThat(r).allMatch(c -> c.position().isFront());
    }

    @Test
    void backRowFoe_returnsAliveInBackPositions() {
        List<Combatant> r = TargetSelector.autoResolve(
                TargetType.BACK_ROW_FOE, attackerHero, encounter, new Random(1L));
        assertThat(r).hasSize(2);
        assertThat(r).allMatch(c -> c.position().isBack());
    }

    @Test
    void lowestHpFoe_picksLowestCurrentHp() {
        encounter.enemies().get(2).takeHpDamage(5);
        List<Combatant> r = TargetSelector.autoResolve(
                TargetType.LOWEST_HP_FOE, attackerHero, encounter, new Random(1L));
        assertThat(r).hasSize(1);
        assertThat(r.get(0)).isEqualTo(encounter.enemies().get(2));
    }

    @Test
    void randomFoe_returnsExactlyOne() {
        List<Combatant> r = TargetSelector.autoResolve(
                TargetType.RANDOM_FOE, attackerHero, encounter, new Random(1L));
        assertThat(r).hasSize(1);
    }

    @Test
    void randomFoe2_returnsExactlyTwo() {
        List<Combatant> r = TargetSelector.autoResolve(
                TargetType.RANDOM_FOE_2, attackerHero, encounter, new Random(1L));
        assertThat(r).hasSize(2);
    }

    @Test
    void allAlly_returnsAllAliveHeroes() {
        List<Combatant> r = TargetSelector.autoResolve(
                TargetType.ALL_ALLY, attackerHero, encounter, new Random(1L));
        assertThat(r).hasSize(4);
    }

    @Test
    void candidates_anyFoe_returnsAllAliveEnemies() {
        List<Combatant> r = TargetSelector.candidates(TargetType.ANY_FOE, attackerHero, encounter);
        assertThat(r).hasSize(4);
    }

    @Test
    void candidates_backFoe_returnsBackAliveEnemies() {
        List<Combatant> r = TargetSelector.candidates(TargetType.BACK_FOE, attackerHero, encounter);
        assertThat(r).hasSize(2);
        assertThat(r).allMatch(c -> c.position().isBack());
    }

    @Test
    void candidates_anyAlly_returnsAllAliveHeroes() {
        List<Combatant> r = TargetSelector.candidates(TargetType.ANY_ALLY, attackerHero, encounter);
        assertThat(r).hasSize(4);
    }

    @Test
    void resolveWithPick_returnsPickedWhenValid() {
        Combatant picked = encounter.enemies().get(2);
        List<Combatant> r = TargetSelector.resolveWithPick(
                TargetType.ANY_FOE, attackerHero, encounter, picked, new Random(1L));
        assertThat(r).containsExactly(picked);
    }

    @Test
    void resolveWithPick_returnsEmptyWhenPickedNotInCandidates() {
        Combatant picked = encounter.heroes().get(1);
        List<Combatant> r = TargetSelector.resolveWithPick(
                TargetType.ANY_FOE, attackerHero, encounter, picked, new Random(1L));
        assertThat(r).isEmpty();
    }

    @Test
    void resolveWithPick_returnsEmptyWhenPickedDead() {
        Combatant picked = encounter.enemies().get(0);
        picked.takeHpDamage(9999);
        List<Combatant> r = TargetSelector.resolveWithPick(
                TargetType.ANY_FOE, attackerHero, encounter, picked, new Random(1L));
        assertThat(r).isEmpty();
    }

    @Test
    void resolveWithPick_fallsBackToAutoForNonPickType() {
        List<Combatant> r = TargetSelector.resolveWithPick(
                TargetType.FRONT_FOE, attackerHero, encounter, null, new Random(1L));
        assertThat(r).hasSize(1);
    }

    @Test
    void enemyAttacker_foesAreHeroes() {
        List<Combatant> r = TargetSelector.autoResolve(
                TargetType.FRONT_FOE, attackerEnemy, encounter, new Random(1L));
        assertThat(r).hasSize(1);
        assertThat(r.get(0)).isInstanceOf(Hero.class);
    }

    @Test
    void unsupported_returnsEmpty() {
        List<Combatant> auto = TargetSelector.autoResolve(
                TargetType.UNSUPPORTED, attackerHero, encounter, new Random(1L));
        List<Combatant> cand = TargetSelector.candidates(
                TargetType.UNSUPPORTED, attackerHero, encounter);
        assertThat(auto).isEmpty();
        assertThat(cand).isEmpty();
    }
}
