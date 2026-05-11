package com.trungbui.projectshadow.meta;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.i18n.I18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 9+ B3 — verifies {@link HamletService} error messages now flow through
 * {@link I18n} so EN-locale players see English exception text. Pre-B3 errors
 * were hard-coded Vietnamese strings.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HamletServiceI18nTest {

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
        Path[] candidates = {
                Path.of("../assets/data"),
                Path.of("assets/data"),
                Path.of("../../assets/data")
        };
        Path dataDir = null;
        for (Path p : candidates) {
            if (Files.isDirectory(p)) { dataDir = p; break; }
        }
        gd = GameData.loadFromDirectory(dataDir);
    }

    @AfterEach
    void resetLocale() {
        I18n.setLocale(I18n.VI);
    }

    @Test
    void notEnoughGold_inVietnamese_byDefault() {
        // FRESH_GOLD = 200; pick a hero whose hireCost > 200 to force the throw.
        // Common = 50, Rare = 80, Legendary = 150 — none exceed 200 alone, so
        // construct a depleted meta with 0 gold.
        MetaState meta = MetaState.fresh(gd, List.of("hero_01")).withGold(0);
        String pickAvailable = gd.heroes().keySet().stream()
                .filter(id -> !meta.hasInRoster(id))
                .findFirst().orElseThrow();

        assertThatThrownBy(() -> HamletService.hireHero(meta, pickAvailable, gd))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("Không đủ gold");
    }

    @Test
    void notEnoughGold_inEnglish_afterSetLocale() {
        I18n.setLocale(I18n.EN);
        MetaState meta = MetaState.fresh(gd, List.of("hero_01")).withGold(0);
        String pickAvailable = gd.heroes().keySet().stream()
                .filter(id -> !meta.hasInRoster(id))
                .findFirst().orElseThrow();

        assertThatThrownBy(() -> HamletService.hireHero(meta, pickAvailable, gd))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("Not enough gold");
    }

    @Test
    void heroNotInRoster_localizes() {
        I18n.setLocale(I18n.EN);
        MetaState meta = MetaState.fresh(gd, List.of("hero_01"));

        assertThatThrownBy(() -> HamletService.levelUpHero(meta, "hero_14_doesntexist", gd))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("Hero not in roster");
    }

    @Test
    void heroDuplicate_localizes() {
        I18n.setLocale(I18n.EN);
        MetaState meta = MetaState.fresh(gd, List.of("hero_01"));

        assertThatThrownBy(() -> HamletService.hireHero(meta, "hero_01", gd))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("Hero already in roster");
    }

    @Test
    void noDisease_localizes() {
        I18n.setLocale(I18n.EN);
        MetaState meta = MetaState.fresh(gd, List.of("hero_01"));

        assertThatThrownBy(() -> HamletService.cureDisease(meta, "hero_01", "dis_01", gd))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("does not have disease");
    }

    @Test
    void noStress_localizes() {
        I18n.setLocale(I18n.EN);
        MetaState meta = MetaState.fresh(gd, List.of("hero_01"));

        assertThatThrownBy(() -> HamletService.reduceStress(meta, "hero_01", gd))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("no stress");
    }
}
