package com.trungbui.projectshadow.effect;

import com.trungbui.projectshadow.combat.AttackResult;
import com.trungbui.projectshadow.combat.DamageFormula;
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CombatScenarioTest {

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
        Path dataDir = resolveDataDir();
        gd = GameData.loadFromDirectory(dataDir);
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
    void bleedScenario_oneStack_dealsDamageOverThreeTurns() {
        Hero warrior = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        Enemy goblin = new Enemy(gd.enemies().get("enemy_01"), Position.POS_1, gd.effects());
        int initialHp = goblin.currentHp();

        goblin.activeEffects().apply("eff_bleed", warrior, goblin, new Random(1L));

        for (int turn = 1; turn <= 3; turn++) {
            goblin.activeEffects().onTurnStart(goblin, new Random(1L));
        }

        assertThat(goblin.currentHp()).isEqualTo(initialHp - 9);
        assertThat(goblin.activeEffects().size()).isZero();
    }

    @Test
    void burnScenario_threeStacks_compoundedDamage() {
        Enemy enemy = new Enemy(gd.enemies().get("enemy_02"), Position.POS_1, gd.effects());
        int initialHp = enemy.currentHp();

        for (int i = 0; i < 3; i++) {
            enemy.activeEffects().apply("eff_burn", null, enemy, new Random(42L));
        }

        Random rng = new Random(42L);
        enemy.activeEffects().onTurnStart(enemy, rng);

        int hpLost = initialHp - enemy.currentHp();
        assertThat(hpLost).isBetween(6, 15);
    }

    @Test
    void regenScenario_healsHotPerTurn() {
        Hero hero = new Hero(gd.heroes().get("hero_03"), Position.POS_1, gd.effects());
        hero.takeHpDamage(20);
        int hpBefore = hero.currentHp();

        hero.activeEffects().apply("eff_regen", null, hero, new Random(1L));
        hero.activeEffects().onTurnStart(hero, new Random(1L));

        assertThat(hero.currentHp()).isGreaterThan(hpBefore);
        assertThat(hero.currentHp()).isLessThanOrEqualTo(hero.maxHp());
    }

    @Test
    void poisonStacks_capAt3() {
        Enemy enemy = new Enemy(gd.enemies().get("enemy_01"), Position.POS_1, gd.effects());
        for (int i = 0; i < 10; i++) {
            enemy.activeEffects().apply("eff_poison", null, enemy, new Random(1L));
        }
        EffectInstance ei = enemy.activeEffects().find("eff_poison").orElseThrow();
        assertThat(ei.stacks()).isLessThanOrEqualTo(3);
    }

    @Test
    void permanentBuff_doesNotExpire() {
        Hero hero = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());

        if (gd.effects().containsKey("eff_perm_dmg")) {
            hero.activeEffects().apply("eff_perm_dmg", null, hero, new Random(1L));
            for (int i = 0; i < 50; i++) {
                hero.activeEffects().onTurnStart(hero, new Random(1L));
            }
            assertThat(hero.activeEffects().has("eff_perm_dmg")).isTrue();
        }
    }

    @Test
    void damageFormula_usesEffectiveAccuracy_fromBuff() {
        Hero attacker = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        Enemy target = new Enemy(gd.enemies().get("enemy_01"), Position.POS_1, gd.effects());
        SkillData skill = gd.skills().get("sk_w1");

        if (gd.effects().containsKey("eff_acc_buff")) {
            int baseAcc = attacker.accuracy();
            int effAccBefore = attacker.effectiveAccuracy();
            assertThat(effAccBefore).isEqualTo(baseAcc);

            attacker.activeEffects().apply("eff_acc_buff", null, attacker, new Random(1L));
            int effAccAfter = attacker.effectiveAccuracy();
            assertThat(effAccAfter).isGreaterThan(effAccBefore);
        }

        AttackResult r = DamageFormula.resolve(attacker, target, skill, new Random(1L));
        assertThat(r).isNotNull();
    }

    @Test
    void damageFormula_dmgBuff_increasesOutgoingDamage() {
        Hero attacker = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        Enemy target = new Enemy(gd.enemies().get("enemy_01"), Position.POS_1, gd.effects());

        if (!gd.effects().containsKey("eff_dmg_buff")) return;

        int baseMin = attacker.dmgMin();
        int baseMax = attacker.dmgMax();
        int effMinBefore = attacker.effectiveDmgMin();
        int effMaxBefore = attacker.effectiveDmgMax();
        assertThat(effMinBefore).isEqualTo(baseMin);
        assertThat(effMaxBefore).isEqualTo(baseMax);

        attacker.activeEffects().apply("eff_dmg_buff", null, attacker, new Random(1L));
        assertThat(attacker.effectiveDmgMin()).isGreaterThan(baseMin);
        assertThat(attacker.effectiveDmgMax()).isGreaterThan(baseMax);
    }

    @Test
    void damageFormula_markIncreasesDamageReceived() {
        Enemy target = new Enemy(gd.enemies().get("enemy_01"), Position.POS_1, gd.effects());

        if (!gd.effects().containsKey("eff_mark")) return;

        double baseMult = target.damageReceivedMultiplier();
        assertThat(baseMult).isEqualTo(1.0);

        target.activeEffects().apply("eff_mark", null, target, new Random(1L));
        double afterMult = target.damageReceivedMultiplier();
        assertThat(afterMult).isGreaterThan(1.0);
    }
}
