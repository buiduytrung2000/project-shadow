package com.trungbui.projectshadow.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class I18nTest {

    @BeforeEach
    void resetToVi() {
        I18n.setLocale(I18n.VI);
    }

    @AfterEach
    void resetAfter() {
        I18n.setLocale(I18n.VI);
    }

    @Test
    void t_returnsVnByDefault() {
        assertThat(I18n.t("hamlet.title")).isEqualTo("HAMLET");
        assertThat(I18n.t("button.quit")).isEqualTo("Thoát");
    }

    @Test
    void t_returnsEnAfterSetLocaleEn() {
        I18n.setLocale(I18n.EN);
        assertThat(I18n.t("button.quit")).isEqualTo("Quit");
        assertThat(I18n.t("hamlet.button.embark")).contains("Start expedition");
    }

    @Test
    void t_substitutesArgsViaMessageFormat() {
        assertThat(I18n.t("hamlet.gold", 250)).isEqualTo("Gold: 250");
        assertThat(I18n.t("error.notEnoughGold", 50, 30))
                .isEqualTo("Không đủ gold (cần 50, có 30)");
    }

    @Test
    void t_returnsSentinelOnMissingKey() {
        assertThat(I18n.t("nonexistent.key")).isEqualTo("??nonexistent.key??");
    }

    @Test
    void setLocale_idempotent() {
        I18n.setLocale(I18n.EN);
        I18n.setLocale(I18n.EN);
        assertThat(I18n.currentLocale()).isEqualTo(I18n.EN);
        assertThat(I18n.isEnglish()).isTrue();
    }

    @Test
    void setLocale_rejectsNull() {
        assertThatThrownBy(() -> I18n.setLocale(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toggleLocale_swapsBetweenViAndEn() {
        assertThat(I18n.currentLocale()).isEqualTo(I18n.VI);
        assertThat(I18n.toggleLocale()).isEqualTo(I18n.EN);
        assertThat(I18n.toggleLocale()).isEqualTo(I18n.VI);
    }

    @Test
    void currentLocale_reflectsLastSet() {
        I18n.setLocale(I18n.EN);
        assertThat(I18n.currentLocale().getLanguage()).isEqualTo("en");
        I18n.setLocale(I18n.VI);
        assertThat(I18n.currentLocale().getLanguage()).isEqualTo("vi");
    }
}
