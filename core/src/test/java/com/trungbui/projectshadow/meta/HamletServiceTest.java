package com.trungbui.projectshadow.meta;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.run.RunSession;
import com.trungbui.projectshadow.save.HeroState;
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
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HamletServiceTest {

    @TempDir
    Path tempBaseDir;

    private GameData gd;
    private MetaState meta;

    @BeforeAll
    void loadAll() throws Exception {
        gd = GameData.loadFromDirectory(resolveDataDir());
    }

    @BeforeEach
    void freshMeta() {
        meta = MetaState.fresh(gd, List.of("hero_01", "hero_13"));
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
        throw new IllegalStateException("Cannot locate assets/data from cwd=" + Path.of("").toAbsolutePath());
    }

    // ---------- Stagecoach ----------

    @Test
    void rollOffers_excludesAlreadyRosteredHeroes() {
        RandomGenerator rng = new Random(42);
        List<String> offers = HamletService.rollStagecoachOffers(meta, gd, rng);
        assertThat(offers).hasSizeLessThanOrEqualTo(HamletService.STAGECOACH_OFFER_COUNT);
        assertThat(offers).doesNotContain("hero_01", "hero_13");
    }

    @Test
    void hireHero_addsToRoster_deductsGold() {
        MetaState after = HamletService.hireHero(meta, "hero_05", gd);
        assertThat(after.gold()).isEqualTo(MetaState.FRESH_GOLD - HamletService.STAGECOACH_HIRE_COST);
        assertThat(after.hasInRoster("hero_05")).isTrue();
        assertThat(after.roster()).hasSize(3);
    }

    @Test
    void hireHero_rejectsDuplicate() {
        assertThatThrownBy(() -> HamletService.hireHero(meta, "hero_01", gd))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("đã có trong roster");
    }

    @Test
    void hireHero_rejectsUnknownId() {
        assertThatThrownBy(() -> HamletService.hireHero(meta, "hero_xyz", gd))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("Unknown");
    }

    @Test
    void hireHero_allowsDebt_perSprint11DebtModel() {
        // Sprint 11 B1: hireHero no longer throws on insufficient gold. Gold can
        // go negative ("supplies debt"). Hero is hired; player owes the cost.
        MetaState broke = meta.withGold(10);
        int hireCost = HamletService.hireCost("hero_05", gd);
        MetaState after = HamletService.hireHero(broke, "hero_05", gd);
        assertThat(after.gold()).isEqualTo(10 - hireCost);
        assertThat(after.gold()).isNegative(); // confirmed in debt
        assertThat(after.hasInRoster("hero_05")).isTrue();
    }

    // ---------- Guild ----------

    @Test
    void levelUpCost_scalesLinearlyWithLevel() {
        assertThat(HamletService.levelUpCost(0)).isEqualTo(100);
        assertThat(HamletService.levelUpCost(1)).isEqualTo(200);
        assertThat(HamletService.levelUpCost(4)).isEqualTo(500);
    }

    @Test
    void levelUpHero_increasesLevel_deductsGold() {
        MetaState rich = meta.withGold(1000);
        MetaState after = HamletService.levelUpHero(rich, "hero_01", gd);
        HeroState hs = after.heroInRoster("hero_01").orElseThrow();
        assertThat(hs.level()).isEqualTo(1);
        assertThat(after.gold()).isEqualTo(1000 - 100);
    }

    @Test
    void levelUpHero_rejectsAtMaxLevel() {
        MetaState rich = meta.withGold(99999);
        // bump hero to max
        for (int i = 0; i < HamletService.GUILD_MAX_LEVEL; i++) {
            rich = HamletService.levelUpHero(rich, "hero_01", gd);
        }
        MetaState atCap = rich;
        assertThatThrownBy(() -> HamletService.levelUpHero(atCap, "hero_01", gd))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("level tối đa");
    }

    @Test
    void levelUpHero_rejectsInsufficientGold() {
        MetaState broke = meta.withGold(50);
        assertThatThrownBy(() -> HamletService.levelUpHero(broke, "hero_01", gd))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("Không đủ gold");
    }

    @Test
    void levelUpHero_rejectsHeroNotInRoster() {
        assertThatThrownBy(() -> HamletService.levelUpHero(meta, "hero_05", gd))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("không có trong roster");
    }

    // ---------- Survivalist ----------

    @Test
    void craftRandomTrinket_addsToInventory_deductsGold() {
        RandomGenerator rng = new Random(1);
        MetaState after = HamletService.craftRandomTrinket(meta, gd, rng);
        assertThat(after.trinketInventory()).hasSize(1);
        assertThat(after.gold()).isEqualTo(MetaState.FRESH_GOLD - HamletService.SURVIVALIST_CRAFT_COST);
        // crafted ID must reference an existing trinket
        String crafted = after.trinketInventory().get(0);
        assertThat(gd.items()).containsKey(crafted);
        assertThat(gd.items().get(crafted).category().toLowerCase()).contains("trinket");
    }

    @Test
    void craftRandomTrinket_rejectsInsufficientGold() {
        MetaState broke = meta.withGold(50);
        RandomGenerator rng = new Random(1);
        assertThatThrownBy(() -> HamletService.craftRandomTrinket(broke, gd, rng))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("Không đủ gold");
    }

    // ---------- Caretaker ----------

    @Test
    void cureDisease_removesFromHero_deductsGold() {
        // mutate roster: add disease to hero_01
        HeroState rs = meta.heroInRoster("hero_01").orElseThrow();
        Hero h = rs.toHero(gd);
        h.addDisease("dis_01");
        MetaState withDisease = meta.withRoster(replaceHero(meta.roster(), HeroState.from(h)));

        MetaState after = HamletService.cureDisease(withDisease, "hero_01", "dis_01", gd);
        assertThat(after.heroInRoster("hero_01").orElseThrow().diseases()).doesNotContain("dis_01");
        assertThat(after.gold()).isEqualTo(MetaState.FRESH_GOLD - HamletService.CARETAKER_DISEASE_CURE_COST);
    }

    @Test
    void cureDisease_rejectsHeroWithoutDisease() {
        assertThatThrownBy(() -> HamletService.cureDisease(meta, "hero_01", "dis_01", gd))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("không mắc bệnh");
    }

    @Test
    void reduceStress_lowersStress_deductsGold() {
        HeroState rs = meta.heroInRoster("hero_01").orElseThrow();
        Hero h = rs.toHero(gd);
        h.takeStressDamage(40);
        int stressedAmount = h.currentStress();
        MetaState withStress = meta.withRoster(replaceHero(meta.roster(), HeroState.from(h)));

        MetaState after = HamletService.reduceStress(withStress, "hero_01", gd);
        HeroState hs = after.heroInRoster("hero_01").orElseThrow();
        assertThat(hs.currentStress()).isEqualTo(Math.max(0, stressedAmount - HamletService.CARETAKER_STRESS_RELIEF_BLOCK));
        assertThat(after.gold()).isEqualTo(MetaState.FRESH_GOLD - HamletService.CARETAKER_STRESS_RELIEF_COST);
    }

    @Test
    void reduceStress_rejectsHeroWithNoStress() {
        assertThatThrownBy(() -> HamletService.reduceStress(meta, "hero_01", gd))
                .isInstanceOf(HamletService.HamletException.class)
                .hasMessageContaining("không có stress");
    }

    // ---------- Run end progression ----------

    @Test
    void applyRunOutcome_victory_addsRunGold_keepsRoster() {
        SaveManager sm = new SaveManager(tempBaseDir.resolve("saves-" + System.nanoTime()));
        RunSession run = RunSession.startNew(gd, sm, "stage_1", 42L, List.of("hero_01", "hero_13"));
        // simulate run gold
        // RunState.gold is private, set via withGold
        // We can still apply outcome with gold=0 default.
        MetaState after = HamletService.applyRunOutcome(meta, run, true);
        // No deaths → roster preserved
        assertThat(after.roster()).hasSize(2);
        assertThat(after.gold()).isEqualTo(MetaState.FRESH_GOLD); // run.gold == 0
    }

    @Test
    void applyRunOutcome_defeat_removesAllPartyMembers_noGold() {
        SaveManager sm = new SaveManager(tempBaseDir.resolve("saves-" + System.nanoTime()));
        RunSession run = RunSession.startNew(gd, sm, "stage_1", 42L, List.of("hero_01", "hero_13"));
        // kill the party
        for (Hero h : run.party()) h.takeHpDamage(99999);

        MetaState after = HamletService.applyRunOutcome(meta, run, false);
        assertThat(after.roster()).isEmpty();
        assertThat(after.gold()).isEqualTo(MetaState.FRESH_GOLD);
    }

    @Test
    void applyRunOutcome_partialDeath_removesOnlyDead() {
        SaveManager sm = new SaveManager(tempBaseDir.resolve("saves-" + System.nanoTime()));
        RunSession run = RunSession.startNew(gd, sm, "stage_1", 42L, List.of("hero_01", "hero_13"));
        run.party().get(1).takeHpDamage(99999); // kill hero_13

        MetaState after = HamletService.applyRunOutcome(meta, run, true);
        assertThat(after.hasInRoster("hero_01")).isTrue();
        assertThat(after.hasInRoster("hero_13")).isFalse();
    }

    @Test
    void applyRunOutcome_survivor_carriesHpDamage() {
        SaveManager sm = new SaveManager(tempBaseDir.resolve("saves-" + System.nanoTime()));
        RunSession run = RunSession.startNew(gd, sm, "stage_1", 42L, List.of("hero_01", "hero_13"));
        Hero alive = run.party().get(0);
        int beforeMax = alive.maxHp();
        alive.takeHpDamage(5);

        MetaState after = HamletService.applyRunOutcome(meta, run, true);
        HeroState rs = after.heroInRoster(alive.id()).orElseThrow();
        assertThat(rs.currentHp()).isEqualTo(beforeMax - 5);
    }

    private static List<HeroState> replaceHero(List<HeroState> roster, HeroState updated) {
        List<HeroState> out = new java.util.ArrayList<>(roster.size());
        for (HeroState rs : roster) {
            out.add(rs.heroId().equals(updated.heroId()) ? updated : rs);
        }
        return out;
    }
}
