package com.trungbui.projectshadow.data.model;

public record EffectData(
        String effectId,
        String nameVn,
        String nameEn,
        String category,
        boolean isRemovable,
        String removalCondition,
        String durationType,
        String defaultDuration,
        String statAffected,
        String modifierType,
        String modifierValue,
        boolean canStack,
        Integer maxStacks,
        String trigger,
        String sourceType,
        String sourceExamples,
        String visualCue,
        String balanceNotes,
        String descriptionVn,
        String iconKey
) {
}
