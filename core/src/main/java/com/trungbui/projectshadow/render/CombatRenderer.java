package com.trungbui.projectshadow.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Disposable;
import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.effect.EffectInstance;
import com.trungbui.projectshadow.ui.SkinLoader;

import java.util.List;
import java.util.Set;

public class CombatRenderer implements Disposable {

    public static final int VIRTUAL_WIDTH = 1920;
    public static final int VIRTUAL_HEIGHT = 1080;

    // Horizontal row layout: POS_1 sát đường ranh giới 2 phe.
    private static final float HERO_FRONT_X = 680f;   // POS_1 hero — rightmost của cluster heroes
    private static final float ENEMY_FRONT_X = 1090f; // POS_1 enemy — leftmost của cluster enemies
    private static final float COMBATANT_GAP_X = 50f;
    private static final float COMBATANT_STEP_X = CombatantView.WIDTH + COMBATANT_GAP_X;
    private static final float COMBATANT_BASE_Y = 440f;
    private static final float BAR_WIDTH = CombatantView.WIDTH;
    private static final float HP_BAR_HEIGHT = 18f;
    private static final float STRESS_BAR_HEIGHT = 12f;

    private static final float ICON_SIZE = 16f;
    private static final float EFFECT_ICON_SIZE = 16f;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch spriteBatch = new SpriteBatch();
    private final SpriteAtlas spriteAtlas = new SpriteAtlas("sprites/combatants.atlas");

    /** Optional skin for drawing UI icons (HP/Stress bar prefix icons, effect icons).
     *  Null in headless / test contexts — renderer falls back to plain bars. */
    private Skin skin;

    /** Sets the skin for icon rendering. Must be called before renderCombatants(). */
    public void setSkin(Skin skin) {
        this.skin = skin;
    }

    public ShapeRenderer shapes() {
        return shapes;
    }

    public List<CombatantView> layoutHeroes(List<? extends Combatant> heroes) {
        // Heroes: POS_1 ở rightmost → back rows trôi sang trái (stepX âm).
        return layoutRow(heroes, HERO_FRONT_X, -COMBATANT_STEP_X);
    }

    public List<CombatantView> layoutEnemies(List<? extends Combatant> enemies) {
        // Enemies: POS_1 ở leftmost → back rows trôi sang phải (stepX dương).
        return layoutRow(enemies, ENEMY_FRONT_X, +COMBATANT_STEP_X);
    }

    private List<CombatantView> layoutRow(List<? extends Combatant> combatants, float frontX, float stepX) {
        return java.util.stream.IntStream.range(0, combatants.size())
                .mapToObj(i -> {
                    CombatantView v = new CombatantView(
                            combatants.get(i),
                            frontX + i * stepX,
                            COMBATANT_BASE_Y);
                    if (spriteAtlas.isLoaded()) {
                        v.attachAnimator(new SpriteAnimator(spriteAtlas, combatants.get(i).id()));
                    }
                    return v;
                })
                .toList();
    }

