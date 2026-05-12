package com.trungbui.projectshadow.save;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import com.trungbui.projectshadow.domain.Fixtures;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 13 B2 — {@link RunState} combo-streak and run-summary accumulator tests.
 */
class RunStateStreakTest {

    private static RunState blankState() {
        return new RunState(
                SaveMigration.CURRENT_RUN_VERSION, "run-test", "stage_1", 42L,
                null, List.of(), List.of(), 0, List.of(),
                Instant.now(), Instant.now(), false,
                0, 0, 0
        );
    }

    // ── streak ──────────────────────────────────────────────────────────────

    @Test
    void withStreakIncrement_startsAt0_returns1() {
        RunState s = blankState();
        assertThat(s.consecutiveNodesCleared()).isZero();
        assertThat(s.withStreakIncrement().consecutiveNodesCleared()).isEqualTo(1);
    }

    @Test
    void withStreakIncrement_chained_accumulatesCorrectly() {
        RunState s = blankState()
                .withStreakIncrement()
                .withStreakIncrement()
                .withStreakIncrement();
        assertThat(s.consecutiveNodesCleared()).isEqualTo(3);
    }

    @Test
    void withStreakReset_afterIncrement_returnsZero() {
        RunState s = blankState()
                .withStreakIncrement()
                .withStreakIncrement()
                .withStreakReset();
        assertThat(s.consecutiveNodesCleared()).isZero();
    }

    @Test
    void withStreakReset_onFreshState_remainsZero() {
        assertThat(blankState().withStreakReset().consecutiveNodesCleared()).isZero();
    }

    // ── enemiesKilled ────────────────────────────────────────────────────────

    @Test
    void withEnemiesKilledDelta_accumulates() {
        RunState s = blankState()
                .withEnemiesKilledDelta(2)
                .withEnemiesKilledDelta(3);
        assertThat(s.enemiesKilled()).isEqualTo(5);
    }

    @Test
    void withEnemiesKilledDelta_negativeIgnored() {
        RunState s = blankState().withEnemiesKilledDelta(-5);
        assertThat(s.enemiesKilled()).isZero();
    }

    // ── heirloomEarned ───────────────────────────────────────────────────────

    @Test
    void withHeirloomEarnedDelta_accumulates() {
        RunState s = blankState()
                .withHeirloomEarnedDelta(1)
                .withHeirloomEarnedDelta(2);
        assertThat(s.heirloomEarned()).isEqualTo(3);
    }

    // ── save/load round-trip ─────────────────────────────────────────────────

    @Test
    void streakPersistedViaJson() throws Exception {
        JsonMapper mapper = JsonMapper.builder().findAndAddModules()
                .enable(SerializationFeature.INDENT_OUTPUT).build();

        RunState original = blankState()
                .withStreakIncrement()
                .withStreakIncrement()
                .withEnemiesKilledDelta(4)
                .withHeirloomEarnedDelta(1);

        String json = mapper.writeValueAsString(original);
        RunState reloaded = SaveMigration.loadRun(mapper, json);

        assertThat(reloaded.consecutiveNodesCleared()).isEqualTo(2);
        assertThat(reloaded.enemiesKilled()).isEqualTo(4);
        assertThat(reloaded.heirloomEarned()).isEqualTo(1);
    }
}
