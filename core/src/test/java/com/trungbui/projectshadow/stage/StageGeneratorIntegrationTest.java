package com.trungbui.projectshadow.stage;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.StageConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StageGeneratorIntegrationTest {

    private static final int RUNS = 200;

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
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
        throw new IllegalStateException(
                "Cannot locate assets/data from cwd=" + Path.of("").toAbsolutePath());
    }

    @Test
    void stage1_alwaysHasPathFromStartToBoss() {
        StageConfig cfg = gd.stages().get("stage_1");
        for (int seed = 1; seed <= RUNS; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            String start = tree.firstNode().orElseThrow().label();
            String boss = tree.bossNode().orElseThrow().label();
            assertThat(tree.hasPath(start, boss))
                    .as("stage_1 seed=%d has path", seed)
                    .isTrue();
        }
    }

    @Test
    void stage1_neverExceedsMaxCombat3() {
        StageConfig cfg = gd.stages().get("stage_1");
        for (int seed = 1; seed <= RUNS; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            String start = tree.firstNode().orElseThrow().label();
            String boss = tree.bossNode().orElseThrow().label();
            for (List<String> path : tree.allPaths(start, boss)) {
                int combatCount = tree.countOnPath(path, NodeType.COMBAT);
                assertThat(combatCount)
                        .as("stage_1 seed=%d path=%s combat count", seed, path)
                        .isLessThanOrEqualTo(3);
            }
        }
    }

    @Test
    void stage1_firstNodeIsCombat() {
        StageConfig cfg = gd.stages().get("stage_1");
        for (int seed = 1; seed <= RUNS; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            assertThat(tree.firstNode().orElseThrow())
                    .as("stage_1 seed=%d first node is combat", seed)
                    .isInstanceOf(CombatNode.class);
        }
    }

    @Test
    void stage1_lastNodeIsBossWithCorrectId() {
        StageConfig cfg = gd.stages().get("stage_1");
        StageTree tree = StageGenerator.generate(cfg, 42L);
        BossNode boss = (BossNode) tree.bossNode().orElseThrow();
        assertThat(boss.bossId()).isEqualTo("enemy_b01");
        assertThat(boss.label()).isEqualTo("BOSS");
    }

    @Test
    void stage1_layerSizes_match4Floors() {
        // B2 L.2: Stage 1 redesigned to 4 floors (L2=2, L3=3, L4=4, L5=3) + PB.
        StageConfig cfg = gd.stages().get("stage_1");
        StageTree tree = StageGenerator.generate(cfg, 42L);
        long l1 = tree.nodes().keySet().stream().filter(k -> k.startsWith("L1")).count();
        long l2 = tree.nodes().keySet().stream().filter(k -> k.startsWith("L2")).count();
        long l3 = tree.nodes().keySet().stream().filter(k -> k.startsWith("L3")).count();
        long l4 = tree.nodes().keySet().stream().filter(k -> k.startsWith("L4")).count();
        long l5 = tree.nodes().keySet().stream().filter(k -> k.startsWith("L5")).count();
        assertThat(l1).isEqualTo(1);
        assertThat(l2).isEqualTo(2);
        assertThat(l3).isEqualTo(3);
        assertThat(l4).isEqualTo(4);
        assertThat(l5).isEqualTo(3);
    }

    @Test
    void stage1_dagBranching_l2NodesReachableFromL1A() {
        // B2 L.1: under dag_branching, connectivity invariant ensures every L2 node
        // has at least one incoming edge (reachable from L1.A since L1.A is the only source).
        StageConfig cfg = gd.stages().get("stage_1");
        StageTree tree = StageGenerator.generate(cfg, 42L);
        List<StageNode> l2 = tree.nodes().values().stream()
                .filter(n -> n.label().startsWith("L2")).toList();
        for (StageNode l2Node : l2) {
            assertThat(tree.hasPath("L1.A", l2Node.label()))
                    .as("L2 node %s should be reachable from L1.A", l2Node.label())
                    .isTrue();
        }
    }

    @Test
    void stage1_sameSeed_producesSameTree() {
        StageConfig cfg = gd.stages().get("stage_1");
        StageTree a = StageGenerator.generate(cfg, 12345L);
        StageTree b = StageGenerator.generate(cfg, 12345L);
        assertThat(a.nodes().keySet()).isEqualTo(b.nodes().keySet());
        for (String label : a.nodes().keySet()) {
            assertThat(a.getNode(label).orElseThrow().type())
                    .isEqualTo(b.getNode(label).orElseThrow().type());
        }
    }

    @Test
    void stage1_differentSeeds_produceDifferentTrees() {
        StageConfig cfg = gd.stages().get("stage_1");
        StageTree a = StageGenerator.generate(cfg, 1L);
        StageTree b = StageGenerator.generate(cfg, 9999L);
        boolean anyDifferent = false;
        for (Map.Entry<String, StageNode> e : a.nodes().entrySet()) {
            StageNode bn = b.getNode(e.getKey()).orElse(null);
            if (bn == null || bn.type() != e.getValue().type()) {
                anyDifferent = true;
                break;
            }
        }
        assertThat(anyDifferent).isTrue();
    }

    @Test
    void stage2_hasPathFromStartToBoss_acrossSeeds() {
        StageConfig cfg = gd.stages().get("stage_2");
        for (int seed = 1; seed <= RUNS; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            String start = tree.firstNode().orElseThrow().label();
            String boss = tree.bossNode().orElseThrow().label();
            assertThat(tree.hasPath(start, boss))
                    .as("stage_2 seed=%d has path", seed)
                    .isTrue();
        }
    }

    @Test
    void stage2_neverExceedsMaxCombat4() {
        StageConfig cfg = gd.stages().get("stage_2");
        for (int seed = 1; seed <= RUNS; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            String start = tree.firstNode().orElseThrow().label();
            String boss = tree.bossNode().orElseThrow().label();
            for (List<String> path : tree.allPaths(start, boss)) {
                int combatCount = tree.countOnPath(path, NodeType.COMBAT);
                assertThat(combatCount)
                        .as("stage_2 seed=%d combat count", seed)
                        .isLessThanOrEqualTo(4);
            }
        }
    }

    @Test
    void stage2_neverExceedsEliteCap1() {
        StageConfig cfg = gd.stages().get("stage_2");
        for (int seed = 1; seed <= RUNS; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            int eliteCount = tree.nodesByType(NodeType.ELITE).size();
            assertThat(eliteCount)
                    .as("stage_2 seed=%d elite count", seed)
                    .isLessThanOrEqualTo(1);
        }
    }

    @Test
    void stage3_hasMinibossLayerBeforeBoss() {
        StageConfig cfg = gd.stages().get("stage_3");
        StageTree tree = StageGenerator.generate(cfg, 42L);
        assertThat(tree.nodesByType(NodeType.MINIBOSS)).hasSize(1);
        MinibossNode mb = (MinibossNode) tree.nodesByType(NodeType.MINIBOSS).get(0);
        assertThat(tree.edgesFrom(mb.label())).isNotEmpty();
    }

    @Test
    void stage3_minibossId_fromPool() {
        StageConfig cfg = gd.stages().get("stage_3");
        for (int seed = 1; seed <= 50; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            List<StageNode> mb = tree.nodesByType(NodeType.MINIBOSS);
            assertThat(mb).hasSize(1);
            String mbId = ((MinibossNode) mb.get(0)).minibossId();
            assertThat(mbId).isIn("enemy_mb01", "enemy_mb02");
        }
    }

    @Test
    void stage3_hasPathFromStartToBoss_acrossSeeds() {
        StageConfig cfg = gd.stages().get("stage_3");
        for (int seed = 1; seed <= RUNS; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            String start = tree.firstNode().orElseThrow().label();
            String boss = tree.bossNode().orElseThrow().label();
            assertThat(tree.hasPath(start, boss))
                    .as("stage_3 seed=%d has path", seed)
                    .isTrue();
        }
    }

    @Test
    void stage3_neverExceedsMaxCombat6() {
        StageConfig cfg = gd.stages().get("stage_3");
        for (int seed = 1; seed <= RUNS; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            String start = tree.firstNode().orElseThrow().label();
            String boss = tree.bossNode().orElseThrow().label();
            for (List<String> path : tree.allPaths(start, boss)) {
                int combatCount = tree.countOnPath(path, NodeType.COMBAT);
                assertThat(combatCount)
                        .as("stage_3 seed=%d combat count", seed)
                        .isLessThanOrEqualTo(6);
            }
        }
    }

    @Test
    void allStages_distributionRoughlyMatchesWeights() {
        StageConfig cfg = gd.stages().get("stage_1");
        Map<NodeType, Integer> typeCounts = new java.util.EnumMap<>(NodeType.class);
        int totalMiddleNodes = 0;
        for (int seed = 1; seed <= 1000; seed++) {
            StageTree tree = StageGenerator.generate(cfg, seed);
            for (StageNode n : tree.nodes().values()) {
                if (n.label().startsWith("L1") || n.label().equals("BOSS")) continue;
                typeCounts.merge(n.type(), 1, Integer::sum);
                totalMiddleNodes++;
            }
        }
        assertThat(totalMiddleNodes).isPositive();
        int combat = typeCounts.getOrDefault(NodeType.COMBAT, 0);
        double combatRatio = (double) combat / totalMiddleNodes;
        assertThat(combatRatio).as("combat ratio across 1000 stage_1 runs")
                .isBetween(0.20, 0.55);
    }
}