    public void renderBackground() {
        com.badlogic.gdx.Gdx.gl.glClearColor(0.10f, 0.10f, 0.13f, 1f);
        com.badlogic.gdx.Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    public void renderCombatants(
            com.badlogic.gdx.graphics.OrthographicCamera camera,
            List<CombatantView> heroes,
            List<CombatantView> enemies,
            Combatant currentActor,
            Set<Combatant> highlightedTargets
    ) {
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (CombatantView v : heroes) drawCombatant(v, currentActor, highlightedTargets);
        for (CombatantView v : enemies) drawCombatant(v, currentActor, highlightedTargets);
        shapes.end();

        // Sprite layer (drawn on top of fallback rectangles when atlas is loaded),
        // plus UI icon overlays (HP/Stress bar icons, effect icons).
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();
        if (spriteAtlas.isLoaded()) {
            for (CombatantView v : heroes) drawSprite(v);
            for (CombatantView v : enemies) drawSprite(v);
        }
        if (skin != null) {
            for (CombatantView v : heroes) drawBarIcons(v);
            for (CombatantView v : enemies) drawBarIcons(v);
            for (CombatantView v : heroes) drawEffectIcons(v);
            for (CombatantView v : enemies) drawEffectIcons(v);
        }
        spriteBatch.end();
    }

    private void drawSprite(CombatantView v) {
        if (v.animator() == null || !v.animator().hasAnimations()) return;
        TextureRegion frame = v.animator().currentFrame();
        if (frame == null) return;
        // Sprint 12 B3 — boss epic-death zoom: scale the sprite around its
        // centre when the view's epicDeathZoom() > 1.0. HP bars stay anchored
        // at their fixed Y in drawCombatant(); only the sprite scales.
        float zoom = v.epicDeathZoom();
        if (zoom <= 1f + 1e-4f) {
            spriteBatch.draw(frame, v.x(), v.y(), v.width(), v.height());
            return;
        }
        float scaledW = v.width() * zoom;
        float scaledH = v.height() * zoom;
        float offsetX = (scaledW - v.width()) * 0.5f;
        float offsetY = (scaledH - v.height()) * 0.5f;
        spriteBatch.draw(frame, v.x() - offsetX, v.y() - offsetY, scaledW, scaledH);
    }

    private void drawCombatant(CombatantView v, Combatant currentActor, Set<Combatant> highlightedTargets) {
        Combatant c = v.combatant();
        boolean alive = c.isAlive();
        boolean targetable = highlightedTargets != null && highlightedTargets.contains(c);

        if (alive && currentActor == c) {
            shapes.setColor(0.95f, 0.85f, 0.20f, 1f);
            float pad = 6f;
            shapes.rect(v.x() - pad, v.y() - pad, v.width() + 2 * pad, v.height() + 2 * pad);
        } else if (targetable) {
            shapes.setColor(0.20f, 0.85f, 0.95f, 1f);
            float pad = 6f;
            shapes.rect(v.x() - pad, v.y() - pad, v.width() + 2 * pad, v.height() + 2 * pad);
        }

        Color body = v.color();
        if (!alive) body = new Color(0.20f, 0.20f, 0.22f, 1f);
        else if (v.isFlashing()) body = new Color(1f, 1f, 1f, 1f);

        shapes.setColor(body);
        shapes.rect(v.x(), v.y(), v.width(), v.height());

        drawHpBar(v);
        if (c instanceof Hero h) drawStressBar(v, h);
    }

    private void drawHpBar(CombatantView v) {
        Combatant c = v.combatant();
        float ratio = c.maxHp() == 0 ? 0f : Math.max(0f, (float) c.currentHp() / c.maxHp());
        float y = v.y() + v.height() + 12f;

        shapes.setColor(0.18f, 0.18f, 0.18f, 1f);
        shapes.rect(v.x(), y, BAR_WIDTH, HP_BAR_HEIGHT);

        Color fill = ratio > 0.6f ? new Color(0.30f, 0.78f, 0.35f, 1f)
                : ratio > 0.3f ? new Color(0.95f, 0.78f, 0.20f, 1f)
                : new Color(0.90f, 0.20f, 0.20f, 1f);
        shapes.setColor(fill);
        shapes.rect(v.x(), y, BAR_WIDTH * ratio, HP_BAR_HEIGHT);
    }

    private void drawStressBar(CombatantView v, Hero h) {
        float ratio = h.maxStress() == 0 ? 0f : Math.max(0f, (float) h.currentStress() / h.maxStress());
        float y = v.y() + v.height() + 12f + HP_BAR_HEIGHT + 4f;

        shapes.setColor(0.18f, 0.18f, 0.18f, 1f);
        shapes.rect(v.x(), y, BAR_WIDTH, STRESS_BAR_HEIGHT);

        shapes.setColor(0.62f, 0.30f, 0.85f, 1f);
        shapes.rect(v.x(), y, BAR_WIDTH * ratio, STRESS_BAR_HEIGHT);
    }

    /**
     * Draws HP and Stress bar prefix icons (16×16) in the spriteBatch pass.
     * Must be called inside spriteBatch.begin/end. Skips if skin is null or icons missing.
     */
    private void drawBarIcons(CombatantView v) {
        if (skin == null) return;
        float hpBarY = v.y() + v.height() + 12f;
        drawIcon(SkinLoader.ICON_HP, v.x() - ICON_SIZE - 2f, hpBarY, ICON_SIZE, HP_BAR_HEIGHT);

        Combatant c = v.combatant();
        if (c instanceof Hero) {
            float stressBarY = v.y() + v.height() + 12f + HP_BAR_HEIGHT + 4f;
            drawIcon(SkinLoader.ICON_STRESS, v.x() - ICON_SIZE - 2f, stressBarY, ICON_SIZE, STRESS_BAR_HEIGHT);
        }
    }

    /**
     * Draws small effect icon boxes (16×16 each) below the combatant sprite.
     * If an atlas icon is available for the effect's iconKey, draws the atlas region.
     * Otherwise falls back to a category-colored box.
     * Must be called inside spriteBatch.begin/end.
     */
    void drawEffectIcons(CombatantView v) {
        List<EffectInstance> effects = v.combatant().activeEffects().instances();
        if (effects.isEmpty()) return;
        float startX = v.x();
        float iconY = v.y() - EFFECT_ICON_SIZE - 4f;
        for (int i = 0; i < effects.size(); i++) {
            EffectInstance ei = effects.get(i);
            float x = startX + i * (EFFECT_ICON_SIZE + 2f);
            String drawableName = effectIconDrawable(ei.effectId());
            if (drawableName != null && skin != null && skin.has(drawableName, Drawable.class)) {
                // Use atlas icon.
                spriteBatch.setColor(Color.WHITE);
                skin.getDrawable(drawableName).draw(spriteBatch, x, iconY, EFFECT_ICON_SIZE, EFFECT_ICON_SIZE);
            } else {
                // Fallback: category color box.
                Color box = categoryColor(ei);
                spriteBatch.setColor(box);
                drawColorBox(x, iconY, EFFECT_ICON_SIZE, EFFECT_ICON_SIZE);
                spriteBatch.setColor(Color.WHITE);
            }
        }
    }

    /**
     * Maps a known effectId to its SkinLoader drawable name.
     * Returns null for unknown effects (triggers color-box fallback).
     */
    static String effectIconDrawable(String effectId) {
        return switch (effectId) {
            case "eff_heal"         -> SkinLoader.STATUS_HEAL;
            case "eff_burn"         -> SkinLoader.STATUS_BURN;
            case "eff_taunt"        -> SkinLoader.STATUS_TAUNT;
            case "eff_dmg_buff"     -> SkinLoader.STATUS_BUFF_DMG;
            case "eff_absorb"       -> SkinLoader.STATUS_SHIELD;
            case "eff_stress_reset" -> SkinLoader.STATUS_MEDITATE;
            case "eff_bleed"        -> SkinLoader.STATUS_BLEED;
            case "eff_poison"       -> SkinLoader.STATUS_POISON;
            case "eff_stun"         -> SkinLoader.STATUS_STUN;
            case "eff_slow"         -> SkinLoader.STATUS_SLOW;
            default -> {
                // Permanent debuffs (affliction-type effects) use the affliction icon.
                if (effectId.startsWith("perm_debuff_") || effectId.startsWith("eff_curse_")) {
                    yield SkinLoader.STATUS_AFFLICTION;
                }
                yield null;
            }
        };
    }

    /** Returns the category color for an effect instance (red=debuff, blue=buff, purple=disease). */
    private static Color categoryColor(EffectInstance ei) {
        String cat = ei.effectId();
        // Use the effect category field from the EffectData if available via catalog.
        // Fallback heuristic based on known ID prefixes.
        if (cat.startsWith("eff_poison") || cat.startsWith("eff_bleed") || cat.startsWith("eff_burn")) {
            return new Color(0.85f, 0.20f, 0.20f, 0.9f); // red — DoT debuff
        }
        if (cat.startsWith("eff_dmg_buff") || cat.startsWith("eff_absorb") || cat.startsWith("eff_heal")) {
            return new Color(0.20f, 0.70f, 0.30f, 0.9f); // green — buff
        }
        // Default purple for disease/other
        return new Color(0.55f, 0.20f, 0.75f, 0.9f);
    }

    /** Draws a solid color box using spriteBatch. Requires spriteBatch color to already be set. */
    private void drawColorBox(float x, float y, float w, float h) {
        // Use the skin's white-pixel drawable if it has one for a proper filled rect.
        // If skin is null or missing, fall back to a transparent draw (no-op).
        if (skin != null && skin.has("white", com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable.class)) {
            Drawable white = skin.getDrawable("white");
            white.draw(spriteBatch, x, y, w, h);
        } else if (skin != null) {
            // Try the default background as a solid fill fallback.
            try {
                Drawable d = skin.getDrawable("default-rect");
                d.draw(spriteBatch, x, y, w, h);
            } catch (Exception ignored) {
                // No drawable available — skip icon in this context (tests / headless).
            }
        }
    }

    /**
     * Sprint 13 B4 — renders all live floating text entries using the given font.
     * Opens a new spriteBatch pass (begin/end). No-op if manager is null or empty.
     *
     * @param manager the floating text manager (may be null)
     * @param camera  projection matrix source
     * @param font    font to draw with
     */
    public void renderFloatingTexts(
            com.badlogic.gdx.graphics.OrthographicCamera camera,
            FloatingTextManager manager,
            BitmapFont font) {
        if (manager == null || font == null || manager.activeCount() == 0) return;
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();
        manager.render(spriteBatch, font);
        spriteBatch.end();
    }

    /** Draws a named icon drawable at the given position. Skips if not in skin. */
    private void drawIcon(String iconKey, float x, float y, float w, float h) {
        if (skin == null || !skin.has(iconKey, Drawable.class)) return;
        Drawable icon = skin.getDrawable(iconKey);
        icon.draw(spriteBatch, x, y, w, h);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        spriteBatch.dispose();
        spriteAtlas.dispose();
    }
}
