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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 12 B2 — Hero.currentXp + addXp/consumeXp accumulate XP for Guild
 * level-up. Fresh heroes start with 0 XP. Negative inputs ignored on addXp;
 * consumeXp throws if insufficient.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HeroXpTest {

    private GameData gd;

    @BeforeAll
    void load() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        gd = GameData.loadFromDirectory(dataDir);
    }

    @Test
    void freshHero_hasZeroXp() {
        HeroData data = gd.heroes().get("hero_01");
        Hero h = new Hero(data, Position.POS_1, gd.effects());
        assertThat(h.currentXp()).isZero();
    }

    @Test
    void addXp_accumulates() {
        Hero h = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        h.addXp(50);
        h.addXp(25);
        assertThat(h.currentXp()).isEqualTo(75);
    }

    @Test
    void addXp_negativeIgnored() {
        Hero h = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        h.addXp(100);
        h.addXp(-10); // ignored
        assertThat(h.currentXp()).isEqualTo(100);
    }

    @Test
    void consumeXp_deducts() {
        Hero h = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        h.addXp(200);
        h.consumeXp(100);
        assertThat(h.currentXp()).isEqualTo(100);
    }

    @Test
    void consumeXp_throwsWhenInsufficient() {
        Hero h = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        h.addXp(50);
        assertThatThrownBy(() -> h.consumeXp(100))
                .isInstanceOf(IllegalStateException.class);
        // Original XP not modified on throw.
        assertThat(h.currentXp()).isEqualTo(50);
    }

    @Test
    void consumeXp_rejectsNegativeAmount() {
        Hero h = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        assertThatThrownBy(() -> h.consumeXp(-5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setCurrentXp_floorsAtZero() {
        Hero h = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        h.setCurrentXp(-100);
        assertThat(h.currentXp()).isZero();
        h.setCurrentXp(42);
        assertThat(h.currentXp()).isEqualTo(42);
    }
}
