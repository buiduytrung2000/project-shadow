package com.trungbui.projectshadow.save;

import com.trungbui.projectshadow.domain.Hero;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 9+ B3 — {@link RunState#withGoldDelta(int)} now throws on overdraw
 * instead of silently flooring to 0. Reward paths (always positive) are
 * unaffected; cost paths self-detect insufficient funds.
 */
class RunStateWithGoldDeltaTest {

    private static RunState withGold(int g) {
        return RunState.newRun("stage_1", 42L, List.<Hero>of()).withGold(g);
    }

    @Test
    void positiveDelta_addsToGold() {
        RunState s = withGold(100).withGoldDelta(50);
        assertThat(s.gold()).isEqualTo(150);
    }

    @Test
    void zeroDelta_isNoOp() {
        RunState s = withGold(100).withGoldDelta(0);
        assertThat(s.gold()).isEqualTo(100);
    }

    @Test
    void negativeDeltaWithinBudget_subtracts() {
        RunState s = withGold(100).withGoldDelta(-30);
        assertThat(s.gold()).isEqualTo(70);
    }

    @Test
    void negativeDeltaToExactZero_isAllowed() {
        RunState s = withGold(100).withGoldDelta(-100);
        assertThat(s.gold()).isZero();
    }

    @Test
    void overdraw_throwsIllegalState() {
        // Pre-B3 this silently floored to 0 — masking a bug where a cost was
        // applied but the player didn't actually pay full price.
        assertThatThrownBy(() -> withGold(100).withGoldDelta(-1000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient gold");
    }

    @Test
    void overdrawByOne_throwsIllegalState() {
        assertThatThrownBy(() -> withGold(50).withGoldDelta(-51))
                .isInstanceOf(IllegalStateException.class);
    }
}
