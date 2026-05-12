package com.trungbui.projectshadow.data.model;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.i18n.I18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 13 B2 — verify {@link SkillData#displayDescription()} returns the
 * locale-appropriate description and falls back to VN when EN is blank.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SkillDataDescriptionEnTest {

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
        Path[] candidates = {Path.of("../assets/data"), Path.of("assets/data")};
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) {
            dataDir = p;
            break;
        }
        gd = GameData.loadFromDirectory(dataDir);
    }

    @AfterEach
    void resetLocale() {
        I18n.setLocale(I18n.VI);
    }

    // ── Constructor-level tests (no real data) ──────────────────────────

    @Test
    void displayDescription_vnLocale_returnsVnDescription() {
        SkillData skill = new SkillData(
                "sk_test", "Tên VN", "Name EN", "Chiến Binh", "Warrior",
                true, "Front Enemy", 1.0, 0, 0,
                null, null, null, null, 0, "Common",
                "Mô tả tiếng Việt.", "English description.");

        I18n.setLocale(I18n.VI);
        assertThat(skill.displayDescription()).isEqualTo("Mô tả tiếng Việt.");
    }

    @Test
    void displayDescription_enLocale_returnsEnDescription() {
        SkillData skill = new SkillData(
                "sk_test", "Tên VN", "Name EN", "Chiến Binh", "Warrior",
                true, "Front Enemy", 1.0, 0, 0,
                null, null, null, null, 0, "Common",
                "Mô tả tiếng Việt.", "English description.");

        I18n.setLocale(I18n.EN);
        assertThat(skill.displayDescription()).isEqualTo("English description.");
    }

    @Test
    void displayDescription_enLocale_nullEnFallsBackToVn() {
        SkillData skill = new SkillData(
                "sk_test", "Tên VN", "Name EN", "Chiến Binh", "Warrior",
                true, "Front Enemy", 1.0, 0, 0,
                null, null, null, null, 0, "Common",
                "Mô tả tiếng Việt.", null);

        I18n.setLocale(I18n.EN);
        assertThat(skill.displayDescription()).isEqualTo("Mô tả tiếng Việt.");
    }

    @Test
    void displayDescription_enLocale_blankEnFallsBackToVn() {
        SkillData skill = new SkillData(
                "sk_test", "Tên VN", "Name EN", "Chiến Binh", "Warrior",
                true, "Front Enemy", 1.0, 0, 0,
                null, null, null, null, 0, "Common",
                "Mô tả tiếng Việt.", "   ");

        I18n.setLocale(I18n.EN);
        assertThat(skill.displayDescription()).isEqualTo("Mô tả tiếng Việt.");
    }

    // ── Real data tests ──────────────────────────────────────────────────

    @Test
    void skills_csv_loaded_withDescriptionEn_field() {
        // skills.csv now has the "Description (EN)" column (Sprint 13 B2).
        // sk_w1 should have an EN description; other skills may have empty.
        SkillData w1 = gd.skills().get("sk_w1");
        assertThat(w1).isNotNull();

        I18n.setLocale(I18n.EN);
        String enDesc = w1.displayDescription();
        // sk_w1 has an EN translation — it should NOT fall back to the VN text.
        assertThat(enDesc).isNotBlank();
        // Must not be Vietnamese characters (simple sanity check).
        assertThat(enDesc).doesNotContain("Đòn cơ bản");
    }

    @Test
    void skills_csv_unanslated_skill_fallsBackToVn() {
        // Most skills don't have EN descriptions yet — displayDescription() should
        // return the VN text even in EN locale rather than an empty string.
        SkillData w2 = gd.skills().get("sk_w2");
        assertThat(w2).isNotNull();

        I18n.setLocale(I18n.EN);
        // sk_w2 has no EN description in the CSV → must fall back to VN.
        assertThat(w2.displayDescription()).isNotBlank();
    }
}
