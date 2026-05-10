package com.trungbui.projectshadow.stage;

public record RestOption(
        String label,
        String effect,
        String target,
        int valueMin,
        int valueMax
) {
}
