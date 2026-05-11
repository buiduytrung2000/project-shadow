package com.trungbui.projectshadow.run;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.save.SaveManager;
import com.trungbui.projectshadow.stage.RestOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 10 B3 — {@link RestOptionApplier} routes rest node option effects to
 * run state mutations.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RestOptionApplierTest {

    @TempDir Path tempDir;
    private GameData gd;
    private RunSession run;

    @BeforeAll
    void load() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data"), Path.of("../../assets/data") };
        Path dataDir = null;
        for (Path p : candidates) {
            if (Files.isDirectory(p)) { dataDir = p; break; }
        }
        gd = GameData.loadFromDirectory(dataDir);
    }

    @BeforeEach
    void freshRun() throws Exception {
        SaveManager sm = new SaveManager(tempDir);
        run = RunSession.startNew(gd, sm, "stage_1", 42L,
                List.of("hero_01", "hero_02", "hero_03", "hero_05"));
    }

    @Test
    void heal_partyTarget_healsAllAliveHeroes() {
        // Damage every hero first.
        for (Hero h : run.party()) h.takeHpDamage(8);
        int before = run.party().stream().mapToInt(Hero::currentHp).sum();
        RestOption opt = new RestOption("Camp", "heal", "party", 5, 5);
        RestOptionApplier.AppliedSummary s = RestOptionApplier.apply(opt, run, new Random(0L));
        int after = run.party().stream().mapToInt(Hero::currentHp).sum();
        assertThat(after - before).isEqualTo(5 * run.party().size());
        assertThat(s.totalHealed).isPositive();
    }

    @Test
    void heal_randomHeroTarget_healsExactlyOne() {
        for (Hero h : run.party()) h.takeHpDamage(8);
        int before = run.party().stream().mapToInt(Hero::currentHp).sum();
        RestOption opt = new RestOption("Bandage", "heal", "random_hero", 7, 7);
        RestOptionApplier.apply(opt, run, new Random(42L));
        int after = run.party().stream().mapToInt(Hero::currentHp).sum();
        assertThat(after - before).isEqualTo(7);
    }

    @Test
    void reduceStress_partyTarget_reducesAllHeroStress() {
        for (Hero h : run.party()) h.takeStressDamage(20);
        RestOption opt = new RestOption("Meditate", "reduce_stress", "party", 8, 8);
        RestOptionApplier.apply(opt, run, new Random(0L));
        for (Hero h : run.party()) {
            assertThat(h.currentStress()).isLessThanOrEqualTo(20 - 8);
        }
    }

    @Test
    void removeDisease_picksDiseasedHero_andRemovesOne() {
        Hero target = run.party().get(1);
        target.addDisease("dis_01");
        target.addDisease("dis_02");
        RestOption opt = new RestOption("Herb", "remove_disease", "random_hero", 0, 0);
        RestOptionApplier.AppliedSummary s = RestOptionApplier.apply(opt, run, new Random(0L));
        assertThat(s.diseaseRemovedFrom).isEqualTo(target.id());
        assertThat(target.diseases()).hasSize(1);
    }

    @Test
    void removeDisease_noDiseases_flagsNoOp() {
        // No hero has any disease.
        RestOption opt = new RestOption("Herb", "remove_disease", "random_hero", 0, 0);
        RestOptionApplier.AppliedSummary s = RestOptionApplier.apply(opt, run, new Random(0L));
        assertThat(s.removeDiseaseNoOp).isTrue();
        assertThat(s.diseaseRemovedFrom).isNull();
    }

    @Test
    void buff_flagsAsSkipped() {
        RestOption opt = new RestOption("Pray", "buff", "party", 0, 0);
        RestOptionApplier.AppliedSummary s = RestOptionApplier.apply(opt, run, new Random(0L));
        assertThat(s.buffSkipped).isTrue();
    }

    @Test
    void skillSwap_flagsNeedsUi() {
        RestOption opt = new RestOption("Practice", "skill_swap", "self", 0, 0);
        RestOptionApplier.AppliedSummary s = RestOptionApplier.apply(opt, run, new Random(0L));
        assertThat(s.skillSwapNeedsUi).isTrue();
    }

    @Test
    void unknownEffect_silentNoOp() {
        RestOption opt = new RestOption("???", "unknown_effect", "party", 0, 0);
        RestOptionApplier.AppliedSummary s = RestOptionApplier.apply(opt, run, new Random(0L));
        assertThat(s.totalHealed).isZero();
        assertThat(s.totalStressReduced).isZero();
        assertThat(s.removeDiseaseNoOp).isFalse();
    }
}
