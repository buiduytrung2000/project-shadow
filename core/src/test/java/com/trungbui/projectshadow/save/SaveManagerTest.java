package com.trungbui.projectshadow.save;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveManagerTest {

    @TempDir
    Path tempBaseDir;

    private GameData gd;
    private SaveManager manager;

    @BeforeAll
    void loadAll() throws Exception {
        gd = GameData.loadFromDirectory(resolveDataDir());
    }

    @BeforeEach
    void freshManager() {
        manager = new SaveManager(tempBaseDir.resolve("saves-" + System.nanoTime()));
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

    private List<Hero> sampleParty() {
        return List.of(
                new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects()),
                new Hero(gd.heroes().get("hero_03"), Position.POS_3, gd.effects())
        );
    }

    @Test
    void save_writesFileToBaseDir() throws IOException {
        RunState state = RunState.newRun("stage_1", 42L, sampleParty());
        Path file = manager.save(state);
        assertThat(Files.exists(file)).isTrue();
        assertThat(file.getFileName().toString()).startsWith("run_").endsWith(".json");
        assertThat(file.getParent()).isEqualTo(manager.baseDir());
    }

    @Test
    void save_then_load_roundTripsAllFields() throws IOException {
        RunState original = RunState.newRun("stage_1", 42L, sampleParty())
                .withGold(150)
                .withInventory(List.of("item_c01", "item_c03"))
                .withCurrentNode("L2.A");

        manager.save(original);
        RunState loaded = manager.load(original.runId());

        assertThat(loaded.runId()).isEqualTo(original.runId());
        assertThat(loaded.stageId()).isEqualTo("stage_1");
        assertThat(loaded.stageSeed()).isEqualTo(42L);
        assertThat(loaded.currentNodeLabel()).isEqualTo("L2.A");
        assertThat(loaded.gold()).isEqualTo(150);
        assertThat(loaded.inventory()).containsExactly("item_c01", "item_c03");
        assertThat(loaded.party()).hasSize(2);
        assertThat(loaded.party().get(0).heroId()).isEqualTo("hero_01");
        assertThat(loaded.party().get(1).heroId()).isEqualTo("hero_03");
        assertThat(loaded.archived()).isFalse();
    }

    @Test
    void load_throws_whenSaveDoesNotExist() {
        assertThatThrownBy(() -> manager.load("nonexistent-id"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Save not found");
    }

    @Test
    void listActiveSaves_returnsSavedRunIds_sorted() throws IOException {
        RunState a = RunState.newRun("stage_1", 1L, sampleParty());
        RunState b = RunState.newRun("stage_1", 2L, sampleParty());
        manager.save(a);
        manager.save(b);

        List<String> ids = manager.listActiveSaves();
        assertThat(ids).containsExactlyInAnyOrder(a.runId(), b.runId());
    }

    @Test
    void listActiveSaves_returnsEmpty_whenBaseDirMissing() throws IOException {
        SaveManager freshMgr = new SaveManager(tempBaseDir.resolve("never-created"));
        assertThat(freshMgr.listActiveSaves()).isEmpty();
        assertThat(freshMgr.listArchivedSaves()).isEmpty();
    }

    @Test
    void archive_movesFileToArchiveSubdir() throws IOException {
        RunState state = RunState.newRun("stage_1", 7L, sampleParty());
        manager.save(state);
        assertThat(manager.listActiveSaves()).contains(state.runId());

        boolean ok = manager.archive(state.runId());
        assertThat(ok).isTrue();
        assertThat(manager.listActiveSaves()).doesNotContain(state.runId());
        assertThat(manager.listArchivedSaves()).contains(state.runId());
    }

    @Test
    void archive_returnsFalse_whenSaveNotFound() throws IOException {
        assertThat(manager.archive("nope")).isFalse();
    }

    @Test
    void load_findsArchivedSaveAfterArchive() throws IOException {
        RunState state = RunState.newRun("stage_1", 7L, sampleParty()).markArchived();
        manager.save(state);
        manager.archive(state.runId());

        RunState reloaded = manager.load(state.runId());
        assertThat(reloaded.runId()).isEqualTo(state.runId());
    }

    @Test
    void delete_removesActiveSave() throws IOException {
        RunState state = RunState.newRun("stage_1", 1L, sampleParty());
        manager.save(state);
        assertThat(manager.delete(state.runId())).isTrue();
        assertThat(manager.exists(state.runId())).isFalse();
    }

    @Test
    void delete_returnsFalse_whenNotFound() throws IOException {
        assertThat(manager.delete("nope")).isFalse();
    }

    @Test
    void exists_findsBothActiveAndArchived() throws IOException {
        RunState state = RunState.newRun("stage_1", 1L, sampleParty());
        manager.save(state);
        assertThat(manager.exists(state.runId())).isTrue();
        manager.archive(state.runId());
        assertThat(manager.exists(state.runId())).isTrue();
    }

    @Test
    void runState_isPartyDead_whenAllHpZero() {
        Hero h1 = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        Hero h2 = new Hero(gd.heroes().get("hero_03"), Position.POS_3, gd.effects());
        h1.takeHpDamage(9999);
        h2.takeHpDamage(9999);

        RunState state = RunState.newRun("stage_1", 1L, List.of(h1, h2));
        assertThat(state.isPartyDead()).isTrue();
    }

    @Test
    void runState_isPartyDead_falseIfAnyAlive() {
        Hero h1 = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        Hero h2 = new Hero(gd.heroes().get("hero_03"), Position.POS_3, gd.effects());
        h1.takeHpDamage(9999);

        RunState state = RunState.newRun("stage_1", 1L, List.of(h1, h2));
        assertThat(state.isPartyDead()).isFalse();
    }

    @Test
    void withCurrentNode_addsPreviousToVisited() {
        RunState state = RunState.newRun("stage_1", 1L, sampleParty());
        state = state.withCurrentNode("L1.A");
        state = state.withCurrentNode("L2.A");
        state = state.withCurrentNode("L3.A");
        assertThat(state.visitedNodes()).containsExactly("L1.A", "L2.A");
        assertThat(state.currentNodeLabel()).isEqualTo("L3.A");
    }

    @Test
    void markArchived_setsFlagTrue_andUpdatesLastSavedAt() throws InterruptedException {
        RunState state = RunState.newRun("stage_1", 1L, sampleParty());
        Instant originalSave = state.lastSavedAt();
        Thread.sleep(5);
        RunState archived = state.markArchived();
        assertThat(archived.archived()).isTrue();
        assertThat(archived.lastSavedAt()).isAfterOrEqualTo(originalSave);
    }

    @Test
    void save_then_archive_then_load_returnsArchivedFlag() throws IOException {
        RunState state = RunState.newRun("stage_1", 1L, sampleParty()).markArchived();
        manager.save(state);
        manager.archive(state.runId());
        RunState loaded = manager.load(state.runId());
        assertThat(loaded.archived()).isTrue();
    }
}
