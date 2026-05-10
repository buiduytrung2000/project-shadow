package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.domain.Hero;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HealSkillTest {

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
        gd = GameData.loadFromDirectory(resolveDataDir());
    }

    private static Path resolveDataDir() {
        Path[] candidates = {
                Path.of("../assets/data"),
                Path.of("assets/data"),
                Path.of("../../assets/data")
        };
        for (Path p : candidates) {
            if (Files.isDirectory(p)) return p;
        }
        throw new IllegalStateException(
                "Cannot locate assets/data from cwd=" + Path.of("").toAbsolutePath());
    }

    @Test
    void clericHeal_targetsPickedAlly_notSelf() {
        CombatEncounter enc = CombatScenario.buildDefault(gd);
        CombatController c = new CombatController(enc, gd, new Random(1L));
        Hero cleric = enc.heroes().stream()
                .filter(h -> h.id().equals("hero_03"))
                .findFirst().orElseThrow();
        Hero injuredAlly = enc.heroes().get(0);
        injuredAlly.takeHpDamage(15);
        int allyHpBefore = injuredAlly.currentHp();
        int clericHpBefore = cleric.currentHp();

        c.start();
        while (!enc.isCombatOver() && c.currentActor().orElseThrow() != cleric) {
            Combatant actor = c.currentActor().orElseThrow();
            if (actor instanceof Hero) {
                if (c.skillRequiresTargetPick(0)) {
                    var picks = c.skillCandidateTargets(0);
                    if (!picks.isEmpty()) c.executePlayerSkill(0, picks.get(0));
                    else break;
                } else {
                    c.executePlayerSkill(0);
                }
            }
        }

        if (c.currentActor().orElseThrow() != cleric) return;

        int healSkillIdx = clericSkillIndex(c, "sk_c2");
        assertThat(healSkillIdx)
                .as("Cleric should have sk_c2 (Chữa Lành) at some skill index")
                .isGreaterThanOrEqualTo(0);
        assertThat(c.skillRequiresTargetPick(healSkillIdx)).isTrue();

        boolean ok = c.executePlayerSkill(healSkillIdx, injuredAlly);
        assertThat(ok).isTrue();

        assertThat(injuredAlly.currentHp())
                .as("picked ally should be healed")
                .isGreaterThan(allyHpBefore);
        assertThat(cleric.currentHp())
                .as("cleric should NOT be healed when targeting another ally")
                .isLessThanOrEqualTo(clericHpBefore);
    }

    @Test
    void clericHeal_appliesSkillMagnitude_14hp() {
        CombatEncounter enc = CombatScenario.buildDefault(gd);
        Hero cleric = enc.heroes().stream()
                .filter(h -> h.id().equals("hero_03"))
                .findFirst().orElseThrow();
        Hero injuredAlly = enc.heroes().get(0);
        injuredAlly.takeHpDamage(20);
        int allyHpBefore = injuredAlly.currentHp();

        injuredAlly.activeEffects().apply(
                "eff_heal", cleric, injuredAlly, new Random(1L), "14"
        );

        int delta = injuredAlly.currentHp() - allyHpBefore;
        assertThat(delta)
                .as("eff_heal with skill magnitude '14' heals exactly 14 HP")
                .isEqualTo(14);
    }

    @Test
    void healWithoutSkillMagnitudeOverride_usesEffectDataValueOnly() {
        CombatEncounter enc = CombatScenario.buildDefault(gd);
        Hero hero = enc.heroes().get(0);
        hero.takeHpDamage(20);
        int hpBefore = hero.currentHp();

        hero.activeEffects().apply("eff_heal", hero, hero, new Random(1L));

        assertThat(hero.currentHp())
                .as("eff_heal with placeholder '+' modifier and no override should not heal")
                .isEqualTo(hpBefore);
    }

    @Test
    void healCannotExceedMaxHp() {
        CombatEncounter enc = CombatScenario.buildDefault(gd);
        Hero hero = enc.heroes().get(0);
        hero.takeHpDamage(5);
        int max = hero.maxHp();

        hero.activeEffects().apply(
                "eff_heal", hero, hero, new Random(1L), "999"
        );

        assertThat(hero.currentHp()).isEqualTo(max);
    }

    private static int clericSkillIndex(CombatController c, String skillId) {
        var skills = c.currentActorSkills();
        for (int i = 0; i < skills.size(); i++) {
            if (skills.get(i).skillId().equals(skillId)) return i;
        }
        return -1;
    }
}
