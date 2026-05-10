package com.trungbui.projectshadow.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CombatEncounter {

    public enum Side { HEROES, ENEMIES }

    private final List<Hero> heroes;
    private final List<Enemy> enemies;
    private CombatPhase phase;
    private List<Combatant> turnOrder;
    private int currentTurnIndex;
    private int roundNumber;

    public CombatEncounter(List<Hero> heroes, List<Enemy> enemies) {
        if (heroes == null || heroes.isEmpty()) {
            throw new IllegalArgumentException("at least one hero required");
        }
        if (enemies == null || enemies.isEmpty()) {
            throw new IllegalArgumentException("at least one enemy required");
        }
        this.heroes = new ArrayList<>(heroes);
        this.enemies = new ArrayList<>(enemies);
        this.phase = CombatPhase.COMBAT_START;
        this.roundNumber = 0;
        this.turnOrder = new ArrayList<>();
        this.currentTurnIndex = 0;
    }

    public void startRound() {
        if (isCombatOver()) {
            phase = winningSide() == Side.HEROES ? CombatPhase.COMBAT_WIN : CombatPhase.COMBAT_LOSE;
            return;
        }
        roundNumber++;
        turnOrder = computeTurnOrder();
        currentTurnIndex = 0;
        if (turnOrder.isEmpty()) {
            phase = CombatPhase.END_OF_ROUND;
            return;
        }
        phase = phaseForActor(turnOrder.get(0));
    }

    public boolean advanceTurn() {
        if (turnOrder.isEmpty()) return false;
        currentTurnIndex++;
        while (currentTurnIndex < turnOrder.size() && !turnOrder.get(currentTurnIndex).isAlive()) {
            currentTurnIndex++;
        }
        if (currentTurnIndex >= turnOrder.size()) {
            phase = CombatPhase.END_OF_ROUND;
            return false;
        }
        phase = phaseForActor(turnOrder.get(currentTurnIndex));
        return true;
    }

    public Combatant currentActor() {
        if (turnOrder.isEmpty() || currentTurnIndex >= turnOrder.size()) return null;
        return turnOrder.get(currentTurnIndex);
    }

    public boolean isCombatOver() {
        return heroes.stream().noneMatch(Combatant::isAlive)
                || enemies.stream().noneMatch(Combatant::isAlive);
    }

    public Side winningSide() {
        boolean heroesAlive = heroes.stream().anyMatch(Combatant::isAlive);
        boolean enemiesAlive = enemies.stream().anyMatch(Combatant::isAlive);
        if (heroesAlive && !enemiesAlive) return Side.HEROES;
        if (enemiesAlive && !heroesAlive) return Side.ENEMIES;
        return null;
    }

    private List<Combatant> computeTurnOrder() {
        List<Combatant> all = new ArrayList<>(heroes.size() + enemies.size());
        for (Hero h : heroes) if (h.isAlive()) all.add(h);
        for (Enemy e : enemies) if (e.isAlive()) all.add(e);
        all.sort(Comparator.comparingInt(Combatant::effectiveSpeed).reversed());
        return all;
    }

    private CombatPhase phaseForActor(Combatant actor) {
        return actor instanceof Hero ? CombatPhase.PLAYER_TURN_START : CombatPhase.ENEMY_TURN_START;
    }

    public List<Hero> heroes() {
        return List.copyOf(heroes);
    }

    public List<Enemy> enemies() {
        return List.copyOf(enemies);
    }

    public List<Combatant> turnOrder() {
        return List.copyOf(turnOrder);
    }

    public CombatPhase phase() {
        return phase;
    }

    public void setPhase(CombatPhase phase) {
        this.phase = phase;
    }

    public int roundNumber() {
        return roundNumber;
    }
}
