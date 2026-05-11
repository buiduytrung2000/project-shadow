package com.trungbui.projectshadow.combat;

import java.util.List;

/**
 * Result of rolling combat rewards (see {@link CombatRewardRoller}).
 *
 * <p>Carries everything the UI needs to display the post-combat popup
 * and the wiring layer needs to update {@link com.trungbui.projectshadow.save.RunState}
 * and party stress.</p>
 *
 * @param gold                  Gold to add to the run wallet.
 * @param items                 Item IDs to append to the run inventory. Item IDs are
 *                              canonical {@code items.csv} keys (e.g. {@code item_c01}),
 *                              or category placeholders like {@code trinket_common}
 *                              for {@code item_random} boss drops (resolved later).
 * @param stressReliefHeroId    Hero ID to apply stress reduction to (one random alive
 *                              hero). {@code null} if the party was fully wiped (shouldn't
 *                              happen on victory but defensive).
 * @param stressReliefAmount    How much stress to subtract from {@code stressReliefHeroId}
 *                              (positive number; 0 if no target).
 */
public record CombatReward(
        int gold,
        List<String> items,
        String stressReliefHeroId,
        int stressReliefAmount
) {
    public CombatReward {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static CombatReward empty() {
        return new CombatReward(0, List.of(), null, 0);
    }

    public boolean isEmpty() {
        return gold == 0 && items.isEmpty() && stressReliefAmount == 0;
    }
}
