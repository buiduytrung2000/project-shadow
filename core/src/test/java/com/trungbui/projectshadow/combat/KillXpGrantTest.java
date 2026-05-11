package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.meta.HamletService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 12 B2 — verify XP is granted to the killer + alive party allies on
 * enemy kill. Variant multipliers (Tank/Boss) scale the base.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KillXpGrantTest {

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        gd = GameData.loadFromDirectory(dataDir);
    }

    /** Drive combat to one kill: 2 heroes vs 1 enemy with HP=1 so any hit ends it. */
    private CombatController runUntilOneKill(String enemyId) {
        CombatEncounter enc = CombatScenario.build(gd,
                List.of("hero_01", "hero_13"),
                List.of(enemyId));
        // Force enemy to die on first hit.
        Enemy e = enc.enemies().get(0);
        e.takeHpDamage(e.currentHp() - 1);
        CombatController c = new CombatController(enc, gd, new Random(42L));
        c.start();
        int safety = 0;
        while (!enc.isCombatOver() && safety++ < 40) {
            if (c.currentActor().orElseThrow() instanceof Hero) {
                boolean acted = false;
                for (int i = 0; i < 4 && !acted; i++) {
                    if (c.skillRequiresTargetPick(i)) {
                        var picks = c.skillCandidateTargets(i);
                        if (!picks.isEmpty()) acted = c.executePlayerSkill(i, picks.get(0));
                    } else {
                        acted = c.executePlayerSkill(i);
                    }
                }
                if (!acted) break;
            }
        }
        return c;
    }

    @Test
    void normalEnemyKill_grantsBaseXpToKiller_halfToAlly() {
        CombatController c = runUntilOneKill("enemy_01");
        var enc = c.encounter();
        // One hero killed; one didn't. Sort: killer has higher XP.
        int xpA = enc.heroes().get(0).currentXp();
        int xpB = enc.heroes().get(1).currentXp();
        int killerXp = Math.max(xpA, xpB);
        int allyXp = Math.min(xpA, xpB);

        int expectedKill = HamletService.killXpForEnemy(gd.enemies().get("enemy_01"));
        int expectedAlly = (int) Math.round(expectedKill * HamletService.KILL_XP_PARTY_SHARE);

        assertThat(killerXp).isEqualTo(expectedKill);
        assertThat(allyXp).isEqualTo(expectedAlly);
    }

    @Test
    void killXpForEnemy_appliesVariantMultiplier() {
        // Default Boss = 5×. Test the helper directly.
        var bossData = gd.enemies().get("enemy_b01");
        assertThat(bossData).isNotNull();
        int bossXp = HamletService.killXpForEnemy(bossData);
        int baseXp = HamletService.KILL_XP_BASE;
        assertThat(bossXp).isEqualTo((int) Math.round(baseXp * HamletService.KILL_XP_VARIANT_MULT_BOSS));

        // Miniboss = 2.5×
        var minibossData = gd.enemies().get("enemy_mb01");
        if (minibossData != null) {
            int mbXp = HamletService.killXpForEnemy(minibossData);
            assertThat(mbXp).isEqualTo((int) Math.round(baseXp * HamletService.KILL_XP_VARIANT_MULT_MINIBOSS));
        }
    }

    @Test
    void killXpForEnemy_nullDefaultsToBase() {
        assertThat(HamletService.killXpForEnemy(null)).isEqualTo(HamletService.KILL_XP_BASE);
    }

    @Test
    void killXpForEnemy_unknownVariantDefaultsToBase() {
        // Tank-variant CSV row exists ("enemy_01_tank"). Verify it returns base × 1.2.
        var tankData = gd.enemies().get("enemy_01_tank");
        if (tankData != null && "Tank".equalsIgnoreCase(tankData.variantType())) {
            int xp = HamletService.killXpForEnemy(tankData);
            assertThat(xp).isEqualTo((int) Math.round(
                    HamletService.KILL_XP_BASE * HamletService.KILL_XP_VARIANT_MULT_TANK));
        }
    }
}
