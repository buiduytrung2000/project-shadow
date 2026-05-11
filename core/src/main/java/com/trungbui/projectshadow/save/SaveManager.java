package com.trungbui.projectshadow.save;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SaveManager {

    private static final String FILE_PREFIX = "run_";
    private static final String FILE_SUFFIX = ".json";
    private static final String TMP_SUFFIX = ".tmp";
    private static final String ARCHIVE_DIRNAME = "archive";

    /** Sprint 9+ B3 — strict regex for path-traversal protection.
     *  Allows UUIDs and alphanumeric IDs; rejects {@code /}, {@code \}, {@code ..}, etc. */
    private static final Pattern VALID_RUN_ID = Pattern.compile("[A-Za-z0-9\\-_]+");

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

    /**
     * Save a {@link RunState} to {@code <baseDir>/run_<id>.json}.
     *
     * <p>Sprint 9+ B3 atomic write: serializes to a {@code .tmp} sibling first, then
     * {@code Files.move(tmp, final, ATOMIC_MOVE, REPLACE_EXISTING)} so a crash or
     * power loss mid-write can never leave a half-written, unparseable save file
     * (permadeath stakes — corrupted save = unrecoverable run).</p>
     *
     * <p>Filesystems that don't support {@code ATOMIC_MOVE} (rare; e.g. some SMB
     * mounts) fall back to a non-atomic {@code REPLACE_EXISTING} move.</p>
     */
    public Path save(RunState state) throws IOException {
        if (state == null) throw new IllegalArgumentException("state must not be null");
        Files.createDirectories(baseDir);
        Path file = filePathFor(state.runId(), false);
        Path tmp = file.resolveSibling(file.getFileName().toString() + TMP_SUFFIX);
        byte[] bytes = mapper.writeValueAsBytes(state);
        Files.write(tmp, bytes);
        try {
            Files.move(tmp, file,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
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
        // Sprint 9+ B3 — schema-version gated load. Refuses saves from a newer build.
        String json = Files.readString(file, StandardCharsets.UTF_8);
        return SaveMigration.loadRun(mapper, json);
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
        // REPLACE_EXISTING defends against a freak collision where an archived save
        // with the same UUID already exists (effectively impossible, but cheap).
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
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
        // Sprint 9+ B3 — path-traversal guard. Rejects ".." / "/" / "\" / any other
        // separator-bearing input. Today runId is always a UUID, but this future-proofs
        // against any path where runId might come from user input or a rename feature.
        if (!VALID_RUN_ID.matcher(runId).matches()) {
            throw new IllegalArgumentException(
                    "runId must match [A-Za-z0-9_-]+ (got: " + runId + ")");
        }
        Path dir = archived ? archiveDir : baseDir;
        return dir.resolve(FILE_PREFIX + runId + FILE_SUFFIX);
    }
}
