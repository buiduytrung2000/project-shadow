package com.trungbui.projectshadow.data.model;

public record DiseaseTraitData(
        String name,
        String type,
        String id,
        String effectStat,
        String effectValue,
        String trigger,
        String condition,
        int removeCost,
        String description,
        String severity,
        String duration,
        String status
) {
}
