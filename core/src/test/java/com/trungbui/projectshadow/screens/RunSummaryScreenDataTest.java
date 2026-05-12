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
 * Sprint 13 B2 — data helpers for {@link RunSummaryScreen} (no libGDX rendering).
 */
class RunSummaryScreenDataTest {

    private static RunState state(int gold, int enemiesKilled, int heirloomEarned) {
        return new RunState(
                SaveMigration.CURRENT_RUN_VERSION, "run-test", "stage_1", 42L,
                null, List.of(), List.of(), gold, List.of(),
                Instant.now(), Instant.now(), false,
                0, enemiesKilled, heirloomEarned
        );
    }

    private static Hero heroWithDamage(String id, int damage) {
        Hero h = new Hero(Fixtures.heroData(id, 30, 5, 8, 90, 0.10, 5), Position.POS_1);
        h.addRunDamage(damage);
        return h;
    }

    // ── findMvp ─────────────────────────────────────────────────────────────

    @Test
    void findMvp_picksHeroWithMostDamage() {
        Hero h1 = heroWithDamage("hero_01", 50);
        Hero h2 = heroWithDamage("hero_02", 120);
        Hero h3 = heroWithDamage("hero_03", 80);
        Hero mvp = RunSummaryScreen.findMvp(List.of(h1, h2, h3));
        assertThat(mvp).isNotNull();
        assertThat(mvp.id()).isEqualTo("hero_02");
    }

    @Test
    void findMvp_allZeroDamage_returnsNull() {
        Hero h1 = new Hero(Fixtures.heroData("h1", 30, 5, 8, 90, 0.10, 5), Position.POS_1);
        Hero h2 = new Hero(Fixtures.heroData("h2", 30, 5, 8, 90, 0.10, 5), Position.POS_1);
        Hero mvp = RunSummaryScreen.findMvp(List.of(h1, h2));
        assertThat(mvp).isNull();
    }

    @Test
    void findMvp_emptyParty_returnsNull() {
        assertThat(RunSummaryScreen.findMvp(List.of())).isNull();
    }

    @Test
    void findMvp_singleHero_returnsThatHero() {
        Hero h = heroWithDamage("solo", 25);
        assertThat(RunSummaryScreen.findMvp(List.of(h))).isSameAs(h);
    }

    // ── RunState stats ───────────────────────────────────────────────────────

    @Test
    void runState_statsReflectAccumulatedValues() {
        RunState s = state(350, 12, 2);
        assertThat(s.gold()).isEqualTo(350);
        assertThat(s.enemiesKilled()).isEqualTo(12);
        assertThat(s.heirloomEarned()).isEqualTo(2);
    }
}
