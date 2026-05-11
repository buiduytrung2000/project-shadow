package com.trungbui.projectshadow.data.model;

import com.trungbui.projectshadow.i18n.I18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the {@code formattedDescription()} helper added for the
 * skill-tooltip panel (feature/skill-description-tooltip).
 *
 * <p>The helper builds a multi-line block combining the CSV's
 * {@code descriptionVn} with stat lines (dmg mult, target, cooldown, stress)
 * and effect details, all run through {@link I18n} so the labels switch
 * between Vietnamese and English.</p>
 */
class SkillDataFormatTest {

    @AfterEach
    void resetLocale() {
        I18n.setLocale(I18n.VI);
    }

    @Test
    void includesDescriptionWhenPresent() {
        SkillData skill = sample("Đòn cơ bản. Đánh 1 địch hàng đầu.");
        String out = skill.formattedDescription();
        assertThat(out).startsWith("Đòn cơ bản. Đánh 1 địch hàng đầu.");
    }

    @Test
    void omitsDescriptionBlockWhenBlank() {
        SkillData skill = sample("");
        String out = skill.formattedDescription();
        // Should not start with the empty blank lines — first content is the dmg line.
        assertThat(out).doesNotContain("\n\nDmg");
        assertThat(out).startsWith("Dmg");
    }

    @Test
    void includesDamageLineAlways() {
        SkillData skill = sample("desc");
        assertThat(skill.formattedDescription()).contains("Dmg ×");
    }

    @Test
    void includesCooldownOnlyWhenPositive() {
        SkillData zero = new SkillData(
                "sk", "Tên", "Name", "Class", "Class",
                true, "front_enemy", 1.0, 0, /*cooldown*/ 0,
                null, null, null, null, 0, "Common", "desc");
        SkillData hasCd = new SkillData(
                "sk", "Tên", "Name", "Class", "Class",
                true, "front_enemy", 1.0, 0, /*cooldown*/ 2,
                null, null, null, null, 0, "Common", "desc");

        assertThat(zero.formattedDescription()).doesNotContain("CD:");
        assertThat(hasCd.formattedDescription()).contains("CD: 2");
    }

    @Test
    void includesEffectLineWhenEffectPresent() {
        SkillData withEffect = new SkillData(
                "sk_w5", "Slash", "Slash", "Warrior", "Warrior",
                true, "front_enemy", 1.0, 0, 1,
                /*effectId*/ "eff_bleed", "3 stacks", "3 turns", "notes",
                0, "Common", "Cause bleed.");
        String out = withEffect.formattedDescription();
        assertThat(out).contains("eff_bleed").contains("3 stacks").contains("3 turns");
    }

    @Test
    void englishLocaleSwitchesLabels() {
        SkillData skill = sample("desc");
        // VN default
        String vn = skill.formattedDescription();
        assertThat(vn).contains("Mục tiêu:");

        I18n.setLocale(I18n.EN);
        String en = skill.formattedDescription();
        assertThat(en).contains("Target:");
    }

    @Test
    void displayNameRespectsLocale() {
        SkillData skill = new SkillData(
                "sk", "Tên VN", "Name EN", "Class", "Class",
                true, "front_enemy", 1.0, 0, 0,
                null, null, null, null, 0, "Common", "desc");

        I18n.setLocale(I18n.VI);
        assertThat(skill.displayName()).isEqualTo("Tên VN");

        I18n.setLocale(I18n.EN);
        assertThat(skill.displayName()).isEqualTo("Name EN");
    }

    private static SkillData sample(String descriptionVn) {
        return new SkillData(
                "sk_w1", "Đòn Kiếm", "Sword Strike", "Warrior", "Warrior",
                true, "front_enemy", 1.2, 0, 1,
                null, null, null, null,
                0, "Common", descriptionVn);
    }
}
