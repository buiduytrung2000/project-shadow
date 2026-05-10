package com.trungbui.projectshadow.stage;

public record MinibossNode(String label, String minibossId) implements StageNode {
    @Override
    public NodeType type() {
        return NodeType.MINIBOSS;
    }
}
