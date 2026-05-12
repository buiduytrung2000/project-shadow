package com.trungbui.projectshadow.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Sprint 12 B4 — persistent user settings (audio volume, fullscreen, locale).
 *
 * <p>Stored at {@code saves/settings.json}, separate from {@code meta.json}
 * so a "reset progress" doesn't wipe player preferences. Atomic write via
 * temp file + rename (same pattern as {@code SaveManager} / {@code MetaStateManager}).</p>
 *
 * <p>Pure logic: takes a {@link Path} directory in the constructor. No
 * libGDX file handle dependencies — testable from JUnit without a Gdx env.</p>
 */
public final class SettingsManager {

    public static final String FILE_NAME = "settings.json";

    public static final float DEFAULT_MUSIC_VOLUME = 0.6f;
    public static final float DEFAULT_SFX_VOLUME = 0.8f;
    public static final boolean DEFAULT_FULLSCREEN = false;
    public static final String DEFAULT_LOCALE = "vi";

    private final Path settingsFile;
    private final ObjectMapper mapper = new ObjectMapper();

    private float musicVolume = DEFAULT_MUSIC_VOLUME;
    private float sfxVolume = DEFAULT_SFX_VOLUME;
    private boolean fullscreen = DEFAULT_FULLSCREEN;
    private String locale = DEFAULT_LOCALE;

    public SettingsManager(Path savesDir) {
        this.settingsFile = savesDir.resolve(FILE_NAME);
    }

    /** Load settings from disk, falling back to defaults if file missing or malformed. */
    public void load() {
        if (!Files.isRegularFile(settingsFile)) return; // defaults stand
        try {
            JsonNode root = mapper.readTree(settingsFile.toFile());
            if (root == null || !root.isObject()) return;
            if (root.has("musicVolume")) musicVolume = clamp01(root.get("musicVolume").floatValue());
            if (root.has("sfxVolume")) sfxVolume = clamp01(root.get("sfxVolume").floatValue());
            if (root.has("fullscreen")) fullscreen = root.get("fullscreen").asBoolean(DEFAULT_FULLSCREEN);
            if (root.has("locale")) locale = root.get("locale").asText(DEFAULT_LOCALE);
        } catch (IOException ignored) {
            // Corrupted file — silently keep defaults. UI will offer a "Reset" button.
        }
    }

    /** Persist current settings atomically. */
    public void save() throws IOException {
        Files.createDirectories(settingsFile.getParent());
        ObjectNode root = mapper.createObjectNode();
        root.put("musicVolume", musicVolume);
        root.put("sfxVolume", sfxVolume);
        root.put("fullscreen", fullscreen);
        root.put("locale", locale);
        Path tmp = settingsFile.resolveSibling(FILE_NAME + ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), root);
        try {
            Files.move(tmp, settingsFile,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tmp, settingsFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public float musicVolume() { return musicVolume; }
    public float sfxVolume() { return sfxVolume; }
    public boolean fullscreen() { return fullscreen; }
    public String locale() { return locale; }

    public void setMusicVolume(float v) { musicVolume = clamp01(v); }
    public void setSfxVolume(float v) { sfxVolume = clamp01(v); }
    public void setFullscreen(boolean v) { fullscreen = v; }
    public void setLocale(String l) {
        if (l != null && !l.isBlank()) locale = l;
    }

    public Path settingsFile() { return settingsFile; }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
}
