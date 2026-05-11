package com.trungbui.projectshadow.save;

import com.trungbui.projectshadow.domain.Hero;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 9+ B3 — verifies the atomic-write contract for {@link SaveManager#save}.
 * The implementation writes to a {@code .tmp} sibling then moves it into place,
 * so a partial write can never leave the final file half-written.
 */
class SaveManagerAtomicWriteTest {

    @Test
    void save_finalFileIsAlwaysFullyWritten(@TempDir Path tmp) throws Exception {
        SaveManager mgr = new SaveManager(tmp);
        RunState run = RunState.newRun("stage_1", 42L, List.<Hero>of());
        Path saved = mgr.save(run);

        // 1. Final file exists and is complete (valid JSON, parses back).
        assertThat(Files.exists(saved)).isTrue();
        RunState round = mgr.load(run.runId());
        assertThat(round.runId()).isEqualTo(run.runId());

        // 2. No leftover .tmp sibling — the move-into-place cleaned up.
        try (var stream = Files.list(tmp)) {
            assertThat(stream.map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith(".tmp"))
                    .toList())
                    .as("no .tmp file should linger after a successful save")
                    .isEmpty();
        }
    }

    @Test
    void save_overwriteReplacesAtomically(@TempDir Path tmp) throws Exception {
        SaveManager mgr = new SaveManager(tmp);
        RunState a = RunState.newRun("stage_1", 1L, List.<Hero>of()).withGold(50);
        RunState b = a.withGold(150); // same runId, different state

        Path first = mgr.save(a);
        Path second = mgr.save(b);

        assertThat(first).isEqualTo(second);
        RunState loaded = mgr.load(a.runId());
        // Atomic replace must yield the LATEST state, not a stale one.
        assertThat(loaded.gold()).isEqualTo(150);
    }

    @Test
    void load_failsCleanlyOnMissingFile(@TempDir Path tmp) throws IOException {
        SaveManager mgr = new SaveManager(tmp);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mgr.load("nonexistent-id"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("nonexistent-id");
    }
}
