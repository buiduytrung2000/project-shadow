package com.trungbui.projectshadow.stage;

import java.util.List;

public record EliteNode(
        String label,
        List<String> enemies,
        List<String> variantsAllowed,
        double hpMult,
        double dmgMult,
        int goldBonus,
        double itemDropChance
) implements StageNode {

    public EliteNode {
        enemies = List.copyOf(enemies);
        variantsAllowed = List.copyOf(variantsAllowed);
    }

    @Override
    public NodeType type() {
        return NodeType.ELITE;
    }
}
