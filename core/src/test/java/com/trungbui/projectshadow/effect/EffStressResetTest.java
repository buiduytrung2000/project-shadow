package com.trungbui.projectshadow.effect;

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
 * Fix I (post-Sprint-13 pack #2) — {@code eff_stress_reset} must set stress to 0
 * and heal 20% max HP. Covers the Monk skill "Thiền Định" (sk_mk3).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EffStressResetTest {

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
        gd = GameData.loadFromDirectory(resolveDataDir());
    }

    private static Path resolveDataDir() {
        Path[] candidates = {
                Path.of("../assets/data"),
                Path.of("assets/data"),
                Path.of("../../assets/data")
        };
        for (Path p : candidates) {
            if (Files.isDirectory(p)) return p;
        }
        throw new IllegalStateException(
                "Cannot locate assets/data from cwd=" + Path.of("").toAbsolutePath());
    }

    @Test
    void effStressReset_setsStressToZero_andHeals20PercentMaxHp() {
        // Use setCurrentStress / setCurrentHp directly to avoid heart-attack side effects.
        Hero hero = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        int maxHp = hero.maxHp();

        // Set hp=10, stress=80 without triggering any threshold
        hero.setCurrentHp(10);
        hero.setCurrentStress(80);

        assertThat(hero.currentStress()).isEqualTo(80);
        assertThat(hero.currentHp()).isEqualTo(10);

        hero.activeEffects().apply("eff_stress_reset", hero, hero, new Random(1L));

        assertThat(hero.currentStress())
                .as("stress must be reset to 0 after eff_stress_reset")
                .isZero();

        int expectedHp = Math.min(maxHp, 10 + Math.round(maxHp * 0.20f));
        assertThat(hero.currentHp())
                .as("HP should increase by 20%% of maxHp (capped at maxHp)")
                .isEqualTo(expectedHp);
    }

    @Test
    void effStressReset_healDoesNotExceedMaxHp() {
        Hero hero = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        // Start at near-full HP, stress=50
        hero.takeHpDamage(1);
        hero.setCurrentStress(50);
        int maxHp = hero.maxHp();

        hero.activeEffects().apply("eff_stress_reset", hero, hero, new Random(1L));

        assertThat(hero.currentHp())
                .as("HP must be capped at maxHp even with 20%% heal")
                .isLessThanOrEqualTo(maxHp);
        assertThat(hero.currentStress()).isZero();
    }

    @Test
    void effStressReset_atZeroStress_stillHeals() {
        Hero hero = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        hero.takeHpDamage(20);
        int hpBefore = hero.currentHp();
        // stress is already 0
        assertThat(hero.currentStress()).isZero();

        hero.activeEffects().apply("eff_stress_reset", hero, hero, new Random(1L));

        assertThat(hero.currentHp())
                .as("heal should still apply even when stress is already 0")
                .isGreaterThan(hpBefore);
        assertThat(hero.currentStress()).isZero();
    }
}
