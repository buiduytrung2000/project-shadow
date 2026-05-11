package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import com.trungbui.projectshadow.stage.CombatNode;
import com.trungbui.projectshadow.stage.StageNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 9+ B2 — combat-reward RNG is now seeded from
 * {@code stageSeed ^ nodeLabel.hashCode()}. Same (seed, label) → same reward.
 * Different label → typically different reward (cannot strictly guarantee due
 * to RNG collisions, but easy to demonstrate with a couple of label pairs).
 */
class CombatRewardRngDeterminismTest {

    private static GameData gd;

    @BeforeAll
    static void load() throws Exception {
        Path dataDir = Paths.get("..", "assets", "data").toAbsolutePath().normalize();
        gd = GameData.loadFromDirectory(dataDir);
    }

    private static Hero hero() {
        return new Hero(gd.heroes().get("hero_01"), Position.POS_1);
    }

    private static CombatReward rollFor(long stageSeed, String label, List<String> enemyIds) {
        long seed = stageSeed ^ label.hashCode();
        StageNode node = new CombatNode(label, enemyIds, List.of(), false);
        return CombatRewardRoller.roll(
                node, enemyIds, List.of(hero()), gd, new Random(seed));
    }

    @Test
    void sameSeedAndLabel_yieldsSameReward() {
        var enemies = List.of("enemy_01", "enemy_02");
        CombatReward a = rollFor(42L, "L1.A", enemies);
        CombatReward b = rollFor(42L, "L1.A", enemies);
        assertThat(a.gold()).isEqualTo(b.gold());
        assertThat(a.items()).isEqualTo(b.items());
        assertThat(a.stressReliefHeroId()).isEqualTo(b.stressReliefHeroId());
    }

    @Test
    void differentLabels_typicallyYieldDifferentRewards() {
        // Verify rewards aren't pinned to seed only — label must influence the RNG.
        var enemies = List.of("enemy_01", "enemy_02", "enemy_03");
        CombatReward a = rollFor(42L, "L1.A", enemies);
        CombatReward b = rollFor(42L, "L1.B", enemies);
        // At least one of gold/items/stress-target should differ.
        boolean differ = a.gold() != b.gold()
                || !a.items().equals(b.items())
                || !java.util.Objects.equals(a.stressReliefHeroId(), b.stressReliefHeroId());
        assertThat(differ)
                .as("Labels L1.A vs L1.B should produce different reward outcomes")
                .isTrue();
    }

    @Test
    void differentSeeds_typicallyYieldDifferentRewards() {
        var enemies = List.of("enemy_01", "enemy_02", "enemy_03");
        CombatReward a = rollFor(1L, "L2.A", enemies);
        CombatReward b = rollFor(999L, "L2.A", enemies);
        boolean differ = a.gold() != b.gold()
                || !a.items().equals(b.items());
        assertThat(differ).isTrue();
    }
}
