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
        /** Sub-classification for Trait rows: "Affliction" or "Virtue".
         *  Blank for Disease rows. Used by {@code AfflictionResolver} for the
         *  70/30 stress-cross roll at {@code AFFLICTION_THRESHOLD}. */
        String resolution,
        String status
) {

    /** Helper: true if this row is a Trait classified as Affliction. */
    public boolean isAffliction() {
        return "Trait".equalsIgnoreCase(type) && "Affliction".equalsIgnoreCase(resolution);
    }

    /** Helper: true if this row is a Trait classified as Virtue. */
    public boolean isVirtue() {
        return "Trait".equalsIgnoreCase(type) && "Virtue".equalsIgnoreCase(resolution);
    }
}
