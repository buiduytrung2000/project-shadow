package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.domain.Hero;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fix H (post-Sprint-13 pack #2) — cooldowns must reset at combat start.
 * A hero with a non-zero cooldown from a previous combat should have
 * that cooldown cleared when the new CombatController calls start().
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CooldownResetBetweenCombatsTest {

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
    void start_clearsCooldownsFromPreviousCombat() {
        CombatEncounter enc = CombatScenario.buildDefault(gd);
        Hero hero = enc.heroes().get(0);

        // Simulate a cooldown carried over from a prior combat.
        String skillId = hero.equippedSkills().get(0);
        hero.putOnCooldown(skillId, 3);
        assertThat(hero.cooldownRemaining(skillId))
                .as("sanity: cooldown should be 3 before start()")
                .isEqualTo(3);

        CombatController controller = new CombatController(enc, gd, new Random(1L));
        controller.start();

        assertThat(hero.cooldownRemaining(skillId))
                .as("cooldown must be reset to 0 when combat starts")
                .isZero();
    }

    @Test
    void start_clearsCooldowns_forAllHeroes() {
        CombatEncounter enc = CombatScenario.buildDefault(gd);

        // Put every hero on cooldown for their first skill.
        for (Hero hero : enc.heroes()) {
            String skillId = hero.equippedSkills().get(0);
            hero.putOnCooldown(skillId, 2);
        }

        CombatController controller = new CombatController(enc, gd, new Random(1L));
        controller.start();

        for (Hero hero : enc.heroes()) {
            String skillId = hero.equippedSkills().get(0);
            assertThat(hero.cooldownRemaining(skillId))
                    .as("hero " + hero.id() + " cooldown must be 0 after start()")
                    .isZero();
        }
    }
}
