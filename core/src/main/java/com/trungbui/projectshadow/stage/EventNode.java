package com.trungbui.projectshadow.stage;

public record EventNode(String label, String eventId) implements StageNode {
    @Override
    public NodeType type() {
        return NodeType.EVENT;
    }
}
