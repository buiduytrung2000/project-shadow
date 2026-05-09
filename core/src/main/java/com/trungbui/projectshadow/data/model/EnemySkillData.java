package com.trungbui.projectshadow.data.model;

public record EnemySkillData(
        String skillId,
        String nameVn,
        String nameEn,
        String userType,
        boolean isOffensive,
        String targetType,
        double damageMultiplier,
        int accuracyModifier,
        int cooldown,
        String effectId,
        String effectValue,
        int stressDamage,
        String descriptionVn
) {
}
