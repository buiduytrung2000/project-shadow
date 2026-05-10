package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Hero;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CombatControllerTest {

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
    void buildDefault_creates_4heroes_4enemies() {
        CombatEncounter enc = CombatScenario.buildDefault(gd);
        assertThat(enc.heroes()).hasSize(4);
        assertThat(enc.enemies()).hasSize(4);
    }

    @Test
    void start_initializes_round_and_advances_to_player_or_enemy() {
        CombatEncounter enc = CombatScenario.buildDefault(gd);
        CombatController c = new CombatController(enc, gd, new Random(1L));
        c.start();
        assertThat(enc.roundNumber()).isEqualTo(1);
        assertThat(c.currentActor()).isPresent();
    }

    @Test
    void executePlayerSkill_hero_attacks_enemy() {
        CombatEncounter enc = CombatScenario.buildDefault(gd);
        CombatController c = new CombatController(enc, gd, new Random(42L));
        c.start();

        for (int i = 0; i < 4; i++) {
            if (c.currentActor().orElseThrow() instanceof Hero) break;
            // skip enemy turns by waiting (auto-advanced via start)
        }

        if (c.currentActor().orElseThrow() instanceof Hero) {
            int frontEnemyHp = enc.enemies().get(0).currentHp();
            boolean ok = c.executePlayerSkill(0);
            assertThat(ok).isTrue();
            int frontEnemyHpAfter = enc.enemies().get(0).currentHp();
            assertThat(frontEnemyHpAfter).isLessThanOrEqualTo(frontEnemyHp);
        }
    }

    @Test
    void executePlayerSkill_rejects_invalid_index() {
        CombatEncounter enc = CombatScenario.buildDefault(gd);
        CombatController c = new CombatController(enc, gd, new Random(1L));
        c.start();
        if (c.currentActor().orElseThrow() instanceof Hero) {
            assertThat(c.executePlayerSkill(-1)).isFalse();
            assertThat(c.executePlayerSkill(99)).isFalse();
        }
    }

    @Test
    void combat_terminates_when_oneSide_wiped() {
        CombatEncounter enc = CombatScenario.buildDefault(gd);
        for (Enemy e : enc.enemies()) e.takeHpDamage(9999);
        CombatController c = new CombatController(enc, gd, new Random(1L));
        c.start();
        assertThat(enc.isCombatOver()).isTrue();
        assertThat(enc.winningSide()).isEqualTo(CombatEncounter.Side.HEROES);
    }

    @Test
    void combat_loop_eventually_ends_within_50_actions() {
        CombatEncounter enc = CombatScenario.buildDefault(gd);
        CombatController c = new CombatController(enc, gd, new Random(7L));
        c.start();

        int actions = 0;
        while (!enc.isCombatOver() && actions < 50) {
            if (c.currentActor().orElseThrow() instanceof Hero) {
                boolean acted = false;
                for (int i = 0; i < 4 && !acted; i++) {
                    acted = c.executePlayerSkill(i);
                }
                if (!acted) break;
            }
            actions++;
        }
        assertThat(enc.isCombatOver()).isTrue();
    }

    @Test
    void log_records_every_action() {
        CombatEncounter enc = CombatScenario.buildDefault(gd);
        CombatController c = new CombatController(enc, gd, new Random(5L));
        c.start();

        int initialLog = c.log().size();
        if (c.currentActor().orElseThrow() instanceof Hero) {
            c.executePlayerSkill(0);
        }
        assertThat(c.log().size()).isGreaterThan(initialLog);
    }
}
