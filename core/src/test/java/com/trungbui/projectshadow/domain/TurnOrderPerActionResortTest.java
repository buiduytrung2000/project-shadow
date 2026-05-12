package com.trungbui.projectshadow.domain;

import com.trungbui.projectshadow.data.GameData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 12 B4 — per-action turn-order re-sort: a speed buff applied
 * mid-round takes effect on the very next pick. Pre-Sprint-12 the order
 * was fixed at round start and buffs only mattered for the next round.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TurnOrderPerActionResortTest {

    private GameData gd;

    @BeforeAll
    void load() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        gd = GameData.loadFromDirectory(dataDir);
    }

    /** Simple Combatant stub with a directly-injectable speed. Avoids
     *  having to roll real effect magnitudes through ActiveEffects. */
    private static class SpeedStubHero extends Hero {
        private int dynamicSpeed;
        SpeedStubHero(com.trungbui.projectshadow.data.model.HeroData data, int speed,
                      java.util.Map<String, com.trungbui.projectshadow.data.model.EffectData> effects) {
            super(data, Position.POS_1, effects);
            this.dynamicSpeed = speed;
        }
        @Override public int speed() { return dynamicSpeed; }
        void setSpeed(int s) { this.dynamicSpeed = s; }
    }

    private static class SpeedStubEnemy extends Enemy {
        private int dynamicSpeed;
        SpeedStubEnemy(com.trungbui.projectshadow.data.model.EnemyData data, int speed,
                       java.util.Map<String, com.trungbui.projectshadow.data.model.EffectData> effects) {
            super(data, Position.POS_1, effects);
            this.dynamicSpeed = speed;
        }
        @Override public int speed() { return dynamicSpeed; }
    }

    @Test
    void advanceTurn_pickedNextActorReflectsCurrentSpeed() {
        // Two heroes + one enemy. A:10, B:6, E:4 → initial order [A,B,E].
        // After A acts, bump B's speed to 9. Remaining: B(9), E(4) → B picked.
        var heroData = gd.heroes().get("hero_01");
        var enemyData = gd.enemies().get("enemy_01");
        SpeedStubHero a = new SpeedStubHero(heroData, 10, gd.effects());
        SpeedStubHero b = new SpeedStubHero(heroData, 6, gd.effects());
        Enemy e = new SpeedStubEnemy(enemyData, 4, gd.effects());

        CombatEncounter enc = new CombatEncounter(List.of(a, b), List.of(e));
        enc.startRound();
        assertThat(enc.currentActor()).isEqualTo(a);

        // Mid-round speed change — pre-Sprint-12 wouldn't be honored until next round.
        b.setSpeed(9);
        enc.advanceTurn();

        assertThat(enc.currentActor()).isEqualTo(b);
        enc.advanceTurn();
        assertThat(enc.currentActor()).isEqualTo(e);
    }

    @Test
    void advanceTurn_tailReordersByCurrentSpeed() {
        // 3 alive after A acts: B(7), C(5), D(3). Initial order [A,B,C,D].
        // Mid-round buff D to speed 12 → new tail order: [D,B,C].
        var heroData = gd.heroes().get("hero_01");
        var enemyData = gd.enemies().get("enemy_01");
        SpeedStubHero a = new SpeedStubHero(heroData, 20, gd.effects());
        SpeedStubHero b = new SpeedStubHero(heroData, 7, gd.effects());
        SpeedStubHero c = new SpeedStubHero(heroData, 5, gd.effects());
        SpeedStubEnemy d = new SpeedStubEnemy(enemyData, 3, gd.effects());

        CombatEncounter enc = new CombatEncounter(List.of(a, b, c), List.of(d));
        enc.startRound();
        assertThat(enc.currentActor()).isEqualTo(a);

        d.dynamicSpeed = 12;
        enc.advanceTurn();

        // Per-action resort: D (now 12) jumps ahead of B (7) and C (5).
        assertThat(enc.currentActor()).isEqualTo(d);
    }

    @Test
    void advanceTurn_actedCombatantNeverPickedTwice() {
        // 2 heroes — A acts, B acts, no more. One-action-per-actor invariant.
        var heroData = gd.heroes().get("hero_01");
        var enemyData = gd.enemies().get("enemy_01");
        SpeedStubHero a = new SpeedStubHero(heroData, 10, gd.effects());
        SpeedStubHero b = new SpeedStubHero(heroData, 5, gd.effects());
        SpeedStubEnemy e = new SpeedStubEnemy(enemyData, 1, gd.effects());

        CombatEncounter enc = new CombatEncounter(List.of(a, b), List.of(e));
        enc.startRound();
        assertThat(enc.currentActor()).isEqualTo(a);
        enc.advanceTurn();
        assertThat(enc.currentActor()).isEqualTo(b);
        enc.advanceTurn();
        assertThat(enc.currentActor()).isEqualTo(e);
        enc.advanceTurn();
        assertThat(enc.phase()).isEqualTo(CombatPhase.END_OF_ROUND);
    }
}
