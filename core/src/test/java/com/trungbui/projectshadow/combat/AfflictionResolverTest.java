package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.model.DiseaseTraitData;
import com.trungbui.projectshadow.data.model.HeroData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 9+ B2 — 70/30 Affliction/Virtue resolution roll.
 */
class AfflictionResolverTest {

    private static DiseaseTraitData trait(String id, String resolution) {
        return new DiseaseTraitData(
                "T-" + id, "Trait", id, "stat", "+0", null, null,
                0, "", "Low", "Permanent", resolution, "Live"
        );
    }

    private static Hero freshHero() {
        HeroData data = new HeroData(
                "Common", "Test", "Test", "h1",
                "DPS", "Front",
                30, 5, 10, 80, 0.05, 5, 0.20,
                3, 1, 0.01, 0.02,
                /* Sprint 11 B3 baseDodge / levelUpDodge */ 0, 0d,
                List.of("sk_test"), List.of("sk_test"),
                ""
        );
        return new Hero(data, Position.POS_1);
    }

    @Test
    void roll_picksFromAfflictionPoolMostOfTheTime() {
        List<DiseaseTraitData> pool = List.of(
                trait("t_a1", "Affliction"),
                trait("t_a2", "Affliction"),
                trait("t_v1", "Virtue"),
                trait("t_v2", "Virtue")
        );
        int afflictionCount = 0;
        int trials = 1000;
        Random rng = new Random(42L);
        for (int i = 0; i < trials; i++) {
            Hero h = freshHero();
            String picked = AfflictionResolver.roll(h, pool, rng);
            assertThat(picked).isNotNull();
            if (picked.startsWith("t_a")) afflictionCount++;
        }
        // Expect ~70% affliction. ±5% tolerance for 1000 trials.
        double ratio = afflictionCount / (double) trials;
        assertThat(ratio).isBetween(0.65, 0.75);
    }

    @Test
    void roll_excludesTraitsHeroAlreadyHas() {
        List<DiseaseTraitData> pool = List.of(
                trait("t_a1", "Affliction"),
                trait("t_v1", "Virtue")
        );
        Hero h = freshHero();
        h.addTrait("t_a1");
        // Force affliction roll many times; should always fall back to virtue since
        // t_a1 is excluded and there are no other affliction options.
        for (int seed = 0; seed < 20; seed++) {
            Hero fresh = freshHero();
            fresh.addTrait("t_a1");
            String picked = AfflictionResolver.roll(fresh, pool, new Random(seed));
            assertThat(picked).isEqualTo("t_v1");
        }
    }

    @Test
    void roll_returnsNullWhenPoolEmpty() {
        Hero h = freshHero();
        assertThat(AfflictionResolver.roll(h, List.of(), new Random(0L))).isNull();
    }

    @Test
    void roll_addsTraitToHero() {
        List<DiseaseTraitData> pool = List.of(trait("t_a1", "Affliction"));
        Hero h = freshHero();
        String picked = AfflictionResolver.roll(h, pool, new Random(0L));
        assertThat(picked).isEqualTo("t_a1");
        assertThat(h.traits()).contains("t_a1");
    }

    @Test
    void roll_isDeterministicForSameSeed() {
        List<DiseaseTraitData> pool = List.of(
                trait("t_a1", "Affliction"),
                trait("t_a2", "Affliction"),
                trait("t_v1", "Virtue")
        );
        Hero h1 = freshHero();
        Hero h2 = freshHero();
        String first = AfflictionResolver.roll(h1, pool, new Random(7L));
        String second = AfflictionResolver.roll(h2, pool, new Random(7L));
        assertThat(first).isEqualTo(second);
    }
}
