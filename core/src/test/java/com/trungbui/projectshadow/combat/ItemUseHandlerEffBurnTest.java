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
 * Sprint 13 B1 — verify {@link ItemUseHandler} wires {@code eff_burn} (DoT)
 * through {@link com.trungbui.projectshadow.effect.ActiveEffects}.
 *
 * <p>eff_burn in effects.csv: category=dot, flat_per_turn -2 to -5, duration=2 turns.
 * Applying via ItemUseHandler registers the DoT instance; ticking via
 * {@code activeEffects().onTurnStart()} reduces HP each turn.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemUseHandlerEffBurnTest {

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

    /** item_c09 = Hỗn Hợp Bùng Cháy, effectId=eff_burn */
    @Test
    void apply_effBurn_registersDoTInstance() {
        Hero target = freshHero();
        // Use a seeded RNG for deterministic results.
        RandomGenerator rng = java.util.random.RandomGeneratorFactory.getDefault().create(42L);
        ItemUseHandler.AppliedSummary s = ItemUseHandler.apply("item_c09", target, target, null, gd, rng);

        assertThat(s.applied()).isTrue();
        assertThat(s.notes()).contains("eff_burn");
        assertThat(target.activeEffects().has("eff_burn")).isTrue();
    }

    @Test
    void apply_effBurn_tickReducesHp() {
        Hero target = freshHero();
        RandomGenerator rng = java.util.random.RandomGeneratorFactory.getDefault().create(42L);
        ItemUseHandler.apply("item_c09", target, target, null, gd, rng);

        int hpBefore = target.currentHp();

        // Tick once — eff_burn is on_turn_start, so a single onTurnStart call should damage.
        // Use UNKNOWN_ROUND so the "just-applied skip" is disabled (pure DoT tick).
        target.activeEffects().onTurnStart(target, rng);

        assertThat(target.currentHp()).isLessThan(hpBefore);
    }

    @Test
    void apply_effBurn_expireAfterDuration() {
        Hero target = freshHero();
        RandomGenerator rng = java.util.random.RandomGeneratorFactory.getDefault().create(42L);
        ItemUseHandler.apply("item_c09", target, target, null, gd, rng);

        // eff_burn has defaultDuration=2. Tick twice to expire it.
        target.activeEffects().onTurnStart(target, rng);
        target.activeEffects().onTurnStart(target, rng);

        // After duration expires, the effect instance should be removed.
        assertThat(target.activeEffects().has("eff_burn")).isFalse();
    }

    @Test
    void apply_effBurn_deadTarget_returnsNotApplied() {
        Hero target = freshHero();
        target.takeHpDamage(9999);
        RandomGenerator rng = java.util.random.RandomGeneratorFactory.getDefault().create(42L);
        ItemUseHandler.AppliedSummary s = ItemUseHandler.apply("item_c09", target, target, null, gd, rng);

        assertThat(s.applied()).isFalse();
    }
}
