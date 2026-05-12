package com.trungbui.projectshadow.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 13 B2 — {@link Hero#addRunDamage(int)} and {@link Hero#currentRunDamage()}.
 */
class HeroRunDamageTest {

    private Hero hero() {
        return new Hero(Fixtures.heroData("h1", 30, 5, 8, 90, 0.10, 5), Position.POS_1);
    }

    @Test
    void freshHero_runDamage_isZero() {
        assertThat(hero().currentRunDamage()).isZero();
    }

    @Test
    void addRunDamage_accumulates() {
        Hero h = hero();
        h.addRunDamage(12);
        h.addRunDamage(8);
        assertThat(h.currentRunDamage()).isEqualTo(20);
    }

    @Test
    void addRunDamage_negativeOrZero_ignored() {
        Hero h = hero();
        h.addRunDamage(-5);
        h.addRunDamage(0);
        assertThat(h.currentRunDamage()).isZero();
    }

    @Test
    void addRunDamage_largeValue_accumulates() {
        Hero h = hero();
        h.addRunDamage(1000);
        assertThat(h.currentRunDamage()).isEqualTo(1000);
    }
}
