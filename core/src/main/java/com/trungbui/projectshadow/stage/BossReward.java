package com.trungbui.projectshadow.stage;

import java.util.List;

/**
 * Reward dropped when a boss is defeated. Parsed from a stage JSON's
 * {@code boss_node.rewards_on_kill} block (gold + weighted drop_table).
 *
 * <p>Sprint 9 (combat-reward-system) — prior to this, {@code boss_node} was
 * stored as a raw JsonNode and never deserialized, so killing the Stage 1
 * boss granted no gold or items. {@link com.trungbui.projectshadow.stage.StageGenerator#generateBossNode}
 * now parses this struct and attaches it to {@link BossNode}.</p>
 *
 * <p>{@code drops} is the unrolled drop table (each call to
 * {@link com.trungbui.projectshadow.combat.CombatRewardRoller} rolls the table
 * to produce concrete {@code DropEntry} instances).</p>
 */
public record BossReward(int gold, List<DropEntry> drops) {
    public BossReward {
        drops = drops == null ? List.of() : List.copyOf(drops);
    }

    public static BossReward none() {
        return new BossReward(0, List.of());
    }
}
