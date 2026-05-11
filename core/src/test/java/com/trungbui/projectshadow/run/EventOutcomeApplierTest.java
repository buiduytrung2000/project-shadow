package com.trungbui.projectshadow.run;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.EventChoice;
import com.trungbui.projectshadow.data.model.EventData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.save.SaveManager;
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
 * Sprint 10 B3 — {@link EventOutcomeApplier} parses + applies CSV outcome DSL.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventOutcomeApplierTest {

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
    void apply_goldOutcome_chance1_alwaysFires() {
        EventChoice choice = new EventChoice("Pick", EventData.parseOutcomes("type=gold|value=50|chance=1.0"));
        EventOutcomeApplier.AppliedSummary s = EventOutcomeApplier.apply(choice, run, new Random(0L));
        assertThat(s.goldDelta).isEqualTo(50);
        assertThat(run.state().gold()).isEqualTo(50);
    }

    @Test
    void apply_goldOutcome_chance0_neverFires() {
        EventChoice choice = new EventChoice("Pick", EventData.parseOutcomes("type=gold|value=999|chance=0.0"));
        EventOutcomeApplier.AppliedSummary s = EventOutcomeApplier.apply(choice, run, new Random(0L));
        assertThat(s.goldDelta).isZero();
        assertThat(run.state().gold()).isZero();
    }

    @Test
    void apply_stressParty_increasesAllAliveHeroes() {
        EventChoice choice = new EventChoice("Pick",
                EventData.parseOutcomes("type=stress|target=party|value=10|chance=1.0"));
        EventOutcomeApplier.apply(choice, run, new Random(0L));
        for (Hero h : run.party()) {
            assertThat(h.currentStress()).isGreaterThan(0); // each hero got hit
        }
    }

    @Test
    void apply_stressRandomHero_increasesExactlyOne() {
        EventChoice choice = new EventChoice("Pick",
                EventData.parseOutcomes("type=stress|target=random_hero|value=15|chance=1.0"));
        EventOutcomeApplier.apply(choice, run, new Random(42L));
        long stressedCount = run.party().stream().filter(h -> h.currentStress() > 0).count();
        assertThat(stressedCount).isEqualTo(1L);
    }

    @Test
    void apply_damageOutcome_reducesHp() {
        EventChoice choice = new EventChoice("Pick",
                EventData.parseOutcomes("type=damage|target=random_hero|value=5|chance=1.0"));
        Hero firstHero = run.party().get(0);
        int beforeHp = firstHero.currentHp();
        EventOutcomeApplier.apply(choice, run, new Random(0L));
        // At least one hero lost HP (target was random).
        int totalHpLoss = run.party().stream()
                .mapToInt(h -> h.maxHp() - h.currentHp())
                .sum();
        assertThat(totalHpLoss).isEqualTo(5);
    }

    @Test
    void apply_rangeValue_picksFromRange() {
        // Use a fixed seed and assert range bounds.
        EventChoice choice = new EventChoice("Pick",
                EventData.parseOutcomes("type=gold|value=10-20|chance=1.0"));
        EventOutcomeApplier.apply(choice, run, new Random(7L));
        assertThat(run.state().gold()).isBetween(10, 20);
    }

    @Test
    void apply_traitApply_addsTraitToHero() {
        EventChoice choice = new EventChoice("Pick",
                EventData.parseOutcomes("type=trait_apply|target=random_hero|value=trait_05|chance=1.0"));
        EventOutcomeApplier.AppliedSummary s = EventOutcomeApplier.apply(choice, run, new Random(0L));
        assertThat(s.traitsApplied).contains("trait_05");
        boolean someoneHasIt = run.party().stream().anyMatch(h -> h.traits().contains("trait_05"));
        assertThat(someoneHasIt).isTrue();
    }

    @Test
    void apply_noneOutcome_doesNothing() {
        EventChoice choice = new EventChoice("Pick",
                EventData.parseOutcomes("type=none|chance=1.0"));
        EventOutcomeApplier.AppliedSummary s = EventOutcomeApplier.apply(choice, run, new Random(0L));
        assertThat(s.goldDelta).isZero();
        assertThat(run.state().gold()).isZero();
    }

    @Test
    void apply_multipleOutcomesIndependent() {
        // Two outcomes, both 100% chance → both fire.
        EventChoice choice = new EventChoice("Pick",
                EventData.parseOutcomes(
                        "type=gold|value=20|chance=1.0; type=stress|target=party|value=3|chance=1.0"));
        EventOutcomeApplier.AppliedSummary s = EventOutcomeApplier.apply(choice, run, new Random(0L));
        assertThat(s.goldDelta).isEqualTo(20);
        assertThat(s.partyStressAdded).isEqualTo(3);
    }

    @Test
    void parseValueOrRange_handlesEdgeCases() {
        Random rng = new Random(0L);
        assertThat(EventOutcomeApplier.parseValueOrRange("42", rng)).isEqualTo(42);
        assertThat(EventOutcomeApplier.parseValueOrRange("5-10", rng)).isBetween(5, 10);
        assertThat(EventOutcomeApplier.parseValueOrRange("", rng)).isZero();
        assertThat(EventOutcomeApplier.parseValueOrRange(null, rng)).isZero();
        assertThat(EventOutcomeApplier.parseValueOrRange("invalid", rng)).isZero();
    }
}
