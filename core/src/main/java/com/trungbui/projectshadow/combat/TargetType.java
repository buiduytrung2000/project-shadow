package com.trungbui.projectshadow.combat;

public enum TargetType {

    SELF,
    FRONT_FOE,
    BACK_FOE,
    ANY_FOE,
    LOWEST_HP_FOE,
    RANDOM_FOE,
    RANDOM_FOE_2,
    FRONT_2_FOE,
    FRONT_ROW_FOE,
    BACK_ROW_FOE,
    ALL_FOE,

    ANY_ALLY,
    LOWEST_HP_ALLY,
    ALL_ALLY,

    UNSUPPORTED;

    public static TargetType parse(String csv) {
        if (csv == null) return UNSUPPORTED;
        String s = csv.trim().toLowerCase();
        return switch (s) {
            case "self" -> SELF;
            case "front enemy", "front hero" -> FRONT_FOE;
            case "back enemy", "back hero" -> BACK_FOE;
            case "any enemy", "single enemy", "single hero" -> ANY_FOE;
            case "single ally" -> ANY_ALLY;
            case "lowest hp enemy" -> LOWEST_HP_FOE;
            case "random", "random enemy", "random hero" -> RANDOM_FOE;
            case "2 random enemies" -> RANDOM_FOE_2;
            case "2 front enemies", "front 2 heroes" -> FRONT_2_FOE;
            case "front row (aoe)" -> FRONT_ROW_FOE;
            case "back row (aoe)" -> BACK_ROW_FOE;
            case "all enemies", "all heroes" -> ALL_FOE;
            case "all allies" -> ALL_ALLY;
            default -> UNSUPPORTED;
        };
    }

    public boolean requiresPlayerPick() {
        return this == ANY_FOE || this == ANY_ALLY || this == BACK_FOE;
    }

    public boolean isSupported() {
        return this != UNSUPPORTED;
    }

    public boolean targetsAllies() {
        return this == SELF || this == ANY_ALLY || this == LOWEST_HP_ALLY || this == ALL_ALLY;
    }

    public boolean targetsFoes() {
        return !targetsAllies() && this != UNSUPPORTED;
    }
}
