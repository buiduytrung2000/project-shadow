package com.trungbui.projectshadow.meta;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.DiseaseTraitData;
import com.trungbui.projectshadow.save.HeroState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 12 B2 — verify hireHero assigns exactly 2 random Virtue traits
 * (or fewer if pool < 2). Never assigns affliction traits at recruit
 * (those are earned via stress crossings instead).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HireHeroAssigns2RandomVirtuesTest {

    private GameData gd;
    private MetaState meta;
    private Set<String> virtueIds;
    private Set<String> afflictionIds;

    @BeforeAll
    void loadAll() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        gd = GameData.loadFromDirectory(dataDir);
        virtueIds = gd.diseasesTraits().values().stream()
                .filter(DiseaseTraitData::isVirtue)
                .map(DiseaseTraitData::id)
                .collect(Collectors.toSet());
        afflictionIds = gd.diseasesTraits().values().stream()
                .filter(DiseaseTraitData::isAffliction)
                .map(DiseaseTraitData::id)
                .collect(Collectors.toSet());
    }

    @BeforeEach
    void freshMeta() {
        meta = MetaState.fresh(gd, List.of("hero_01", "hero_13")).withGold(1000);
    }

    @Test
    void hireHero_assigns2VirtueTraits() {
        MetaState after = HamletService.hireHero(meta, "hero_05", gd, new Random(42L));
        HeroState newHero = after.heroInRoster("hero_05").orElseThrow();
        assertThat(newHero.traits()).hasSize(HamletService.TRAITS_AT_RECRUIT);
        // All assigned traits must be Virtues.
        for (String t : newHero.traits()) {
            assertThat(virtueIds).as("trait %s should be Virtue", t).contains(t);
        }
    }

    @Test
    void hireHero_neverAssignsAfflictions() {
        // Run several seeds — none should produce an affliction trait at recruit.
        for (long seed = 1L; seed <= 20L; seed++) {
            MetaState fresh = MetaState.fresh(gd, List.of("hero_01")).withGold(1000);
            MetaState after = HamletService.hireHero(fresh, "hero_05", gd, new Random(seed));
            HeroState newHero = after.heroInRoster("hero_05").orElseThrow();
            for (String t : newHero.traits()) {
                assertThat(afflictionIds).as("seed %d trait %s", seed, t).doesNotContain(t);
            }
        }
    }

    @Test
    void hireHero_traitsAreNotDuplicated() {
        MetaState after = HamletService.hireHero(meta, "hero_05", gd, new Random(123L));
        HeroState newHero = after.heroInRoster("hero_05").orElseThrow();
        assertThat(newHero.traits()).doesNotHaveDuplicates();
    }
}
