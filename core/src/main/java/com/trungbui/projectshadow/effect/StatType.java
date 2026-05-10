package com.trungbui.projectshadow.effect;

public enum StatType {
    HP,
    ACCURACY,
    CRIT,
    SPEED,
    DAMAGE,
    DAMAGE_RECEIVED,
    DODGE,
    STRESS,
    STRESS_RESIST,
    MAX_HP;

    public static StatType from(String csvName) {
        if (csvName == null) return null;
        return switch (csvName.toLowerCase().trim()) {
            case "hp" -> HP;
            case "accuracy" -> ACCURACY;
            case "crit" -> CRIT;
            case "speed" -> SPEED;
            case "dmg" -> DAMAGE;
            case "dmg_received" -> DAMAGE_RECEIVED;
            case "dodge" -> DODGE;
            case "stress" -> STRESS;
            case "stress_resist" -> STRESS_RESIST;
            case "max_hp" -> MAX_HP;
            default -> null;
        };
    }
}
