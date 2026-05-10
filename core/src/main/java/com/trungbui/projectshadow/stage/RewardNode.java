package com.trungbui.projectshadow.stage;

import java.util.List;

public record RewardNode(String label, List<DropEntry> drops) implements StageNode {

    public RewardNode {
        drops = List.copyOf(drops);
    }

    @Override
    public NodeType type() {
        return NodeType.REWARD;
    }
}
