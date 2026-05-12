package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.SkillData;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 11 B1 — verify post-buff enemy hit rates. Player feedback:
 * "Enemy đang miss rất nhiều". Per design lock, normal = 80, boss = 90.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnemyHitRateBoostTest {

    private GameData gd;

    @BeforeAll
    void load() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        gd = GameData.loadFromDirectory(dataDir);
    }

    private static SkillData wrapEnemySkill(com.trungbui.projectshadow.data.model.EnemySkillData es) {
        return new SkillData(
                es.skillId(), es.nameVn(), es.nameEn(),
                es.userType(), es.userType(),
                es.isOffensive(), es.targetType(),
                es.damageMultiplier(), es.accuracyModifier(), es.cooldown(),
                es.effectId(), es.effectValue(), null, null,
                es.stressDamage(), "Common", es.descriptionVn(), null
        );
    }

    @Test
    void skeleton_postBuff_acc80() {
        // Skeleton was acc 75, now acc 80 (Sprint 11 B1 normalize).
        // sk_e_bone_club: accuracyModifier 0 (verify in csv)
        assertThat(gd.enemies().get("enemy_02").accuracy()).isEqualTo(80);

        Enemy skel = new Enemy(gd.enemies().get("enemy_02"), Position.POS_1, gd.effects());
        SkillData stab = wrapEnemySkill(gd.enemySkills().get("sk_e_bone_club"));
        int trials = 1000, hits = 0;
        Random rng = new Random(7L);
        for (int i = 0; i < trials; i++) {
            Hero h = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
            if (DamageFormula.resolve(skel, h, stab, rng).hit()) hits++;
        }
        // Hit chance = clamp(80 + 0 - 0, 0, 100) = 80. Expect ~80%.
        double rate = hits / (double) trials;
        assertThat(rate).isBetween(0.74, 0.86);
    }

    @Test
    void boss_b01_postBuff_acc90() {
        // Boss b01 was acc 78, now acc 90 (Sprint 11 B1 boost).
        assertThat(gd.enemies().get("enemy_b01").accuracy()).isEqualTo(90);

        Enemy boss = new Enemy(gd.enemies().get("enemy_b01"), Position.POS_1, gd.effects());
        SkillData smash = wrapEnemySkill(gd.enemySkills().get("sk_e_b01_smash"));
        int trials = 1000, hits = 0;
        Random rng = new Random(1L);
        for (int i = 0; i < trials; i++) {
            Hero h = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
            if (DamageFormula.resolve(boss, h, smash, rng).hit()) hits++;
        }
        double rate = hits / (double) trials;
        // Hit chance = clamp(90 + 0 - 0, 0, 100) = 90. Expect ~90%.
        assertThat(rate).isBetween(0.84, 0.96);
    }

    @Test
    void allBossesAreNowAcc90() {
        assertThat(gd.enemies().get("enemy_b01").accuracy()).isEqualTo(90);
        assertThat(gd.enemies().get("enemy_b02").accuracy()).isEqualTo(90);
        assertThat(gd.enemies().get("enemy_b03").accuracy()).isEqualTo(90);
    }

    @Test
    void minibossesAreNowAcc85() {
        assertThat(gd.enemies().get("enemy_mb01").accuracy()).isEqualTo(85);
        assertThat(gd.enemies().get("enemy_mb02").accuracy()).isEqualTo(85);
    }
}
