package com.trungbui.projectshadow.meta;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 10 B2 — Heirloom drop from boss kills.
 * Drop amounts per design lock 2026-05-11:
 *   Stage 1 boss = 1, Stage 2 = 2, Stage 3 = 4.
 */
class HeirloomDropTest {

    @Test
    void heirloomFromBoss_matchesDesignLock() {
        assertThat(HamletService.heirloomFromBoss(1)).isEqualTo(1);
        assertThat(HamletService.heirloomFromBoss(2)).isEqualTo(2);
        assertThat(HamletService.heirloomFromBoss(3)).isEqualTo(4);
    }

    @Test
    void heirloomFromBoss_unknownStage_isZero() {
        assertThat(HamletService.heirloomFromBoss(0)).isZero();
        assertThat(HamletService.heirloomFromBoss(99)).isZero();
        assertThat(HamletService.heirloomFromBoss(-1)).isZero();
    }

    @Test
    void metaState_withHeirloomDelta_accumulates() {
        // Simulate 3 boss kills across the run.
        MetaState start = new MetaState(2, 0, 0,
                java.util.List.of(), java.util.List.of(),
                java.util.Map.of(), 0,
                java.time.Instant.now(), java.time.Instant.now());

        MetaState afterB1 = start.withHeirloomDelta(HamletService.heirloomFromBoss(1));
        MetaState afterB2 = afterB1.withHeirloomDelta(HamletService.heirloomFromBoss(2));
        MetaState afterB3 = afterB2.withHeirloomDelta(HamletService.heirloomFromBoss(3));

        assertThat(afterB1.heirloom()).isEqualTo(1);
        assertThat(afterB2.heirloom()).isEqualTo(3);
        assertThat(afterB3.heirloom()).isEqualTo(7); // 1+2+4
    }
}
