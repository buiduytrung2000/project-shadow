package com.trungbui.projectshadow.meta;

import com.trungbui.projectshadow.save.HeroState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 10 B2 — Supplies Tax (Option C, non-refundable gold deduction at embark).
 * Stage 1: 100g, Stage 2: 200g, Stage 3: 400g per design lock 2026-05-11.
 */
class SuppliesTaxTest {

    private static MetaState withGold(int gold) {
        Instant now = Instant.now();
        return new MetaState(2, gold, 0,
                List.<HeroState>of(), List.<String>of(),
                java.util.Map.<String, Integer>of(), 0, now, now);
    }

    @Test
    void taxValues_matchDesignLock() {
        assertThat(HamletService.suppliesTax(1)).isEqualTo(100);
        assertThat(HamletService.suppliesTax(2)).isEqualTo(200);
        assertThat(HamletService.suppliesTax(3)).isEqualTo(400);
        assertThat(HamletService.suppliesTax(0)).isZero();
        assertThat(HamletService.suppliesTax(99)).isZero();
    }

    @Test
    void payTax_stage1_deducts100g() {
        MetaState after = HamletService.paySuppliesTax(withGold(500), 1);
        assertThat(after.gold()).isEqualTo(400);
    }

    @Test
    void payTax_stage3_deducts400g() {
        MetaState after = HamletService.paySuppliesTax(withGold(1000), 3);
        assertThat(after.gold()).isEqualTo(600);
    }

    @Test
    void payTax_insufficient_allowsDebt_perSprint11() {
        // Sprint 11 B1: paySuppliesTax no longer throws on insufficient gold.
        // Player can embark while broke; gold goes negative as "supplies debt".
        MetaState after = HamletService.paySuppliesTax(withGold(50), 1);
        assertThat(after.gold()).isEqualTo(50 - 100); // -50
        assertThat(after.gold()).isNegative();
    }

    @Test
    void payTax_stage0_isNoOp() {
        MetaState before = withGold(50);
        MetaState after = HamletService.paySuppliesTax(before, 0);
        assertThat(after.gold()).isEqualTo(50);
    }

    @Test
    void payTax_exactGold_succeedsAndZeroes() {
        MetaState after = HamletService.paySuppliesTax(withGold(100), 1);
        assertThat(after.gold()).isZero();
    }
}
