package com.trungbui.projectshadow.save;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 13 B2 — {@link SaveMigration} v1→v2 for {@link RunState}.
 *
 * <p>v2 adds {@code consecutiveNodesCleared}, {@code enemiesKilled},
 * {@code heirloomEarned}. Legacy v1 saves get defaults (0) during load.
 * Saves from version > 2 must be refused.</p>
 */
class RunStateSaveMigrationV1ToV2Test {

    private final JsonMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    @Test
    void v1Json_getsDefaultsForNewFields() throws Exception {
        String v1Json = """
                {
                  "saveVersion": 1,
                  "runId": "run-v1",
                  "stageId": "stage_1",
                  "stageSeed": 99,
                  "currentNodeLabel": null,
                  "visitedNodes": [],
                  "party": [],
                  "gold": 100,
                  "inventory": [],
                  "createdAt": "2026-05-01T00:00:00Z",
                  "lastSavedAt": "2026-05-01T00:00:00Z",
                  "archived": false
                }
                """;
        RunState s = SaveMigration.loadRun(mapper, v1Json);

        assertThat(s.saveVersion()).isEqualTo(SaveMigration.CURRENT_RUN_VERSION);
        assertThat(s.gold()).isEqualTo(100);
        // New fields — must default to 0.
        assertThat(s.consecutiveNodesCleared()).isZero();
        assertThat(s.enemiesKilled()).isZero();
        assertThat(s.heirloomEarned()).isZero();
    }

    @Test
    void v1JsonRoundTrip_writesCurrentVersionOnNextSave() throws Exception {
        String v1Json = """
                {
                  "saveVersion": 1,
                  "runId": "run-rt",
                  "stageId": "stage_1",
                  "stageSeed": 42,
                  "currentNodeLabel": null,
                  "visitedNodes": [],
                  "party": [],
                  "gold": 50,
                  "inventory": [],
                  "createdAt": "2026-05-01T00:00:00Z",
                  "lastSavedAt": "2026-05-01T00:00:00Z",
                  "archived": false
                }
                """;
        RunState loaded = SaveMigration.loadRun(mapper, v1Json);
        String serialized = mapper.writeValueAsString(loaded);

        assertThat(serialized).contains("\"saveVersion\" : " + SaveMigration.CURRENT_RUN_VERSION);
        assertThat(serialized).contains("consecutiveNodesCleared");
        assertThat(serialized).contains("enemiesKilled");
        assertThat(serialized).contains("heirloomEarned");
        assertThat(serialized).contains("heroDamageDealt");

        // Reload from current version — clean round-trip.
        RunState reloaded = SaveMigration.loadRun(mapper, serialized);
        assertThat(reloaded.saveVersion()).isEqualTo(SaveMigration.CURRENT_RUN_VERSION);
        assertThat(reloaded.gold()).isEqualTo(50);
    }

    @Test
    void v2Json_preservesValues() throws Exception {
        String v2Json = """
                {
                  "saveVersion": 2,
                  "runId": "run-v2",
                  "stageId": "stage_1",
                  "stageSeed": 7,
                  "currentNodeLabel": null,
                  "visitedNodes": [],
                  "party": [],
                  "gold": 200,
                  "inventory": [],
                  "createdAt": "2026-05-12T00:00:00Z",
                  "lastSavedAt": "2026-05-12T00:00:00Z",
                  "archived": false,
                  "consecutiveNodesCleared": 3,
                  "enemiesKilled": 7,
                  "heirloomEarned": 1
                }
                """;
        RunState s = SaveMigration.loadRun(mapper, v2Json);

        assertThat(s.consecutiveNodesCleared()).isEqualTo(3);
        assertThat(s.enemiesKilled()).isEqualTo(7);
        assertThat(s.heirloomEarned()).isEqualTo(1);
    }

    @Test
    void vFuture_throws() {
        String futureJson = """
                {
                  "saveVersion": 99,
                  "runId": "run-future",
                  "stageId": "stage_1",
                  "stageSeed": 0,
                  "currentNodeLabel": null,
                  "visitedNodes": [],
                  "party": [],
                  "gold": 0,
                  "inventory": [],
                  "createdAt": "2030-01-01T00:00:00Z",
                  "lastSavedAt": "2030-01-01T00:00:00Z",
                  "archived": false
                }
                """;
        assertThatThrownBy(() -> SaveMigration.loadRun(mapper, futureJson))
                .isInstanceOf(SaveMigration.IncompatibleSaveException.class)
                .hasMessageContaining("saveVersion=99");
    }
}
