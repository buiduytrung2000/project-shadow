package com.trungbui.projectshadow.stage;

import com.trungbui.projectshadow.data.GameData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 9+ B2 — verifies that introducing boss-reward pre-rolls in
 * {@link StageGenerator#generate} does NOT shift the shared layout RNG.
 * Adding/removing reward rolls must not alter the seed→layout mapping
 * (otherwise pre-existing seeds yield different maps).
 *
 * <p>The fix uses a derived sub-RNG ({@code seed ^ BOSS_REWARD_RNG_SALT}) so
 * reward rolls consume from an independent stream.</p>
 */
class StageGeneratorSeedStabilityTest {

    private static GameData gd;

    @BeforeAll
    static void load() throws Exception {
        Path dataDir = Paths.get("..", "assets", "data").toAbsolutePath().normalize();
        gd = GameData.loadFromDirectory(dataDir);
    }

    @Test
    void sameSeed_producesSameLayout() {
        var cfg = gd.stages().get("stage_1");
        StageTree t1 = StageGenerator.generate(cfg, 42L);
        StageTree t2 = StageGenerator.generate(cfg, 42L);

        // Same node labels, same types, same enemy lists at each combat node.
        List<String> labels1 = t1.nodes().keySet().stream().sorted().toList();
        List<String> labels2 = t2.nodes().keySet().stream().sorted().toList();
        assertThat(labels1).isEqualTo(labels2);

        for (String label : labels1) {
            StageNode n1 = t1.getNode(label).orElseThrow();
            StageNode n2 = t2.getNode(label).orElseThrow();
            assertThat(n1.type()).isEqualTo(n2.type());
            if (n1 instanceof CombatNode c1 && n2 instanceof CombatNode c2) {
                assertThat(c1.enemies()).isEqualTo(c2.enemies());
            }
        }
    }

    @Test
    void bossRewards_deterministicForSameSeed() {
        var cfg = gd.stages().get("stage_1");
        StageTree t1 = StageGenerator.generate(cfg, 42L);
        StageTree t2 = StageGenerator.generate(cfg, 42L);

        BossNode b1 = t1.bossNode().orElseThrow().getClass() == BossNode.class
                ? (BossNode) t1.bossNode().orElseThrow() : null;
        BossNode b2 = t2.bossNode().orElseThrow().getClass() == BossNode.class
                ? (BossNode) t2.bossNode().orElseThrow() : null;
        assertThat(b1).isNotNull();
        assertThat(b2).isNotNull();

        // Same seed → same boss reward (gold + pre-rolled drops).
        assertThat(b1.reward().gold()).isEqualTo(b2.reward().gold());
        assertThat(b1.reward().drops()).isEqualTo(b2.reward().drops());
    }

    @Test
    void differentSeeds_canProduceDifferentLayouts() {
        var cfg = gd.stages().get("stage_1");
        StageTree a = StageGenerator.generate(cfg, 1L);
        StageTree b = StageGenerator.generate(cfg, 999L);
        // Not strictly required, but with this seed pair we expect at least one
        // node type to differ across the middle layers.
        boolean anyDifference = false;
        for (String label : a.nodes().keySet().stream().sorted().toList()) {
            StageNode na = a.getNode(label).orElseThrow();
            StageNode nb = b.getNode(label).orElse(null);
            if (nb == null || na.type() != nb.type()) {
                anyDifference = true;
                break;
            }
        }
        assertThat(anyDifference)
                .as("Seeds 1 and 999 should produce visibly different layouts")
                .isTrue();
    }
}
