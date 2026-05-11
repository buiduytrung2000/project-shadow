package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 11 B2 — Cowardly trait (trait_02) skip-turn behavior.
 * Threshold: stress > 50. Chance: 20% per turn-start.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConditionResolverCowardlyTest {

    private GameData gd;

    @BeforeAll
    void load() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        gd = GameData.loadFromDirectory(dataDir);
    }

    private Hero cowardlyHero(int stress) {
        Hero h = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        h.addTrait(ConditionResolver.TRAIT_COWARDLY);
        h.setCurrentStress(stress);
        return h;
    }

    private CombatEncounter encounterWith(Hero h) {
        Enemy e = new Enemy(gd.enemies().get("enemy_01"), Position.POS_1, gd.effects());
        return new CombatEncounter(List.of(h), List.of(e));
    }

    @Test
    void stressUnder50_neverSkips() {
        Hero h = cowardlyHero(40);
        CombatEncounter enc = encounterWith(h);
        Random rng = new Random(42L);
        for (int i = 0; i < 200; i++) {
            h.setSkipNextAction(false);
            ConditionResolver.onTurnStart(h, enc, rng);
            assertThat(h.skipNextAction()).isFalse();
        }
    }

    @Test
    void stressOver50_skipsRoughly20Percent() {
        Hero h = cowardlyHero(80);
        CombatEncounter enc = encounterWith(h);
        Random rng = new Random(42L);
        int skips = 0;
        int trials = 1000;
        for (int i = 0; i < trials; i++) {
            h.setSkipNextAction(false);
            ConditionResolver.onTurnStart(h, enc, rng);
            if (h.skipNextAction()) skips++;
        }
        double rate = skips / (double) trials;
        // 20% target, ±5% band for 1000 trials.
        assertThat(rate).isBetween(0.15, 0.25);
    }

    @Test
    void consumeSkipNextAction_clearsFlag() {
        Hero h = cowardlyHero(80);
        h.setSkipNextAction(true);
        assertThat(h.consumeSkipNextAction()).isTrue();
        assertThat(h.skipNextAction()).isFalse();
        // Second consume returns false.
        assertThat(h.consumeSkipNextAction()).isFalse();
    }

    @Test
    void heroWithoutCowardlyTrait_neverSkips() {
        Hero h = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        // No trait_02 added.
        h.setCurrentStress(80);
        CombatEncounter enc = encounterWith(h);
        Random rng = new Random(42L);
        for (int i = 0; i < 200; i++) {
            h.setSkipNextAction(false);
            ConditionResolver.onTurnStart(h, enc, rng);
            assertThat(h.skipNextAction()).isFalse();
        }
    }
}
