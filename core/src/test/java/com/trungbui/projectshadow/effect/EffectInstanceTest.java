package com.trungbui.projectshadow.effect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EffectInstanceTest {

    @Test
    void construct_withValid_fields() {
        EffectInstance ei = new EffectInstance("eff_bleed", "hero_01", 3, 1);
        assertThat(ei.effectId()).isEqualTo("eff_bleed");
        assertThat(ei.sourceId()).isEqualTo("hero_01");
        assertThat(ei.remainingDuration()).isEqualTo(3);
        assertThat(ei.stacks()).isEqualTo(1);
        assertThat(ei.isPermanent()).isFalse();
        assertThat(ei.isExpired()).isFalse();
    }

    @Test
    void construct_rejects_blank_effectId() {
        assertThatThrownBy(() -> new EffectInstance("", null, 3, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EffectInstance(null, null, 3, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stacks_clamped_to_at_least_one() {
        EffectInstance ei = new EffectInstance("eff_bleed", null, 3, 0);
        assertThat(ei.stacks()).isEqualTo(1);
    }

    @Test
    void permanent_when_duration_negative_one() {
        EffectInstance ei = new EffectInstance("eff_perm", null, EffectInstance.PERMANENT, 1);
        assertThat(ei.isPermanent()).isTrue();
        assertThat(ei.isExpired()).isFalse();
    }

    @Test
    void tickDuration_decrements_until_zero() {
        EffectInstance ei = new EffectInstance("eff_bleed", null, 2, 1);
        ei.tickDuration();
        assertThat(ei.remainingDuration()).isEqualTo(1);
        ei.tickDuration();
        assertThat(ei.remainingDuration()).isZero();
        assertThat(ei.isExpired()).isTrue();
        ei.tickDuration();
        assertThat(ei.remainingDuration()).isZero();
    }

    @Test
    void tickDuration_no_op_when_permanent() {
        EffectInstance ei = new EffectInstance("eff_perm", null, EffectInstance.PERMANENT, 1);
        ei.tickDuration();
        assertThat(ei.isPermanent()).isTrue();
    }

    @Test
    void addStack_clamps_to_max() {
        EffectInstance ei = new EffectInstance("eff_bleed", null, 3, 1);
        ei.addStack(3);
        assertThat(ei.stacks()).isEqualTo(2);
        ei.addStack(3);
        assertThat(ei.stacks()).isEqualTo(3);
        ei.addStack(3);
        assertThat(ei.stacks()).isEqualTo(3);
    }

    @Test
    void refreshDuration_resets_remaining() {
        EffectInstance ei = new EffectInstance("eff_bleed", null, 1, 1);
        ei.refreshDuration(5);
        assertThat(ei.remainingDuration()).isEqualTo(5);
    }
}
