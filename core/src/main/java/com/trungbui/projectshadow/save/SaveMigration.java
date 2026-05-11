package com.trungbui.projectshadow.save;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trungbui.projectshadow.meta.MetaState;

import java.io.IOException;

/**
 * Sprint 9+ B3 — JSON-save schema versioning utility.
 *
 * <p>Adds a {@code saveVersion} envelope check around {@link RunState} and
 * {@link MetaState} loads. Refuses to deserialize a file whose
 * {@code saveVersion} is higher than the running game's
 * {@link #CURRENT_RUN_VERSION} / {@link #CURRENT_META_VERSION} — preventing
 * silent data loss when a player loads a save from a newer build.</p>
 *
 * <p>Backward compat: saves shipped before this field existed deserialize with
 * {@code saveVersion = 0}, which the record's compact constructor normalizes
 * to {@code 1} (treated as the current version). This keeps Sprint 9-era saves
 * loadable without migration.</p>
 *
 * <p>The class is structured to make adding migrations (1 → 2, 2 → 3, …) a
 * matter of inserting a switch arm in {@link #migrateRun} / {@link #migrateMeta}.</p>
 */
public final class SaveMigration {

    /** Current schema version for {@link RunState}. */
    public static final int CURRENT_RUN_VERSION = 1;

    /** Current schema version for {@link MetaState}. Bumped 1→2 in Sprint 10 B1
     *  when {@code heirloom} + {@code buildingLevels} + {@code cureSlotsUsedThisVisit}
     *  fields were added. Legacy v1 saves get default values during load via the
     *  v1→v2 migration arm below. */
    public static final int CURRENT_META_VERSION = 2;

    private SaveMigration() {
    }

    /**
     * Read a {@link RunState} JSON envelope and refuse if its version is newer
     * than this build supports. Older versions are migrated through the chain
     * (currently a no-op since only v1 exists).
     *
     * @throws IncompatibleSaveException if {@code saveVersion > CURRENT_RUN_VERSION}
     * @throws IOException on parse errors
     */
    public static RunState loadRun(ObjectMapper mapper, String json) throws IOException {
        JsonNode root = mapper.readTree(json);
        int version = root.path("saveVersion").asInt(1); // missing field → legacy v1
        if (version > CURRENT_RUN_VERSION) {
            throw new IncompatibleSaveException(
                    "RunState saveVersion=" + version
                            + " is newer than supported (" + CURRENT_RUN_VERSION + ")"
                            + ". This save was created by a newer build of the game.");
        }
        // Future: chain v1 → v2 → … migrations here before final deserialize.
        return mapper.treeToValue(root, RunState.class);
    }

    /**
     * Read a {@link MetaState} JSON envelope with the same versioning rules
     * as {@link #loadRun}.
     */
    public static MetaState loadMeta(ObjectMapper mapper, String json) throws IOException {
        JsonNode root = mapper.readTree(json);
        int version = root.path("saveVersion").asInt(1);
        if (version > CURRENT_META_VERSION) {
            throw new IncompatibleSaveException(
                    "MetaState saveVersion=" + version
                            + " is newer than supported (" + CURRENT_META_VERSION + ")"
                            + ". This save was created by a newer build of the game.");
        }
        // Sprint 10 B1 — v1→v2 migration: legacy v1 saves don't have heirloom /
        // buildingLevels / cureSlotsUsedThisVisit. Jackson will default the missing
        // fields (ints → 0, Map → null); MetaState's compact constructor then
        // back-fills (heirloom=0, buildingLevels=all-Lv1, cureSlots=0). We just
        // bump the version field unconditionally so the loaded record reports v2.
        if (version < CURRENT_META_VERSION && root.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) root)
                    .put("saveVersion", CURRENT_META_VERSION);
        }
        return mapper.treeToValue(root, MetaState.class);
    }

    /** Thrown when a save file's {@code saveVersion} is newer than the current build supports. */
    public static final class IncompatibleSaveException extends IOException {
        public IncompatibleSaveException(String msg) {
            super(msg);
        }
    }
}
