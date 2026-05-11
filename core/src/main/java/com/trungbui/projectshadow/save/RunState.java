package com.trungbui.projectshadow.save;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trungbui.projectshadow.domain.Hero;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record RunState(
        /** Sprint 9+ B3 — JSON schema version. Bumped when fields are added/renamed
         *  in a backward-incompatible way. {@link com.trungbui.projectshadow.save.SaveMigration}
         *  refuses to load a file whose {@code saveVersion} exceeds
         *  {@link SaveMigration#CURRENT_RUN_VERSION}. Legacy saves without this
         *  field load as version 1 (compact constructor normalizes 0 → 1). */
        int saveVersion,
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
        if (saveVersion < 1) saveVersion = 1; // Backward-compat: pre-B3 saves had no version field.
        visitedNodes = visitedNodes == null ? List.of() : List.copyOf(visitedNodes);
        party = party == null ? List.of() : List.copyOf(party);
        inventory = inventory == null ? List.of() : List.copyOf(inventory);
    }

    public static RunState newRun(String stageId, long stageSeed, List<Hero> heroes) {
        Instant now = Instant.now();
        List<HeroState> snapshots = heroes.stream().map(HeroState::from).toList();
        return new RunState(
                SaveMigration.CURRENT_RUN_VERSION,
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
                saveVersion, runId, stageId, stageSeed, nodeLabel,
                newVisited, party, gold, inventory,
                createdAt, Instant.now(), archived
        );
    }

    public RunState withParty(List<Hero> liveHeroes) {
        List<HeroState> newParty = liveHeroes.stream().map(HeroState::from).toList();
        return new RunState(
                saveVersion, runId, stageId, stageSeed, currentNodeLabel,
                visitedNodes, newParty, gold, inventory,
                createdAt, Instant.now(), archived
        );
    }

    public RunState withGold(int newGold) {
        return new RunState(
                saveVersion, runId, stageId, stageSeed, currentNodeLabel,
                visitedNodes, party, newGold, inventory,
                createdAt, Instant.now(), archived
        );
    }

    public RunState withInventory(List<String> newInventory) {
        return new RunState(
                saveVersion, runId, stageId, stageSeed, currentNodeLabel,
                visitedNodes, party, gold, newInventory,
                createdAt, Instant.now(), archived
        );
    }

    /**
     * Add a delta to the current run gold pool.
     * <p>Sprint 9+ B3: a negative delta that would push gold below 0 now throws
     * {@link IllegalStateException} rather than silently flooring to 0. Reward
     * paths (always positive) are unaffected. Cost paths now self-detect overdraw
     * instead of producing a misleadingly-applied transaction.</p>
     */
    public RunState withGoldDelta(int delta) {
        int next = gold + delta;
        if (next < 0) {
            throw new IllegalStateException(
                    "Insufficient gold for delta=" + delta + " (current=" + gold + ")");
        }
        return withGold(next);
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
                saveVersion, runId, stageId, stageSeed, currentNodeLabel,
                visitedNodes, party, gold, inventory,
                createdAt, Instant.now(), true
        );
    }

    @JsonIgnore
    public boolean isPartyDead() {
        return party.stream().allMatch(h -> h.currentHp() <= 0);
    }
}
