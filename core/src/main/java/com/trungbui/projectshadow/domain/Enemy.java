package com.trungbui.projectshadow.domain;

import com.trungbui.projectshadow.data.model.EffectData;
import com.trungbui.projectshadow.data.model.EnemyData;
import com.trungbui.projectshadow.effect.ActiveEffects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Enemy implements Combatant {

    public static final int DEFAULT_SPEED = 5;

    private final EnemyData data;
    private int currentHp;
    private Position position;
    private final Map<String, Integer> skillCooldowns;
    private final boolean boss;
    private final ActiveEffects activeEffects;

    public Enemy(EnemyData data, Position position) {
        this(data, position, Collections.emptyMap());
    }

    public Enemy(EnemyData data, Position position, Map<String, EffectData> effectCatalog) {
        if (data == null) throw new IllegalArgumentException("data must not be null");
        if (position == null) throw new IllegalArgumentException("position must not be null");
        this.data = data;
        this.position = position;
        this.currentHp = data.baseHp();
        this.skillCooldowns = new HashMap<>();
        this.boss = "Boss".equalsIgnoreCase(data.variantType());
        this.activeEffects = new ActiveEffects(effectCatalog);
    }

    @Override
    public ActiveEffects activeEffects() {
        return activeEffects;
    }

    @Override
    public String id() {
        return data.enemyId();
    }

    @Override
    public int currentHp() {
        return currentHp;
    }

    @Override
    public int maxHp() {
        return data.baseHp();
    }

    @Override
    public void setCurrentHp(int hp) {
        this.currentHp = Math.max(0, Math.min(maxHp(), hp));
    }

    @Override
    public Position position() {
        return position;
    }

    @Override
    public void setPosition(Position pos) {
        if (pos == null) throw new IllegalArgumentException("position must not be null");
        this.position = pos;
    }

    @Override
    public int speed() {
        return DEFAULT_SPEED;
    }

    @Override
    public int accuracy() {
        return data.accuracy();
    }

    @Override
    public int dodge() {
        return 0;
    }

    @Override
    public double critChance() {
        return data.criticalChance();
    }

    @Override
    public int dmgMin() {
        return data.damageMin();
    }

    @Override
    public int dmgMax() {
        return data.damageMax();
    }

    @Override
    public boolean isOnCooldown(String skillId) {
        return skillCooldowns.getOrDefault(skillId, 0) > 0;
    }

    @Override
    public void putOnCooldown(String skillId, int turns) {
        if (turns <= 0) return;
        skillCooldowns.put(skillId, turns);
    }

    @Override
    public void tickCooldowns() {
        skillCooldowns.replaceAll((k, v) -> Math.max(0, v - 1));
    }

    public double stressResist() {
        return data.stressResist();
    }

    public boolean isBoss() {
        return boss;
    }

    public EnemyData data() {
        return data;
    }
}
