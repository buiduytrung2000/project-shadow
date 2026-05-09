package com.trungbui.projectshadow.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CombatantStateTest {

    @Test
    void hero_starts_at_full_hp_zero_stress() {
        Hero h = new Hero(Fixtures.heroData("hero_01", 30, 5, 10, 80, 0.05, 5), Position.POS_1);
        assertThat(h.currentHp()).isEqualTo(30);
        assertThat(h.maxHp()).isEqualTo(30);
        assertThat(h.currentStress()).isZero();
        assertThat(h.isAlive()).isTrue();
        assertThat(h.isAfflicted()).isFalse();
    }

    @Test
    void hero_takeHpDamage_clamps_to_zero() {
        Hero h = new Hero(Fixtures.heroData("hero_01", 30, 5, 10, 80, 0.05, 5), Position.POS_1);
        h.takeHpDamage(100);
        assertThat(h.currentHp()).isZero();
        assertThat(h.isAlive()).isFalse();
    }

    @Test
    void hero_heal_clamps_to_maxHp() {
        Hero h = new Hero(Fixtures.heroData("hero_01", 30, 5, 10, 80, 0.05, 5), Position.POS_1);
        h.takeHpDamage(20);
        h.heal(50);
        assertThat(h.currentHp()).isEqualTo(30);
    }

    @Test
    void hero_stressResist_reduces_incoming_stress() {
        Hero h = new Hero(Fixtures.heroData("hero_01", 30, 5, 10, 80, 0.05, 5), Position.POS_1);
        h.takeStressDamage(10);
        assertThat(h.currentStress()).isEqualTo(8);
    }

    @Test
    void hero_isAfflicted_at_threshold_100() {
        Hero h = new Hero(Fixtures.heroData("hero_01", 30, 5, 10, 80, 0.05, 5), Position.POS_1);
        h.takeStressDamage(125);
        assertThat(h.currentStress()).isEqualTo(100);
        assertThat(h.isAfflicted()).isTrue();
    }

    @Test
    void hero_stress_clamps_to_max_200() {
        Hero h = new Hero(Fixtures.heroData("hero_01", 30, 5, 10, 80, 0.0, 5), Position.POS_1);
        for (int i = 0; i < 30; i++) h.takeStressDamage(20);
        assertThat(h.currentStress()).isEqualTo(Hero.STRESS_MAX);
    }

    @Test
    void hero_reduceStress_clamps_to_zero() {
        Hero h = new Hero(Fixtures.heroData("hero_01", 30, 5, 10, 80, 0.0, 5), Position.POS_1);
        h.takeStressDamage(50);
        h.reduceStress(100);
        assertThat(h.currentStress()).isZero();
    }

    @Test
    void hero_levelUp_scales_stats() {
        Hero h = new Hero(Fixtures.heroData("hero_01", 30, 5, 10, 80, 0.05, 5), Position.POS_1);
        h.setLevel(2);
        assertThat(h.maxHp()).isEqualTo(30 + 2 * 3);
        assertThat(h.dmgMin()).isEqualTo(5 + 2);
        assertThat(h.dmgMax()).isEqualTo(10 + 2);
        assertThat(h.critChance()).isCloseTo(0.05 + 2 * 0.01, org.assertj.core.api.Assertions.within(1e-9));
    }

    @Test
    void hero_cooldown_lifecycle() {
        Hero h = new Hero(Fixtures.heroData("hero_01", 30, 5, 10, 80, 0.05, 5), Position.POS_1);
        h.putOnCooldown("sk_w2", 2);
        assertThat(h.isOnCooldown("sk_w2")).isTrue();
        h.tickCooldowns();
        assertThat(h.isOnCooldown("sk_w2")).isTrue();
        h.tickCooldowns();
        assertThat(h.isOnCooldown("sk_w2")).isFalse();
    }

    @Test
    void hero_constructor_rejects_more_than_4_skills() {
        assertThatThrownBy(() -> new Hero(
                Fixtures.heroData("hero_01", 30, 5, 10, 80, 0.05, 5),
                0, Position.POS_1,
                java.util.List.of("a", "b", "c", "d", "e")
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enemy_starts_at_full_hp() {
        Enemy e = new Enemy(Fixtures.enemyData("enemy_01", 12, 3, 6, 80, 0.05), Position.POS_1);
        assertThat(e.currentHp()).isEqualTo(12);
        assertThat(e.maxHp()).isEqualTo(12);
        assertThat(e.isAlive()).isTrue();
        assertThat(e.isBoss()).isFalse();
    }

    @Test
    void enemy_takeHpDamage_kills() {
        Enemy e = new Enemy(Fixtures.enemyData("enemy_01", 12, 3, 6, 80, 0.05), Position.POS_1);
        e.takeHpDamage(20);
        assertThat(e.currentHp()).isZero();
        assertThat(e.isAlive()).isFalse();
    }

    @Test
    void position_rank_and_front_back() {
        assertThat(Position.POS_1.rank()).isEqualTo(1);
        assertThat(Position.POS_4.rank()).isEqualTo(4);
        assertThat(Position.POS_1.isFront()).isTrue();
        assertThat(Position.POS_2.isFront()).isTrue();
        assertThat(Position.POS_3.isBack()).isTrue();
        assertThat(Position.POS_4.isBack()).isTrue();
        assertThat(Position.ofRank(3)).isEqualTo(Position.POS_3);
        assertThatThrownBy(() -> Position.ofRank(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.ofRank(5)).isInstanceOf(IllegalArgumentException.class);
    }
}
