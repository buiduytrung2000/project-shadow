package com.trungbui.projectshadow.effect;

import com.trungbui.projectshadow.data.model.EffectData;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Fixtures;
import com.trungbui.projectshadow.domain.Position;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 9+ B2 — verifies the hybrid effect-tick scheme. Pre-fix bugs:
 * <ol>
 *   <li>AoE skills caused DoT on the attacker to tick N times (once per target)
 *       because {@code resolveAction} ran per-target and each call invoked
 *       {@code onTurnStart}.</li>
 *   <li>End-of-round and per-action both ticked, so durations decremented 2×/round.</li>
 *   <li>Newly-applied effects ticked immediately in the same round they were applied.</li>
 * </ol>
 * The hybrid scheme: per-actor tick at action start, deduped by round via
 * {@code tickedThisRound}, and just-applied effects skip their first tick.
 */
class EffectTickHybridTest {

    private static EffectData bleed3turns() {
        return new EffectData(
                "eff_bleed_t1", "Bleed", "Bleed", "dot", true, null,
                "turns", "3", "hp", "flat_per_turn", "-1",
                true, 3,
                "on_turn_start", null, null, null, null, ""
        );
    }

    @Test
    void onTurnStart_dedupsWithinSameRound() {
        Map<String, EffectData> cat = Map.of("eff_bleed_t1", bleed3turns());
        Enemy target = new Enemy(Fixtures.enemyData("e1", 30, 3, 6, 80, 0.05), Position.POS_1, cat);
        // Apply on round 1; tick should happen once at the START of round 2 onwards.
        // For this isolated test we feed currentRound directly.
        target.activeEffects().apply("eff_bleed_t1", null, target, new Random(1L), null, 1);

        int hp = target.currentHp();
        // Tick three times in round 2 (e.g. AoE) → should only deal 1 dmg total.
        target.activeEffects().onTurnStart(target, new Random(1L), 2);
        target.activeEffects().onTurnStart(target, new Random(1L), 2);
        target.activeEffects().onTurnStart(target, new Random(1L), 2);

        assertThat(target.currentHp()).isEqualTo(hp - 1);
    }

    @Test
    void onTurnStart_freshEffectDoesNotTickSameRound() {
        Map<String, EffectData> cat = Map.of("eff_bleed_t1", bleed3turns());
        Enemy target = new Enemy(Fixtures.enemyData("e1", 30, 3, 6, 80, 0.05), Position.POS_1, cat);
        int hp = target.currentHp();

        // Apply on round 5; same-round onTurnStart must NOT tick (skip first round).
        target.activeEffects().apply("eff_bleed_t1", null, target, new Random(1L), null, 5);
        target.activeEffects().onTurnStart(target, new Random(1L), 5);

        assertThat(target.currentHp()).isEqualTo(hp);
    }

    @Test
    void onTurnStart_ticksOncePerRoundAfterEndRoundReset() {
        Map<String, EffectData> cat = Map.of("eff_bleed_t1", bleed3turns());
        Enemy target = new Enemy(Fixtures.enemyData("e1", 30, 3, 6, 80, 0.05), Position.POS_1, cat);
        target.activeEffects().apply("eff_bleed_t1", null, target, new Random(1L), null, 1);
        int hp = target.currentHp();

        // Round 2: tick once → 1 dmg, dedup set has eff_bleed_t1
        target.activeEffects().onTurnStart(target, new Random(1L), 2);
        // Even calling again in round 2 (AoE) → no extra dmg.
        target.activeEffects().onTurnStart(target, new Random(1L), 2);
        assertThat(target.currentHp()).isEqualTo(hp - 1);

        // End round 2 → reset dedup set.
        target.activeEffects().endRoundReset();

        // Round 3: ticks again → 1 more dmg.
        target.activeEffects().onTurnStart(target, new Random(1L), 3);
        assertThat(target.currentHp()).isEqualTo(hp - 2);
    }

    @Test
    void durationDecrementsOncePerRound_notPerCall() {
        Map<String, EffectData> cat = Map.of("eff_bleed_t1", bleed3turns());
        Enemy target = new Enemy(Fixtures.enemyData("e1", 30, 3, 6, 80, 0.05), Position.POS_1, cat);
        target.activeEffects().apply("eff_bleed_t1", null, target, new Random(1L), null, 1);

        // 3 calls in same round → still 1 instance (duration not double-decremented).
        target.activeEffects().onTurnStart(target, new Random(1L), 2);
        target.activeEffects().onTurnStart(target, new Random(1L), 2);
        target.activeEffects().onTurnStart(target, new Random(1L), 2);
        assertThat(target.activeEffects().size()).isEqualTo(1);

        // Advance 2 more rounds → duration was 3, after 3 round-tick cycles → expired.
        target.activeEffects().endRoundReset();
        target.activeEffects().onTurnStart(target, new Random(1L), 3);
        target.activeEffects().endRoundReset();
        target.activeEffects().onTurnStart(target, new Random(1L), 4);
        assertThat(target.activeEffects().size()).isZero();
    }
}
