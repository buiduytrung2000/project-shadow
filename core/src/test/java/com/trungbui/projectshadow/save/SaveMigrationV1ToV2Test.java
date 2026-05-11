package com.trungbui.projectshadow.save;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.trungbui.projectshadow.meta.MetaState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 10 B1 — v1→v2 migration for {@link MetaState}.
 *
 * <p>The schema added 3 fields: {@code heirloom}, {@code buildingLevels},
 * {@code cureSlotsUsedThisVisit}. v1 saves get default values via the
 * compact constructor + migration arm in {@link SaveMigration#loadMeta}.</p>
 */
class SaveMigrationV1ToV2Test {

    private final JsonMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    @Test
    void v1JsonWithoutNewFields_getsDefaults() throws Exception {
        String v1Json = """
                {
                  "saveVersion": 1,
                  "gold": 150,
                  "roster": [],
                  "trinketInventory": ["item_t02"],
                  "createdAt": "2026-04-01T00:00:00Z",
                  "lastSavedAt": "2026-04-01T00:00:00Z"
                }
                """;
        MetaState m = SaveMigration.loadMeta(mapper, v1Json);
        assertThat(m.saveVersion()).isEqualTo(SaveMigration.CURRENT_META_VERSION);
        assertThat(m.gold()).isEqualTo(150);
        assertThat(m.trinketInventory()).containsExactly("item_t02");

        // New fields: defaults applied
        assertThat(m.heirloom()).isZero();
        assertThat(m.cureSlotsUsedThisVisit()).isZero();
        assertThat(m.buildingLevel(MetaState.B_STAGECOACH)).isEqualTo(1);
        assertThat(m.buildingLevel(MetaState.B_GUILD)).isEqualTo(1);
        assertThat(m.buildingLevel(MetaState.B_SURVIVALIST)).isEqualTo(1);
        assertThat(m.buildingLevel(MetaState.B_CARETAKER)).isEqualTo(1);
    }

    @Test
    void v1JsonRoundTrip_writesV2OnNextSave() throws Exception {
        String v1Json = """
                {
                  "saveVersion": 1,
                  "gold": 300,
                  "roster": [],
                  "trinketInventory": [],
                  "createdAt": "2026-04-01T00:00:00Z",
                  "lastSavedAt": "2026-04-01T00:00:00Z"
                }
                """;
        MetaState loaded = SaveMigration.loadMeta(mapper, v1Json);
        String serialized = mapper.writeValueAsString(loaded);
        assertThat(serialized).contains("\"saveVersion\" : 2");
        assertThat(serialized).contains("\"heirloom\" : 0");
        assertThat(serialized).contains("\"buildingLevels\"");

        // Reload — clean v2 round-trip.
        MetaState reloaded = SaveMigration.loadMeta(mapper, serialized);
        assertThat(reloaded.saveVersion()).isEqualTo(2);
        assertThat(reloaded.gold()).isEqualTo(300);
    }

    @Test
    void v2JsonWithExplicitFields_preservesValues() throws Exception {
        String v2Json = """
                {
                  "saveVersion": 2,
                  "gold": 500,
                  "heirloom": 4,
                  "roster": [],
                  "trinketInventory": [],
                  "buildingLevels": { "stagecoach": 2, "guild": 3, "survivalist": 1, "caretaker": 2 },
                  "cureSlotsUsedThisVisit": 1,
                  "createdAt": "2026-05-11T00:00:00Z",
                  "lastSavedAt": "2026-05-11T00:00:00Z"
                }
                """;
        MetaState m = SaveMigration.loadMeta(mapper, v2Json);
        assertThat(m.heirloom()).isEqualTo(4);
        assertThat(m.buildingLevel(MetaState.B_STAGECOACH)).isEqualTo(2);
        assertThat(m.buildingLevel(MetaState.B_GUILD)).isEqualTo(3);
        assertThat(m.cureSlotsUsedThisVisit()).isEqualTo(1);
    }
}
