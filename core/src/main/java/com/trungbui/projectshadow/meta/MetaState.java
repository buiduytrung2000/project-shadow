package com.trungbui.projectshadow.meta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import com.trungbui.projectshadow.save.HeroState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
         *  Legacy meta saves load as version 1 (compact constructor normalizes 0 → 1). */
        int saveVersion,
        int gold,
        List<HeroState> roster,
        List<String> trinketInventory,
        Instant createdAt,
        Instant lastSavedAt
) {
    public static final int FRESH_GOLD = 200;

    public MetaState {
        if (saveVersion < 1) saveVersion = 1; // Backward-compat: pre-B3 saves had no version field.
        roster = roster == null ? List.of() : List.copyOf(roster);
        trinketInventory = trinketInventory == null ? List.of() : List.copyOf(trinketInventory);
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
                FRESH_GOLD, initial, List.of(), now, now);
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
        return new MetaState(saveVersion, newGold, roster, trinketInventory, createdAt, Instant.now());
    }

    public MetaState withRoster(List<HeroState> newRoster) {
        return new MetaState(saveVersion, gold, newRoster, trinketInventory, createdAt, Instant.now());
    }

    public MetaState withTrinketInventory(List<String> newInventory) {
        return new MetaState(saveVersion, gold, roster, newInventory, createdAt, Instant.now());
    }
}
