package com.trungbui.projectshadow.render;

import com.trungbui.projectshadow.ui.SkinLoader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B2 K.3 — verifies that {@link CombatRenderer#effectIconDrawable(String)} maps
 * known effect IDs to the correct SkinLoader drawable constants, and returns
 * {@code null} for unknown effects (triggering the color-box fallback).
 *
 * <p>No libGDX Skin is needed — this tests only the mapping logic.</p>
 */
class CombatRendererEffectIconAtlasTest {

    @Test
    void knownEffects_mapToCorrectDrawables() {
        assertThat(CombatRenderer.effectIconDrawable("eff_heal")).isEqualTo(SkinLoader.STATUS_HEAL);
        assertThat(CombatRenderer.effectIconDrawable("eff_burn")).isEqualTo(SkinLoader.STATUS_BURN);
        assertThat(CombatRenderer.effectIconDrawable("eff_taunt")).isEqualTo(SkinLoader.STATUS_TAUNT);
        assertThat(CombatRenderer.effectIconDrawable("eff_dmg_buff")).isEqualTo(SkinLoader.STATUS_BUFF_DMG);
        assertThat(CombatRenderer.effectIconDrawable("eff_absorb")).isEqualTo(SkinLoader.STATUS_SHIELD);
        assertThat(CombatRenderer.effectIconDrawable("eff_stress_reset")).isEqualTo(SkinLoader.STATUS_MEDITATE);
        assertThat(CombatRenderer.effectIconDrawable("eff_bleed")).isEqualTo(SkinLoader.STATUS_BLEED);
        assertThat(CombatRenderer.effectIconDrawable("eff_poison")).isEqualTo(SkinLoader.STATUS_POISON);
        assertThat(CombatRenderer.effectIconDrawable("eff_stun")).isEqualTo(SkinLoader.STATUS_STUN);
        assertThat(CombatRenderer.effectIconDrawable("eff_slow")).isEqualTo(SkinLoader.STATUS_SLOW);
    }

    @Test
    void afflictionPrefixEffects_mapToAfflictionIcon() {
        assertThat(CombatRenderer.effectIconDrawable("perm_debuff_01")).isEqualTo(SkinLoader.STATUS_AFFLICTION);
        assertThat(CombatRenderer.effectIconDrawable("perm_debuff_08")).isEqualTo(SkinLoader.STATUS_AFFLICTION);
        assertThat(CombatRenderer.effectIconDrawable("eff_curse_perm")).isEqualTo(SkinLoader.STATUS_AFFLICTION);
    }

    @Test
    void unknownEffect_returnsNull() {
        assertThat(CombatRenderer.effectIconDrawable("eff_unknown_xyz")).isNull();
        assertThat(CombatRenderer.effectIconDrawable("eff_regen")).isNull();
        assertThat(CombatRenderer.effectIconDrawable("eff_doom")).isNull();
    }

    @Test
    void permanentBuffEffects_returnNull() {
        // Permanent buffs (virtue/trait) don't have a specific icon — fall back to color box.
        assertThat(CombatRenderer.effectIconDrawable("eff_perm_dmg")).isNull();
        assertThat(CombatRenderer.effectIconDrawable("perm_buff_01")).isNull();
    }
}
