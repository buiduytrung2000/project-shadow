package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import com.trungbui.projectshadow.stage.BossNode;
import com.trungbui.projectshadow.stage.BossReward;
import com.trungbui.projectshadow.stage.CombatNode;
import com.trungbui.projectshadow.stage.DropEntry;
import com.trungbui.projectshadow.stage.MinibossNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the reward roll logic added in feature/combat-reward-system.
 *
 * <p>Loads real {@code assets/data} so enemy stats and item references are
 * authentic. Uses a fixed-seed RNG to make assertions deterministic.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CombatRewardRollerTest {

    private GameData gd;

    @BeforeAll
    void load() throws Exception {
        gd = GameData.loadFromDirectory(resolveDataDir());
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
        throw new IllegalStateException("Cannot locate assets/data");
    }

    @Test
    void combatNode_yieldsGoldInExpectedRange() {
        CombatNode node = new CombatNode("L1.A", List.of("enemy_01"), List.of("Base"), false);
        Hero hero = aliveHero("hero_01");
        Random rng = new Random(42L);

        CombatReward reward = CombatRewardRoller.roll(node, node.enemies(),
                List.of(hero), gd, rng);

        // Base goblin: 5..10g
        assertThat(reward.gold()).isBetween(5, 10);
    }

    @Test
    void tankVariant_appliesGoldMultiplier() {
        // Sprint 9+ B2 test fix: replace flaky 200-trial "tank > base" with a tight
        // ratio band derived from many fixed-seed pairs. Also assert the precondition
        // that enemy_01_tank exists in the CSV — without it, the loop body silently
        // contributes 0 to tankTotal and "tank > base" can pass for the wrong reason.
        assertThat(gd.enemies().get("enemy_01_tank"))
                .as("enemy_01_tank must exist in enemies.csv as the Tank variant fixture")
                .isNotNull();

        int trials = 200;
        int baseTotal = 0;
        int tankTotal = 0;
        Hero h = aliveHero("hero_01");
        CombatNode base = new CombatNode("L", List.of("enemy_01"), List.of("Base"), false);
        CombatNode tank = new CombatNode("L", List.of("enemy_01_tank"), List.of("Tank"), false);
        for (int seed = 0; seed < trials; seed++) {
            baseTotal += CombatRewardRoller.roll(base, base.enemies(), List.of(h), gd, new Random(seed)).gold();
            tankTotal += CombatRewardRoller.roll(tank, tank.enemies(), List.of(h), gd, new Random(seed)).gold();
        }
        double ratio = tankTotal / (double) baseTotal;
        // Tank multiplier is 1.2× on base 5..10g rolls. Expected ratio ~ 1.2; allow
        // a generous band that excludes "no multiplier at all" (1.0) and runaway scaling.
        assertThat(ratio).isBetween(1.10, 1.30);
    }

    @Test
    void bossNode_includesPreRolledGold() {
        BossReward br = new BossReward(200, List.of(DropEntry.item("item_t07")));
        BossNode boss = new BossNode("BOSS", "enemy_b01", br);
        Hero hero = aliveHero("hero_01");

        // Use a seed where the random rolls don't fail
        CombatReward reward = CombatRewardRoller.roll(
                boss, List.of("enemy_b01"), List.of(hero), gd, new Random(1L));

        // Boss reward = pre-rolled 200g + enemy_b01 own gold scale (small)
        assertThat(reward.gold()).isGreaterThanOrEqualTo(200);
        assertThat(reward.items()).contains("item_t07");
    }

    @Test
    void minibossNode_grantsGoldButNoBossReward() {
        MinibossNode mb = new MinibossNode("MB", "enemy_mb01");
        Hero hero = aliveHero("hero_01");

        CombatReward reward = CombatRewardRoller.roll(
                mb, List.of("enemy_mb01"), List.of(hero), gd, new Random(7L));

        // No BossReward attached → just enemy gold scaling (small, 5..10 base)
        assertThat(reward.gold()).isBetween(5, 25);
    }

    @Test
    void stressReliefAlwaysTargetsLiveHero() {
        CombatNode node = new CombatNode("L", List.of("enemy_01"), List.of("Base"), false);
        Hero h1 = aliveHero("hero_01");
        Hero h2 = aliveHero("hero_03");

        CombatReward reward = CombatRewardRoller.roll(
                node, node.enemies(), List.of(h1, h2), gd, new Random(13L));

        assertThat(reward.stressReliefHeroId()).isIn(h1.id(), h2.id());
        assertThat(reward.stressReliefAmount()).isEqualTo(3);
    }

    @Test
    void emptyParty_noStressReliefTarget() {
        CombatNode node = new CombatNode("L", List.of("enemy_01"), List.of("Base"), false);
        CombatReward reward = CombatRewardRoller.roll(
                node, node.enemies(), List.of(), gd, new Random(0L));

        assertThat(reward.stressReliefHeroId()).isNull();
        assertThat(reward.stressReliefAmount()).isZero();
    }

    @Test
    void itemGoldPseudoDrop_convertsToBonusGold() {
        // enemy_02 has dropItem=item_gold (50% chance) — force a high chance via many trials
        // and verify gold accumulates without item_gold ending up in inventory.
        CombatNode node = new CombatNode("L", List.of("enemy_02"), List.of("Base"), false);
        Hero h = aliveHero("hero_01");
        int totalRuns = 100;
        int runsWithGoldDropDetected = 0;
        for (int i = 0; i < totalRuns; i++) {
            CombatReward reward = CombatRewardRoller.roll(
                    node, node.enemies(), List.of(h), gd, new Random(i));
            assertThat(reward.items()).doesNotContain("item_gold"); // never an item
            if (reward.gold() >= 20) runsWithGoldDropDetected++;     // 20g bonus path hit
        }
        assertThat(runsWithGoldDropDetected).isGreaterThan(0); // at least once across 100 trials
    }

    private Hero aliveHero(String heroId) {
        var data = gd.heroes().get(heroId);
        return new Hero(data, Position.POS_1, gd.effects());
    }
}
