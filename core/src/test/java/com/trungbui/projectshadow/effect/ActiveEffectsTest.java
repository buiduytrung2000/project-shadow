package com.trungbui.projectshadow.effect;

import com.trungbui.projectshadow.data.model.EffectData;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Fixtures;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveEffectsTest {

    private static EffectData dot(String id, String value, int duration, boolean canStack, int maxStacks) {
        return new EffectData(
                id, "Test", "Test", "dot", true, null,
                "turns", String.valueOf(duration), "hp", "flat_per_turn", value,
                canStack, canStack ? maxStacks : null,
                "on_turn_start", null, null, null, null, "", null
        );
    }

    private static EffectData hot(String id, String value, int duration) {
        return new EffectData(
                id, "Test", "Test", "hot", true, null,
                "turns", String.valueOf(duration), "hp", "flat_per_turn", value,
                false, null,
                "on_turn_start", null, null, null, null, "", null
        );
    }

    private static EffectData buffPercent(String id, String stat, String value, int duration) {
        return new EffectData(
                id, "Test", "Test", "buff", true, null,
                "turns", String.valueOf(duration), stat, "percent", value,
                false, null,
                "passive", null, null, null, null, "", null
        );
    }

    private static EffectData buffFlat(String id, String stat, String value, int duration) {
        return new EffectData(
                id, "Test", "Test", "buff", true, null,
                "turns", String.valueOf(duration), stat, "flat", value,
                false, null,
                "passive", null, null, null, null, "", null
        );
    }

    private static EffectData heal(String id, String value) {
        return new EffectData(
                id, "Test", "Test", "heal", true, null,
                "instant", "", "hp", "flat", value,
                false, null,
                "on_use", null, null, null, null, "", null
        );
    }

    private static EffectData stun(String id) {
        return new EffectData(
                id, "Test", "Test", "debuff", true, null,
                "turns", "1", "action", "skip", "skip turn",
                false, null,
                "on_hit", null, null, null, null, "", null
        );
    }

    @Test
    void apply_addsNewInstance() {
        Map<String, EffectData> cat = Map.of("eff_bleed", dot("eff_bleed", "-3", 3, true, 3));
        ActiveEffects ae = new ActiveEffects(cat);
        Enemy target = new Enemy(Fixtures.enemyData("e1", 20, 3, 6, 80, 0.05), Position.POS_1, cat);
        EffectInstance ei = ae.apply("eff_bleed", null, target, new Random(1L));
        assertThat(ae.size()).isEqualTo(1);
        assertThat(ei.stacks()).isEqualTo(1);
        assertThat(ei.remainingDuration()).isEqualTo(3);
    }

    @Test
    void apply_stacksWhenCanStack() {
        Map<String, EffectData> cat = Map.of("eff_bleed", dot("eff_bleed", "-3", 3, true, 3));
        ActiveEffects ae = new ActiveEffects(cat);
        Enemy target = new Enemy(Fixtures.enemyData("e1", 20, 3, 6, 80, 0.05), Position.POS_1, cat);
        ae.apply("eff_bleed", null, target, new Random(1L));
        ae.apply("eff_bleed", null, target, new Random(1L));
        ae.apply("eff_bleed", null, target, new Random(1L));
        ae.apply("eff_bleed", null, target, new Random(1L));
        assertThat(ae.size()).isEqualTo(1);
        assertThat(ae.find("eff_bleed").orElseThrow().stacks()).isEqualTo(3);
    }

    @Test
    void apply_refreshesDurationWhenNotStacking() {
        Map<String, EffectData> cat = Map.of("eff_def_buff", buffPercent("eff_def_buff", "dmg_received", "-30%", 2));
        ActiveEffects ae = new ActiveEffects(cat);
        Hero h = new Hero(Fixtures.heroData("h1", 30, 5, 10, 80, 0.05, 5), Position.POS_1, cat);
        ae.apply("eff_def_buff", null, h, new Random(1L));
        ae.find("eff_def_buff").orElseThrow().tickDuration();
        ae.apply("eff_def_buff", null, h, new Random(1L));
        assertThat(ae.find("eff_def_buff").orElseThrow().remainingDuration()).isEqualTo(2);
    }

    @Test
    void onTurnStart_dot_dealsDamagePerStack() {
        Map<String, EffectData> cat = Map.of("eff_bleed", dot("eff_bleed", "-3", 3, true, 3));
        Enemy target = new Enemy(Fixtures.enemyData("e1", 30, 3, 6, 80, 0.05), Position.POS_1, cat);
        target.activeEffects().apply("eff_bleed", null, target, new Random(1L));
        target.activeEffects().apply("eff_bleed", null, target, new Random(1L));
        target.activeEffects().apply("eff_bleed", null, target, new Random(1L));
        target.activeEffects().onTurnStart(target, new Random(1L));
        assertThat(target.currentHp()).isEqualTo(30 - 9);
    }

    @Test
    void onTurnStart_hot_healsPerStack() {
        Map<String, EffectData> cat = Map.of("eff_regen", hot("eff_regen", "+5", 3));
        Hero h = new Hero(Fixtures.heroData("h1", 30, 5, 10, 80, 0.05, 5), Position.POS_1, cat);
        h.takeHpDamage(20);
        h.activeEffects().apply("eff_regen", null, h, new Random(1L));
        h.activeEffects().onTurnStart(h, new Random(1L));
        assertThat(h.currentHp()).isEqualTo(15);
    }

    @Test
    void onTurnStart_decrementsDuration_andRemovesExpired() {
        Map<String, EffectData> cat = Map.of("eff_bleed", dot("eff_bleed", "-3", 2, true, 3));
        Enemy target = new Enemy(Fixtures.enemyData("e1", 30, 3, 6, 80, 0.05), Position.POS_1, cat);
        target.activeEffects().apply("eff_bleed", null, target, new Random(1L));
        target.activeEffects().onTurnStart(target, new Random(1L));
        assertThat(target.activeEffects().size()).isEqualTo(1);
        target.activeEffects().onTurnStart(target, new Random(1L));
        assertThat(target.activeEffects().size()).isZero();
    }

    @Test
    void onTurnStart_permanent_neverExpires() {
        EffectData perm = new EffectData(
                "eff_perm", "Test", "Test", "permanent_buff", false, null,
                "permanent", "", "accuracy", "flat", "+5",
                false, null, "passive", null, null, null, null, "", null
        );
        Map<String, EffectData> cat = Map.of("eff_perm", perm);
        ActiveEffects ae = new ActiveEffects(cat);
        Hero h = new Hero(Fixtures.heroData("h1", 30, 5, 10, 80, 0.05, 5), Position.POS_1, cat);
        ae.apply("eff_perm", null, h, new Random(1L));
        for (int i = 0; i < 100; i++) {
            h.activeEffects().onTurnStart(h, new Random(1L));
        }
        assertThat(ae.size()).isEqualTo(1);
        assertThat(ae.find("eff_perm").orElseThrow().isPermanent()).isTrue();
    }

    @Test
    void healCategory_appliedImmediately() {
        Map<String, EffectData> cat = Map.of("eff_heal", heal("eff_heal", "+15"));
        ActiveEffects ae = new ActiveEffects(cat);
        Hero h = new Hero(Fixtures.heroData("h1", 30, 5, 10, 80, 0.05, 5), Position.POS_1, cat);
        h.takeHpDamage(25);
        assertThat(h.currentHp()).isEqualTo(5);
        ae.apply("eff_heal", null, h, new Random(1L));
        assertThat(h.currentHp()).isEqualTo(20);
    }

    @Test
    void sumFlatModifier_aggregatesAcrossEffects() {
        EffectData accBuff = buffFlat("eff_acc_buff", "accuracy", "+10", 3);
        EffectData accDebuff = buffFlat("eff_acc_debuff", "accuracy", "-3", 3);
        Map<String, EffectData> cat = Map.of("eff_acc_buff", accBuff, "eff_acc_debuff", accDebuff);
        ActiveEffects ae = new ActiveEffects(cat);
        Hero h = new Hero(Fixtures.heroData("h1", 30, 5, 10, 80, 0.05, 5), Position.POS_1, cat);
        ae.apply("eff_acc_buff", null, h, new Random(1L));
        ae.apply("eff_acc_debuff", null, h, new Random(1L));
        assertThat(ae.sumFlatModifier(StatType.ACCURACY)).isEqualTo(7);
    }

    @Test
    void sumPercentModifier_returnsFraction() {
        EffectData dmgBuff = buffPercent("eff_dmg_buff", "dmg", "+20%", 3);
        Map<String, EffectData> cat = Map.of("eff_dmg_buff", dmgBuff);
        ActiveEffects ae = new ActiveEffects(cat);
        Hero h = new Hero(Fixtures.heroData("h1", 30, 5, 10, 80, 0.05, 5), Position.POS_1, cat);
        ae.apply("eff_dmg_buff", null, h, new Random(1L));
        assertThat(ae.sumPercentModifier(StatType.DAMAGE)).isCloseTo(0.20, within());
    }

    @Test
    void parseFlatValue_handlesRange() {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        Random rng = new Random(42L);
        for (int i = 0; i < 1000; i++) {
            int v = ActiveEffects.parseFlatValue("-2 to -5", rng);
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        assertThat(min).isEqualTo(-5);
        assertThat(max).isEqualTo(-2);
    }

    @Test
    void parseFlatValue_handlesPlusPrefix() {
        assertThat(ActiveEffects.parseFlatValue("+5", null)).isEqualTo(5);
        assertThat(ActiveEffects.parseFlatValue("-3", null)).isEqualTo(-3);
        assertThat(ActiveEffects.parseFlatValue("10", null)).isEqualTo(10);
    }

    @Test
    void parsePercentValue_handlesRange() {
        double v = ActiveEffects.parsePercentValue("+15 to +40%");
        assertThat(v).isCloseTo(0.275, within());
    }

    @Test
    void parsePercentValue_handlesSimple() {
        assertThat(ActiveEffects.parsePercentValue("+20%")).isCloseTo(0.20, within());
        assertThat(ActiveEffects.parsePercentValue("-30%")).isCloseTo(-0.30, within());
    }

    @Test
    void isStunned_detectsStunEffect() {
        Map<String, EffectData> cat = Map.of("eff_stun", stun("eff_stun"));
        ActiveEffects ae = new ActiveEffects(cat);
        Hero h = new Hero(Fixtures.heroData("h1", 30, 5, 10, 80, 0.05, 5), Position.POS_1, cat);
        assertThat(ae.isStunned()).isFalse();
        ae.apply("eff_stun", null, h, new Random(1L));
        assertThat(ae.isStunned()).isTrue();
    }

    @Test
    void removeFirst_removesGivenEffect() {
        Map<String, EffectData> cat = Map.of("eff_bleed", dot("eff_bleed", "-3", 3, true, 3));
        ActiveEffects ae = new ActiveEffects(cat);
        Enemy target = new Enemy(Fixtures.enemyData("e1", 30, 3, 6, 80, 0.05), Position.POS_1, cat);
        ae.apply("eff_bleed", null, target, new Random(1L));
        assertThat(ae.removeFirst("eff_bleed")).isTrue();
        assertThat(ae.size()).isZero();
    }

    @Test
    void apply_throwsOn_unknownEffectId() {
        ActiveEffects ae = new ActiveEffects(Map.of());
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                ae.apply("eff_unknown", null, null, new Random(1L))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    private static org.assertj.core.data.Offset<Double> within() {
        return org.assertj.core.data.Offset.offset(1e-9);
    }
}
