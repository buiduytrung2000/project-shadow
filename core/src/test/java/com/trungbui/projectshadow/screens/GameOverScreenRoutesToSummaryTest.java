package com.trungbui.projectshadow.screens;

import com.trungbui.projectshadow.domain.Fixtures;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import com.trungbui.projectshadow.save.RunState;
import com.trungbui.projectshadow.save.SaveMigration;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 13 B2 — verify that the data passed to {@link RunSummaryScreen} is
 * properly captured before routing (no libGDX rendering needed — tests logic only).
 *
 * <p>The actual screen-transition routing lives in {@code ProjectShadowGame}, which
 * requires a full libGDX environment. Here we test that the {@link RunState} and
 * party list carry the right data for summary display — the routing call-site is
 * smoke-tested via the game's manual playthrough verification.</p>
 */
class GameOverScreenRoutesToSummaryTest {

    private static RunState defeatState() {
        return new RunState(
                SaveMigration.CURRENT_RUN_VERSION, "run-defeat", "stage_1", 42L,
                "L1.A", List.of("L1.A"), List.of(), 80, List.of(),
                Instant.now(), Instant.now(), true,
                2, 5, 0, java.util.Map.of()
        );
    }

    private static RunState victoryState() {
        return new RunState(
                SaveMigration.CURRENT_RUN_VERSION, "run-victory", "stage_1", 42L,
                "boss", List.of("L1.A", "L2.A", "boss"), List.of(), 500, List.of(),
                Instant.now(), Instant.now(), false,
                3, 8, 1, java.util.Map.of()
        );
    }

    @Test
    void defeatState_hasArchivedFlag() {
        assertThat(defeatState().archived()).isTrue();
    }

    @Test
    void victoryState_accumulates_enemiesAndHeirloom() {
        RunState s = victoryState();
        assertThat(s.enemiesKilled()).isEqualTo(8);
        assertThat(s.heirloomEarned()).isEqualTo(1);
        assertThat(s.gold()).isEqualTo(500);
    }

    @Test
    void findMvp_fromVictoryParty_picksMaxDamage() {
        Hero h1 = new Hero(Fixtures.heroData("h1", 30, 5, 8, 90, 0.10, 5), Position.POS_1);
        Hero h2 = new Hero(Fixtures.heroData("h2", 30, 5, 8, 90, 0.10, 5), Position.POS_2);
        h1.addRunDamage(45);
        h2.addRunDamage(100);
        Hero mvp = RunSummaryScreen.findMvp(List.of(h1, h2));
        assertThat(mvp).isNotNull();
        assertThat(mvp.id()).isEqualTo("h2");
    }

    @Test
    void runSummaryScreen_findMvp_withDeadHero_countsDamageEvenIfDead() {
        // Dead heroes (hp=0) still count toward MVP if they dealt the most damage.
        Hero h1 = new Hero(Fixtures.heroData("h1", 30, 5, 8, 90, 0.10, 5), Position.POS_1);
        Hero h2 = new Hero(Fixtures.heroData("h2", 30, 5, 8, 90, 0.10, 5), Position.POS_2);
        h1.addRunDamage(200);
        h2.addRunDamage(50);
        h1.takeHpDamage(9999); // h1 is dead
        Hero mvp = RunSummaryScreen.findMvp(List.of(h1, h2));
        assertThat(mvp).isNotNull();
        assertThat(mvp.id()).isEqualTo("h1");
    }
}
