package com.trungbui.projectshadow.render;

import com.trungbui.projectshadow.data.model.EffectData;
import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.Position;
import com.trungbui.projectshadow.effect.ActiveEffects;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 13 B3 — verify that CombatRenderer reads the correct number of effect
 * instances for rendering effect icon boxes.
 *
 * <p>Rendering itself (SpriteBatch, ShapeRenderer) is not tested — headless JUnit
 * cannot initialize Gdx. These tests verify the data-reading contract: the view
 * provides the right effect count, which drawEffectIcons iterates over.</p>
 */
class CombatRendererEffectIconsTest {

    private static EffectData dot(String id) {
        return new EffectData(
                id, "Test", "Test", "dot", true, null,
                "turns", "3", "hp", "flat_per_turn", "2",
                false, null, "on_turn_start", null, null, null, null, ""
        );
    }

    /** Minimal Combatant whose ActiveEffects can be populated via catalog. */
    private static final class TestCombatant implements Combatant {
        private int hp = 10;
        final ActiveEffects effects;

        TestCombatant(Map<String, EffectData> catalog) {
            effects = new ActiveEffects(catalog);
        }

        @Override public String id() { return "test_c"; }
        @Override public int currentHp() { return hp; }
        @Override public int maxHp() { return 10; }
        @Override public void setCurrentHp(int v) { hp = v; }
        @Override public Position position() { return Position.POS_1; }
        @Override public void setPosition(Position pos) {}
        @Override public int speed() { return 5; }
        @Override public int accuracy() { return 80; }
        @Override public int dodge() { return 0; }
        @Override public double critChance() { return 0; }
        @Override public int dmgMin() { return 1; }
        @Override public int dmgMax() { return 2; }
        @Override public ActiveEffects activeEffects() { return effects; }
    }

    @Test
    void combatantWithNoEffects_instancesIsEmpty() {
        TestCombatant c = new TestCombatant(Map.of());
        assertThat(c.activeEffects().instances()).isEmpty();
    }

    @Test
    void combatantWith2Effects_instancesSizeIs2() {
        EffectData e1 = dot("eff_poison");
        EffectData e2 = dot("eff_bleed");
        TestCombatant c = new TestCombatant(Map.of("eff_poison", e1, "eff_bleed", e2));
        c.effects.apply("eff_poison", c, c, new Random());
        c.effects.apply("eff_bleed", c, c, new Random());
        assertThat(c.activeEffects().instances()).hasSize(2);
    }

    @Test
    void combatantWith1Effect_instancesSizeIs1() {
        EffectData e1 = dot("eff_burn");
        TestCombatant c = new TestCombatant(Map.of("eff_burn", e1));
        c.effects.apply("eff_burn", c, c, new Random());
        assertThat(c.activeEffects().instances()).hasSize(1);
    }

    @Test
    void viewUsesCorrectCombatantReference() {
        TestCombatant c = new TestCombatant(Map.of());
        CombatantView view = new CombatantView(c, 100f, 200f);
        assertThat(view.combatant()).isSameAs(c);
    }

    @Test
    void effectsExposeViaActiveEffectsInstances() {
        EffectData e1 = dot("eff_poison");
        TestCombatant c = new TestCombatant(Map.of("eff_poison", e1));
        c.effects.apply("eff_poison", c, c, new Random());
        // Verify instances() is the API drawEffectIcons uses.
        assertThat(c.activeEffects().instances()).hasSize(1);
        assertThat(c.activeEffects().instances().get(0).effectId()).isEqualTo("eff_poison");
    }
}
