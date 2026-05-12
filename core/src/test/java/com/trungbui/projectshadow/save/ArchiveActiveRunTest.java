package com.trungbui.projectshadow.save;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.run.RunSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fix D (post-Sprint-13) — {@link SaveManager#archiveActiveRun()} archives all
 * active saves, simulating the "Bắt đầu mới" (new game) flow that must clear
 * the active run slot without deleting saves (permadeath archive contract).
 */
class ArchiveActiveRunTest {

    @TempDir Path tempDir;

    private GameData gd;
    private SaveManager saveManager;

    @BeforeAll
    static void loadStatic() {}

    @BeforeEach
    void setUp() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        if (dataDir == null) throw new IllegalStateException("Cannot find assets/data");
        gd = GameData.loadFromDirectory(dataDir);
        saveManager = new SaveManager(tempDir.resolve("saves-" + System.nanoTime()));
    }

    @Test
    void archiveActiveRun_noActiveSaves_returnsZero() throws IOException {
        assertThat(saveManager.listActiveSaves()).isEmpty();
        int archived = saveManager.archiveActiveRun();
        assertThat(archived).isZero();
    }

    @Test
    void archiveActiveRun_oneActiveSave_archivesIt() throws IOException {
        // Create an active run by starting a session and completing a node.
        RunSession run = RunSession.startNew(gd, saveManager, "stage_1", 42L,
                List.of("hero_01", "hero_13"));
        String firstNode = run.reachableNextNodes().get(0).label();
        run.completeNode(firstNode); // writes run_<id>.json to active saves dir

        assertThat(saveManager.listActiveSaves()).hasSize(1);

        // Archive active runs — simulates "Bắt đầu mới"
        int archived = saveManager.archiveActiveRun();
        assertThat(archived).isEqualTo(1);

        // Active saves are now empty
        assertThat(saveManager.listActiveSaves()).isEmpty();

        // Archived save is present (not deleted)
        assertThat(saveManager.listArchivedSaves()).hasSize(1);
    }

    @Test
    void archiveActiveRun_multipleSaves_archivesAll() throws IOException {
        // Create two runs
        RunSession run1 = RunSession.startNew(gd, saveManager, "stage_1", 1L,
                List.of("hero_01", "hero_13"));
        run1.completeNode(run1.reachableNextNodes().get(0).label());

        RunSession run2 = RunSession.startNew(gd, saveManager, "stage_1", 2L,
                List.of("hero_01", "hero_13"));
        run2.completeNode(run2.reachableNextNodes().get(0).label());

        assertThat(saveManager.listActiveSaves()).hasSize(2);

        int archived = saveManager.archiveActiveRun();
        assertThat(archived).isEqualTo(2);
        assertThat(saveManager.listActiveSaves()).isEmpty();
        assertThat(saveManager.listArchivedSaves()).hasSize(2);
    }
}
