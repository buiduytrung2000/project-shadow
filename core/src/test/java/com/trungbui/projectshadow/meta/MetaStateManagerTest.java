package com.trungbui.projectshadow.meta;

import com.trungbui.projectshadow.data.GameData;
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetaStateManagerTest {

    @TempDir
    Path tempBaseDir;

    private GameData gd;
    private MetaStateManager mgr;

    @BeforeAll
    void loadAll() throws Exception {
        gd = GameData.loadFromDirectory(resolveDataDir());
    }

    @BeforeEach
    void freshMgr() {
        mgr = new MetaStateManager(tempBaseDir.resolve("saves-" + System.nanoTime()));
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
        throw new IllegalStateException("Cannot locate assets/data from cwd=" + Path.of("").toAbsolutePath());
    }

    @Test
    void loadOrInit_returnsFresh_whenNoFile() throws IOException {
        MetaState state = mgr.loadOrInit(gd, List.of("hero_01", "hero_13"));
        assertThat(state.gold()).isEqualTo(MetaState.FRESH_GOLD);
        assertThat(state.roster()).hasSize(2);
        assertThat(state.hasInRoster("hero_01")).isTrue();
    }

    @Test
    void save_then_loadOrInit_roundTrips() throws IOException {
        MetaState original = MetaState.fresh(gd, List.of("hero_01", "hero_03"))
                .withGold(500)
                .withTrinketInventory(List.of("item_t01"));
        mgr.save(original);

        MetaState loaded = mgr.loadOrInit(gd, List.of("hero_99")); // initial ignored, file exists
        assertThat(loaded.gold()).isEqualTo(500);
        assertThat(loaded.roster()).hasSize(2);
        assertThat(loaded.hasInRoster("hero_01")).isTrue();
        assertThat(loaded.trinketInventory()).containsExactly("item_t01");
    }

    @Test
    void save_writesIndentedJson_atMetaJson() throws IOException {
        MetaState state = MetaState.fresh(gd, List.of("hero_01"));
        mgr.save(state);
        assertThat(Files.exists(mgr.file())).isTrue();
        assertThat(mgr.file().getFileName().toString()).isEqualTo("meta.json");
        String content = Files.readString(mgr.file());
        assertThat(content).contains("\n  \"gold\""); // indented
    }

    @Test
    void save_createsParentDirIfMissing() throws IOException {
        MetaStateManager nested = new MetaStateManager(tempBaseDir.resolve("nested/deep/dir"));
        MetaState state = MetaState.fresh(gd, List.of("hero_01"));
        nested.save(state);
        assertThat(Files.exists(nested.file())).isTrue();
    }
}
