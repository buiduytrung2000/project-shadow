package com.trungbui.projectshadow.meta;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.trungbui.projectshadow.data.GameData;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Sprint 8 — disk persistence for {@link MetaState} at {@code <baseDir>/meta.json}.
 *
 * <p>Unlike {@link com.trungbui.projectshadow.save.SaveManager} which stores per-run
 * snapshots, only one {@code meta.json} exists per profile.</p>
 */
public class MetaStateManager {

    private static final String FILE_NAME = "meta.json";

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
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return mapper.readValue(r, MetaState.class);
        }
    }

    public void save(MetaState state) throws IOException {
        if (state == null) throw new IllegalArgumentException("state must not be null");
        Files.createDirectories(file.getParent());
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            mapper.writeValue(w, state);
        }
    }
}
