package com.trungbui.projectshadow.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.Hero;

import java.util.List;
import java.util.Set;

public class CombatRenderer implements Disposable {

    public static final int VIRTUAL_WIDTH = 1920;
    public static final int VIRTUAL_HEIGHT = 1080;

    private static final float HERO_X = 250f;
    private static final float ENEMY_X = 1520f;
    private static final float ROW_TOP_Y = 950f;
    private static final float ROW_GAP_Y = 220f;
    private static final float BAR_WIDTH = CombatantView.WIDTH;
    private static final float HP_BAR_HEIGHT = 18f;
    private static final float STRESS_BAR_HEIGHT = 12f;

    private final ShapeRenderer shapes = new ShapeRenderer();

    public List<CombatantView> layoutHeroes(List<? extends Combatant> heroes) {
        return layoutColumn(heroes, HERO_X);
    }

    public List<CombatantView> layoutEnemies(List<? extends Combatant> enemies) {
        return layoutColumn(enemies, ENEMY_X);
    }

    private List<CombatantView> layoutColumn(List<? extends Combatant> combatants, float baseX) {
        return java.util.stream.IntStream.range(0, combatants.size())
                .mapToObj(i -> new CombatantView(
                        combatants.get(i),
                        baseX,
                        ROW_TOP_Y - i * ROW_GAP_Y - CombatantView.HEIGHT))
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

    @Override
    public void dispose() {
        shapes.dispose();
    }
}
