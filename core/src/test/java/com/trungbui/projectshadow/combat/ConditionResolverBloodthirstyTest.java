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
 * Sprint 11 B2 — Bloodthirsty (trait_07) Affliction reclass with forced-attack
 * + per-kill stack damage bonus (+5% per stack, max 5).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConditionResolverBloodthirstyTest {

    private GameData gd;

    @BeforeAll
    void load() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        gd = GameData.loadFromDirectory(dataDir);
    }

    @Test
    void bloodthirstyIsReclassedAsAffliction_postSprint11() {
        // trait_07 was Virtue pre-Sprint-11, now Affliction.
        var t = gd.diseasesTraits().get("trait_07");
        assertThat(t).isNotNull();
        assertThat(t.isAffliction()).isTrue();
        assertThat(t.isVirtue()).isFalse();
    }

    @Test
    void hasForcedAttack_trueForBloodthirstyHero() {
        Hero h = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        assertThat(ConditionResolver.hasForcedAttack(h)).isFalse();
        h.addTrait(ConditionResolver.TRAIT_BLOODTHIRSTY);
        assertThat(ConditionResolver.hasForcedAttack(h)).isTrue();
    }

    @Test
    void pickForcedAttackSkill_returnsFirstOffensiveIndex() {
        Hero h = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        // Warrior default skills include sk_w1 Đòn Kiếm (offensive).
        int idx = ConditionResolver.pickForcedAttackSkill(h, gd);
        assertThat(idx).isGreaterThanOrEqualTo(0);
        // The picked skill must be offensive.
        String skillId = h.equippedSkills().get(idx);
        assertThat(gd.skills().get(skillId).isOffensive()).isTrue();
    }

    @Test
    void onKill_addsStack_capsAt5() {
        Hero killer = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        killer.addTrait(ConditionResolver.TRAIT_BLOODTHIRSTY);
        for (int i = 0; i < 10; i++) {
            ConditionResolver.onKill(killer);
        }
        assertThat(killer.bloodthirstyStacks()).isEqualTo(5);
    }

    @Test
    void onKill_doesNothingForHeroWithoutTrait() {
        Hero killer = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        ConditionResolver.onKill(killer);
        assertThat(killer.bloodthirstyStacks()).isZero();
    }

    @Test
    void effectiveDmg_appliesBloodthirstyBonus() {
        Hero h = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        int baseDmgMin = h.effectiveDmgMin();
        int baseDmgMax = h.effectiveDmgMax();

        // Add 5 stacks → +25% dmg.
        h.addTrait(ConditionResolver.TRAIT_BLOODTHIRSTY);
        for (int i = 0; i < 5; i++) ConditionResolver.onKill(h);
        assertThat(h.bloodthirstyStacks()).isEqualTo(5);

        // Round-trip floor differences may shift by 1 — check ≥ 1.20 ratio (loose).
        int boostedMin = h.effectiveDmgMin();
        int boostedMax = h.effectiveDmgMax();
        // 5 stacks × 5% = 25% → 1.25× multiplier. Allow rounding.
        assertThat(boostedMin).isGreaterThan(baseDmgMin);
        assertThat(boostedMax).isGreaterThan(baseDmgMax);
        // Specific: floor of base*1.25 rounded to int.
        assertThat(boostedMin).isEqualTo((int) Math.round(baseDmgMin * 1.25));
    }

    @Test
    void onCombatStart_resetsStacks() {
        Hero h = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        h.addTrait(ConditionResolver.TRAIT_BLOODTHIRSTY);
        for (int i = 0; i < 3; i++) ConditionResolver.onKill(h);
        assertThat(h.bloodthirstyStacks()).isEqualTo(3);

        Enemy e = new Enemy(gd.enemies().get("enemy_01"), Position.POS_1, gd.effects());
        CombatEncounter enc = new CombatEncounter(List.of(h), List.of(e));
        ConditionResolver.onCombatStart(enc);
        assertThat(h.bloodthirstyStacks()).isZero();
    }

    @Test
    void afflictionPool_postBloodthirstyReclass_has7Afflictions() {
        // Pre-Sprint-11 pool: 6 Affliction (Cowardly, Masochist, Paranoid, Selfish,
        // Fearful, Hopeless) / 6 Virtue. Post: 7 Affliction / 5 Virtue.
        long afflictionCount = gd.diseasesTraits().values().stream()
                .filter(t -> "Trait".equalsIgnoreCase(t.type()))
                .filter(t -> t.isAffliction())
                .count();
        long virtueCount = gd.diseasesTraits().values().stream()
                .filter(t -> "Trait".equalsIgnoreCase(t.type()))
                .filter(t -> t.isVirtue())
                .count();
        assertThat(afflictionCount).isEqualTo(7);
        assertThat(virtueCount).isEqualTo(5);
    }
}
