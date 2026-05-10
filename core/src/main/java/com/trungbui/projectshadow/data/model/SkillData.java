package com.trungbui.projectshadow.data.model;

import com.trungbui.projectshadow.i18n.I18n;

public record SkillData(
        String skillId,
        String nameVn,
        String nameEn,
        String heroClassVn,
        String heroClassEn,
        boolean isOffensive,
        String targetType,
        double damageMultiplier,
        int accuracyModifier,
        int cooldown,
        String primaryEffectId,
        String effectMagnitude,
        String effectDuration,
        String effectNotes,
        int stressDamage,
        String rarity,
        String descriptionVn
) {
    /** Returns the locale-appropriate name (EN if {@link I18n#isEnglish()}, else VN). */
    public String displayName() {
        return I18n.isEnglish() && nameEn != null && !nameEn.isBlank() ? nameEn : nameVn;
    }
}
