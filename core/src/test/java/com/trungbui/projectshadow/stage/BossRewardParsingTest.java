package com.trungbui.projectshadow.stage;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.StageConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the {@code boss_node.rewards_on_kill} parsing introduced in
 * feature/combat-reward-system. Before this fix, the boss reward block was
 * stored as a raw JsonNode and never deserialized, so killing the Stage 1 boss
 * granted 0 gold.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BossRewardParsingTest {

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
    void stage1Boss_yields200GoldOnKill() {
        StageConfig cfg = gd.stages().get("stage_1");
        StageTree tree = StageGenerator.generate(cfg, 42L);
        BossNode boss = (BossNode) tree.bossNode().orElseThrow();

        assertThat(boss.reward().gold()).isEqualTo(200);
    }

    @Test
    void stage1Boss_dropsExactlyOneItem() {
        // stage_1.json declares boss rolls=1, so exactly 1 drop entry should be pre-rolled.
        StageConfig cfg = gd.stages().get("stage_1");
        StageTree tree = StageGenerator.generate(cfg, 42L);
        BossNode boss = (BossNode) tree.bossNode().orElseThrow();

        assertThat(boss.reward().drops()).hasSize(1);
    }

    @Test
    void stage1Boss_dropIsFromExpectedCategories() {
        // drop_table weights: trinket_common 35, trinket_uncommon 30, consumable_rare 15,
        // relic 15, cursed 5. All entries are item_random with a category.
        StageConfig cfg = gd.stages().get("stage_1");
        StageTree tree = StageGenerator.generate(cfg, 42L);
        BossNode boss = (BossNode) tree.bossNode().orElseThrow();

        DropEntry drop = boss.reward().drops().get(0);
        assertThat(drop.type()).isEqualTo("item_random");
        assertThat(drop.category()).isIn(
                "trinket_common", "trinket_uncommon", "consumable_rare", "relic", "cursed");
    }

    @Test
    void differentSeed_producesDifferentDrop() {
        // The drop is deterministic given a seed but should differ across seeds.
        StageConfig cfg = gd.stages().get("stage_1");
        BossNode b1 = (BossNode) StageGenerator.generate(cfg, 1L).bossNode().orElseThrow();
        BossNode b2 = (BossNode) StageGenerator.generate(cfg, 999L).bossNode().orElseThrow();

        // Gold is fixed (200), but the rolled drop category should sometimes differ.
        // Try a few seed pairs — at least one should yield different categories.
        boolean anyDifferent = false;
        for (long s = 1; s <= 20 && !anyDifferent; s++) {
            BossNode a = (BossNode) StageGenerator.generate(cfg, s).bossNode().orElseThrow();
            BossNode bb = (BossNode) StageGenerator.generate(cfg, s + 100).bossNode().orElseThrow();
            if (!a.reward().drops().get(0).category().equals(bb.reward().drops().get(0).category())) {
                anyDifferent = true;
            }
        }
        assertThat(anyDifferent).isTrue();
    }

    @Test
    void legacyBossNodeConstructor_yieldsNoReward() {
        // Backwards compat: code using the 2-arg constructor gets BossReward.none()
        BossNode legacy = new BossNode("BOSS", "enemy_b01");
        assertThat(legacy.reward()).isEqualTo(BossReward.none());
        assertThat(legacy.reward().gold()).isZero();
        assertThat(legacy.reward().drops()).isEmpty();
    }
}
