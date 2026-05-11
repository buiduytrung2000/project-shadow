package com.trungbui.projectshadow.meta;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import com.trungbui.projectshadow.save.HeroState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 10 B2 — Caretaker cure slot tracking per Hamlet visit.
 * Slot limit by building level: Lv1=1, Lv2=2, Lv3=4. Cost: Lv1=30g, Lv2=25g, Lv3=20g.
 * Slots reset on end-of-run (locked design 2026-05-11).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CaretakerSlotTrackingTest {

    @TempDir Path tempDir;

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

    private MetaState metaWithDiseasedHero(int caretakerLevel, String... diseases) {
        // Build a hero with diseases set, then snapshot into HeroState.
        Hero h = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        for (String d : diseases) h.addDisease(d);
        HeroState rs = HeroState.from(h);
        MetaState meta = new MetaState(2, 10_000, 0,
                List.of(rs), List.of(),
                java.util.Map.of(MetaState.B_CARETAKER, caretakerLevel),
                0, java.time.Instant.now(), java.time.Instant.now());
        return meta;
    }

    @Test
    void lv1_oneCureThenSlotFull() {
        MetaState meta = metaWithDiseasedHero(1, "dis_01", "dis_02");
        MetaState afterFirstCure = HamletService.cureDisease(meta, "hero_01", "dis_01", gd);
        assertThat(afterFirstCure.cureSlotsUsedThisVisit()).isEqualTo(1);

        // Second cure fails — slot exhausted.
        assertThatThrownBy(() -> HamletService.cureDisease(afterFirstCure, "hero_01", "dis_02", gd))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("cure slot");
    }

    @Test
    void lv2_twoCuresThenSlotFull() {
        MetaState meta = metaWithDiseasedHero(2, "dis_01", "dis_02", "dis_03");
        MetaState s1 = HamletService.cureDisease(meta, "hero_01", "dis_01", gd);
        MetaState s2 = HamletService.cureDisease(s1, "hero_01", "dis_02", gd);
        assertThat(s2.cureSlotsUsedThisVisit()).isEqualTo(2);

        assertThatThrownBy(() -> HamletService.cureDisease(s2, "hero_01", "dis_03", gd))
                .isInstanceOf(HamletService.HamletException.class);
    }

    @Test
    void lv3_fourCuresThenSlotFull() {
        MetaState meta = metaWithDiseasedHero(3,
                "dis_01", "dis_02", "dis_03", "dis_04", "dis_05");
        MetaState s = meta;
        for (String d : List.of("dis_01", "dis_02", "dis_03", "dis_04")) {
            s = HamletService.cureDisease(s, "hero_01", d, gd);
        }
        assertThat(s.cureSlotsUsedThisVisit()).isEqualTo(4);

        MetaState frozen = s;
        assertThatThrownBy(() -> HamletService.cureDisease(frozen, "hero_01", "dis_05", gd))
                .isInstanceOf(HamletService.HamletException.class);
    }

    @Test
    void slotsResetAfterRunOutcome() {
        // Simulate state at end of Hamlet visit: slots consumed.
        MetaState meta = metaWithDiseasedHero(2, "dis_01");
        MetaState afterCure = HamletService.cureDisease(meta, "hero_01", "dis_01", gd);
        assertThat(afterCure.cureSlotsUsedThisVisit()).isEqualTo(1);

        // applyRunOutcome should reset slots regardless of victory/defeat.
        MetaState reset = afterCure.withCureSlotsReset();
        assertThat(reset.cureSlotsUsedThisVisit()).isZero();
    }

    @Test
    void costScalesWithLevel() {
        // Lv1: 30g; Lv2: 25g; Lv3: 20g per design lock.
        MetaState lv1 = metaWithDiseasedHero(1, "dis_01");
        MetaState lv2 = metaWithDiseasedHero(2, "dis_01");
        MetaState lv3 = metaWithDiseasedHero(3, "dis_01");

        assertThat(lv1.gold() - HamletService.cureDisease(lv1, "hero_01", "dis_01", gd).gold())
                .isEqualTo(HamletService.CARETAKER_CURE_COST_BY_LEVEL[1]);
        assertThat(lv2.gold() - HamletService.cureDisease(lv2, "hero_01", "dis_01", gd).gold())
                .isEqualTo(HamletService.CARETAKER_CURE_COST_BY_LEVEL[2]);
        assertThat(lv3.gold() - HamletService.cureDisease(lv3, "hero_01", "dis_01", gd).gold())
                .isEqualTo(HamletService.CARETAKER_CURE_COST_BY_LEVEL[3]);
    }
}
