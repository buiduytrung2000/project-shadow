package com.trungbui.projectshadow.effect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 9+ B2 — {@link EffectInstance#refreshDuration(int)} must take the max
 * of current and incoming so re-applying a 1-turn version of a 5-turn buff
 * doesn't accidentally shorten it.
 */
class EffectInstanceRefreshDurationTest {

    @Test
    void refresh_takesMaxOfCurrentAndIncoming() {
        EffectInstance ei = new EffectInstance("eff_x", "src", 5, 1);
        ei.refreshDuration(1);
        assertThat(ei.remainingDuration()).isEqualTo(5);
    }

    @Test
    void refresh_extendsWhenIncomingIsLonger() {
        EffectInstance ei = new EffectInstance("eff_x", "src", 2, 1);
        ei.refreshDuration(5);
        assertThat(ei.remainingDuration()).isEqualTo(5);
    }

    @Test
    void refresh_keepsPermanentPermanent() {
        EffectInstance ei = new EffectInstance("eff_x", "src", EffectInstance.PERMANENT, 1);
        ei.refreshDuration(3);
        assertThat(ei.isPermanent()).isTrue();
    }

    @Test
    void refresh_upgradesToPermanentWhenIncomingIsPermanent() {
        EffectInstance ei = new EffectInstance("eff_x", "src", 3, 1);
        ei.refreshDuration(EffectInstance.PERMANENT);
        assertThat(ei.isPermanent()).isTrue();
    }
}
