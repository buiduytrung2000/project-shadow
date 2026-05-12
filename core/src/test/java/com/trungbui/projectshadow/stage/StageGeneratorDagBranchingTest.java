package com.trungbui.projectshadow.stage;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.StageConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B2 L.1 — DAG branching edge strategy tests for Stage 1 (4-floor redesign).
 *
 * <p>Key invariants checked:
 * <ul>
 *   <li>Every node is reachable from the start node.</li>
 *   <li>The boss node is reachable from start via at least one path.</li>
 *   <li>Same seed → same tree (determinism).</li>
 *   <li>Total intermediate node count matches the JSON spec (~12 nodes, 4 floors).</li>
 * </ul>
 */
class StageGeneratorDagBranchingTest {

    private static final long TEST_SEED = 42L;
    private static final int RUNS = 100;

    private static GameData gd;

    @BeforeAll
    static void load() throws Exception {
        Path dataDir = resolveDataDir();
        gd = GameData.loadFromDirectory(dataDir);
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
    void stage1_dagBranching_bossAlwaysReachableFromStart() {
        StageConfig cfg = gd.stages().get("stage_1");
        for (int seed = 1; seed <= RUNS; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            String start = tree.firstNode().orElseThrow().label();
            String boss = tree.bossNode().orElseThrow().label();
            assertThat(tree.hasPath(start, boss))
                    .as("stage_1 DAG seed=%d: path from start to boss", seed)
                    .isTrue();
        }
    }

    @Test
    void stage1_dagBranching_totalNodes_inExpectedRange() {
        StageConfig cfg = gd.stages().get("stage_1");
        StageTree tree = StageGenerator.generate(cfg, TEST_SEED);
        // Stage 1 new spec: L1(1) + L2(2) + L3(3) + L4(4) + L5(3) + PB(1) + BOSS(1) = 15
        int total = tree.nodes().size();
        assertThat(total)
                .as("Stage 1 DAG total nodes")
                .isBetween(11, 17);
    }

    @Test
    void stage1_dagBranching_multipleFloors() {
        StageConfig cfg = gd.stages().get("stage_1");
        StageTree tree = StageGenerator.generate(cfg, TEST_SEED);
        // Must have nodes in L2, L3, L4 layers.
        long l2count = tree.nodes().keySet().stream().filter(k -> k.startsWith("L2")).count();
        long l3count = tree.nodes().keySet().stream().filter(k -> k.startsWith("L3")).count();
        long l4count = tree.nodes().keySet().stream().filter(k -> k.startsWith("L4")).count();
        assertThat(l2count).as("L2 nodes").isGreaterThanOrEqualTo(1);
        assertThat(l3count).as("L3 nodes").isGreaterThanOrEqualTo(1);
        assertThat(l4count).as("L4 nodes").isGreaterThanOrEqualTo(1);
    }

    @Test
    void stage1_dagBranching_deterministic() {
        StageConfig cfg = gd.stages().get("stage_1");
        StageTree t1 = StageGenerator.generate(cfg, TEST_SEED);
        StageTree t2 = StageGenerator.generate(cfg, TEST_SEED);

        assertThat(t1.nodes().keySet()).isEqualTo(t2.nodes().keySet());
        for (String label : t1.nodes().keySet()) {
            assertThat(t1.getNode(label).orElseThrow().type())
                    .as("node type for label %s", label)
                    .isEqualTo(t2.getNode(label).orElseThrow().type());
        }
        // Also verify edges are identical.
        for (String label : t1.nodes().keySet()) {
            assertThat(t1.edgesFrom(label))
                    .as("edges from %s", label)
                    .containsExactlyInAnyOrderElementsOf(t2.edgesFrom(label));
        }
    }

    @Test
    void stage1_dagBranching_differentSeeds_produceDifferentTrees() {
        StageConfig cfg = gd.stages().get("stage_1");
        StageTree a = StageGenerator.generate(cfg, 1L);
        StageTree b = StageGenerator.generate(cfg, 99999L);
        // Not required to always differ, but with these seeds we expect at least one difference.
        boolean anyDiff = false;
        for (String label : a.nodes().keySet()) {
            StageNode nb = b.getNode(label).orElse(null);
            if (nb == null || nb.type() != a.getNode(label).orElseThrow().type()) {
                anyDiff = true;
                break;
            }
        }
        assertThat(anyDiff)
                .as("Seeds 1 and 99999 should produce visibly different layouts")
                .isTrue();
    }

    @Test
    void stage1_dagBranching_allNodesReachableFromStart_acrossSeeds() {
        StageConfig cfg = gd.stages().get("stage_1");
        for (int seed = 1; seed <= 50; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            String start = tree.firstNode().orElseThrow().label();
            String boss = tree.bossNode().orElseThrow().label();

            // All non-start nodes should be reachable from start.
            for (String label : tree.nodes().keySet()) {
                if (label.equals(start)) continue;
                assertThat(tree.hasPath(start, label))
                        .as("seed=%d: %s should be reachable from start", seed, label)
                        .isTrue();
            }
        }
    }

    @Test
    void stage1_dagBranching_preBossEliteStillPresent() {
        StageConfig cfg = gd.stages().get("stage_1");
        StageTree tree = StageGenerator.generate(cfg, TEST_SEED);
        long preBossEliteCount = tree.nodes().keySet().stream()
                .filter(label -> label.startsWith("PB."))
                .count();
        assertThat(preBossEliteCount)
                .as("Pre-boss elite nodes (PB.*) should still be generated")
                .isGreaterThanOrEqualTo(1);
    }
}
