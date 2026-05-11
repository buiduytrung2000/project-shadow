package com.trungbui.projectshadow.data.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 10 B3 — verify {@link EventData} parsing helpers:
 * {@code eligibleStages()} (multi-value "1,2,3") and
 * {@code choices()} / {@code parseOutcomes()} (CSV DSL).
 */
class EventDataParseTest {

    private static EventData event(String stages, String c1Text, String c1Out,
                                   String c2Text, String c2Out, String c3Text, String c3Out) {
        return new EventData("ev_test", "Test", "Test",
                stages, "random", "Common", "desc",
                c1Text, c1Out, c2Text, c2Out, c3Text, c3Out,
                "Live", "");
    }

    @Test
    void eligibleStages_singleValue() {
        EventData e = event("1", "", "", "", "", null, null);
        assertThat(e.eligibleStages()).containsExactly(1);
    }

    @Test
    void eligibleStages_multiValue() {
        EventData e = event("1,2,3", "", "", "", "", null, null);
        assertThat(e.eligibleStages()).containsExactly(1, 2, 3);
    }

    @Test
    void eligibleStages_emptyOrMalformed_returnsEmpty() {
        assertThat(event("", "", "", "", "", null, null).eligibleStages()).isEmpty();
        assertThat(event(null, "", "", "", "", null, null).eligibleStages()).isEmpty();
        assertThat(event("abc,xyz", "", "", "", "", null, null).eligibleStages()).isEmpty();
        // Mixed valid + invalid: only valid ints survive.
        assertThat(event("1,abc,3", "", "", "", "", null, null).eligibleStages())
                .containsExactly(1, 3);
    }

    @Test
    void choices_skipsBlankChoices() {
        EventData e = event("1", "Pick A", "type=gold|value=10",
                "", "", null, null);
        List<EventChoice> choices = e.choices();
        assertThat(choices).hasSize(1);
        assertThat(choices.get(0).text()).isEqualTo("Pick A");
    }

    @Test
    void choices_fullThree() {
        EventData e = event("1", "A", "type=gold|value=10",
                "B", "type=stress|target=party|value=5",
                "C", "type=none|chance=1.0");
        assertThat(e.choices()).hasSize(3);
    }

    @Test
    void parseOutcomes_singleEffect() {
        List<EventOutcome> outs = EventData.parseOutcomes("type=gold|value=300|chance=0.5");
        assertThat(outs).hasSize(1);
        EventOutcome o = outs.get(0);
        assertThat(o.type()).isEqualTo("gold");
        assertThat(o.value()).isEqualTo("300");
        assertThat(o.chance()).isEqualTo(0.5);
    }

    @Test
    void parseOutcomes_multipleEffectsSeparatedBySemicolon() {
        String raw = "type=gold|value=300|chance=0.5; type=damage|target=random_hero|value=8-15|chance=0.3";
        List<EventOutcome> outs = EventData.parseOutcomes(raw);
        assertThat(outs).hasSize(2);
        assertThat(outs.get(0).type()).isEqualTo("gold");
        assertThat(outs.get(1).type()).isEqualTo("damage");
        assertThat(outs.get(1).target()).isEqualTo("random_hero");
        assertThat(outs.get(1).value()).isEqualTo("8-15");
    }

    @Test
    void parseOutcomes_missingChance_defaultsTo1() {
        List<EventOutcome> outs = EventData.parseOutcomes("type=gold|value=100");
        assertThat(outs.get(0).chance()).isEqualTo(1.0);
    }

    @Test
    void parseOutcomes_blankOrNull_returnsEmpty() {
        assertThat(EventData.parseOutcomes("")).isEmpty();
        assertThat(EventData.parseOutcomes(null)).isEmpty();
    }

    @Test
    void parseOutcomes_chanceClampedTo01() {
        List<EventOutcome> overOne = EventData.parseOutcomes("type=gold|value=10|chance=2.5");
        assertThat(overOne.get(0).chance()).isEqualTo(1.0);
        List<EventOutcome> negative = EventData.parseOutcomes("type=gold|value=10|chance=-0.5");
        assertThat(negative.get(0).chance()).isZero();
    }
}
