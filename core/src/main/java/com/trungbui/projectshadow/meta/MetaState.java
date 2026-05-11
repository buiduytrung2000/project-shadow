package com.trungbui.projectshadow.meta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import com.trungbui.projectshadow.save.HeroState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Sprint 8 — meta progression snapshot persisted across runs at {@code saves/meta.json}.
 *
 * <p>Tracks Hamlet wallet ({@link #gold}), the recruited hero {@link #roster} (each
 * hero state carries between runs — HP/stress/diseases/level), and the crafted
 * {@link #trinketInventory}. Each transformer ({@code with*}) bumps {@link #lastSavedAt}.</p>
 *
 * <p>Hero IDs are unique within {@link #roster} — duplicates are rejected on hire.</p>
 */
public record MetaState(
        /** Sprint 9+ B3 — JSON schema version. See {@code RunState.saveVersion} javadoc.
         *  Legacy meta saves load as version 1 (compact constructor normalizes 0 → 1).
         *  Sprint 10 B1: bumped to v2 (added heirloom + buildingLevels). */
        int saveVersion,
        int gold,
        /** Sprint 10 B1 — second currency (Heirloom). Drops from bosses (1/2/4 per stage)
         *  and is consumed by Hamlet building upgrades. */
        int heirloom,
        List<HeroState> roster,
        List<String> trinketInventory,
        /** Sprint 10 B1 — per-building tier level. Keys: "stagecoach", "guild",
         *  "survivalist", "caretaker". Values: 1..3. Default = all 1. */
        Map<String, Integer> buildingLevels,
        /** Sprint 10 B2 (planned) — Caretaker cure slot tracking. Resets to 0 in
         *  {@code HamletService.applyRunOutcome} per design lock (cure slot reset =
         *  end-of-run). Currently always 0 in B1; consumed by B2 when cure UI lands. */
        int cureSlotsUsedThisVisit,
        Instant createdAt,
        Instant lastSavedAt
) {
    public static final int FRESH_GOLD = 200;

    /** Sprint 10 B1 — soft cap for {@link #roster}. Above this, {@code HamletScreen}
     *  shows a warning and {@code startNewRun} auto-culls random excess heroes. */
    public static final int SOFT_ROSTER_CAP = 20;

    /** Sprint 10 B1 — keys for {@link #buildingLevels}. Don't typo. */
    public static final String B_STAGECOACH = "stagecoach";
    public static final String B_GUILD = "guild";
    public static final String B_SURVIVALIST = "survivalist";
    public static final String B_CARETAKER = "caretaker";

    public MetaState {
        if (saveVersion < 1) saveVersion = 1; // Backward-compat: pre-B3 saves had no version field.
        roster = roster == null ? List.of() : List.copyOf(roster);
        trinketInventory = trinketInventory == null ? List.of() : List.copyOf(trinketInventory);
        // Sprint 10 B1 — buildingLevels default if missing (legacy v1 save load path).
        buildingLevels = buildingLevels == null
                ? defaultBuildingLevels()
                : Map.copyOf(buildingLevels);
        if (heirloom < 0) heirloom = 0; // Defensive: never negative.
        if (cureSlotsUsedThisVisit < 0) cureSlotsUsedThisVisit = 0;
    }

    private static Map<String, Integer> defaultBuildingLevels() {
        Map<String, Integer> m = new HashMap<>();
        m.put(B_STAGECOACH, 1);
        m.put(B_GUILD, 1);
        m.put(B_SURVIVALIST, 1);
        m.put(B_CARETAKER, 1);
        return Map.copyOf(m);
    }

    public static MetaState fresh(GameData gd, List<String> initialHeroIds) {
        Instant now = Instant.now();
        List<HeroState> initial = new ArrayList<>(initialHeroIds.size());
        for (String id : initialHeroIds) {
            var data = gd.heroes().get(id);
            if (data == null) throw new IllegalArgumentException("Unknown heroId: " + id);
            Hero h = new Hero(data, Position.POS_1, gd.effects());
            initial.add(HeroState.from(h));
        }
        return new MetaState(
                com.trungbui.projectshadow.save.SaveMigration.CURRENT_META_VERSION,
                FRESH_GOLD, /* heirloom */ 0, initial, List.of(),
                defaultBuildingLevels(), /* cureSlots */ 0, now, now);
    }

    /** Sprint 10 B1 — lookup building level; 1 if key not present (defensive). */
    public int buildingLevel(String building) {
        return buildingLevels.getOrDefault(building, 1);
    }

    /** Sprint 10 B1 — true if roster size exceeds {@link #SOFT_ROSTER_CAP}. */
    @JsonIgnore
    public boolean isRosterOverCap() {
        return roster.size() > SOFT_ROSTER_CAP;
    }

    /** Sprint 10 B1 — how many heroes need culling to fit cap. */
    @JsonIgnore
    public int rosterCullExcess() {
        return Math.max(0, roster.size() - SOFT_ROSTER_CAP);
    }

    public Optional<HeroState> heroInRoster(String heroId) {
        for (HeroState h : roster) if (h.heroId().equals(heroId)) return Optional.of(h);
        return Optional.empty();
    }

    @JsonIgnore
    public boolean hasInRoster(String heroId) {
        return heroInRoster(heroId).isPresent();
    }

    public MetaState withGold(int newGold) {
        return new MetaState(saveVersion, newGold, heirloom, roster, trinketInventory,
                buildingLevels, cureSlotsUsedThisVisit, createdAt, Instant.now());
    }

    public MetaState withRoster(List<HeroState> newRoster) {
        return new MetaState(saveVersion, gold, heirloom, newRoster, trinketInventory,
                buildingLevels, cureSlotsUsedThisVisit, createdAt, Instant.now());
    }

    public MetaState withTrinketInventory(List<String> newInventory) {
        return new MetaState(saveVersion, gold, heirloom, roster, newInventory,
                buildingLevels, cureSlotsUsedThisVisit, createdAt, Instant.now());
    }

    /** Sprint 10 B1 — add heirloom delta (post-boss-kill grants 1/2/4 per stage). */
    public MetaState withHeirloomDelta(int delta) {
        int next = heirloom + delta;
        if (next < 0) {
            throw new IllegalStateException(
                    "Insufficient heirloom for delta=" + delta + " (current=" + heirloom + ")");
        }
        return new MetaState(saveVersion, gold, next, roster, trinketInventory,
                buildingLevels, cureSlotsUsedThisVisit, createdAt, Instant.now());
    }

    /** Sprint 10 B1 — set a single building's level (1..3). Used by upgrade flow in B2. */
    public MetaState withBuildingLevel(String building, int newLevel) {
        if (newLevel < 1 || newLevel > 3) {
            throw new IllegalArgumentException(
                    "Building level must be in [1, 3], got " + newLevel);
        }
        Map<String, Integer> newLevels = new HashMap<>(buildingLevels);
        newLevels.put(building, newLevel);
        return new MetaState(saveVersion, gold, heirloom, roster, trinketInventory,
                Map.copyOf(newLevels), cureSlotsUsedThisVisit, createdAt, Instant.now());
    }

    /** Sprint 10 B2 — increment cure slots used this visit. Caps at INT_MAX (defensive). */
    public MetaState withCureSlotConsumed() {
        return new MetaState(saveVersion, gold, heirloom, roster, trinketInventory,
                buildingLevels, cureSlotsUsedThisVisit + 1, createdAt, Instant.now());
    }

    /** Sprint 10 B2 — reset cure slots to 0 (called at end-of-run per design lock). */
    public MetaState withCureSlotsReset() {
        return new MetaState(saveVersion, gold, heirloom, roster, trinketInventory,
                buildingLevels, 0, createdAt, Instant.now());
    }
}
