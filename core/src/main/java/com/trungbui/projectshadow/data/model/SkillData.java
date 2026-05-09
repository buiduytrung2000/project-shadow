package com.trungbui.projectshadow.data.model;

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
}
