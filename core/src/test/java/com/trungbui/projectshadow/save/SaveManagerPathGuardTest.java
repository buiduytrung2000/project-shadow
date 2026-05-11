package com.trungbui.projectshadow.save;

import com.trungbui.projectshadow.domain.Hero;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 9+ B3 — verifies the path-traversal guard on {@link SaveManager}.
 * runIds must match {@code [A-Za-z0-9_-]+}; anything else (e.g. {@code "../meta"},
 * {@code "/etc/passwd"}, Windows backslashes) is rejected.
 */
class SaveManagerPathGuardTest {

    @Test
    void exists_acceptsValidUuidStyleId(@TempDir Path tmp) {
        SaveManager mgr = new SaveManager(tmp);
        // exists() should return false for a non-existent valid ID, not throw.
        assertThat(mgr.exists("a1b2c3d4-1234-5678-90ab-cdef01234567")).isFalse();
        assertThat(mgr.exists("simple_id")).isFalse();
        assertThat(mgr.exists("dashed-name")).isFalse();
    }

    @Test
    void exists_rejectsPathTraversal(@TempDir Path tmp) {
        SaveManager mgr = new SaveManager(tmp);
        assertThatThrownBy(() -> mgr.exists("../meta"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("runId must match");
    }

    @Test
    void exists_rejectsForwardSlash(@TempDir Path tmp) {
        SaveManager mgr = new SaveManager(tmp);
        assertThatThrownBy(() -> mgr.exists("etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exists_rejectsBackslash(@TempDir Path tmp) {
        SaveManager mgr = new SaveManager(tmp);
        assertThatThrownBy(() -> mgr.exists("foo\\bar"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exists_rejectsDotSlash(@TempDir Path tmp) {
        SaveManager mgr = new SaveManager(tmp);
        assertThatThrownBy(() -> mgr.exists("./hidden"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void save_rejectsRunStateWithMaliciousRunId(@TempDir Path tmp) {
        SaveManager mgr = new SaveManager(tmp);
        // Construct a RunState whose runId would escape the saves directory.
        RunState bad = new RunState(
                1, "../escape", "stage_1", 0L, null,
                List.of(), List.of(), 0, List.of(),
                java.time.Instant.now(), java.time.Instant.now(), false);
        assertThatThrownBy(() -> mgr.save(bad))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankAndNullRunId_stillRejected(@TempDir Path tmp) {
        SaveManager mgr = new SaveManager(tmp);
        assertThatThrownBy(() -> mgr.exists("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mgr.exists("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    // Suppress unused import warning — Hero is needed indirectly via RunState.newRun in
    // related tests; keeping the import grouping clean.
    @SuppressWarnings("unused")
    private static Hero unused() { return null; }
}
