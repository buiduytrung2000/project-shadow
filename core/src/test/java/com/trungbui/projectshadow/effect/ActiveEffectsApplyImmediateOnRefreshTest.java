package com.trungbui.projectshadow.effect;

import com.trungbui.projectshadow.data.model.EffectData;
import com.trungbui.projectshadow.domain.Fixtures;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 9+ B2 — re-applying a heal/damage effect now triggers
 * {@code applyImmediate} a second time. Previously only the first apply
 * triggered immediate heal, so re-casting a heal while the buff was up
 * silently did nothing.
 */
class ActiveEffectsApplyImmediateOnRefreshTest {

    private static EffectData instantHeal(String id, String value) {
        return new EffectData(
                id, "Test", "Test", "heal", true, null,
                "instant", "", "hp", "flat", value,
                false, null,
                "on_use", null, null, null, null, ""
        );
    }

    @Test
    void castingHealTwice_healsTwice() {
        Map<String, EffectData> cat = Map.of("eff_heal_t1", instantHeal("eff_heal_t1", "5"));
        Hero h = new Hero(Fixtures.heroData("h1", 30, 5, 10, 80, 0.05, 5), Position.POS_1, cat);
        h.setCurrentHp(10); // 20 below max

        h.activeEffects().apply("eff_heal_t1", null, h, new Random(1L));
        int afterFirst = h.currentHp();
        assertThat(afterFirst).isEqualTo(15);

        // Refresh — should heal again.
        h.activeEffects().apply("eff_heal_t1", null, h, new Random(1L));
        assertThat(h.currentHp()).isEqualTo(20);
    }
}
