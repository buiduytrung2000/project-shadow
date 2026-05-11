package com.trungbui.projectshadow.stage;

import com.trungbui.projectshadow.data.GameData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 10 B3 — boss-alt-path layer adds 1-2 elite nodes between the last
 * middle layer and the boss. Same seed must produce same layout (regression
 * against the {@code BOSS_REWARD_RNG_SALT} pattern).
 */
class StageGeneratorBossAltPathTest {

    private static GameData gd;

    @BeforeAll
    static void load() throws Exception {
        Path dataDir = Paths.get("..", "assets", "data").toAbsolutePath().normalize();
        gd = GameData.loadFromDirectory(dataDir);
    }

    @Test
    void stage1_includesPreBossEliteNode() {
        var cfg = gd.stages().get("stage_1");
        StageTree tree = StageGenerator.generate(cfg, 42L);
        long eliteCount = tree.nodes().values().stream()
                .filter(n -> n.type() == NodeType.ELITE)
                .count();
        // Stage 1 may already have middle-layer elites from random rolls, but the
        // pre_boss_alt_layer must always add at least 1 elite labeled "PB.*".
        long preBossEliteCount = tree.nodes().keySet().stream()
                .filter(label -> label.startsWith("PB."))
                .count();
        assertThat(preBossEliteCount).isGreaterThanOrEqualTo(1);
        assertThat(eliteCount).isGreaterThanOrEqualTo(preBossEliteCount);
    }

    @Test
    void preBossElite_connectsToBoss() {
        var cfg = gd.stages().get("stage_1");
        StageTree tree = StageGenerator.generate(cfg, 42L);
        String bossLabel = tree.bossNode().orElseThrow().label();
        // Every PB.* node must have an outgoing edge to the boss.
        for (String label : tree.nodes().keySet()) {
            if (!label.startsWith("PB.")) continue;
            assertThat(tree.edgesFrom(label)).contains(bossLabel);
        }
    }

    @Test
    void preBossElite_seedDeterministic() {
        var cfg = gd.stages().get("stage_1");
        StageTree t1 = StageGenerator.generate(cfg, 42L);
        StageTree t2 = StageGenerator.generate(cfg, 42L);
        // Same seed → same PB.* labels.
        var preBoss1 = t1.nodes().keySet().stream()
                .filter(l -> l.startsWith("PB.")).sorted().toList();
        var preBoss2 = t2.nodes().keySet().stream()
                .filter(l -> l.startsWith("PB.")).sorted().toList();
        assertThat(preBoss1).isEqualTo(preBoss2);
    }
}
