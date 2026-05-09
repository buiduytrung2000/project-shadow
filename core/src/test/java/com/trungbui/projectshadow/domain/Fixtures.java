package com.trungbui.projectshadow.domain;

import com.trungbui.projectshadow.data.model.EnemyData;
import com.trungbui.projectshadow.data.model.HeroData;
import com.trungbui.projectshadow.data.model.SkillData;

import java.util.List;

public final class Fixtures {

    private Fixtures() {
    }

    public static HeroData heroData(String id, int hp, int dmgMin, int dmgMax, int acc, double crit, int speed) {
        return new HeroData(
                "Common", "Test", "Test", id,
                "DPS", "Front",
                hp, dmgMin, dmgMax, acc, crit, speed, 0.20,
                3, 1, 0.01, 0.02,
                List.of("sk_test"), List.of("sk_test"),
                ""
        );
    }

    public static EnemyData enemyData(String id, int hp, int dmgMin, int dmgMax, int acc, double crit) {
        return new EnemyData(
                "Test " + id, id,
                hp, dmgMin, dmgMax, crit, 0.10, acc,
                "sk_e_stab", null, null, 0.0, null,
                "", "Base"
        );
    }

    public static SkillData skill(String id, double dmgMult, int accMod, int stressDmg) {
        return new SkillData(
                id, "Test", "Test", "Warrior", "Warrior",
                true, "Front Enemy",
                dmgMult, accMod, 0,
                null, null, null, null,
                stressDmg, "Common", ""
        );
    }
}
