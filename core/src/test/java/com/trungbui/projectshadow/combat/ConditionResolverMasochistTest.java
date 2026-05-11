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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 11 B2 — Masochist trait (trait_04) propagates +5 stress to all
 * alive allies when the bearer takes self-damage.
 *
 * <p>Note: No HP-cost skills exist in skills.csv yet. The handler is wired
 * but won't trigger from gameplay until Sprint 12+. This test directly
 * invokes the handler to verify behavior.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConditionResolverMasochistTest {

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
        Enemy e = new Enemy(gd.enemies().get("enemy_01"), Position.POS_1, gd.effects());
        return new CombatEncounter(heroes, List.of(e));
    }

    @Test
    void masochistSelfDamage_propagatesStressToAllies() {
        Hero masochist = makeHero("hero_01");
        masochist.addTrait(ConditionResolver.TRAIT_MASOCHIST);
        Hero ally1 = makeHero("hero_02");
        Hero ally2 = makeHero("hero_03");
        CombatEncounter enc = encounter(List.of(masochist, ally1, ally2));

        ConditionResolver.onSelfDamage(masochist, enc);
        // takeStressDamage applies hero's stressResist reduction, so actual value
        // may be less than the raw amount. Verify stress increased + bounded by
        // raw amount.
        assertThat(ally1.currentStress()).isPositive();
        assertThat(ally2.currentStress()).isPositive();
        assertThat(ally1.currentStress())
                .isLessThanOrEqualTo(ConditionResolver.MASOCHIST_STRESS_TO_ALLIES);
        // Self should NOT receive the stress.
        assertThat(masochist.currentStress()).isZero();
    }

    @Test
    void heroWithoutTrait_noPropagation() {
        Hero notMasochist = makeHero("hero_01");
        Hero ally = makeHero("hero_02");
        CombatEncounter enc = encounter(List.of(notMasochist, ally));

        ConditionResolver.onSelfDamage(notMasochist, enc);
        assertThat(ally.currentStress()).isZero();
    }

    @Test
    void deadAllies_notAffected() {
        Hero masochist = makeHero("hero_01");
        masochist.addTrait(ConditionResolver.TRAIT_MASOCHIST);
        Hero dead = makeHero("hero_02");
        dead.setCurrentHp(0); // dead
        Hero alive = makeHero("hero_03");
        CombatEncounter enc = encounter(List.of(masochist, dead, alive));

        ConditionResolver.onSelfDamage(masochist, enc);
        assertThat(dead.currentStress()).isZero();
        // Live ally received SOME stress (resist reduced from raw).
        assertThat(alive.currentStress()).isPositive();
        assertThat(alive.currentStress())
                .isLessThanOrEqualTo(ConditionResolver.MASOCHIST_STRESS_TO_ALLIES);
    }
}
