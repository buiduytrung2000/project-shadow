package com.trungbui.projectshadow.save;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trungbui.projectshadow.domain.Hero;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record RunState(
        String runId,
        String stageId,
        long stageSeed,
        String currentNodeLabel,
        List<String> visitedNodes,
        List<HeroState> party,
        int gold,
        List<String> inventory,
        Instant createdAt,
        Instant lastSavedAt,
        boolean archived
) {
    public RunState {
        visitedNodes = visitedNodes == null ? List.of() : List.copyOf(visitedNodes);
        party = party == null ? List.of() : List.copyOf(party);
        inventory = inventory == null ? List.of() : List.copyOf(inventory);
    }

    public static RunState newRun(String stageId, long stageSeed, List<Hero> heroes) {
        Instant now = Instant.now();
        List<HeroState> snapshots = heroes.stream().map(HeroState::from).toList();
        return new RunState(
                UUID.randomUUID().toString(),
                stageId,
                stageSeed,
                null,
                List.of(),
                snapshots,
                0,
                List.of(),
                now,
                now,
                false
        );
    }

    public RunState withCurrentNode(String nodeLabel) {
        List<String> newVisited = new ArrayList<>(visitedNodes);
        if (currentNodeLabel != null && !newVisited.contains(currentNodeLabel)) {
            newVisited.add(currentNodeLabel);
        }
        return new RunState(
                runId, stageId, stageSeed, nodeLabel,
                newVisited, party, gold, inventory,
                createdAt, Instant.now(), archived
        );
    }

    public RunState withParty(List<Hero> liveHeroes) {
        List<HeroState> newParty = liveHeroes.stream().map(HeroState::from).toList();
        return new RunState(
                runId, stageId, stageSeed, currentNodeLabel,
                visitedNodes, newParty, gold, inventory,
                createdAt, Instant.now(), archived
        );
    }

    public RunState withGold(int newGold) {
        return new RunState(
                runId, stageId, stageSeed, currentNodeLabel,
                visitedNodes, party, newGold, inventory,
                createdAt, Instant.now(), archived
        );
    }

    public RunState withInventory(List<String> newInventory) {
        return new RunState(
                runId, stageId, stageSeed, currentNodeLabel,
                visitedNodes, party, gold, newInventory,
                createdAt, Instant.now(), archived
        );
    }

    /** Add a delta to the current run gold pool (negative deltas allowed). */
    public RunState withGoldDelta(int delta) {
        return withGold(Math.max(0, gold + delta));
    }

    /** Append an item ID to the run inventory (immutable copy). */
    public RunState withInventoryAdd(String itemId) {
        if (itemId == null || itemId.isBlank()) return this;
        List<String> newInv = new ArrayList<>(inventory);
        newInv.add(itemId);
        return withInventory(newInv);
    }

    public RunState markArchived() {
        return new RunState(
                runId, stageId, stageSeed, currentNodeLabel,
                visitedNodes, party, gold, inventory,
                createdAt, Instant.now(), true
        );
    }

    @JsonIgnore
    public boolean isPartyDead() {
        return party.stream().allMatch(h -> h.currentHp() <= 0);
    }
}
