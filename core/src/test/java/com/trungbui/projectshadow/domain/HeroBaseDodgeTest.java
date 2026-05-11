package com.trungbui.projectshadow.domain;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.HeroData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 11 B3 — Hero.dodge() reads HeroData.baseDodge + level × levelUpDodge.
 * Pre-Sprint-11 it was hardcoded 0.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HeroBaseDodgeTest {

    private GameData gd;

    @BeforeAll
    void load() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        gd = GameData.loadFromDirectory(dataDir);
    }

    @Test
    void heroData_hasBaseDodgeField_defaultFromPosition() {
        // CSV doesn't define Base Dodge column yet; DataLoader defaults
        // Position=Back → 2, else → 0.
        HeroData hero01 = gd.heroes().get("hero_01"); // Warrior, Front
        HeroData hero11 = gd.heroes().get("hero_11"); // Bard, Back
        assertThat(hero01.baseDodge()).isEqualTo(0); // Front
        assertThat(hero11.baseDodge()).isEqualTo(2); // Back
    }

    @Test
    void heroDodge_computesFromBaseAndLevel() {
        // Direct record construction to test the formula.
        HeroData data = new HeroData(
                "Common", "Test", "Test", "h_test",
                "DPS", "Front",
                30, 5, 10, 80, 0.05, 5, 0.20,
                3, 1, 0.01, 0.02,
                /* baseDodge */ 5, /* levelUpDodge */ 1.0,
                List.of(), List.of(), ""
        );
        // Level 0 → 5 + 0*1.0 = 5
        Hero lv0 = new Hero(data, 0, Position.POS_1, List.of());
        assertThat(lv0.dodge()).isEqualTo(5);

        // Level 3 → 5 + 3*1.0 = 8
        Hero lv3 = new Hero(data, 3, Position.POS_1, List.of());
        assertThat(lv3.dodge()).isEqualTo(8);
    }

    @Test
    void heroDodge_isNonNegativeFloor() {
        HeroData data = new HeroData(
                "Common", "Test", "Test", "h_test",
                "DPS", "Front",
                30, 5, 10, 80, 0.05, 5, 0.20,
                3, 1, 0.01, 0.02,
                /* baseDodge */ 0, /* levelUpDodge */ 0,
                List.of(), List.of(), ""
        );
        Hero h = new Hero(data, 0, Position.POS_1, List.of());
        assertThat(h.dodge()).isZero();
    }
}
