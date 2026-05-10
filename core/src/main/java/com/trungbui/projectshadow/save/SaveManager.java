package com.trungbui.projectshadow.save;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SaveManager {

    private static final String FILE_PREFIX = "run_";
    private static final String FILE_SUFFIX = ".json";
    private static final String ARCHIVE_DIRNAME = "archive";

    private final Path baseDir;
    private final Path archiveDir;
    private final JsonMapper mapper;

    public SaveManager(Path baseDir) {
        if (baseDir == null) throw new IllegalArgumentException("baseDir must not be null");
        this.baseDir = baseDir;
        this.archiveDir = baseDir.resolve(ARCHIVE_DIRNAME);
        this.mapper = JsonMapper.builder()
                .findAndAddModules()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    public Path baseDir() {
        return baseDir;
    }

    public Path save(RunState state) throws IOException {
        if (state == null) throw new IllegalArgumentException("state must not be null");
        Files.createDirectories(baseDir);
        Path file = filePathFor(state.runId(), false);
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            mapper.writeValue(w, state);
        }
        return file;
    }

    public RunState load(String runId) throws IOException {
        Path file = filePathFor(runId, false);
        if (!Files.exists(file)) {
            file = filePathFor(runId, true);
        }
        if (!Files.exists(file)) {
            throw new IOException("Save not found: " + runId);
        }
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return mapper.readValue(r, RunState.class);
        }
    }

    public List<String> listActiveSaves() throws IOException {
        return listIn(baseDir);
    }

    public List<String> listArchivedSaves() throws IOException {
        return listIn(archiveDir);
    }

    private List<String> listIn(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        List<String> ids = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX);
                    })
                    .map(p -> {
                        String name = p.getFileName().toString();
                        return name.substring(FILE_PREFIX.length(), name.length() - FILE_SUFFIX.length());
                    })
                    .sorted()
                    .forEach(ids::add);
        }
        return List.copyOf(ids);
    }

    public boolean archive(String runId) throws IOException {
        Path source = filePathFor(runId, false);
        if (!Files.exists(source)) return false;
        Files.createDirectories(archiveDir);
        Path target = filePathFor(runId, true);
        Files.move(source, target);
        return true;
    }

    public boolean delete(String runId) throws IOException {
        return Files.deleteIfExists(filePathFor(runId, false));
    }

    public boolean deleteArchived(String runId) throws IOException {
        return Files.deleteIfExists(filePathFor(runId, true));
    }

    public boolean exists(String runId) {
        return Files.exists(filePathFor(runId, false))
                || Files.exists(filePathFor(runId, true));
    }

    private Path filePathFor(String runId, boolean archived) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        Path dir = archived ? archiveDir : baseDir;
        return dir.resolve(FILE_PREFIX + runId + FILE_SUFFIX);
    }
}
