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
        // Sprint 11 B1: hireHero/refresh/supplies tax follow debt model (no throw).
        // For i18n testing of the "not enough gold" error path we route through
        // levelUpHero which still throws on insufficient gold (upgrades aren't
        // survival needs — keep strict).
        MetaState meta = MetaState.fresh(gd, List.of("hero_01")).withGold(0);
        assertThatThrownBy(() -> HamletService.levelUpHero(meta, "hero_01", gd))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("Không đủ gold");
    }

    @Test
    void notEnoughGold_inEnglish_afterSetLocale() {
        I18n.setLocale(I18n.EN);
        MetaState meta = MetaState.fresh(gd, List.of("hero_01")).withGold(0);
        assertThatThrownBy(() -> HamletService.levelUpHero(meta, "hero_01", gd))
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
