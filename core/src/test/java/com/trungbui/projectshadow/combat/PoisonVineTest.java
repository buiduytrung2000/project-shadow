package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.EnemyData;
import com.trungbui.projectshadow.data.model.EnemySkillData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 12 B3 — verify Poison Vine enemy + sk_e_poison_strike are wired
 * into data CSVs with the right shape. Behavioral test (poison-on-hit rolls)
 * is implicit through existing ActiveEffects coverage — this just verifies the
 * data layer.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PoisonVineTest {

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        gd = GameData.loadFromDirectory(dataDir);
    }

    @Test
    void poisonVine_isLoadedAsBaseVariant() {
        EnemyData ed = gd.enemies().get("enemy_05");
        assertThat(ed).as("enemy_05 Poison Vine should be loaded").isNotNull();
        assertThat(ed.variantType()).isEqualTo("Base");
        assertThat(ed.skill1()).isEqualTo("sk_e_poison_strike");
    }

    @Test
    void poisonStrike_skillIsLoaded() {
        EnemySkillData s = gd.enemySkills().get("sk_e_poison_strike");
        assertThat(s).as("sk_e_poison_strike should be loaded").isNotNull();
        assertThat(s.isOffensive()).isTrue();
        assertThat(s.effectId()).isEqualTo("eff_poison");
    }

    @Test
    void poisonStrike_effectIsValidReference() {
        EnemySkillData s = gd.enemySkills().get("sk_e_poison_strike");
        assertThat(gd.effects()).containsKey(s.effectId());
    }
}
