package com.trungbui.projectshadow.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

/**
 * Sprint 9 — runtime audio loader with graceful fallback.
 *
 * <p>Music tracks live at {@code assets/audio/music/<key>.ogg}; SFX at
 * {@code assets/audio/sfx/<key>.ogg}. If a file is missing the call logs a warning
 * and returns silently — the game must remain playable without any audio assets.</p>
 *
 * <p>Both Music and Sound caches are populated lazily on first request, so unused
 * tracks never load.</p>
 */
public class AudioManager implements Disposable {

    private static final String MUSIC_PATH = "audio/music/";
    private static final String SFX_PATH = "audio/sfx/";
    private static final String EXT = ".ogg";

    private final Map<String, Music> musicCache = new HashMap<>();
    private final Map<String, Sound> sfxCache = new HashMap<>();
    private final Map<String, Boolean> missingMusicLogged = new HashMap<>();
    private final Map<String, Boolean> missingSfxLogged = new HashMap<>();

    private Music currentMusic;
    private String currentMusicKey;
    private float musicVolume = 0.6f;
    private float sfxVolume = 0.8f;

    public void playMusic(String key, boolean looping) {
        if (key == null) return;
        Music m = loadMusic(key);
        if (m == null) return;
        if (currentMusic == m && m.isPlaying()) return; // already playing same track
        if (currentMusic != null && currentMusic != m) currentMusic.stop();
        currentMusic = m;
        currentMusicKey = key;
        m.setLooping(looping);
        m.setVolume(musicVolume);
        m.play();
    }

    public void playSfx(String key) {
        if (key == null) return;
        Sound s = loadSfx(key);
        if (s != null) s.play(sfxVolume);
    }

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
            currentMusicKey = null;
        }
    }

    public String currentMusicKey() {
        return currentMusicKey;
    }

    public void setMusicVolume(float v) {
        musicVolume = clamp01(v);
        if (currentMusic != null) currentMusic.setVolume(musicVolume);
    }

    public void setSfxVolume(float v) {
        sfxVolume = clamp01(v);
    }

    public float musicVolume() {
        return musicVolume;
    }

    public float sfxVolume() {
        return sfxVolume;
    }

    private Music loadMusic(String key) {
        if (musicCache.containsKey(key)) return musicCache.get(key);
        FileHandle f = filehandle(MUSIC_PATH + key + EXT);
        if (f == null || !f.exists()) {
            if (!missingMusicLogged.containsKey(key)) {
                log("Music missing, skipping: " + key);
                missingMusicLogged.put(key, true);
            }
            musicCache.put(key, null);
            return null;
        }
        try {
            Music m = Gdx.audio.newMusic(f);
            musicCache.put(key, m);
            return m;
        } catch (Exception e) {
            log("Failed to load music " + key + ": " + e.getMessage());
            musicCache.put(key, null);
            return null;
        }
    }

    private Sound loadSfx(String key) {
        if (sfxCache.containsKey(key)) return sfxCache.get(key);
        FileHandle f = filehandle(SFX_PATH + key + EXT);
        if (f == null || !f.exists()) {
            if (!missingSfxLogged.containsKey(key)) {
                log("SFX missing, skipping: " + key);
                missingSfxLogged.put(key, true);
            }
            sfxCache.put(key, null);
            return null;
        }
        try {
            Sound s = Gdx.audio.newSound(f);
            sfxCache.put(key, s);
            return s;
        } catch (Exception e) {
            log("Failed to load sfx " + key + ": " + e.getMessage());
            sfxCache.put(key, null);
            return null;
        }
    }

    private static FileHandle filehandle(String path) {
        try {
            return Gdx.files != null ? Gdx.files.internal(path) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void log(String msg) {
        if (Gdx.app != null) {
            Gdx.app.log("AudioManager", msg);
        } else {
            System.err.println("[AudioManager] " + msg);
        }
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    @Override
    public void dispose() {
        for (Music m : musicCache.values()) if (m != null) m.dispose();
        musicCache.clear();
        for (Sound s : sfxCache.values()) if (s != null) s.dispose();
        sfxCache.clear();
        currentMusic = null;
        currentMusicKey = null;
    }
}
