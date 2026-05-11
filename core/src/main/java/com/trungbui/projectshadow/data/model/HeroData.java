package com.trungbui.projectshadow.data.model;

import com.trungbui.projectshadow.i18n.I18n;

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
        /** Sprint 11 B3 — base dodge stat. Was hardcoded 0 on Hero pre-Sprint-11.
         *  Distribution: Tank/Warrior 0, Cleric/Caster 2-3, Archer/Rogue 4-6,
         *  Assassin/Monk 6-8. Cursed disease applies -30 on top of this. */
        int baseDodge,
        /** Sprint 11 B3 — per-level dodge gain (small; 1 every 2-3 levels). */
        double levelUpDodge,
        List<String> defaultSkills,
        List<String> availableSkills,
        String notes
) {
    /** Returns the locale-appropriate name (EN if {@link I18n#isEnglish()}, else VN). */
    public String displayName() {
        return I18n.isEnglish() && nameEn != null && !nameEn.isBlank() ? nameEn : nameVn;
    }
}
