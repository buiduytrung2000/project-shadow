package com.trungbui.projectshadow.save;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HeroStateTest {

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
        gd = GameData.loadFromDirectory(resolveDataDir());
    }

    private static Path resolveDataDir() {
        Path[] candidates = {
                Path.of("../assets/data"),
                Path.of("assets/data"),
                Path.of("../../assets/data")
        };
        for (Path p : candidates) {
            if (Files.isDirectory(p)) return p;
        }
        throw new IllegalStateException(
                "Cannot locate assets/data from cwd=" + Path.of("").toAbsolutePath());
    }

    @Test
    void from_capturesAllFields_freshHero() {
        Hero hero = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        HeroState state = HeroState.from(hero);
        assertThat(state.heroId()).isEqualTo("hero_01");
        assertThat(state.level()).isZero();
        assertThat(state.currentHp()).isEqualTo(hero.maxHp());
        assertThat(state.currentStress()).isZero();
        assertThat(state.positionRank()).isEqualTo(1);
        assertThat(state.equippedSkills()).containsExactlyElementsOf(hero.equippedSkills());
        assertThat(state.traits()).isEmpty();
        assertThat(state.diseases()).isEmpty();
        assertThat(state.skillCooldowns()).isEmpty();
    }

    @Test
    void roundtrip_preservesHpStressLevelPosition() {
        Hero hero = new Hero(gd.heroes().get("hero_03"), 2, Position.POS_3, gd.heroes().get("hero_03").defaultSkills(), gd.effects());
        hero.takeHpDamage(5);
        hero.takeStressDamage(40);

        HeroState snapshot = HeroState.from(hero);
        Hero restored = snapshot.toHero(gd);

        assertThat(restored.id()).isEqualTo(hero.id());
        assertThat(restored.level()).isEqualTo(hero.level());
        assertThat(restored.currentHp()).isEqualTo(hero.currentHp());
        assertThat(restored.currentStress()).isEqualTo(hero.currentStress());
        assertThat(restored.position()).isEqualTo(hero.position());
        assertThat(restored.maxHp()).isEqualTo(hero.maxHp());
    }

    @Test
    void roundtrip_preservesTraitsAndDiseases() {
        Hero hero = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        hero.addTrait("trait_01");
        hero.addTrait("trait_05");
        hero.addDisease("dis_01");

        HeroState snapshot = HeroState.from(hero);
        Hero restored = snapshot.toHero(gd);

        assertThat(restored.traits()).containsExactlyInAnyOrder("trait_01", "trait_05");
        assertThat(restored.diseases()).containsExactly("dis_01");
    }

    @Test
    void roundtrip_preservesSkillCooldowns() {
        Hero hero = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        hero.putOnCooldown("sk_w2", 2);
        hero.putOnCooldown("sk_w4", 1);

        HeroState snapshot = HeroState.from(hero);
        Hero restored = snapshot.toHero(gd);

        assertThat(restored.isOnCooldown("sk_w2")).isTrue();
        assertThat(restored.isOnCooldown("sk_w4")).isTrue();
        assertThat(restored.isOnCooldown("sk_w1")).isFalse();
    }

    @Test
    void toHero_rejectsUnknownHeroId() {
        HeroState invalid = new HeroState(
                "hero_does_not_exist", 0, 30, 0, 1,
                java.util.List.of("sk_w1"),
                java.util.List.of(),
                java.util.List.of(),
                java.util.Map.of()
        );
        assertThatThrownBy(() -> invalid.toHero(gd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hero_does_not_exist");
    }

    @Test
    void heroState_record_immutableLists() {
        HeroState state = new HeroState(
                "hero_01", 0, 30, 0, 1,
                java.util.List.of("sk_w1", "sk_w2"),
                java.util.List.of("trait_01"),
                java.util.List.of(),
                java.util.Map.of("sk_w2", 2)
        );
        assertThatThrownBy(() -> state.equippedSkills().add("oops"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> state.traits().add("oops"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> state.skillCooldowns().put("oops", 1))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void from_thenToHero_isPureFunction_noSideEffects() {
        Hero original = new Hero(gd.heroes().get("hero_01"), Position.POS_2, gd.effects());
        original.takeHpDamage(3);
        int hpBefore = original.currentHp();

        HeroState.from(original);
        HeroState.from(original);
        HeroState.from(original);

        assertThat(original.currentHp()).isEqualTo(hpBefore);
    }
}
