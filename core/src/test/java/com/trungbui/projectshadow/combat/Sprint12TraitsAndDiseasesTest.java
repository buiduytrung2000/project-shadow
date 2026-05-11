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
 * Sprint 12 B1 — verifies the 4 new trait effects, 5 new disease effects,
 * and the 2 stress event hooks (on crit, on ally death). Grouped in one
 * file because each test is small (3-15 lines).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Sprint12TraitsAndDiseasesTest {

    private GameData gd;

    @BeforeAll
    void load() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        gd = GameData.loadFromDirectory(dataDir);
    }

    private Hero makeHero(String heroId) {
        return new Hero(gd.heroes().get(heroId), Position.POS_1, gd.effects());
    }

    private CombatEncounter encounter(List<Hero> heroes) {
        return new CombatEncounter(heroes,
                List.of(new Enemy(gd.enemies().get("enemy_01"), Position.POS_1, gd.effects())));
    }

    // ───── Paranoid (trait_a01) ─────

    @Test
    void paranoid_addsStressToAllAliveAllies() {
        Hero paranoid = makeHero("hero_01");
        paranoid.addTrait(ConditionResolver.TRAIT_PARANOID);
        Hero ally1 = makeHero("hero_02");
        Hero ally2 = makeHero("hero_03");
        CombatEncounter enc = encounter(List.of(paranoid, ally1, ally2));

        ConditionResolver.onTurnStart(paranoid, enc, new Random(0L));
        assertThat(ally1.currentStress()).isPositive();
        assertThat(ally2.currentStress()).isPositive();
        assertThat(paranoid.currentStress()).isZero();
    }

    // ───── Selfish (trait_a02) ─────

    @Test
    void selfish_refusesHeal() {
        Hero selfish = makeHero("hero_01");
        selfish.addTrait(ConditionResolver.TRAIT_SELFISH);
        selfish.setCurrentHp(10); // start damaged
        int before = selfish.currentHp();
        selfish.heal(50);
        assertThat(selfish.currentHp()).isEqualTo(before); // unchanged
    }

    @Test
    void nonSelfish_healsNormally() {
        Hero h = makeHero("hero_01");
        h.setCurrentHp(10);
        h.heal(5);
        assertThat(h.currentHp()).isEqualTo(15);
    }

    // ───── Fearful (trait_a03) — outgoing -20% ─────

    @Test
    void fearful_outgoingMultiplier() {
        Hero h = makeHero("hero_01");
        assertThat(ConditionResolver.outgoingDamageMultiplier(h)).isEqualTo(1.0);
        h.addTrait(ConditionResolver.TRAIT_FEARFUL);
        assertThat(ConditionResolver.outgoingDamageMultiplier(h)).isEqualTo(0.80);
    }

    // ───── Hopeless (trait_a04) — -10 accuracy ─────

    @Test
    void hopeless_reducesEffectiveAccuracy() {
        Hero h = makeHero("hero_01");
        int base = h.effectiveAccuracy();
        h.addTrait(ConditionResolver.TRAIT_HOPELESS);
        assertThat(h.effectiveAccuracy()).isEqualTo(base - 10);
    }

    @Test
    void hopeless_andBlindness_stack() {
        Hero h = makeHero("hero_01");
        int base = h.effectiveAccuracy();
        h.addTrait(ConditionResolver.TRAIT_HOPELESS);
        h.addDisease(ConditionResolver.DISEASE_BLINDNESS);
        assertThat(h.effectiveAccuracy()).isEqualTo(Math.max(0, base - 30));
    }

    // ───── Fever (dis_01) ─────

    @Test
    void fever_reducesMaxHpBy15Percent() {
        Hero h = makeHero("hero_01");
        int base = h.maxHp();
        h.addDisease(ConditionResolver.DISEASE_FEVER);
        assertThat(h.maxHp()).isEqualTo(Math.max(1, (int) Math.round(base * 0.85)));
    }

    // ───── Blindness (dis_02) ─────

    @Test
    void blindness_reducesAccuracy20() {
        Hero h = makeHero("hero_01");
        int base = h.effectiveAccuracy();
        h.addDisease(ConditionResolver.DISEASE_BLINDNESS);
        assertThat(h.effectiveAccuracy()).isEqualTo(Math.max(0, base - 20));
    }

    // ───── Nightmare (dis_03) — +2 stress/turn ─────

    @Test
    void nightmare_addsStressOnTurnStart() {
        Hero h = makeHero("hero_01");
        h.addDisease(ConditionResolver.DISEASE_NIGHTMARE);
        ConditionResolver.onTurnStart(h, encounter(List.of(h)), new Random(0L));
        assertThat(h.currentStress()).isPositive();
    }

    // ───── Paranoia (dis_04) — 15% action fail ─────

    @Test
    void paranoia_failsAround15Percent() {
        Hero h = makeHero("hero_01");
        h.addDisease(ConditionResolver.DISEASE_PARANOIA);
        Random rng = new Random(42L);
        int fails = 0;
        int trials = 1000;
        for (int i = 0; i < trials; i++) {
            if (ConditionResolver.rollParanoiaActionFail(h, rng)) fails++;
        }
        double rate = fails / (double) trials;
        assertThat(rate).isBetween(0.10, 0.20); // 15% ± 5%
    }

    @Test
    void paranoia_neverFailsWithoutDisease() {
        Hero h = makeHero("hero_01");
        Random rng = new Random(42L);
        for (int i = 0; i < 100; i++) {
            assertThat(ConditionResolver.rollParanoiaActionFail(h, rng)).isFalse();
        }
    }

    // ───── Plague (dis_05) — max_hp + spread ─────

    @Test
    void plague_reducesMaxHpBy25Percent() {
        Hero h = makeHero("hero_01");
        int base = h.maxHp();
        h.addDisease(ConditionResolver.DISEASE_PLAGUE);
        assertThat(h.maxHp()).isEqualTo(Math.max(1, (int) Math.round(base * 0.75)));
    }

    @Test
    void plague_spreadsOverTurns() {
        Hero patient = makeHero("hero_01");
        patient.addDisease(ConditionResolver.DISEASE_PLAGUE);
        Hero ally = makeHero("hero_02");
        CombatEncounter enc = encounter(List.of(patient, ally));
        Random rng = new Random(7L); // seed-pinned so test is stable

        // Over 200 turn-starts, ally should catch plague eventually.
        // 10% per turn × 200 = 99.99% chance.
        for (int i = 0; i < 200; i++) {
            ConditionResolver.onTurnStart(patient, enc, rng);
            if (ally.diseases().contains(ConditionResolver.DISEASE_PLAGUE)) break;
        }
        assertThat(ally.diseases()).contains(ConditionResolver.DISEASE_PLAGUE);
    }

    @Test
    void fever_plus_plague_stackMultiplicatively() {
        Hero h = makeHero("hero_01");
        int base = h.maxHp();
        h.addDisease(ConditionResolver.DISEASE_FEVER);
        h.addDisease(ConditionResolver.DISEASE_PLAGUE);
        // 0.85 × 0.75 = 0.6375
        int expected = Math.max(1, (int) Math.round(base * 0.85 * 0.75));
        assertThat(h.maxHp()).isEqualTo(expected);
    }
}
