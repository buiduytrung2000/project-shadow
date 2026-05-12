package com.trungbui.projectshadow.data;

import com.trungbui.projectshadow.data.model.EffectData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B2 K.1 — verifies that the "Icon Key" CSV column is parsed correctly by
 * {@link DataLoader#loadEffects} and that the new {@link EffectData#iconKey()}
 * accessor returns the expected values.
 */
class EffectDataIconKeyTest {

    private static List<EffectData> effects;

    @BeforeAll
    static void load() throws Exception {
        Path csv = resolveEffectsPath();
        try (var reader = Files.newBufferedReader(csv)) {
            effects = DataLoader.loadEffects(reader);
        }
    }

    private static Path resolveEffectsPath() {
        Path[] candidates = {
                Path.of("../assets/data/effects.csv"),
                Path.of("assets/data/effects.csv"),
                Path.of("../../assets/data/effects.csv")
        };
        for (Path p : candidates) {
            if (Files.exists(p)) return p;
        }
        throw new IllegalStateException(
                "Cannot locate effects.csv from cwd=" + Path.of("").toAbsolutePath());
    }

    @Test
    void totalEffectsLoadedCorrectly() {
        assertThat(effects).hasSize(134);
    }

    @Test
    void iconKey_forKnownEffects_isCorrect() {
        assertIcon("eff_bleed", "icon_bleed");
        assertIcon("eff_burn", "icon_burn");
        assertIcon("eff_poison", "icon_poison");
        assertIcon("eff_heal", "icon_heal");
        assertIcon("eff_stun", "icon_stun");
        assertIcon("eff_slow", "icon_slow");
        assertIcon("eff_taunt", "icon_taunt");
        assertIcon("eff_dmg_buff", "icon_buff_dmg");
        assertIcon("eff_absorb", "icon_shield");
        assertIcon("eff_stress_reset", "icon_meditate");
    }

    @Test
    void iconKey_forUnknownEffect_isNull() {
        // Effects without a specific icon mapping should return null (not empty string).
        EffectData regen = findById("eff_regen");
        assertThat(regen).isNotNull();
        assertThat(regen.iconKey()).isNull();
    }

    @Test
    void iconKey_forPermanentDebuffs_isAffliction() {
        // Permanent debuff effects (affliction-type) should use icon_affliction.
        EffectData pd = findById("perm_debuff_01");
        assertThat(pd).isNotNull();
        assertThat(pd.iconKey()).isEqualTo("icon_affliction");
    }

    @Test
    void totalIconKeyPopulated_isAtLeast10() {
        long count = effects.stream().filter(e -> e.iconKey() != null).count();
        assertThat(count).isGreaterThanOrEqualTo(10)
                .as("At least 10 effects should have an iconKey populated");
    }

    private void assertIcon(String effectId, String expectedKey) {
        EffectData e = findById(effectId);
        assertThat(e).as("Effect not found: %s", effectId).isNotNull();
        assertThat(e.iconKey())
                .as("iconKey for %s", effectId)
                .isEqualTo(expectedKey);
    }

    private EffectData findById(String id) {
        return effects.stream()
                .filter(e -> id.equals(e.effectId()))
                .findFirst()
                .orElse(null);
    }
}
