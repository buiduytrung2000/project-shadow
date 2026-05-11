package com.trungbui.projectshadow.data.model;

import java.util.ArrayList;
import java.util.List;

public record EventData(
        String eventId,
        String nameVn,
        String nameEn,
        /** Multi-value comma-separated, e.g. {@code "1,2,3"} = appears in all 3
         *  stages. Use {@link #eligibleStages()} to parse. */
        String stage,
        String triggerType,
        String rarity,
        String descriptionVn,
        String choice1Text,
        String choice1Outcomes,
        String choice2Text,
        String choice2Outcomes,
        String choice3Text,
        String choice3Outcomes,
        String status,
        String notes
) {

    /**
     * Sprint 10 B3 — parse the multi-value {@link #stage} column into a list of stage
     * acts (1, 2, 3). Returns empty list if missing/malformed (event won't be
     * filtered into any stage — caller should treat as deprecated event).
     */
    public List<Integer> eligibleStages() {
        if (stage == null || stage.isBlank()) return List.of();
        List<Integer> out = new ArrayList<>();
        for (String token : stage.split(",")) {
            try {
                out.add(Integer.parseInt(token.trim()));
            } catch (NumberFormatException ignored) {
                // skip bad token
            }
        }
        return List.copyOf(out);
    }

    /**
     * Sprint 10 B3 — parse the 3 choice columns into a structured list (skipping
     * blank choices). Each choice has text + parsed outcomes.
     */
    public List<EventChoice> choices() {
        List<EventChoice> out = new ArrayList<>();
        addChoiceIfPresent(out, choice1Text, choice1Outcomes);
        addChoiceIfPresent(out, choice2Text, choice2Outcomes);
        addChoiceIfPresent(out, choice3Text, choice3Outcomes);
        return List.copyOf(out);
    }

    private static void addChoiceIfPresent(List<EventChoice> out, String text, String outcomesRaw) {
        if (text == null || text.isBlank()) return;
        out.add(new EventChoice(text, parseOutcomes(outcomesRaw)));
    }

    /**
     * Sprint 10 B3 — parse the CSV outcome DSL into structured {@link EventOutcome}.
     *
     * <p>Format: {@code "type=X|target=Y|value=Z|chance=0.5; type=...; ..."}.
     * Effects are separated by semicolons; key-value pairs within an effect by
     * pipes. Missing keys yield null/0 in the record.</p>
     */
    public static List<EventOutcome> parseOutcomes(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<EventOutcome> out = new ArrayList<>();
        for (String effect : raw.split(";")) {
            if (effect.isBlank()) continue;
            String type = null, target = null, value = null;
            double chance = 1.0;
            for (String pair : effect.split("\\|")) {
                String[] kv = pair.split("=", 2);
                if (kv.length != 2) continue;
                String key = kv[0].trim();
                String val = kv[1].trim();
                switch (key) {
                    case "type" -> type = val;
                    case "target" -> target = val;
                    case "value" -> value = val;
                    case "chance" -> {
                        try {
                            chance = Double.parseDouble(val);
                        } catch (NumberFormatException ignored) {
                            chance = 1.0;
                        }
                    }
                    default -> { /* forward-compat: unknown key, skip */ }
                }
            }
            if (type != null) {
                out.add(new EventOutcome(type, target, value, chance));
            }
        }
        return List.copyOf(out);
    }
}
