package com.trungbui.projectshadow.data.model;

import com.trungbui.projectshadow.i18n.I18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 11 B1 — SkillData.formattedDescription(int, int) overload renders
 * the actual damage range from the actor's stats, not the abstract multiplier.
 */
class SkillDataActualDamageTest {

    @AfterEach
    void resetLocale() {
        I18n.setLocale(I18n.VI);
    }

    private static SkillData offensiveSkill(double damageMult) {
        return new SkillData(
                "sk_test", "Thử", "Test", "Warrior", "Warrior",
                true, "Front Enemy", damageMult, 0, 0,
                null, null, null, null, 0, "Common", "Đòn thử.");
    }

    @Test
    void withActorContext_rendersActualRange() {
        SkillData skill = offensiveSkill(1.2);
        // Actor with effectiveDmgMin=5, effectiveDmgMax=10
        // Expected dmgLo = floor(5 × 1.2) = 6; dmgHi = ceil(10 × 1.2) = 12
        String out = skill.formattedDescription(5, 10);
        assertThat(out).contains("6").contains("12");
        assertThat(out).doesNotContain("×1.2");
    }

    @Test
    void withoutContext_fallsBackToMultiplier() {
        SkillData skill = offensiveSkill(1.5);
        String out = skill.formattedDescription();
        // Old "Dmg ×1.5" path. The multiplier formats as a double per MessageFormat.
        assertThat(out).contains("1.5");
    }

    @Test
    void nonOffensiveSkill_suppressesDamageLine() {
        SkillData heal = new SkillData(
                "sk_heal", "Heal", "Heal", "Cleric", "Cleric",
                false, "Single Ally", 0d, 0, 1,
                "eff_heal", "10", "instant", null, 0, "Common", "Heal.");
        String out = heal.formattedDescription(5, 10);
        // Non-offensive with mult 0 → no Dmg line at all
        assertThat(out).doesNotContain("Dmg ");
    }

    @Test
    void offensiveWithZeroMultiplier_suppressesDamageLine() {
        // Some skills are offensive but pure-utility (mult 0). E.g. accuracy debuff.
        SkillData utility = new SkillData(
                "sk_debuff", "Debuff", "Debuff", "Assassin", "Assassin",
                true, "All Enemies", 0d, 0, 2,
                "eff_acc_debuff", "-25", "2 turns", null, 0, "Uncommon", "Debuff acc.");
        String out = utility.formattedDescription(5, 10);
        assertThat(out).doesNotContain("Dmg ");
    }
}
