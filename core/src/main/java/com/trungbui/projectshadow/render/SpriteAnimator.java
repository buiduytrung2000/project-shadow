package com.trungbui.projectshadow.render;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import java.util.EnumMap;
import java.util.Map;

/**
 * Sprint 9 — state machine wrapping per-tag {@link Animation} from a sprite atlas.
 *
 * <p>States: IDLE (looping), ATTACKING / HURT (one-shot, auto-return to IDLE),
 * DEAD (held). When the atlas is missing, {@link #hasAnimations()} returns
 * {@code false} so callers can fall back to drawing primitives.</p>
 */
public class SpriteAnimator {

    public enum State { IDLE, ATTACKING, HURT, DEAD }

    private static final float IDLE_FRAME_DURATION = 0.125f;
    private static final float ATTACK_FRAME_DURATION = 0.083f;
    private static final float HURT_FRAME_DURATION = 0.125f;
    private static final float DEAD_FRAME_DURATION = 1f;

    private final Map<State, Animation<TextureAtlas.AtlasRegion>> animations =
            new EnumMap<>(State.class);
    private State current = State.IDLE;
    private float stateTime = 0f;

    public SpriteAnimator(SpriteAtlas atlas, String spriteKey) {
        if (atlas != null && atlas.isLoaded()) {
            putIfFound(atlas, spriteKey, "idle", State.IDLE, IDLE_FRAME_DURATION);
            putIfFound(atlas, spriteKey, "attack", State.ATTACKING, ATTACK_FRAME_DURATION);
            putIfFound(atlas, spriteKey, "hurt", State.HURT, HURT_FRAME_DURATION);
            putIfFound(atlas, spriteKey, "dead", State.DEAD, DEAD_FRAME_DURATION);
        }
    }

    private void putIfFound(SpriteAtlas atlas, String spriteKey, String tag,
                            State s, float frameDuration) {
        // Aseprite TexturePacker convention: regions are named e.g. "idle_0", "idle_1", ...
        // OR "<spriteKey>_idle_0" depending on packing. Try both.
        Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions(spriteKey + "_" + tag);
        if (regions == null || regions.isEmpty()) regions = atlas.findRegions(tag);
        if (regions != null && !regions.isEmpty()) {
            Animation<TextureAtlas.AtlasRegion> a = new Animation<>(frameDuration, regions);
            a.setPlayMode(s == State.IDLE
                    ? Animation.PlayMode.LOOP
                    : s == State.DEAD
                        ? Animation.PlayMode.NORMAL
                        : Animation.PlayMode.NORMAL);
            animations.put(s, a);
        }
    }

    public void setState(State s) {
        if (s == null) return;
        current = s;
        stateTime = 0f;
    }

    public State currentState() {
        return current;
    }

    public void update(float delta) {
        stateTime += delta;
        Animation<TextureAtlas.AtlasRegion> anim = animations.get(current);
        if (anim != null
                && (current == State.ATTACKING || current == State.HURT)
                && anim.isAnimationFinished(stateTime)) {
            current = State.IDLE;
            stateTime = 0f;
        }
    }

    public TextureRegion currentFrame() {
        Animation<TextureAtlas.AtlasRegion> anim = animations.get(current);
        if (anim == null) return null;
        return anim.getKeyFrame(stateTime, current == State.IDLE);
    }

    public boolean hasAnimations() {
        return !animations.isEmpty();
    }
}
