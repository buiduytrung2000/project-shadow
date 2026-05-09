package com.trungbui.projectshadow.data.model;

public record ItemData(
        String itemId,
        String nameVn,
        String nameEn,
        String category,
        String subcategory,
        boolean isRemovable,
        String restrictedToClass,
        String effectId,
        String effectValue,
        String effectDuration,
        String secondaryEffectId,
        String secondaryEffectValue,
        String rarity,
        int priceGold,
        boolean canDrop,
        boolean canBuy,
        String descriptionVn
) {
}
