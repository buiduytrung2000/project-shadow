package com.trungbui.projectshadow.meta;

import com.trungbui.projectshadow.save.HeroState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 10 B1 — verifies the {@link MetaState} extensions: heirloom + buildingLevels
 * + cureSlotsUsedThisVisit + roster cap helpers.
 */
class MetaStateBuildingLevelsTest {

    private static MetaState empty() {
        Instant now = Instant.now();
        return new MetaState(2, 0, 0, List.<HeroState>of(), List.<String>of(),
                Map.<String, Integer>of(), 0, now, now);
    }

    @Test
    void buildingLevels_defaultsToAllLv1WhenNull() {
        Instant now = Instant.now();
        MetaState m = new MetaState(2, 0, 0, List.of(), List.of(), null, 0, now, now);
        assertThat(m.buildingLevel(MetaState.B_STAGECOACH)).isEqualTo(1);
        assertThat(m.buildingLevel(MetaState.B_GUILD)).isEqualTo(1);
        assertThat(m.buildingLevel(MetaState.B_SURVIVALIST)).isEqualTo(1);
        assertThat(m.buildingLevel(MetaState.B_CARETAKER)).isEqualTo(1);
    }

    @Test
    void buildingLevels_defaultsToAllLv1WhenEmpty() {
        // Empty map (not null) should still back-fill defaults via compact constructor.
        // Map.of() is empty; compact constructor takes Map.copyOf, so unknown keys still
        // return 1 from buildingLevel(...) via getOrDefault.
        MetaState m = empty();
        assertThat(m.buildingLevel(MetaState.B_STAGECOACH)).isEqualTo(1);
    }

    @Test
    void withBuildingLevel_setsAndCopies() {
        MetaState m = empty();
        MetaState m2 = m.withBuildingLevel(MetaState.B_STAGECOACH, 2);
        assertThat(m.buildingLevel(MetaState.B_STAGECOACH)).isEqualTo(1); // original unchanged
        assertThat(m2.buildingLevel(MetaState.B_STAGECOACH)).isEqualTo(2);
    }

    @Test
    void withBuildingLevel_rejectsOutOfRange() {
        MetaState m = empty();
        assertThatThrownBy(() -> m.withBuildingLevel(MetaState.B_STAGECOACH, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> m.withBuildingLevel(MetaState.B_STAGECOACH, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withHeirloomDelta_addsAndRejectsNegative() {
        MetaState m = empty();
        MetaState m2 = m.withHeirloomDelta(2);
        assertThat(m2.heirloom()).isEqualTo(2);
        MetaState m3 = m2.withHeirloomDelta(-1);
        assertThat(m3.heirloom()).isEqualTo(1);
        assertThatThrownBy(() -> m3.withHeirloomDelta(-10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient heirloom");
    }

    @Test
    void cureSlot_consumeAndReset() {
        MetaState m = empty();
        assertThat(m.cureSlotsUsedThisVisit()).isZero();
        MetaState used1 = m.withCureSlotConsumed();
        MetaState used2 = used1.withCureSlotConsumed();
        assertThat(used2.cureSlotsUsedThisVisit()).isEqualTo(2);
        MetaState reset = used2.withCureSlotsReset();
        assertThat(reset.cureSlotsUsedThisVisit()).isZero();
    }

    @Test
    void rosterCap_helpers() {
        // Build 21 hero states to exceed soft cap of 20. HeroState signature:
        // (heroId, level, currentHp, currentStress, positionRank,
        //  equippedSkills, traits, diseases, skillCooldowns)
        List<HeroState> bigRoster = java.util.stream.IntStream.range(0, 21)
                .mapToObj(i -> new HeroState("hero_" + i, 0, 10, 10, 0,
                        java.util.List.<String>of(), java.util.List.<String>of(),
                        java.util.List.<String>of(), java.util.Map.<String, Integer>of()))
                .toList();
        Instant now = Instant.now();
        MetaState m = new MetaState(2, 100, 0, bigRoster, List.of(),
                Map.of(), 0, now, now);

        assertThat(m.isRosterOverCap()).isTrue();
        assertThat(m.rosterCullExcess()).isEqualTo(1);
    }
}
