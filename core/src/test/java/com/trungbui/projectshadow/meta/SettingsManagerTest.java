package com.trungbui.projectshadow.meta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 12 B4 — verify SettingsManager round-trips values through
 * saves/settings.json + falls back to defaults when the file is missing or
 * corrupt.
 */
class SettingsManagerTest {

    @Test
    void load_returnsDefaultsWhenFileMissing(@TempDir Path tempDir) {
        SettingsManager s = new SettingsManager(tempDir);
        s.load();
        assertThat(s.musicVolume()).isEqualTo(SettingsManager.DEFAULT_MUSIC_VOLUME);
        assertThat(s.sfxVolume()).isEqualTo(SettingsManager.DEFAULT_SFX_VOLUME);
        assertThat(s.fullscreen()).isEqualTo(SettingsManager.DEFAULT_FULLSCREEN);
        assertThat(s.locale()).isEqualTo(SettingsManager.DEFAULT_LOCALE);
    }

    @Test
    void save_thenLoad_roundTripsAllFields(@TempDir Path tempDir) throws Exception {
        SettingsManager s1 = new SettingsManager(tempDir);
        s1.setMusicVolume(0.25f);
        s1.setSfxVolume(0.5f);
        s1.setFullscreen(true);
        s1.setLocale("en");
        s1.save();

        SettingsManager s2 = new SettingsManager(tempDir);
        s2.load();
        assertThat(s2.musicVolume()).isEqualTo(0.25f);
        assertThat(s2.sfxVolume()).isEqualTo(0.5f);
        assertThat(s2.fullscreen()).isTrue();
        assertThat(s2.locale()).isEqualTo("en");
    }

    @Test
    void setMusicVolume_clampsToZeroAndOne(@TempDir Path tempDir) {
        SettingsManager s = new SettingsManager(tempDir);
        s.setMusicVolume(-0.5f);
        assertThat(s.musicVolume()).isZero();
        s.setMusicVolume(2.0f);
        assertThat(s.musicVolume()).isEqualTo(1.0f);
    }

    @Test
    void setLocale_blankIgnored(@TempDir Path tempDir) {
        SettingsManager s = new SettingsManager(tempDir);
        s.setLocale("en");
        s.setLocale("");
        assertThat(s.locale()).isEqualTo("en"); // unchanged
        s.setLocale(null);
        assertThat(s.locale()).isEqualTo("en");
    }

    @Test
    void load_corruptFile_fallsBackToDefaults(@TempDir Path tempDir) throws Exception {
        Path settings = tempDir.resolve(SettingsManager.FILE_NAME);
        Files.writeString(settings, "this is not json {{");
        SettingsManager s = new SettingsManager(tempDir);
        s.load();
        // Defaults preserved.
        assertThat(s.musicVolume()).isEqualTo(SettingsManager.DEFAULT_MUSIC_VOLUME);
    }
}
