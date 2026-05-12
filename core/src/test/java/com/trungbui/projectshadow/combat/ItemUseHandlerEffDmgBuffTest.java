package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 13 B1 — verify {@link ItemUseHandler} wires {@code eff_dmg_buff}
 * (passive outgoing damage multiplier).
 *
 * <p>eff_dmg_buff in effects.csv: category=buff, modifierType=percent,
 * modifierValue="+15 to +40%", trigger=passive. Applying it adds a passive
 * buff instance tracked via {@link com.trungbui.projectshadow.effect.ActiveEffects};
 * {@link com.trungbui.projectshadow.domain.Combatant#effectiveDmgMin()} /
 * {@code effectiveDmgMax()} scale accordingly.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemUseHandlerEffDmgBuffTest {

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

    private Hero freshHero() {
        return new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
    }

    /** item_c06 = Đá Mài Kiếm, effectId=eff_dmg_buff */
    @Test
    void apply_effDmgBuff_registersBuffInstance() {
        Hero hero = freshHero();
        RandomGenerator rng = java.util.random.RandomGeneratorFactory.getDefault().create(42L);
        ItemUseHandler.AppliedSummary s = ItemUseHandler.apply("item_c06", hero, hero, null, gd, rng);

        assertThat(s.applied()).isTrue();
        assertThat(hero.activeEffects().has("eff_dmg_buff")).isTrue();
    }

    @Test
    void apply_effDmgBuff_raisesEffectiveDamage() {
        Hero hero = freshHero();
        int baseDmgMin = hero.effectiveDmgMin();
        int baseDmgMax = hero.effectiveDmgMax();

        RandomGenerator rng = java.util.random.RandomGeneratorFactory.getDefault().create(42L);
        ItemUseHandler.apply("item_c06", hero, hero, null, gd, rng);

        // eff_dmg_buff adds +15 to +40% via sumPercentModifier(DAMAGE).
        // After applying, effectiveDmgMin/Max must be strictly greater.
        assertThat(hero.effectiveDmgMin()).isGreaterThan(baseDmgMin);
        assertThat(hero.effectiveDmgMax()).isGreaterThan(baseDmgMax);
    }

    @Test
    void apply_effDmgBuff_deadTarget_returnsNotApplied() {
        Hero hero = freshHero();
        hero.takeHpDamage(9999);
        RandomGenerator rng = java.util.random.RandomGeneratorFactory.getDefault().create(42L);
        ItemUseHandler.AppliedSummary s = ItemUseHandler.apply("item_c06", hero, hero, null, gd, rng);

        assertThat(s.applied()).isFalse();
    }
}
