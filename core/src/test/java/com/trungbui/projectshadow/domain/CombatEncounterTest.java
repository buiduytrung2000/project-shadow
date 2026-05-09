package com.trungbui.projectshadow.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CombatEncounterTest {

    private Hero hero(String id, int speed) {
        return new Hero(Fixtures.heroData(id, 30, 5, 10, 80, 0.05, speed), Position.POS_1);
    }

    private Enemy enemy(String id) {
        return new Enemy(Fixtures.enemyData(id, 12, 3, 6, 80, 0.05), Position.POS_1);
    }

    @Test
    void constructor_requires_at_least_one_hero_and_one_enemy() {
        assertThatThrownBy(() -> new CombatEncounter(List.of(), List.of(enemy("e1"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CombatEncounter(List.of(hero("h1", 5)), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void initial_state_is_combatStart_round0() {
        CombatEncounter c = new CombatEncounter(List.of(hero("h1", 5)), List.of(enemy("e1")));
        assertThat(c.phase()).isEqualTo(CombatPhase.COMBAT_START);
        assertThat(c.roundNumber()).isZero();
        assertThat(c.currentActor()).isNull();
    }

    @Test
    void startRound_orders_combatants_by_speed_descending() {
        Hero fast = hero("hf", 8);
        Hero slow = hero("hs", 3);
        Enemy mid = enemy("em");
        CombatEncounter c = new CombatEncounter(List.of(fast, slow), List.of(mid));
        c.startRound();
        assertThat(c.roundNumber()).isEqualTo(1);
        List<Combatant> order = c.turnOrder();
        assertThat(order).hasSize(3);
        assertThat(order.get(0).id()).isEqualTo("hf");
        assertThat(order.get(1).id()).isEqualTo("em");
        assertThat(order.get(2).id()).isEqualTo("hs");
    }

    @Test
    void startRound_sets_phase_based_on_first_actor() {
        Hero h = hero("h1", 9);
        Enemy e = enemy("e1");
        CombatEncounter c = new CombatEncounter(List.of(h), List.of(e));
        c.startRound();
        assertThat(c.phase()).isEqualTo(CombatPhase.PLAYER_TURN_START);
        assertThat(c.currentActor()).isEqualTo(h);
    }

    @Test
    void advanceTurn_progresses_through_order() {
        Hero h = hero("h1", 9);
        Enemy e = enemy("e1");
        CombatEncounter c = new CombatEncounter(List.of(h), List.of(e));
        c.startRound();
        assertThat(c.currentActor()).isEqualTo(h);
        boolean moved = c.advanceTurn();
        assertThat(moved).isTrue();
        assertThat(c.currentActor()).isEqualTo(e);
        assertThat(c.phase()).isEqualTo(CombatPhase.ENEMY_TURN_START);
        moved = c.advanceTurn();
        assertThat(moved).isFalse();
        assertThat(c.phase()).isEqualTo(CombatPhase.END_OF_ROUND);
    }

    @Test
    void advanceTurn_skips_dead_combatants() {
        Hero h = hero("h1", 9);
        Enemy e1 = enemy("e1");
        Enemy e2 = enemy("e2");
        CombatEncounter c = new CombatEncounter(List.of(h), List.of(e1, e2));
        c.startRound();
        e1.takeHpDamage(100);
        c.advanceTurn();
        assertThat(c.currentActor()).isEqualTo(e2);
    }

    @Test
    void isCombatOver_when_all_heroes_dead() {
        Hero h = hero("h1", 9);
        Enemy e = enemy("e1");
        CombatEncounter c = new CombatEncounter(List.of(h), List.of(e));
        h.takeHpDamage(100);
        assertThat(c.isCombatOver()).isTrue();
        assertThat(c.winningSide()).isEqualTo(CombatEncounter.Side.ENEMIES);
    }

    @Test
    void isCombatOver_when_all_enemies_dead() {
        Hero h = hero("h1", 9);
        Enemy e = enemy("e1");
        CombatEncounter c = new CombatEncounter(List.of(h), List.of(e));
        e.takeHpDamage(100);
        assertThat(c.isCombatOver()).isTrue();
        assertThat(c.winningSide()).isEqualTo(CombatEncounter.Side.HEROES);
    }

    @Test
    void notOver_when_both_sides_have_alive() {
        Hero h = hero("h1", 9);
        Enemy e = enemy("e1");
        CombatEncounter c = new CombatEncounter(List.of(h), List.of(e));
        assertThat(c.isCombatOver()).isFalse();
        assertThat(c.winningSide()).isNull();
    }

    @Test
    void startRound_after_oneSide_wiped_sets_winLose_phase() {
        Hero h = hero("h1", 9);
        Enemy e = enemy("e1");
        CombatEncounter c = new CombatEncounter(List.of(h), List.of(e));
        e.takeHpDamage(100);
        c.startRound();
        assertThat(c.phase()).isEqualTo(CombatPhase.COMBAT_WIN);
    }
}
