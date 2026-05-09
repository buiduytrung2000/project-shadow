package com.trungbui.projectshadow.data.model;

public record EnemyData(
        String name,
        String enemyId,
        int baseHp,
        int damageMin,
        int damageMax,
        double criticalChance,
        double stressResist,
        int accuracy,
        String skill1,
        String skill2,
        String dropItem,
        double dropChance,
        String specialSkill,
        String notes,
        String variantType
) {
}
