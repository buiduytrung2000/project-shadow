package com.trungbui.projectshadow.audio;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Headless tests for {@link AudioManager}. Cannot test actual playback (no GL/audio
 * context in JUnit), but verifies graceful fallback + state management.
 */
class AudioManagerTest {

    @Test
    void playMusic_doesNotCrashWhenFileMissing() {
        AudioManager am = new AudioManager();
        am.playMusic("nonexistent_track", true); // should silently warn
        assertThat(am.currentMusicKey()).isNull();
    }

    @Test
    void playSfx_doesNotCrashWhenFileMissing() {
        AudioManager am = new AudioManager();
        am.playSfx("nonexistent_sfx"); // should silently warn
    }

    @Test
    void playMusic_nullKey_isNoOp() {
        AudioManager am = new AudioManager();
        am.playMusic(null, true);
        assertThat(am.currentMusicKey()).isNull();
    }

    @Test
    void playSfx_nullKey_isNoOp() {
        AudioManager am = new AudioManager();
        am.playSfx(null);
        // no exception
    }

    @Test
    void setMusicVolume_clampsToValidRange() {
        AudioManager am = new AudioManager();
        am.setMusicVolume(-0.5f);
        assertThat(am.musicVolume()).isEqualTo(0f);
        am.setMusicVolume(2.0f);
        assertThat(am.musicVolume()).isEqualTo(1f);
        am.setMusicVolume(0.5f);
        assertThat(am.musicVolume()).isEqualTo(0.5f);
    }

    @Test
    void setSfxVolume_clampsToValidRange() {
        AudioManager am = new AudioManager();
        am.setSfxVolume(-1f);
        assertThat(am.sfxVolume()).isEqualTo(0f);
        am.setSfxVolume(5f);
        assertThat(am.sfxVolume()).isEqualTo(1f);
    }

    @Test
    void stopMusic_clearsCurrentKey() {
        AudioManager am = new AudioManager();
        am.stopMusic();
        assertThat(am.currentMusicKey()).isNull();
    }

    @Test
    void dispose_doesNotCrash() {
        AudioManager am = new AudioManager();
        am.playMusic("missing_track", true);
        am.playSfx("missing_sfx");
        am.dispose();
        // re-dispose should also be safe
        am.dispose();
    }
}
