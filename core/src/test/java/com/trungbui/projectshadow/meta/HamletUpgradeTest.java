package com.trungbui.projectshadow.meta;

import com.trungbui.projectshadow.save.HeroState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 10 B2 — 4 Hamlet building upgrade flows.
 * Verifies cost (gold + heirloom), level transitions, and max-level rejection.
 */
class HamletUpgradeTest {

    private static MetaState rich(int gold, int heirloom) {
        Instant now = Instant.now();
        return new MetaState(2, gold, heirloom,
                List.<HeroState>of(), List.<String>of(),
                java.util.Map.<String, Integer>of(), 0, now, now);
    }

    // ───── Stagecoach ─────

    @Test
    void stagecoach_lv1ToLv2_deductsCorrectCost() {
        MetaState before = rich(500, 5);
        MetaState after = HamletService.upgradeStagecoach(before);
        assertThat(after.buildingLevel(MetaState.B_STAGECOACH)).isEqualTo(2);
        assertThat(after.gold()).isEqualTo(500 - HamletService.STAGECOACH_UPGRADE_LV2_GOLD);
        assertThat(after.heirloom()).isEqualTo(5 - HamletService.STAGECOACH_UPGRADE_LV2_HEIRLOOM);
    }

    @Test
    void stagecoach_lv2ToLv3_deductsCorrectCost() {
        MetaState lv2 = rich(1500, 10).withBuildingLevel(MetaState.B_STAGECOACH, 2);
        MetaState lv3 = HamletService.upgradeStagecoach(lv2);
        assertThat(lv3.buildingLevel(MetaState.B_STAGECOACH)).isEqualTo(3);
        assertThat(lv3.gold()).isEqualTo(1500 - HamletService.STAGECOACH_UPGRADE_LV3_GOLD);
        assertThat(lv3.heirloom()).isEqualTo(10 - HamletService.STAGECOACH_UPGRADE_LV3_HEIRLOOM);
    }

    @Test
    void stagecoach_lv3IsMaxed_throws() {
        MetaState lv3 = rich(9999, 99).withBuildingLevel(MetaState.B_STAGECOACH, 3);
        assertThatThrownBy(() -> HamletService.upgradeStagecoach(lv3))
                .isInstanceOf(HamletService.HamletException.class);
    }

    @Test
    void stagecoach_insufficientGold_throws() {
        MetaState broke = rich(0, 10);
        assertThatThrownBy(() -> HamletService.upgradeStagecoach(broke))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("gold");
    }

    @Test
    void stagecoach_insufficientHeirloom_throws() {
        MetaState noHeir = rich(9999, 0);
        assertThatThrownBy(() -> HamletService.upgradeStagecoach(noHeir))
                .isInstanceOf(HamletService.HamletException.class);
    }

    // ───── Guild ─────

    @Test
    void guild_lv1ToLv2_works() {
        MetaState after = HamletService.upgradeGuild(rich(500, 5));
        assertThat(after.buildingLevel(MetaState.B_GUILD)).isEqualTo(2);
    }

    // ───── Survivalist ─────

    @Test
    void survivalist_lv1ToLv2_works() {
        MetaState after = HamletService.upgradeSurvivalist(rich(500, 5));
        assertThat(after.buildingLevel(MetaState.B_SURVIVALIST)).isEqualTo(2);
    }

    // ───── Caretaker ─────

    @Test
    void caretaker_lv1ToLv2_works() {
        MetaState after = HamletService.upgradeCaretaker(rich(500, 5));
        assertThat(after.buildingLevel(MetaState.B_CARETAKER)).isEqualTo(2);
    }

    // ───── Helpers ─────

    @Test
    void upgradeGoldCost_returnsCorrectAmount() {
        assertThat(HamletService.upgradeGoldCost(MetaState.B_STAGECOACH, 1))
                .isEqualTo(HamletService.STAGECOACH_UPGRADE_LV2_GOLD);
        assertThat(HamletService.upgradeGoldCost(MetaState.B_STAGECOACH, 2))
                .isEqualTo(HamletService.STAGECOACH_UPGRADE_LV3_GOLD);
        assertThat(HamletService.upgradeGoldCost(MetaState.B_STAGECOACH, 3))
                .isEqualTo(-1); // maxed
    }

    @Test
    void upgradeHeirloomCost_returnsCorrectAmount() {
        assertThat(HamletService.upgradeHeirloomCost(MetaState.B_GUILD, 1))
                .isEqualTo(HamletService.GUILD_UPGRADE_LV2_HEIRLOOM);
        assertThat(HamletService.upgradeHeirloomCost(MetaState.B_GUILD, 3))
                .isEqualTo(-1);
    }
}
