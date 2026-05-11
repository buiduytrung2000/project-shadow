package com.trungbui.projectshadow.meta;

import com.trungbui.projectshadow.save.HeroState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 10 B1 — when roster exceeds soft cap, embark auto-culls random
 * excess heroes. Picked-party heroes are protected.
 */
class RosterAutoCullTest {

    private static HeroState hero(String id) {
        return new HeroState(id, 0, 10, 0, 0,
                List.<String>of(), List.<String>of(),
                List.<String>of(), java.util.Map.<String, Integer>of());
    }

    private static MetaState withRoster(int size) {
        List<HeroState> roster = IntStream.range(0, size)
                .mapToObj(i -> hero("hero_" + i))
                .toList();
        return new MetaState(2, 0, 0, roster, List.of(),
                java.util.Map.of(), 0,
                java.time.Instant.now(), java.time.Instant.now());
    }

    @Test
    void underCap_isNoOp() {
        MetaState m = withRoster(15);
        MetaState culled = HamletService.autoCullRosterToCap(m, List.of("hero_0", "hero_1", "hero_2", "hero_3"));
        assertThat(culled.roster()).hasSize(15);
        assertThat(culled).isSameAs(m);
    }

    @Test
    void atCap_isNoOp() {
        MetaState m = withRoster(20);
        MetaState culled = HamletService.autoCullRosterToCap(m, List.of("hero_0", "hero_1", "hero_2", "hero_3"));
        assertThat(culled.roster()).hasSize(20);
    }

    @Test
    void overCap_cullsToExactCap() {
        MetaState m = withRoster(25);
        MetaState culled = HamletService.autoCullRosterToCap(m, List.of("hero_0", "hero_1", "hero_2", "hero_3"));
        assertThat(culled.roster()).hasSize(MetaState.SOFT_ROSTER_CAP);
    }

    @Test
    void overCap_protectsPickedParty() {
        MetaState m = withRoster(25);
        List<String> party = List.of("hero_20", "hero_21", "hero_22", "hero_23");
        MetaState culled = HamletService.autoCullRosterToCap(m, party);
        List<String> remainingIds = culled.roster().stream().map(HeroState::heroId).toList();
        // All 4 picked-party heroes must survive.
        assertThat(remainingIds).containsAll(party);
    }

    @Test
    void overCap_cullingIsRandom_acrossSeeds() {
        // Run cull twice on identical inputs — results SHOULD differ given fresh RNG
        // (`autoCullRosterToCap` uses `new Random()` internally). We probabilistically
        // check that at least 1 out of 5 trials produces different roster ids.
        MetaState m = withRoster(25);
        List<String> party = List.of("hero_0", "hero_1", "hero_2", "hero_3");

        java.util.Set<List<String>> seenResults = new java.util.HashSet<>();
        for (int i = 0; i < 5; i++) {
            MetaState c = HamletService.autoCullRosterToCap(m, party);
            seenResults.add(c.roster().stream().map(HeroState::heroId).sorted().toList());
        }
        // Across 5 trials we expect at least 2 different roster outcomes
        // (probability of all 5 collapsing to the same random subset is astronomically low).
        assertThat(seenResults).hasSizeGreaterThanOrEqualTo(2);
    }
}
