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

    /** Sentinel for "no actor context" in {@link #formattedDescription(int, int)}. */
    public static final int NO_ACTOR_CONTEXT = -1;

    /**
     * Human-readable description block for the SkillDescriptionPanel tooltip
     * <strong>without</strong> actor context. Damage line shows the raw
     * multiplier (e.g. {@code "Dmg ×1.2"}) — used when no hero is available
     * (skill catalog browsing, tests).
     */
    public String formattedDescription() {
        return formattedDescription(NO_ACTOR_CONTEXT, NO_ACTOR_CONTEXT);
    }

    /**
     * Sprint 11 B1 — overload that computes the <strong>actual</strong> damage
     * range from the acting hero's stats. Player sees "Dmg 6..12" instead of
     * the abstract "Dmg ×1.2".
     *
     * @param actorDmgMin acting combatant's effectiveDmgMin (or
     *                    {@link #NO_ACTOR_CONTEXT} for catalog mode).
     * @param actorDmgMax acting combatant's effectiveDmgMax.
     */
    public String formattedDescription(int actorDmgMin, int actorDmgMax) {
        StringBuilder sb = new StringBuilder();
        if (descriptionVn != null && !descriptionVn.isBlank()) {
            sb.append(descriptionVn).append("\n\n");
        }
        // Sprint 11 B1: prefer actual damage range when context is provided AND
        // skill is offensive with positive multiplier. Otherwise show the
        // multiplier (or skip if non-offensive with mult 0).
        boolean hasContext = actorDmgMin != NO_ACTOR_CONTEXT && actorDmgMax != NO_ACTOR_CONTEXT;
        boolean dealsDamage = isOffensive && damageMultiplier > 0d;
        if (hasContext && dealsDamage) {
            int dmgLo = (int) Math.floor(actorDmgMin * damageMultiplier);
            int dmgHi = (int) Math.ceil(actorDmgMax * damageMultiplier);
            sb.append(I18n.t("skill.tooltip.damageRange", dmgLo, dmgHi));
        } else if (dealsDamage) {
            sb.append(I18n.t("skill.tooltip.damage", damageMultiplier));
        }
        // If non-offensive (heal/buff/self) we suppress the damage line entirely.

        if (targetType != null && !targetType.isBlank()) {
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') sb.append("  |  ");
            sb.append(I18n.t("skill.tooltip.target", targetType));
        }
        if (cooldown > 0) {
            sb.append("  |  ").append(I18n.t("skill.tooltip.cooldown", cooldown));
        }
        if (stressDamage > 0) {
            sb.append("  |  ").append(I18n.t("skill.tooltip.stress", stressDamage));
        }
        if (primaryEffectId != null && !primaryEffectId.isBlank()) {
            // Sprint 9+ B2: gate the "(magnitude duration)" suffix so we don't render
            // ugly "(  )" when both are blank. If either is present we still show it.
            boolean hasMag = effectMagnitude != null && !effectMagnitude.isBlank();
            boolean hasDur = effectDuration != null && !effectDuration.isBlank();
            if (hasMag || hasDur) {
                sb.append("\n").append(I18n.t("skill.tooltip.effect",
                        primaryEffectId,
                        hasMag ? effectMagnitude : "",
                        hasDur ? effectDuration : ""));
            } else {
                sb.append("\n").append(I18n.t("skill.tooltip.effectOnly", primaryEffectId));
            }
        }
        return sb.toString();
    }
}
