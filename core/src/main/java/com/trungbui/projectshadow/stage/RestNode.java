package com.trungbui.projectshadow.stage;

import java.util.List;

public record RestNode(String label, List<RestOption> options) implements StageNode {

    public RestNode {
        options = List.copyOf(options);
    }

    @Override
    public NodeType type() {
        return NodeType.REST;
    }
}
