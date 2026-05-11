package com.trungbui.projectshadow.meta;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 10 B1 — paid Stagecoach refresh closes the save-scum loophole
 * where players could free-reroll until a Legendary appeared.
 */
class StagecoachRefreshCostTest {

    private static MetaState metaWith(int gold) {
        return new MetaState(2, gold, 0,
                java.util.List.of(), java.util.List.of(),
                java.util.Map.of(), 0,
                java.time.Instant.now(), java.time.Instant.now());
    }

    @Test
    void payRefresh_succeedsWhenAffordable() {
        MetaState before = metaWith(100);
        MetaState after = HamletService.payStagecoachRefresh(before);
        assertThat(after.gold()).isEqualTo(100 - HamletService.STAGECOACH_REFRESH_COST);
    }

    @Test
    void payRefresh_throwsWhenInsufficient() {
        MetaState broke = metaWith(20);
        assertThatThrownBy(() -> HamletService.payStagecoachRefresh(broke))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("gold");
    }

    @Test
    void payRefresh_exactlyEnough_succeedsAndZeroesGold() {
        MetaState barely = metaWith(HamletService.STAGECOACH_REFRESH_COST);
        MetaState after = HamletService.payStagecoachRefresh(barely);
        assertThat(after.gold()).isZero();
    }

    @Test
    void payRefresh_costIs50g_perDesignLock() {
        // Hard-pin the value so a future accidental edit will fail this test.
        assertThat(HamletService.STAGECOACH_REFRESH_COST).isEqualTo(50);
    }
}
