package com.trungbui.projectshadow.stage;

import com.fasterxml.jackson.databind.JsonNode;

public record StageRules(
        boolean noConsecutiveRest,
        boolean noConsecutiveReward,
        boolean noConsecutiveEventUnless30,
        Integer maxCombatPerRun,
        Integer minEventPerRun,
        Integer minRestPerRun,
        Integer eliteNodeMaxPerRun
) {
    public static StageRules empty() {
        return new StageRules(false, false, false, null, null, null, null);
    }

    public static StageRules parse(JsonNode rulesArray) {
        if (rulesArray == null || !rulesArray.isArray()) return empty();

        boolean noRest = false;
        boolean noReward = false;
        boolean noEventUnless30 = false;
        Integer maxCombat = null;
        Integer minEvent = null;
        Integer minRest = null;
        Integer eliteMax = null;

        for (JsonNode r : rulesArray) {
            String s = r.asText("").trim();
            if (s.isEmpty()) continue;

            if (s.startsWith("no_consecutive_rest")) noRest = true;
            else if (s.startsWith("no_consecutive_reward")) noReward = true;
            else if (s.startsWith("no_consecutive_event_unless_30%")) noEventUnless30 = true;
            else if (s.startsWith("max_combat_per_run:")) maxCombat = parseValue(s);
            else if (s.startsWith("min_event_per_run:")) minEvent = parseValue(s);
            else if (s.startsWith("min_rest_per_run:")) minRest = parseValue(s);
            else if (s.startsWith("elite_node_max_per_run:")) eliteMax = parseValue(s);
        }

        return new StageRules(noRest, noReward, noEventUnless30, maxCombat, minEvent, minRest, eliteMax);
    }

    private static Integer parseValue(String s) {
        int colon = s.indexOf(':');
        if (colon < 0) return null;
        try {
            return Integer.parseInt(s.substring(colon + 1).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
