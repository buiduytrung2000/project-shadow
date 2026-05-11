package com.trungbui.projectshadow.meta;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.save.SaveMigration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Sprint 8 — disk persistence for {@link MetaState} at {@code <baseDir>/meta.json}.
 *
 * <p>Unlike {@link com.trungbui.projectshadow.save.SaveManager} which stores per-run
 * snapshots, only one {@code meta.json} exists per profile.</p>
 *
 * <p>Sprint 9+ B3: writes are now atomic (tmp + ATOMIC_MOVE), and loads go through
 * {@link SaveMigration#loadMeta} so a save from a newer build is refused
 * cleanly instead of silently corrupting state.</p>
 */
public class MetaStateManager {

    private static final String FILE_NAME = "meta.json";
    private static final String TMP_SUFFIX = ".tmp";

    private final Path file;
    private final JsonMapper mapper;

    public MetaStateManager(Path baseDir) {
        if (baseDir == null) throw new IllegalArgumentException("baseDir must not be null");
        this.file = baseDir.resolve(FILE_NAME);
        this.mapper = JsonMapper.builder()
                .findAndAddModules()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    public Path file() {
        return file;
    }

    /** Loads existing meta state, or returns a fresh state seeded with the given heroes. */
    public MetaState loadOrInit(GameData gd, List<String> initialHeroIds) throws IOException {
        if (!Files.exists(file)) {
            return MetaState.fresh(gd, initialHeroIds);
        }
        String json = Files.readString(file, StandardCharsets.UTF_8);
        return SaveMigration.loadMeta(mapper, json);
    }

    public void save(MetaState state) throws IOException {
        if (state == null) throw new IllegalArgumentException("state must not be null");
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName().toString() + TMP_SUFFIX);
        byte[] bytes = mapper.writeValueAsBytes(state);
        Files.write(tmp, bytes);
        try {
            Files.move(tmp, file,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
