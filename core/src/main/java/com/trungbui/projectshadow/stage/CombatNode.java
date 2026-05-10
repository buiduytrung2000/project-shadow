package com.trungbui.projectshadow.stage;

import java.util.List;

public record CombatNode(
        String label,
        List<String> enemies,
        List<String> variantsAllowed,
        boolean preBoss
) implements StageNode {

    public CombatNode {
        enemies = List.copyOf(enemies);
        variantsAllowed = List.copyOf(variantsAllowed);
    }

    @Override
    public NodeType type() {
        return NodeType.COMBAT;
    }
}
