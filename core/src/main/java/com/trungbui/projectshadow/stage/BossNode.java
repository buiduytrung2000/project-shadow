package com.trungbui.projectshadow.stage;

public record BossNode(String label, String bossId) implements StageNode {
    @Override
    public NodeType type() {
        return NodeType.BOSS;
    }
}
