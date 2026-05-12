package com.trungbui.projectshadow.run;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.save.RunState;
import com.trungbui.projectshadow.save.SaveManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fix E (post-Sprint-13 bug pack) — MVP damage accumulator moves from
 * {@code Hero.currentRunDamage} (transient, resets on resume) to
 * {@code RunState.heroDamageDealt} (persisted, survive save/load).
 *
 * <p>Scenario 1: Multi-combat accumulation — damage from two simulated combats
 * sums correctly and the hero with the highest total wins MVP.</p>
 *
 * <p>Scenario 2: Save then resume — accumulated damage totals survive a
 * {@code SaveManager.save / RunSession.resume} round-trip.</p>
 */
class RunSessionMvpDamageAccumulationTest {

    @TempDir
    Path tempDir;

    private GameData gd;
    private SaveManager saveManager;

    @BeforeAll
    static void loadGameData() {
        // GameData is loaded per-instance so nothing is needed here.
    }

    @BeforeEach
    void setUp() throws Exception {
        gd = GameData.loadFromDirectory(resolveDataDir());
        saveManager = new SaveManager(tempDir.resolve("saves-" + System.nanoTime()));
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

    private static final List<String> PARTY = List.of("hero_01", "hero_13");

    // ── Scenario 1 ──────────────────────────────────────────────────────────

    @Test
    void scenario1_multiCombatAccumulation_heroAWins() {
        RunSession run = RunSession.startNew(gd, saveManager, "stage_1", 42L, PARTY);

        String heroAId = run.party().get(0).id(); // hero_01
        String heroBId = run.party().get(1).id(); // hero_13

        // Combat 1: A=20, B=30
        run.recordHeroDamage(heroAId, 20);
        run.recordHeroDamage(heroBId, 30);

        // Combat 2: A=30, B=0
        run.recordHeroDamage(heroAId, 30);

        Map<String, Integer> dmg = run.state().heroDamageDealt();
        assertThat(dmg.getOrDefault(heroAId, 0)).isEqualTo(50);
        assertThat(dmg.getOrDefault(heroBId, 0)).isEqualTo(30);

        // Hero A has more total damage → MVP
        String mvpId = dmg.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        assertThat(mvpId).isEqualTo(heroAId);
    }

    // ── Scenario 2 ──────────────────────────────────────────────────────────

    @Test
    void scenario2_saveAndResume_preservesDamage() throws IOException {
        RunSession run = RunSession.startNew(gd, saveManager, "stage_1", 42L, PARTY);

        String heroAId = run.party().get(0).id();
        String heroBId = run.party().get(1).id();

        run.recordHeroDamage(heroAId, 20);
        run.recordHeroDamage(heroBId, 30);

        // Complete a node to persist state to disk.
        String firstNodeLabel = run.reachableNextNodes().get(0).label();
        run.completeNode(firstNodeLabel);

        // Verify in-memory
        assertThat(run.state().heroDamageDealt().getOrDefault(heroAId, 0)).isEqualTo(20);
        assertThat(run.state().heroDamageDealt().getOrDefault(heroBId, 0)).isEqualTo(30);

        // Resume from disk
        String runId = run.state().runId();
        RunSession resumed = RunSession.resume(gd, saveManager, runId);

        Map<String, Integer> dmgAfterResume = resumed.state().heroDamageDealt();
        assertThat(dmgAfterResume.getOrDefault(heroAId, 0)).isEqualTo(20);
        assertThat(dmgAfterResume.getOrDefault(heroBId, 0)).isEqualTo(30);
    }
}
