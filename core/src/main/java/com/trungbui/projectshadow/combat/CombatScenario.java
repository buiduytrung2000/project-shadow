package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.domain.Enemy;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;

import java.util.ArrayList;
import java.util.List;

public final class CombatScenario {

    public static final List<String> DEFAULT_HERO_IDS = List.of("hero_01", "hero_13", "hero_05", "hero_03");

    public static final List<String> DEFAULT_ENEMY_IDS = List.of(
            "enemy_01", "enemy_01_tank", "enemy_01_assassin", "enemy_01_special"
    );

    private CombatScenario() {
    }

    public static CombatEncounter buildDefault(GameData gd) {
        return build(gd, DEFAULT_HERO_IDS, DEFAULT_ENEMY_IDS);
    }

    public static CombatEncounter build(GameData gd, List<String> heroIds, List<String> enemyIds) {
        if (heroIds.isEmpty() || heroIds.size() > 4) {
            throw new IllegalArgumentException("heroIds size must be 1..4, got " + heroIds.size());
        }
        if (enemyIds.isEmpty() || enemyIds.size() > 4) {
            throw new IllegalArgumentException("enemyIds size must be 1..4, got " + enemyIds.size());
        }

        List<Hero> heroes = new ArrayList<>();
        for (int i = 0; i < heroIds.size(); i++) {
            String id = heroIds.get(i);
            var data = gd.heroes().get(id);
            if (data == null) throw new IllegalArgumentException("Unknown heroId: " + id);
            heroes.add(new Hero(data, Position.values()[i], gd.effects()));
        }

        List<Enemy> enemies = new ArrayList<>();
        for (int i = 0; i < enemyIds.size(); i++) {
            String id = enemyIds.get(i);
            var data = gd.enemies().get(id);
            if (data == null) throw new IllegalArgumentException("Unknown enemyId: " + id);
            enemies.add(new Enemy(data, Position.values()[i], gd.effects()));
        }

        return new CombatEncounter(heroes, enemies);
    }
}
