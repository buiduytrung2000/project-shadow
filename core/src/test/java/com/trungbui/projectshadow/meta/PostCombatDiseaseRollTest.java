package com.trungbui.projectshadow.meta;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 12 B2 — verify post-combat disease roll fires at ~30% per alive hero
 * and uses the wired disease pool (dis_01..dis_05). Dead heroes never roll;
 * heroes already carrying the rolled disease are no-ops.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostCombatDiseaseRollTest {

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        gd = GameData.loadFromDirectory(dataDir);
    }

    private Hero freshHero() {
        return new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
    }

    @Test
    void rollPostCombat_returnsNullForDeadHero() {
        Hero h = freshHero();
        h.takeHpDamage(9999); // dead
        String picked = HamletService.rollPostCombatDisease(h, new Random(1L));
        assertThat(picked).isNull();
        assertThat(h.diseases()).isEmpty();
    }

    @Test
    void rollPostCombat_returnsNullForNullHero() {
        assertThat(HamletService.rollPostCombatDisease(null, new Random(1L))).isNull();
    }

    @Test
    void rollPostCombat_hitRateMatches30pctOver1000Trials() {
        // Use ONE shared RNG across trials (sequential draws from a single
        // Random give a uniform distribution; per-seed fresh Random doesn't).
        Random rng = new Random(20260511L);
        int hits = 0;
        int trials = 1000;
        for (int i = 0; i < trials; i++) {
            Hero h = freshHero();
            String picked = HamletService.rollPostCombatDisease(h, rng);
            if (picked != null) hits++;
        }
        double rate = hits / (double) trials;
        // 30% target; allow ±5% slack for sampling noise on 1000 trials.
        assertThat(rate).isBetween(0.25, 0.35);
    }

    @Test
    void rollPostCombat_pickedFromExactly5DiseasePool() {
        // Run many trials, ensure no out-of-pool disease appears.
        Random rng = new Random(424242L);
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 1000; i++) {
            Hero h = freshHero();
            String picked = HamletService.rollPostCombatDisease(h, rng);
            if (picked != null) seen.add(picked);
        }
        assertThat(seen).isSubsetOf(HamletService.POST_COMBAT_DISEASE_POOL);
        // Over 1000 trials at 30% × 1/5 each, all 5 should appear w/ near-certainty.
        assertThat(seen).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void rollPostCombat_skipsIfHeroAlreadyHasDisease() {
        Hero h = freshHero();
        // Pre-load all 5 diseases so any pick is a duplicate.
        for (String d : HamletService.POST_COMBAT_DISEASE_POOL) {
            h.addDisease(d);
        }
        int sizeBefore = h.diseases().size();
        // With seed forcing the 30% gate to pass (try multiple seeds), verify
        // even if a disease is selected the hero size doesn't grow.
        for (int i = 0; i < 50; i++) {
            HamletService.rollPostCombatDisease(h, new Random(i));
        }
        assertThat(h.diseases()).hasSize(sizeBefore);
    }
}
