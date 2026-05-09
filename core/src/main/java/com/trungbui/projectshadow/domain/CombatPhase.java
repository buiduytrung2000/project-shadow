package com.trungbui.projectshadow.domain;

public enum CombatPhase {
    COMBAT_START,
    PLAYER_TURN_START,
    AWAITING_ACTION,
    RESOLVING_ACTION,
    ENEMY_TURN_START,
    END_OF_ROUND,
    COMBAT_WIN,
    COMBAT_LOSE
}
