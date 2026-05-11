package com.trungbui.projectshadow.save;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.trungbui.projectshadow.meta.MetaState;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 9+ B3 — {@link SaveMigration} version-gated loading.
 */
class SaveMigrationTest {

    private final JsonMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    @Test
    void loadRun_currentVersion_succeeds() throws Exception {
        String json = """
                {
                  "saveVersion": 1,
                  "runId": "abc-123",
                  "stageId": "stage_1",
                  "stageSeed": 42,
                  "currentNodeLabel": null,
                  "visitedNodes": [],
                  "party": [],
                  "gold": 0,
                  "inventory": [],
                  "createdAt": "2026-05-11T00:00:00Z",
                  "lastSavedAt": "2026-05-11T00:00:00Z",
                  "archived": false
                }
                """;
        RunState s = SaveMigration.loadRun(mapper, json);
        assertThat(s.saveVersion()).isEqualTo(1);
        assertThat(s.runId()).isEqualTo("abc-123");
    }

    @Test
    void loadRun_legacyMissingVersion_loadsAsVersion1() throws Exception {
        // Pre-B3 saves had no saveVersion field at all. Must still load successfully.
        String json = """
                {
                  "runId": "legacy-001",
                  "stageId": "stage_1",
                  "stageSeed": 42,
                  "currentNodeLabel": null,
                  "visitedNodes": [],
                  "party": [],
                  "gold": 0,
                  "inventory": [],
                  "createdAt": "2026-05-01T00:00:00Z",
                  "lastSavedAt": "2026-05-01T00:00:00Z",
                  "archived": false
                }
                """;
        RunState s = SaveMigration.loadRun(mapper, json);
        // Compact constructor normalizes saveVersion=0 → 1.
        assertThat(s.saveVersion()).isEqualTo(1);
    }

    @Test
    void loadRun_newerVersion_throwsIncompatibleSave() {
        String json = """
                {
                  "saveVersion": 99,
                  "runId": "future-001",
                  "stageId": "stage_1",
                  "stageSeed": 42,
                  "currentNodeLabel": null,
                  "visitedNodes": [],
                  "party": [],
                  "gold": 0,
                  "inventory": [],
                  "createdAt": "2027-01-01T00:00:00Z",
                  "lastSavedAt": "2027-01-01T00:00:00Z",
                  "archived": false
                }
                """;
        assertThatThrownBy(() -> SaveMigration.loadRun(mapper, json))
                .isInstanceOf(SaveMigration.IncompatibleSaveException.class)
                .hasMessageContaining("saveVersion=99")
                .hasMessageContaining("newer than supported");
    }

    @Test
    void loadMeta_currentVersion_succeeds() throws Exception {
        // Sprint 10 B1: CURRENT_META_VERSION bumped 1→2. v2 saves include heirloom +
        // buildingLevels fields. v1 saves are auto-migrated; see legacy test below.
        String json = """
                {
                  "saveVersion": 2,
                  "gold": 100,
                  "heirloom": 0,
                  "roster": [],
                  "trinketInventory": [],
                  "buildingLevels": { "stagecoach": 1, "guild": 1, "survivalist": 1, "caretaker": 1 },
                  "cureSlotsUsedThisVisit": 0,
                  "createdAt": "2026-05-11T00:00:00Z",
                  "lastSavedAt": "2026-05-11T00:00:00Z"
                }
                """;
        MetaState m = SaveMigration.loadMeta(mapper, json);
        assertThat(m.saveVersion()).isEqualTo(2);
        assertThat(m.gold()).isEqualTo(100);
        assertThat(m.heirloom()).isZero();
        assertThat(m.buildingLevel("stagecoach")).isEqualTo(1);
    }

    @Test
    void loadMeta_legacyMissingVersion_migratesToCurrent() throws Exception {
        // Pre-B3 saves had no saveVersion field. Sprint 10 B1 migration bumps them
        // to CURRENT_META_VERSION (=2) so subsequent saves write the new schema.
        // Default values fill in for new fields (heirloom=0, buildingLevels=all-Lv1).
        String json = """
                {
                  "gold": 200,
                  "roster": [],
                  "trinketInventory": [],
                  "createdAt": "2026-04-15T00:00:00Z",
                  "lastSavedAt": "2026-04-15T00:00:00Z"
                }
                """;
        MetaState m = SaveMigration.loadMeta(mapper, json);
        assertThat(m.saveVersion()).isEqualTo(SaveMigration.CURRENT_META_VERSION);
        assertThat(m.heirloom()).isZero();
        assertThat(m.buildingLevel("stagecoach")).isEqualTo(1);
        assertThat(m.cureSlotsUsedThisVisit()).isZero();
    }

    @Test
    void loadMeta_newerVersion_throws() throws IOException {
        String json = """
                {
                  "saveVersion": 50,
                  "gold": 0,
                  "roster": [],
                  "trinketInventory": [],
                  "createdAt": "2027-01-01T00:00:00Z",
                  "lastSavedAt": "2027-01-01T00:00:00Z"
                }
                """;
        assertThatThrownBy(() -> SaveMigration.loadMeta(mapper, json))
                .isInstanceOf(SaveMigration.IncompatibleSaveException.class)
                .hasMessageContaining("saveVersion=50");
    }
}
