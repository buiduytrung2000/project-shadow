package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 11 B3 — boss phase logic: HP-threshold-based skill dispatch.
 * Phase rules:
 *   - HP < 30% AND specialSkill unused → specialSkill (one-shot)
 *   - HP < 60% AND skill2 exists → 50/50 skill1 vs skill2
 *   - else → skill1
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BossPhaseLogicTest {

    private GameData gd;

    @BeforeAll
    void load() throws Exception {
        Path[] candidates = { Path.of("../assets/data"), Path.of("assets/data") };
        Path dataDir = null;
        for (Path p : candidates) if (Files.isDirectory(p)) { dataDir = p; break; }
        gd = GameData.loadFromDirectory(dataDir);
    }

    private CombatController makeController(Enemy boss, long seed) {
        Hero hero = new Hero(gd.heroes().get("hero_01"), Position.POS_1, gd.effects());
        CombatEncounter enc = new CombatEncounter(List.of(hero), List.of(boss));
        CombatController c = new CombatController(enc, gd, new Random(seed));
        c.start();
        return c;
    }

    @Test
    void normalEnemy_alwaysUsesSkill1() {
        Enemy goblin = new Enemy(gd.enemies().get("enemy_01"), Position.POS_1, gd.effects());
        CombatController c = makeController(goblin, 42L);
        // Drop goblin to 1 HP to simulate low-HP edge case
        goblin.takeHpDamage(goblin.currentHp() - 1);
        String picked = c.pickEnemySkillId(goblin);
        assertThat(picked).isEqualTo(goblin.data().skill1()); // even at 1 HP
    }

    @Test
    void boss_atFullHp_picksSkill1() {
        Enemy boss = new Enemy(gd.enemies().get("enemy_b01"), Position.POS_1, gd.effects());
        CombatController c = makeController(boss, 42L);
        assertThat(boss.currentHp()).isEqualTo(boss.maxHp());
        assertThat(c.pickEnemySkillId(boss)).isEqualTo("sk_e_b01_smash");
    }

    @Test
    void boss_below60pct_canPickSkill2() {
        Enemy boss = new Enemy(gd.enemies().get("enemy_b01"), Position.POS_1, gd.effects());
        CombatController c = makeController(boss, 1L);
        // Damage to 50% HP
        int dmg = (int) (boss.maxHp() * 0.5);
        boss.takeHpDamage(dmg);
        // Across many picks, skill2 should appear sometimes.
        boolean sawSkill2 = false;
        for (int i = 0; i < 30; i++) {
            String p = c.pickEnemySkillId(boss);
            if ("sk_e_b01_putrid_breath".equals(p)) { sawSkill2 = true; break; }
        }
        assertThat(sawSkill2).isTrue();
    }

    @Test
    void boss_below30pct_picksSpecialSkillOnce() {
        Enemy boss = new Enemy(gd.enemies().get("enemy_b01"), Position.POS_1, gd.effects());
        CombatController c = makeController(boss, 0L);
        // Damage to 20% HP
        int dmg = (int) (boss.maxHp() * 0.80);
        boss.takeHpDamage(dmg);
        String first = c.pickEnemySkillId(boss);
        assertThat(first).isEqualTo("sk_e_b01_enrage"); // special triggered

        // Second call: enrage was one-shot. Falls back to skill1 or skill2 mix.
        String second = c.pickEnemySkillId(boss);
        assertThat(second).isNotEqualTo("sk_e_b01_enrage");
    }

    @Test
    void boss_specialResetsAtCombatStart() {
        Enemy boss = new Enemy(gd.enemies().get("enemy_b01"), Position.POS_1, gd.effects());
        CombatController c = makeController(boss, 0L);
        boss.takeHpDamage((int) (boss.maxHp() * 0.80));
        c.pickEnemySkillId(boss); // consumes enrage flag

        // Re-start combat → flag reset → enrage available again.
        Enemy boss2 = new Enemy(gd.enemies().get("enemy_b01"), Position.POS_1, gd.effects());
        CombatController c2 = makeController(boss2, 0L);
        boss2.takeHpDamage((int) (boss2.maxHp() * 0.80));
        assertThat(c2.pickEnemySkillId(boss2)).isEqualTo("sk_e_b01_enrage");
    }
}
