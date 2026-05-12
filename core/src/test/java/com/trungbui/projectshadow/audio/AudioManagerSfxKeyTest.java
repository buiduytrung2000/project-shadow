package com.trungbui.projectshadow.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Sprint 13 B4 — verify that {@link AudioManager#playSfx(String)} does not throw
 * for any manifest key, even when OGG files are absent (lazy-load tolerant).
 *
 * <p>These keys come from {@code assets/audio/.sfx-manifest.json}. The files may not
 * exist in the repo (user runs {@code fetch_sfx.sh} separately); AudioManager must
 * degrade gracefully.</p>
 */
class AudioManagerSfxKeyTest {

    /** All SFX keys expected by Sprint 13 B4 wiring code. */
    @ParameterizedTest
    @ValueSource(strings = {
        "sfx_attack_slash",
        "sfx_attack_blunt",
        "sfx_attack_magic",
        "sfx_hurt_hero",
        "sfx_hurt_enemy",
        "sfx_death_hero",
        "sfx_death_enemy",
        "sfx_stress_tick",
        "sfx_affliction",
        "sfx_virtue",
        "sfx_item_use",
        "sfx_ui_click",
        "sfx_ui_hover",
        "sfx_level_enter",
        "sfx_reward_gold",
        "sfx_heal",
        "sfx_miss"
    })
    void playSfx_manifestKey_doesNotThrow(String key) {
        AudioManager am = new AudioManager();
        assertThatNoException().isThrownBy(() -> am.playSfx(key));
    }

    @Test
    void playSfx_unknownKey_doesNotThrow() {
        AudioManager am = new AudioManager();
        assertThatNoException().isThrownBy(() -> am.playSfx("sfx_nonexistent_key_xyz"));
    }

    @Test
    void playSfx_nullKey_doesNotThrow() {
        AudioManager am = new AudioManager();
        assertThatNoException().isThrownBy(() -> am.playSfx(null));
    }
}
