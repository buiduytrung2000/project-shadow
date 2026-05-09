package com.trungbui.projectshadow.data.model;

import java.util.List;

public record HeroData(
        String rarity,
        String nameVn,
        String nameEn,
        String heroId,
        String role,
        String position,
        int baseHp,
        int baseDmgMin,
        int baseDmgMax,
        int baseAccuracy,
        double baseCrit,
        int speed,
        double baseStressResist,
        int levelUpHp,
        int levelUpDmg,
        double levelUpCrit,
        double levelUpStressResist,
        List<String> defaultSkills,
        List<String> availableSkills,
        String notes
) {
}
