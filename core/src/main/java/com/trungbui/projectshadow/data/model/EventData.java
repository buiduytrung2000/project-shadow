package com.trungbui.projectshadow.data.model;

public record EventData(
        String eventId,
        String nameVn,
        String nameEn,
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
}
