package com.trungbui.projectshadow.stage;

/**
 * Final fight of a stage. {@link #reward} carries the deserialized
 * {@code boss_node.rewards_on_kill} block from the stage JSON
 * (gold + drop_table). May be {@link BossReward#none()} for legacy
 * configs that don't declare rewards.
 */
public record BossNode(String label, String bossId, BossReward reward) implements StageNode {
    public BossNode(String label, String bossId) {
        this(label, bossId, BossReward.none());
    }

    @Override
    public NodeType type() {
        return NodeType.BOSS;
    }
}
