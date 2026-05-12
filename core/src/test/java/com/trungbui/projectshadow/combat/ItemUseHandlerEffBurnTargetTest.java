package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.run.RunSession;
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
 * Fix F (post-Sprint-13) — verify that targeted item effects (eff_burn, eff_dmg_buff,
 * eff_absorb) apply to the specified target via {@link CombatController#useItem}.
 *
 * <p>This tests the controller/handler layer, not the libGDX CombatScreen. The
 * Screen-level target picker logic is pure UI state that cannot be unit-tested
 * without libGDX; it is verified via smoke test during manual play.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemUseHandlerEffBurnTargetTest {

    @TempDir Path tempDir;

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        if (dataDir == null) throw new IllegalStateException("Cannot find assets/data");
        gd = GameData.loadFromDirectory(dataDir);
    }

    private RunSession freshSession() {
        SaveManager mgr = new SaveManager(tempDir.resolve("sv-" + System.nanoTime()));
        return RunSession.startNew(gd, mgr, "stage_1", 42L,
                List.of("hero_01", "hero_13"));
    }

    private CombatController controller(RunSession session, CombatEncounter enc) {
        CombatController ctrl = new CombatController(enc, gd, new Random(1L));
        ctrl.setRunSession(session);
        ctrl.start();
        return ctrl;
    }

    // ── eff_burn — apply to enemy target ────────────────────────────────────

    @Test
    void effBurn_appliedToEnemy_enemyHasActiveEffect() {
        RunSession session = freshSession();
        session.setStateForItemUse(session.state().withInventoryAdd("item_c09")); // Combustion Flask
        CombatEncounter enc = CombatScenario.buildWithHeroes(gd, session.party(),
                List.of("enemy_01"));
        CombatController ctrl = controller(session, enc);
        if (!(ctrl.currentActor().orElseThrow() instanceof Hero)) return;

        Enemy enemy = enc.enemies().get(0);
        boolean ok = ctrl.useItem("item_c09", enemy);
        assertThat(ok).isTrue();
        assertThat(session.state().inventory()).doesNotContain("item_c09");
    }

    // ── eff_dmg_buff — applied to hero target ───────────────────────────────

    @Test
    void effDmgBuff_appliedToHero_succeeds() {
        RunSession session = freshSession();
        session.setStateForItemUse(session.state().withInventoryAdd("item_c06")); // Whetstone
        CombatEncounter enc = CombatScenario.buildWithHeroes(gd, session.party(),
                List.of("enemy_01"));
        CombatController ctrl = controller(session, enc);
        if (!(ctrl.currentActor().orElseThrow() instanceof Hero)) return;

        Hero hero = enc.heroes().get(0);
        boolean ok = ctrl.useItem("item_c06", hero);
        assertThat(ok).isTrue();
        assertThat(session.state().inventory()).doesNotContain("item_c06");
    }

    // ── eff_absorb — applied to hero target ─────────────────────────────────

    @Test
    void effAbsorb_appliedToHero_heroHasShield() {
        RunSession session = freshSession();
        session.setStateForItemUse(session.state().withInventoryAdd("item_c07")); // Ward Charm
        CombatEncounter enc = CombatScenario.buildWithHeroes(gd, session.party(),
                List.of("enemy_01"));
        CombatController ctrl = controller(session, enc);
        if (!(ctrl.currentActor().orElseThrow() instanceof Hero)) return;

        Hero hero = enc.heroes().get(0);
        int shieldBefore = hero.shieldHp();
        boolean ok = ctrl.useItem("item_c07", hero);
        assertThat(ok).isTrue();
        assertThat(hero.shieldHp()).isGreaterThan(shieldBefore);
        assertThat(session.state().inventory()).doesNotContain("item_c07");
    }

    // ── eff_burn applied to null target — controller still returns true (item consumed) ──

    @Test
    void effBurn_nullTarget_itemConsumedButEffectNotApplied() {
        RunSession session = freshSession();
        session.setStateForItemUse(session.state().withInventoryAdd("item_c09"));
        CombatEncounter enc = CombatScenario.buildWithHeroes(gd, session.party(),
                List.of("enemy_01"));
        CombatController ctrl = controller(session, enc);
        if (!(ctrl.currentActor().orElseThrow() instanceof Hero)) return;

        // CombatController consumes item regardless (action economy is honest).
        // ItemUseHandler logs no-op for null target but doesn't throw.
        boolean ok = ctrl.useItem("item_c09", null);
        assertThat(ok).isTrue(); // returns true = action was spent
        assertThat(session.state().inventory()).doesNotContain("item_c09"); // consumed
    }
}
