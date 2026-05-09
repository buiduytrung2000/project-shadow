package com.trungbui.projectshadow.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StageConfig(
        String id,
        String name,
        @JsonProperty("name_en") String nameEn,
        int act,
        int difficulty,
        String description,
        JsonNode presentation,
        JsonNode layout,
        JsonNode rules,
        JsonNode pools,
        @JsonProperty("boss_node") JsonNode bossNode,
        @JsonProperty("validation_refs") ValidationRefs validationRefs
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValidationRefs(
            @JsonProperty("enemies_used") List<String> enemiesUsed,
            @JsonProperty("enemy_skills_used") List<String> enemySkillsUsed,
            @JsonProperty("events_used") List<String> eventsUsed,
            @JsonProperty("items_referenced") List<String> itemsReferenced
    ) {
    }
}
