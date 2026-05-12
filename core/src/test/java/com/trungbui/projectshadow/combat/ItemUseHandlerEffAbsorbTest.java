package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import com.trungbui.projectshadow.domain.Fixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 13 B1 — verify {@link ItemUseHandler} applies {@code eff_absorb} shield and
 * that {@link DamageFormula} absorbs damage from the shield before HP.
 *
 * <p>item_c07 (Ward Charm) has effectId=eff_absorb and effectValue="absorb next 20 dmg".
 * Because the effectValue has no leading integer, ItemUseHandler falls back to 17 shield HP.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemUseHandlerEffAbsorbTest {

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
        Path[] candidates = {Path.of("../assets/data"), Path.of("assets/data")};
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) {
            dataDir = p;
            break;
        }
        gd = GameData.loadFromDirectory(dataDir);
    }

    @BeforeEach
    void resetStageState() {
        // Guard against static stage-env-mod leaking from StageEnvModifierTest.
        ConditionResolver.stageAccuracyMod = 0;
        ConditionResolver.stageStressPerTurn = 0;
    }

    private Hero freshHero() {
        return new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
    }

    /** item_c07 = Bùa Hộ Mệnh, effectId=eff_absorb */
    @Test
    void apply_effAbsorb_setsShieldHp() {
        Hero hero = freshHero();
        assertThat(hero.shieldHp()).isZero();

        ItemUseHandler.AppliedSummary s = ItemUseHandler.apply("item_c07", hero, hero, null, gd);

        assertThat(s.applied()).isTrue();
        // Falls back to 17 because effectValue has no leading integer.
        assertThat(hero.shieldHp()).isEqualTo(17);
    }

    @Test
    void shield_partiallyAbsorbsDamage_attackResultReflectsRemainder() {
        Hero hero = freshHero();
        hero.setShieldHp(30);
        int hpBefore = hero.currentHp();

        // Attacker with exactly 50 dmg (min==max, so no RNG variance).
        Enemy attacker = new Enemy(Fixtures.enemyData("e_test", 50, 50, 50, 100, 0.0), Position.POS_3);
        var skill = Fixtures.skill("sk_test", 1.0, 0, 0);
        RandomGenerator rng = java.util.random.RandomGeneratorFactory.getDefault().create(1L);

        // DamageFormula: rolls 50, shield absorbs 30, AttackResult.hpDamage = 20.
        // Note: DamageFormula does NOT apply HP damage — it returns it for the caller.
        AttackResult result = DamageFormula.resolve(attacker, hero, skill, rng);

        assertThat(result.hit()).isTrue();
        // Shield fully depleted by 50-dmg hit.
        assertThat(hero.shieldHp()).isZero();
        // AttackResult.hpDamage is the post-shield remainder (caller must apply it).
        assertThat(result.hpDamage()).isEqualTo(20);
        // HP is unchanged until caller applies the result.
        assertThat(hero.currentHp()).isEqualTo(hpBefore);
    }

    @Test
    void shield_fullyAbsorbsSmallDamage_attackResultZeroHpDamage() {
        Hero hero = freshHero();
        hero.setShieldHp(50);
        int hpBefore = hero.currentHp();

        // Attack with exactly 10 damage (dmgMin=dmgMax=10).
        Enemy attacker = new Enemy(Fixtures.enemyData("e_test", 50, 10, 10, 100, 0.0), Position.POS_3);
        var skill = Fixtures.skill("sk_test", 1.0, 0, 0);
        RandomGenerator rng = java.util.random.RandomGeneratorFactory.getDefault().create(1L);

        AttackResult result = DamageFormula.resolve(attacker, hero, skill, rng);

        assertThat(result.hit()).isTrue();
        // Shield absorbs all 10 damage; HP unchanged.
        assertThat(hero.currentHp()).isEqualTo(hpBefore);
        // Shield reduced from 50 to 40.
        assertThat(hero.shieldHp()).isEqualTo(40);
        // AttackResult.hpDamage = 0 (fully absorbed).
        assertThat(result.hpDamage()).isZero();
    }

    @Test
    void apply_effAbsorb_deadTarget_returnsNotApplied() {
        Hero hero = freshHero();
        hero.takeHpDamage(9999);
        ItemUseHandler.AppliedSummary s = ItemUseHandler.apply("item_c07", hero, hero, null, gd);

        assertThat(s.applied()).isFalse();
    }
}
