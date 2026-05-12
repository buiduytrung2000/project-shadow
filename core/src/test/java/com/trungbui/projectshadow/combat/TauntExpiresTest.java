package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Hero;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fix J (post-Sprint-13 pack #2) — when {@code eff_taunt} expires after its
 * duration, the enemy must revert to normal target selection.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TauntExpiresTest {

    private GameData gd;
    private CombatEncounter encounter;
    private Hero heroA;
    private Hero heroB;
    private Enemy enemy;

    @BeforeAll
    void loadAll() throws Exception {
        gd = GameData.loadFromDirectory(resolveDataDir());
    }

    @BeforeEach
    void setUp() {
        encounter = CombatScenario.buildDefault(gd);
        heroA = encounter.heroes().get(0);
        heroB = encounter.heroes().get(1);
        enemy = encounter.enemies().get(0);
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
    void taunt_expiredAfterDuration_normalTargetingResumes() {
        // eff_taunt has defaultDuration=2 turns. Apply it to heroB.
        heroB.activeEffects().apply("eff_taunt", heroB, heroB, new Random(1L));
        assertThat(heroB.activeEffects().isTaunting()).isTrue();

        // Taunt is active — enemy should target heroB.
        List<Combatant> duringTaunt = TargetSelector.autoResolve(
                TargetType.FRONT_FOE, enemy, encounter, new Random(1L));
        assertThat(duringTaunt).containsExactly(heroB);

        // Tick 3 rounds to expire the 2-turn taunt.
        Random rng = new Random(1L);
        for (int i = 0; i < 3; i++) {
            heroB.activeEffects().onTurnStart(heroB, rng);
        }

        assertThat(heroB.activeEffects().isTaunting())
                .as("taunt should have expired after ticking 3 times (duration=2)")
                .isFalse();

        // Now enemy should target normally (frontmost = heroA).
        List<Combatant> afterTaunt = TargetSelector.autoResolve(
                TargetType.FRONT_FOE, enemy, encounter, new Random(1L));
        assertThat(afterTaunt).hasSize(1);
        assertThat(afterTaunt.get(0))
                .as("after taunt expires, FRONT_FOE should return to normal front targeting")
                .isEqualTo(heroA);
    }

    @Test
    void taunt_whileActive_forcesTarget_multipleRounds() {
        heroB.activeEffects().apply("eff_taunt", heroB, heroB, new Random(1L));

        // Still active after 1 tick (duration starts at 2, ticks to 1).
        heroB.activeEffects().onTurnStart(heroB, new Random(1L));
        assertThat(heroB.activeEffects().isTaunting())
                .as("taunt should still be active after 1 tick")
                .isTrue();

        List<Combatant> targets = TargetSelector.autoResolve(
                TargetType.FRONT_FOE, enemy, encounter, new Random(1L));
        assertThat(targets).containsExactly(heroB);
    }
}
