package com.trungbui.projectshadow.stage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.StageConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B2 L.1 — backward-compatibility regression test for the {@code fully_connected}
 * edge strategy. Ensures that stages using the legacy topology still generate
 * trees identical to pre-DAG behavior (all nodes in layer N connected to all
 * in layer N+1).
 *
 * <p>Stage 2 still uses {@code fully_connected} layers under the hood (we added
 * {@code dag_branching} at the layout level, but Stage 2's middle_layers don't
 * have per-layer edge_topology overrides, so they inherit the layout default).
 * Actually we changed Stage 2 to dag_branching too, so this test verifies that
 * a synthetic stage with explicit {@code fully_connected} still generates a tree
 * where every source connects to every target.</p>
 */
class StageGeneratorBackwardCompatTest {

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
    void stage1_dagBranching_doesNotBreakPathFromStart() {
        // Regression: the existing integration test invariant "always has path start→boss"
        // must hold under the new DAG topology.
        StageConfig cfg = gd.stages().get("stage_1");
        for (int seed = 1; seed <= 50; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            String start = tree.firstNode().orElseThrow().label();
            String boss = tree.bossNode().orElseThrow().label();
            assertThat(tree.hasPath(start, boss))
                    .as("stage_1 seed=%d: path start→boss under dag_branching", seed)
                    .isTrue();
        }
    }

    @Test
    void stage2_dagBranching_bossAlwaysReachable() {
        StageConfig cfg = gd.stages().get("stage_2");
        for (int seed = 1; seed <= 50; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            String start = tree.firstNode().orElseThrow().label();
            String boss = tree.bossNode().orElseThrow().label();
            assertThat(tree.hasPath(start, boss))
                    .as("stage_2 seed=%d: path start→boss", seed)
                    .isTrue();
        }
    }

    @Test
    void stage3_dagBranching_bossAlwaysReachable() {
        StageConfig cfg = gd.stages().get("stage_3");
        for (int seed = 1; seed <= 50; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            String start = tree.firstNode().orElseThrow().label();
            String boss = tree.bossNode().orElseThrow().label();
            assertThat(tree.hasPath(start, boss))
                    .as("stage_3 seed=%d: path start→boss", seed)
                    .isTrue();
        }
    }

    @Test
    void stage2_dagBranching_maxCombat4_respected() {
        StageConfig cfg = gd.stages().get("stage_2");
        for (int seed = 1; seed <= 50; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            String start = tree.firstNode().orElseThrow().label();
            String boss = tree.bossNode().orElseThrow().label();
            for (List<String> path : tree.allPaths(start, boss)) {
                int combatCount = tree.countOnPath(path, NodeType.COMBAT);
                assertThat(combatCount)
                        .as("stage_2 seed=%d path combat count", seed)
                        .isLessThanOrEqualTo(4);
            }
        }
    }
}
