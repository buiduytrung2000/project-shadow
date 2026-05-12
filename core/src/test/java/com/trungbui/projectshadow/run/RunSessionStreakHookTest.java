package com.trungbui.projectshadow.run;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.save.SaveManager;
import com.trungbui.projectshadow.stage.NodeType;
import com.trungbui.projectshadow.stage.StageNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 13 B2 — streak hook in {@link RunSession#completeNode(String)}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RunSessionStreakHookTest {

    @TempDir
    Path tempBaseDir;

    private GameData gd;
    private SaveManager saveManager;

    @BeforeAll
    void loadAll() throws Exception {
        gd = GameData.loadFromDirectory(resolveDataDir());
    }

    @BeforeEach
    void freshSaveDir() {
        saveManager = new SaveManager(tempBaseDir.resolve("saves-" + System.nanoTime()));
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

    private static final List<String> PARTY = List.of("hero_01", "hero_13", "hero_05", "hero_03");

    private RunSession newRun() {
        return RunSession.startNew(gd, saveManager, "stage_1", 42L, PARTY);
    }

    @Test
    void freshRun_streak_isZero() {
        assertThat(newRun().state().consecutiveNodesCleared()).isZero();
    }

    @Test
    void completeNonRestNode_incrementsStreak() throws IOException {
        RunSession run = newRun();
        // Find the first non-rest node in stage_1.
        StageNode first = run.stageTree().firstNode().get();
        // First node in stage_1 is a combat node — verify it's non-REST.
        assertThat(first.type()).isNotEqualTo(NodeType.REST);
        run.completeNode(first.label());
        assertThat(run.state().consecutiveNodesCleared()).isEqualTo(1);
    }

    @Test
    void completeMultipleNonRestNodes_streakAccumulates() throws IOException {
        RunSession run = newRun();
        // Advance through first two nodes (both non-REST in stage_1 layer_1).
        StageNode n1 = run.stageTree().firstNode().get();
        run.completeNode(n1.label());
        assertThat(run.state().consecutiveNodesCleared()).isEqualTo(1);

        // Get next node
        List<StageNode> next = run.reachableNextNodes();
        if (!next.isEmpty() && next.get(0).type() != NodeType.REST) {
            run.completeNode(next.get(0).label());
            assertThat(run.state().consecutiveNodesCleared()).isEqualTo(2);
        }
    }

    @Test
    void recordEnemiesKilled_accumulatesInState() {
        RunSession run = newRun();
        run.recordEnemiesKilled(3);
        run.recordEnemiesKilled(2);
        assertThat(run.state().enemiesKilled()).isEqualTo(5);
    }

    @Test
    void recordHeirloomEarned_accumulatesInState() {
        RunSession run = newRun();
        run.recordHeirloomEarned(1);
        assertThat(run.state().heirloomEarned()).isEqualTo(1);
    }
}
