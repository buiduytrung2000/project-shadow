package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.model.SkillData;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Fixtures;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class DamageFormulaTest {

    @Test
    void hit_when_roll_below_hitChance() {
        Hero attacker = new Hero(Fixtures.heroData("hero_test", 30, 5, 10, 80, 0.0, 5), Position.POS_1);
        Enemy target = new Enemy(Fixtures.enemyData("enemy_test", 20, 3, 6, 80, 0.0), Position.POS_1);
        SkillData skill = Fixtures.skill("sk_test", 1.0, 0, 0);

        Random alwaysLow = new Random() {
            @Override public int nextInt(int bound) { return 0; }
            @Override public double nextDouble() { return 1.0; }
        };
        AttackResult r = DamageFormula.resolve(attacker, target, skill, alwaysLow);
        assertThat(r.hit()).isTrue();
    }

    @Test
    void miss_when_roll_at_or_above_hitChance() {
        Hero attacker = new Hero(Fixtures.heroData("hero_test", 30, 5, 10, 80, 0.0, 5), Position.POS_1);
        Enemy target = new Enemy(Fixtures.enemyData("enemy_test", 20, 3, 6, 80, 0.0), Position.POS_1);
        SkillData skill = Fixtures.skill("sk_test", 1.0, 0, 0);

        Random alwaysHigh = new Random() {
            @Override public int nextInt(int bound) { return 99; }
            @Override public double nextDouble() { return 1.0; }
        };
        AttackResult r = DamageFormula.resolve(attacker, target, skill, alwaysHigh);
        assertThat(r.hit()).isFalse();
        assertThat(r.hpDamage()).isZero();
        assertThat(r.stressDamage()).isZero();
    }

    @Test
    void hitChance_clamped_to_0_when_negative() {
        Hero attacker = new Hero(Fixtures.heroData("hero_test", 30, 5, 10, 50, 0.0, 5), Position.POS_1);
        Enemy target = new Enemy(Fixtures.enemyData("enemy_test", 20, 3, 6, 80, 0.0), Position.POS_1) {
            @Override public int dodge() { return 200; }
        };
        SkillData skill = Fixtures.skill("sk_test", 1.0, 0, 0);

        Random rng = new Random(42L);
        for (int i = 0; i < 100; i++) {
            AttackResult r = DamageFormula.resolve(attacker, target, skill, rng);
            assertThat(r.hit()).as("iter %d", i).isFalse();
        }
    }

    @Test
    void hitChance_clamped_to_100_when_over() {
        Hero attacker = new Hero(Fixtures.heroData("hero_test", 30, 5, 10, 200, 0.0, 5), Position.POS_1);
        Enemy target = new Enemy(Fixtures.enemyData("enemy_test", 20, 3, 6, 80, 0.0), Position.POS_1);
        SkillData skill = Fixtures.skill("sk_test", 1.0, 50, 0);

        Random rng = new Random(42L);
        for (int i = 0; i < 100; i++) {
            AttackResult r = DamageFormula.resolve(attacker, target, skill, rng);
            assertThat(r.hit()).as("iter %d", i).isTrue();
        }
    }

    @Test
    void damage_within_min_max_times_multiplier_no_crit() {
        Hero attacker = new Hero(Fixtures.heroData("hero_test", 30, 5, 10, 100, 0.0, 5), Position.POS_1);
        Enemy target = new Enemy(Fixtures.enemyData("enemy_test", 20, 3, 6, 80, 0.0), Position.POS_1);
        SkillData skill = Fixtures.skill("sk_test", 1.0, 0, 0);

        Random rng = new Random(123L);
        for (int i = 0; i < 200; i++) {
            AttackResult r = DamageFormula.resolve(attacker, target, skill, rng);
            assertThat(r.hit()).isTrue();
            if (!r.crit()) {
                assertThat(r.hpDamage()).isBetween(5, 10);
            }
        }
    }

    @Test
    void crit_multiplies_damage_by_1_5() {
        Hero attacker = new Hero(Fixtures.heroData("hero_test", 30, 10, 10, 100, 1.0, 5), Position.POS_1);
        Enemy target = new Enemy(Fixtures.enemyData("enemy_test", 20, 3, 6, 80, 0.0), Position.POS_1);
        SkillData skill = Fixtures.skill("sk_test", 1.0, 0, 0);

        Random rng = new Random(7L);
        AttackResult r = DamageFormula.resolve(attacker, target, skill, rng);
        assertThat(r.hit()).isTrue();
        assertThat(r.crit()).isTrue();
        assertThat(r.hpDamage()).isEqualTo(15);
    }

    @Test
    void skill_damageMultiplier_scales_damage() {
        Hero attacker = new Hero(Fixtures.heroData("hero_test", 30, 10, 10, 100, 0.0, 5), Position.POS_1);
        Enemy target = new Enemy(Fixtures.enemyData("enemy_test", 20, 3, 6, 80, 0.0), Position.POS_1);
        SkillData skill = Fixtures.skill("sk_test", 0.5, 0, 0);

        Random rng = new Random(1L);
        AttackResult r = DamageFormula.resolve(attacker, target, skill, rng);
        assertThat(r.hit()).isTrue();
        assertThat(r.crit()).isFalse();
        assertThat(r.hpDamage()).isEqualTo(5);
    }

    @Test
    void stressDamage_passed_through_from_skill() {
        Hero attacker = new Hero(Fixtures.heroData("hero_test", 30, 10, 10, 100, 0.0, 5), Position.POS_1);
        Enemy target = new Enemy(Fixtures.enemyData("enemy_test", 20, 3, 6, 80, 0.0), Position.POS_1);
        SkillData skill = Fixtures.skill("sk_test", 1.0, 0, 8);

        Random rng = new Random(1L);
        AttackResult r = DamageFormula.resolve(attacker, target, skill, rng);
        assertThat(r.stressDamage()).isEqualTo(8);
    }
}
