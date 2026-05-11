package com.trungbui.projectshadow.meta;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.save.HeroState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 11 B1 — debt model regression tests.
 *
 * <p>Gold can go negative on these "survival" operations:
 * hire, stagecoach refresh, supplies tax. Other operations
 * (cure, levelup, craft, upgrades) still throw on insufficient gold.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetaStateDebtTest {

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        gd = GameData.loadFromDirectory(dataDir);
    }

    @Test
    void metaState_withGold_acceptsNegative() {
        MetaState m = MetaState.fresh(gd, List.of("hero_01"));
        MetaState debt = m.withGold(-150);
        assertThat(debt.gold()).isEqualTo(-150);
    }

    @Test
    void hireHero_fromZeroGold_goesIntoDebt() {
        MetaState broke = MetaState.fresh(gd, List.of("hero_01")).withGold(0);
        String pickAvailable = gd.heroes().keySet().stream()
                .filter(id -> !broke.hasInRoster(id))
                .findFirst().orElseThrow();
        int cost = HamletService.hireCost(pickAvailable, gd);
        MetaState after = HamletService.hireHero(broke, pickAvailable, gd);
        assertThat(after.gold()).isEqualTo(-cost);
        assertThat(after.hasInRoster(pickAvailable)).isTrue();
    }

    @Test
    void refresh_fromZeroGold_goesIntoDebt() {
        MetaState broke = MetaState.fresh(gd, List.of("hero_01")).withGold(0);
        MetaState after = HamletService.payStagecoachRefresh(broke);
        assertThat(after.gold()).isEqualTo(-HamletService.STAGECOACH_REFRESH_COST);
    }

    @Test
    void suppliesTax_fromZeroGold_goesIntoDebt() {
        MetaState broke = MetaState.fresh(gd, List.of("hero_01")).withGold(0);
        MetaState after = HamletService.paySuppliesTax(broke, 1);
        assertThat(after.gold()).isEqualTo(-100);
    }

    @Test
    void cureDisease_stillThrowsOnInsufficientGold() {
        // Per debt model design: only survival operations allow debt.
        // Upgrades / cures stay strict.
        MetaState broke = MetaState.fresh(gd, List.of("hero_01")).withGold(0);
        // Add a disease to the hero so we get past the "no disease" check.
        HeroState rs = broke.heroInRoster("hero_01").orElseThrow();
        // Build a hero state with disease, replace in roster.
        HeroState withDisease = new HeroState(rs.heroId(), rs.level(),
                rs.currentHp(), rs.currentStress(), rs.positionRank(),
                rs.equippedSkills(), rs.traits(),
                java.util.List.of("dis_01"), rs.skillCooldowns());
        List<HeroState> newRoster = new java.util.ArrayList<>(broke.roster());
        newRoster.set(0, withDisease);
        MetaState diseased = broke.withRoster(newRoster);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> HamletService.cureDisease(diseased, "hero_01", "dis_01", gd))
                .isInstanceOf(HamletService.HamletException.class);
    }

    @Test
    void buildingUpgrade_stillThrowsOnInsufficientGold() {
        MetaState broke = MetaState.fresh(gd, List.of("hero_01")).withGold(0);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> HamletService.upgradeStagecoach(broke))
                .isInstanceOf(HamletService.HamletException.class);
    }
}
