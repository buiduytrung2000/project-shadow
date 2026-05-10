package com.trungbui.projectshadow.stage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StageTreeTest {

    private static CombatNode combat(String label) {
        return new CombatNode(label, List.of("enemy_01"), List.of("Base"), false);
    }

    private static BossNode boss(String label) {
        return new BossNode(label, "enemy_b01");
    }

    @Test
    void construct_withValidStageId() {
        StageTree t = new StageTree("stage_1", 42L);
        assertThat(t.stageId()).isEqualTo("stage_1");
        assertThat(t.seed()).isEqualTo(42L);
        assertThat(t.size()).isZero();
    }

    @Test
    void construct_rejectsBlankStageId() {
        assertThatThrownBy(() -> new StageTree("", 1L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StageTree(null, 1L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addNode_storesAndExposes() {
        StageTree t = new StageTree("s", 1L);
        CombatNode n = combat("L1.A");
        t.addNode(n);
        assertThat(t.size()).isEqualTo(1);
        assertThat(t.getNode("L1.A")).contains(n);
    }

    @Test
    void addNode_rejectsDuplicateLabel() {
        StageTree t = new StageTree("s", 1L);
        t.addNode(combat("L1.A"));
        assertThatThrownBy(() -> t.addNode(combat("L1.A")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addEdge_rejectsUnknownNode() {
        StageTree t = new StageTree("s", 1L);
        t.addNode(combat("L1.A"));
        assertThatThrownBy(() -> t.addEdge("L1.A", "L2.A"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> t.addEdge("L0", "L1.A"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addEdge_dedupesDuplicates() {
        StageTree t = new StageTree("s", 1L);
        t.addNode(combat("L1.A"));
        t.addNode(combat("L2.A"));
        t.addEdge("L1.A", "L2.A");
        t.addEdge("L1.A", "L2.A");
        assertThat(t.edgesFrom("L1.A")).containsExactly("L2.A");
    }

    @Test
    void hasPath_findsDirectPath() {
        StageTree t = simpleTree();
        assertThat(t.hasPath("L1.A", "BOSS")).isTrue();
    }

    @Test
    void hasPath_returnsFalseWhenDisconnected() {
        StageTree t = new StageTree("s", 1L);
        t.addNode(combat("L1.A"));
        t.addNode(boss("BOSS"));
        assertThat(t.hasPath("L1.A", "BOSS")).isFalse();
    }

    @Test
    void allPaths_returnsAllRoutes() {
        StageTree t = simpleTree();
        List<List<String>> paths = t.allPaths("L1.A", "BOSS");
        assertThat(paths).hasSize(2);
        assertThat(paths).allSatisfy(p -> {
            assertThat(p).startsWith("L1.A");
            assertThat(p).endsWith("BOSS");
        });
    }

    @Test
    void countOnPath_typeCount() {
        StageTree t = simpleTree();
        List<String> path = t.allPaths("L1.A", "BOSS").get(0);
        assertThat(t.countOnPath(path, NodeType.COMBAT)).isEqualTo(2);
        assertThat(t.countOnPath(path, NodeType.BOSS)).isEqualTo(1);
    }

    @Test
    void nodesByType_filters() {
        StageTree t = simpleTree();
        assertThat(t.nodesByType(NodeType.COMBAT)).hasSize(3);
        assertThat(t.nodesByType(NodeType.BOSS)).hasSize(1);
        assertThat(t.nodesByType(NodeType.EVENT)).isEmpty();
    }

    @Test
    void replaceNode_swapsContent() {
        StageTree t = new StageTree("s", 1L);
        t.addNode(combat("L1.A"));
        t.replaceNode("L1.A", new RestNode("L1.A", List.of()));
        assertThat(t.getNode("L1.A").orElseThrow().type()).isEqualTo(NodeType.REST);
    }

    @Test
    void replaceNode_rejectsLabelMismatch() {
        StageTree t = new StageTree("s", 1L);
        t.addNode(combat("L1.A"));
        assertThatThrownBy(() -> t.replaceNode("L1.A", combat("L2.A")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void firstNode_returnsInsertionOrder() {
        StageTree t = simpleTree();
        assertThat(t.firstNode().orElseThrow().label()).isEqualTo("L1.A");
    }

    @Test
    void bossNode_findsByType() {
        StageTree t = simpleTree();
        assertThat(t.bossNode().orElseThrow().label()).isEqualTo("BOSS");
    }

    private static StageTree simpleTree() {
        StageTree t = new StageTree("s", 1L);
        t.addNode(combat("L1.A"));
        t.addNode(combat("L2.A"));
        t.addNode(combat("L2.B"));
        t.addNode(boss("BOSS"));
        t.addEdge("L1.A", "L2.A");
        t.addEdge("L1.A", "L2.B");
        t.addEdge("L2.A", "BOSS");
        t.addEdge("L2.B", "BOSS");
        return t;
    }
}
