package com.trungbui.projectshadow.stage;

public enum NodeType {
    COMBAT, EVENT, REST, REWARD, ELITE, MINIBOSS, BOSS;

    public static NodeType parse(String s) {
        if (s == null) throw new IllegalArgumentException("type string is null");
        return switch (s.toLowerCase().trim()) {
            case "combat" -> COMBAT;
            case "event" -> EVENT;
            case "rest" -> REST;
            case "reward" -> REWARD;
            case "elite" -> ELITE;
            case "miniboss" -> MINIBOSS;
            case "boss" -> BOSS;
            default -> throw new IllegalArgumentException("Unknown node type: " + s);
        };
    }
}
