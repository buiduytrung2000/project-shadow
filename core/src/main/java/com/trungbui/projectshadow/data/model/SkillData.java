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

    /**
     * Human-readable description block for the SkillDescriptionPanel tooltip.
     * Format: "<descriptionVn>\n\nDmg ×<mult>  |  Target: <type>  |  CD: <cooldown>\n
     * Effect: <effect_id> (<magnitude>, <duration>)".
     *
     * <p>Returns just {@link #descriptionVn} (or empty) if descriptionVn is null —
     * the panel always shows at least the name above this block.</p>
     */
    public String formattedDescription() {
        StringBuilder sb = new StringBuilder();
        if (descriptionVn != null && !descriptionVn.isBlank()) {
            sb.append(descriptionVn).append("\n\n");
        }
        sb.append(I18n.t("skill.tooltip.damage", damageMultiplier));
        if (targetType != null && !targetType.isBlank()) {
            sb.append("  |  ").append(I18n.t("skill.tooltip.target", targetType));
        }
        if (cooldown > 0) {
            sb.append("  |  ").append(I18n.t("skill.tooltip.cooldown", cooldown));
        }
        if (stressDamage > 0) {
            sb.append("  |  ").append(I18n.t("skill.tooltip.stress", stressDamage));
        }
        if (primaryEffectId != null && !primaryEffectId.isBlank()) {
            sb.append("\n").append(I18n.t("skill.tooltip.effect",
                    primaryEffectId,
                    effectMagnitude == null ? "" : effectMagnitude,
                    effectDuration == null ? "" : effectDuration));
        }
        return sb.toString();
    }
}
