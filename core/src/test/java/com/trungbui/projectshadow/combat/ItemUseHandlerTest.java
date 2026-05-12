package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 12 B3 — verify {@link ItemUseHandler} for the two wired consumables
 * (heal + stress reduce). Non-supported effects no-op cleanly.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemUseHandlerTest {

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        gd = GameData.loadFromDirectory(dataDir);
    }

    private Hero freshHero() {
        return new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
    }

    @Test
    void parseLeadingInt_handlesSignedAndUnsigned() {
        assertThat(ItemUseHandler.parseLeadingInt("15 HP")).isEqualTo(15);
        assertThat(ItemUseHandler.parseLeadingInt("-20 stress all allies")).isEqualTo(-20);
        assertThat(ItemUseHandler.parseLeadingInt("+50% dmg next attack")).isEqualTo(50);
        assertThat(ItemUseHandler.parseLeadingInt("nothing here")).isEqualTo(0);
        assertThat(ItemUseHandler.parseLeadingInt(null)).isEqualTo(0);
        assertThat(ItemUseHandler.parseLeadingInt("")).isEqualTo(0);
    }

    @Test
    void apply_unknownItem_returnsNotApplied() {
        Hero h = freshHero();
        ItemUseHandler.AppliedSummary s = ItemUseHandler.apply("item_does_not_exist", h, h, null, gd);
        assertThat(s.applied()).isFalse();
        assertThat(s.notes()).contains("Unknown item");
    }

    @Test
    void apply_trinketItem_rejected() {
        // item_t01 etc. are not consumable.
        String trinketId = gd.items().values().stream()
                .filter(it -> !"consumable".equalsIgnoreCase(it.category()))
                .map(it -> it.itemId()).findFirst().orElseThrow();
        Hero h = freshHero();
        ItemUseHandler.AppliedSummary s = ItemUseHandler.apply(trinketId, h, h, null, gd);
        assertThat(s.applied()).isFalse();
        assertThat(s.notes()).contains("not consumable");
    }

    @Test
    void apply_healItem_restoresHpOnTarget() {
        Hero h = freshHero();
        // Damage hero so heal has room.
        h.takeHpDamage(20);
        int before = h.currentHp();
        ItemUseHandler.AppliedSummary s = ItemUseHandler.apply("item_c01", h, h, null, gd);
        assertThat(s.applied()).isTrue();
        assertThat(h.currentHp()).isGreaterThan(before);
        assertThat(s.healedAmount()).isPositive();
    }

    @Test
    void apply_healItem_failsOnDeadTarget() {
        Hero h = freshHero();
        h.takeHpDamage(9999); // dead
        ItemUseHandler.AppliedSummary s = ItemUseHandler.apply("item_c01", h, h, null, gd);
        assertThat(s.applied()).isFalse();
    }

    @Test
    void apply_unsupportedEffect_returnsNotApplied() {
        // item_c04 = eff_remove (disease cure). Not supported in Phase 2 either.
        // eff_burn / eff_dmg_buff / eff_absorb were wired in Sprint 13 B1.
        Hero h = freshHero();
        ItemUseHandler.AppliedSummary s = ItemUseHandler.apply("item_c04", h, h, null, gd);
        assertThat(s.applied()).isFalse();
        assertThat(s.notes()).contains("not yet supported");
    }
}
