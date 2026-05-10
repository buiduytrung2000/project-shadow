package com.trungbui.projectshadow.domain;

import com.trungbui.projectshadow.data.model.EffectData;
import com.trungbui.projectshadow.data.model.HeroData;
import com.trungbui.projectshadow.effect.ActiveEffects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Hero implements Combatant {

    public static final int STRESS_MAX = 200;
    public static final int AFFLICTION_THRESHOLD = 100;

    private final HeroData data;
    private int level;
    private int currentHp;
    private int currentStress;
    private Position position;
    private final List<String> equippedSkills;
    private final Set<String> traits;
    private final Set<String> diseases;
    private final Map<String, Integer> skillCooldowns;
    private final ActiveEffects activeEffects;

    public Hero(HeroData data, Position position) {
        this(data, 0, position, new ArrayList<>(data.defaultSkills()), Collections.emptyMap());
    }

    public Hero(HeroData data, int level, Position position, List<String> equippedSkills) {
        this(data, level, position, equippedSkills, Collections.emptyMap());
    }

    public Hero(HeroData data, Position position, Map<String, EffectData> effectCatalog) {
        this(data, 0, position, new ArrayList<>(data.defaultSkills()), effectCatalog);
    }

    public Hero(HeroData data, int level, Position position, List<String> equippedSkills, Map<String, EffectData> effectCatalog) {
        if (data == null) throw new IllegalArgumentException("data must not be null");
        if (position == null) throw new IllegalArgumentException("position must not be null");
        if (level < 0) throw new IllegalArgumentException("level must be >= 0, got " + level);
        if (equippedSkills.size() > 4) {
            throw new IllegalArgumentException("hero may equip up to 4 skills, got " + equippedSkills.size());
        }
        this.data = data;
        this.level = level;
        this.position = position;
        this.equippedSkills = new ArrayList<>(equippedSkills);
        this.traits = new HashSet<>();
        this.diseases = new HashSet<>();
        this.skillCooldowns = new HashMap<>();
        this.activeEffects = new ActiveEffects(effectCatalog);
        this.currentHp = maxHp();
        this.currentStress = 0;
    }

    @Override
    public ActiveEffects activeEffects() {
        return activeEffects;
    }

    @Override
    public String id() {
        return data.heroId();
    }

    @Override
    public int currentHp() {
        return currentHp;
    }

    @Override
    public int maxHp() {
        return data.baseHp() + level * data.levelUpHp();
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
        return data.speed();
    }

    @Override
    public int accuracy() {
        return data.baseAccuracy();
    }

    @Override
    public int dodge() {
        return 0;
    }

    @Override
    public double critChance() {
        return data.baseCrit() + level * data.levelUpCrit();
    }

    @Override
    public int dmgMin() {
        return data.baseDmgMin() + level * data.levelUpDmg();
    }

    @Override
    public int dmgMax() {
        return data.baseDmgMax() + level * data.levelUpDmg();
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

    public int currentStress() {
        return currentStress;
    }

    public int maxStress() {
        return STRESS_MAX;
    }

    public double stressResist() {
        return data.baseStressResist() + level * data.levelUpStressResist();
    }

    public boolean isAfflicted() {
        return currentStress >= AFFLICTION_THRESHOLD;
    }

    public void takeStressDamage(int amount) {
        if (amount <= 0) return;
        int reduced = (int) Math.round(amount * (1.0 - stressResist()));
        currentStress = Math.min(STRESS_MAX, currentStress + Math.max(0, reduced));
    }

    public void reduceStress(int amount) {
        if (amount <= 0) return;
        currentStress = Math.max(0, currentStress - amount);
    }

    public List<String> equippedSkills() {
        return List.copyOf(equippedSkills);
    }

    public Map<String, Integer> skillCooldownsMap() {
        return Map.copyOf(skillCooldowns);
    }

    public void setCurrentStress(int stress) {
        this.currentStress = Math.max(0, Math.min(STRESS_MAX, stress));
    }

    public Set<String> traits() {
        return Set.copyOf(traits);
    }

    public void addTrait(String traitId) {
        traits.add(traitId);
    }

    public void removeTrait(String traitId) {
        traits.remove(traitId);
    }

    public Set<String> diseases() {
        return Set.copyOf(diseases);
    }

    public void addDisease(String diseaseId) {
        diseases.add(diseaseId);
    }

    public void removeDisease(String diseaseId) {
        diseases.remove(diseaseId);
    }

    public HeroData data() {
        return data;
    }

    public int level() {
        return level;
    }

    public void setLevel(int level) {
        if (level < 0) throw new IllegalArgumentException("level must be >= 0");
        int oldMax = maxHp();
        this.level = level;
        int newMax = maxHp();
        if (currentHp > newMax) currentHp = newMax;
        else if (newMax > oldMax) currentHp += (newMax - oldMax);
    }
}
