package com.trungbui.projectshadow.stage;

public sealed interface StageNode
        permits CombatNode, EventNode, RestNode, RewardNode, EliteNode, MinibossNode, BossNode {

    String label();

    NodeType type();
}
