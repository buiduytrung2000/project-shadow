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
 * Investigation test (post Sprint 10): user reported enemies "auto-missing"
 * every attack. This empirically measures hit rate to validate or reject the
 * claim. Goblin (accuracy 80) attacking Warrior (dodge 0, no buffs) should
 * hit ≥ 70% of the time per the design formula.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnemyHitRateTest {

    private GameData gd;

    @BeforeAll
    void load() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        gd = GameData.loadFromDirectory(dataDir);
    }

    @Test
    void goblinAttacksFreshHero_hitsAtAdvertisedRate() {
        // enemy_01 Goblin: accuracy 80. sk_e_stab: accuracyModifier 0, damageMultiplier 1.0.
        // hero_01 Warrior: dodge 0 (Hero.dodge() hardcoded returns 0).
        // → hit chance = clamp(80 + 0 - 0, 0, 100) = 80. Expect ~80% hits.
        Enemy goblin = new Enemy(gd.enemies().get("enemy_01"), Position.POS_1, gd.effects());
        SkillData stab = wrapEnemyStab();

        int trials = 1000;
        int hits = 0;
        Random rng = new Random(42L);
        for (int i = 0; i < trials; i++) {
            // Fresh hero each trial — no carried-over effects.
            Hero hero = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
            AttackResult r = DamageFormula.resolve(goblin, hero, stab, rng);
            if (r.hit()) hits++;
        }

        double rate = hits / (double) trials;
        assertThat(rate)
                .as("Goblin vs Warrior hit rate (expect ~0.80; <0.20 = bug)")
                .isBetween(0.70, 0.90);
    }

    @Test
    void bossB01AttacksFreshHero_hitsAtAdvertisedRate() {
        // enemy_b01 Giant Zombie: accuracy 78. sk_e_b01_smash: accuracyModifier 0.
        // Expect ~78% hits.
        Enemy boss = new Enemy(gd.enemies().get("enemy_b01"), Position.POS_1, gd.effects());
        var skillData = gd.enemySkills().get("sk_e_b01_smash");
        SkillData wrapped = new SkillData(
                skillData.skillId(), skillData.nameVn(), skillData.nameEn(),
                skillData.userType(), skillData.userType(),
                skillData.isOffensive(), skillData.targetType(),
                skillData.damageMultiplier(), skillData.accuracyModifier(), skillData.cooldown(),
                skillData.effectId(), skillData.effectValue(), null, null,
                skillData.stressDamage(), "Common", skillData.descriptionVn()
        );

        int trials = 1000;
        int hits = 0;
        Random rng = new Random(1L);
        for (int i = 0; i < trials; i++) {
            Hero hero = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
            AttackResult r = DamageFormula.resolve(boss, hero, wrapped, rng);
            if (r.hit()) hits++;
        }
        double rate = hits / (double) trials;
        assertThat(rate).isBetween(0.68, 0.88);
    }

    private SkillData wrapEnemyStab() {
        var es = gd.enemySkills().get("sk_e_stab");
        return new SkillData(
                es.skillId(), es.nameVn(), es.nameEn(),
                es.userType(), es.userType(),
                es.isOffensive(), es.targetType(),
                es.damageMultiplier(), es.accuracyModifier(), es.cooldown(),
                es.effectId(), es.effectValue(), null, null,
                es.stressDamage(), "Common", es.descriptionVn()
        );
    }
}
