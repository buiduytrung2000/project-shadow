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

    /**
     * Sprint 12 B4 — per-action turn-order re-sort.
     *
     * <p>After the current actor finishes, we recompute the order of the
     * REMAINING combatants based on each one's <em>current</em>
     * {@link Combatant#effectiveSpeed()}. This means a speed buff applied
     * mid-round (e.g. via {@code item_c11} Amber Essence) takes effect on
     * the very next turn — pre-Sprint-12 the order was fixed at round start.</p>
     *
     * <p>"One action per actor per round" still holds: an actor that has
     * already acted is never picked twice. Dead combatants are skipped.</p>
     */
    public boolean advanceTurn() {
        if (turnOrder.isEmpty()) return false;
        currentTurnIndex++;
        // Sprint 12 B4 — resort the tail of turnOrder by current speed before
        // picking the next alive actor. We sort the sublist [currentTurnIndex,
        // turnOrder.size()) in place so the snapshot returned by turnOrder()
        // also reflects the latest computed order.
        if (currentTurnIndex < turnOrder.size()) {
            List<Combatant> tail = new ArrayList<>(
                    turnOrder.subList(currentTurnIndex, turnOrder.size()));
            tail.sort(Comparator.comparingInt(Combatant::effectiveSpeed).reversed());
            for (int i = 0; i < tail.size(); i++) {
                turnOrder.set(currentTurnIndex + i, tail.get(i));
            }
        }
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
