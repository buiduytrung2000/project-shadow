package com.trungbui.projectshadow.stage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StageRulesTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void empty_returnsAllFalseAndNullCaps() {
        StageRules r = StageRules.empty();
        assertThat(r.noConsecutiveRest()).isFalse();
        assertThat(r.noConsecutiveReward()).isFalse();
        assertThat(r.noConsecutiveEventUnless30()).isFalse();
        assertThat(r.maxCombatPerRun()).isNull();
        assertThat(r.minEventPerRun()).isNull();
        assertThat(r.minRestPerRun()).isNull();
        assertThat(r.eliteNodeMaxPerRun()).isNull();
    }

    @Test
    void parseNull_returnsEmpty() {
        assertThat(StageRules.parse(null)).isEqualTo(StageRules.empty());
    }

    @Test
    void parseStage1Rules() throws Exception {
        var node = JSON.readTree("""
                [
                  "no_consecutive_rest",
                  "no_consecutive_reward",
                  "max_combat_per_run: 3",
                  "min_event_per_run: 1"
                ]
                """);
        StageRules r = StageRules.parse(node);
        assertThat(r.noConsecutiveRest()).isTrue();
        assertThat(r.noConsecutiveReward()).isTrue();
        assertThat(r.maxCombatPerRun()).isEqualTo(3);
        assertThat(r.minEventPerRun()).isEqualTo(1);
    }

    @Test
    void parseStage2Rules_includesElite() throws Exception {
        var node = JSON.readTree("""
                [
                  "no_consecutive_rest",
                  "no_consecutive_event_unless_30%",
                  "max_combat_per_run: 4",
                  "min_event_per_run: 2",
                  "min_rest_per_run: 1",
                  "elite_node_max_per_run: 1"
                ]
                """);
        StageRules r = StageRules.parse(node);
        assertThat(r.noConsecutiveEventUnless30()).isTrue();
        assertThat(r.maxCombatPerRun()).isEqualTo(4);
        assertThat(r.minEventPerRun()).isEqualTo(2);
        assertThat(r.minRestPerRun()).isEqualTo(1);
        assertThat(r.eliteNodeMaxPerRun()).isEqualTo(1);
    }

    @Test
    void parseUnknownRule_isIgnored() throws Exception {
        var node = JSON.readTree("""
                [
                  "no_consecutive_rest",
                  "if_player_has_full_party_hp_before_boss: spawn_extra_reward_node",
                  "global_stress_apply: x1.5"
                ]
                """);
        StageRules r = StageRules.parse(node);
        assertThat(r.noConsecutiveRest()).isTrue();
        assertThat(r.maxCombatPerRun()).isNull();
    }
}
