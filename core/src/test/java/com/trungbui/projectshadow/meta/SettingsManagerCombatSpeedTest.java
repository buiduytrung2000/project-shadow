package com.trungbui.projectshadow.meta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 13 B3 — SettingsManager persists combatSpeed2x field.
 */
class SettingsManagerCombatSpeedTest {

    @Test
    void defaultCombatSpeed2x_isFalse(@TempDir Path tmpDir) {
        SettingsManager s = new SettingsManager(tmpDir);
        s.load(); // no file → defaults
        assertThat(s.combatSpeed2x()).isEqualTo(SettingsManager.DEFAULT_COMBAT_SPEED_2X);
        assertThat(s.combatSpeed2x()).isFalse();
    }

    @Test
    void setCombatSpeed2x_true_persists(@TempDir Path tmpDir) throws Exception {
        SettingsManager s1 = new SettingsManager(tmpDir);
        s1.setCombatSpeed2x(true);
        s1.save();

        SettingsManager s2 = new SettingsManager(tmpDir);
        s2.load();
        assertThat(s2.combatSpeed2x()).isTrue();
    }

    @Test
    void setCombatSpeed2x_false_persists(@TempDir Path tmpDir) throws Exception {
        // Write true first, then overwrite with false.
        SettingsManager s1 = new SettingsManager(tmpDir);
        s1.setCombatSpeed2x(true);
        s1.save();

        SettingsManager s2 = new SettingsManager(tmpDir);
        s2.setCombatSpeed2x(false);
        s2.save();

        SettingsManager s3 = new SettingsManager(tmpDir);
        s3.load();
        assertThat(s3.combatSpeed2x()).isFalse();
    }

    @Test
    void load_legacyFile_withoutSpeedField_defaultsFalse(@TempDir Path tmpDir) throws Exception {
        // Legacy settings.json that doesn't contain combatSpeed2x — should default to false.
        Path file = tmpDir.resolve(SettingsManager.FILE_NAME);
        java.nio.file.Files.writeString(file, "{\"musicVolume\":0.5,\"locale\":\"en\"}");

        SettingsManager s = new SettingsManager(tmpDir);
        s.load();
        assertThat(s.combatSpeed2x()).isFalse();
        // Other fields parsed correctly.
        assertThat(s.locale()).isEqualTo("en");
    }

    @Test
    void roundTrip_allFields_preserved(@TempDir Path tmpDir) throws Exception {
        SettingsManager s1 = new SettingsManager(tmpDir);
        s1.setMusicVolume(0.3f);
        s1.setSfxVolume(0.7f);
        s1.setFullscreen(true);
        s1.setLocale("en");
        s1.setCombatSpeed2x(true);
        s1.save();

        SettingsManager s2 = new SettingsManager(tmpDir);
        s2.load();
        assertThat(s2.musicVolume()).isEqualTo(0.3f);
        assertThat(s2.sfxVolume()).isEqualTo(0.7f);
        assertThat(s2.fullscreen()).isTrue();
        assertThat(s2.locale()).isEqualTo("en");
        assertThat(s2.combatSpeed2x()).isTrue();
    }
}
