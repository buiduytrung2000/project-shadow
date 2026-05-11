package com.trungbui.projectshadow.data.model;

import java.util.List;

/**
 * Sprint 10 B3 — a single choice within an event, with display text + outcomes.
 *
 * <p>Per design lock (2026-05-11), the choice text is shown to the player but
 * the outcomes are <strong>NOT previewed</strong> — mystery mode. Player picks
 * by trusting the choice text alone.</p>
 *
 * @param text Display text (Vietnamese).
 * @param outcomes List of weighted outcome rolls — each with a chance; multiple
 *                 outcomes can fire in the same choice.
 */
public record EventChoice(String text, List<EventOutcome> outcomes) {
    public EventChoice {
        outcomes = outcomes == null ? List.of() : List.copyOf(outcomes);
    }
}
