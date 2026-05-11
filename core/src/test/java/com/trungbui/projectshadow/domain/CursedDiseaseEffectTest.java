package com.trungbui.projectshadow.domain;

import com.trungbui.projectshadow.data.model.HeroData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 11 B3 — Cursed disease (dis_06) applies -30 dodge to the hero,
 * floored at 0. Tests Hero.effectiveDodge() override behavior.
 */
class CursedDiseaseEffectTest {

    private static Hero makeHero(int baseDodge) {
        HeroData data = new HeroData(
                "Common", "Test", "Test", "h_test",
                "DPS", "Front",
                30, 5, 10, 80, 0.05, 5, 0.20,
                3, 1, 0.01, 0.02,
                baseDodge, 0d,
                List.of(), List.of(), ""
        );
        return new Hero(data, 0, Position.POS_1, List.of());
    }

    @Test
    void healthy_effectiveDodge_equalsBase() {
        Hero h = makeHero(10);
        assertThat(h.effectiveDodge()).isEqualTo(10);
    }

    @Test
    void cursed_effectiveDodge_subtracts30() {
        Hero h = makeHero(40);
        h.addDisease("dis_06");
        assertThat(h.effectiveDodge()).isEqualTo(10); // 40 - 30
    }

    @Test
    void cursed_belowFloor_clampedToZero() {
        Hero h = makeHero(10);
        h.addDisease("dis_06");
        // 10 - 30 = -20 → floor 0
        assertThat(h.effectiveDodge()).isZero();
    }

    @Test
    void cursed_curedRestores_dodge() {
        Hero h = makeHero(25);
        h.addDisease("dis_06");
        assertThat(h.effectiveDodge()).isZero(); // floor

        h.removeDisease("dis_06");
        assertThat(h.effectiveDodge()).isEqualTo(25);
    }
}
