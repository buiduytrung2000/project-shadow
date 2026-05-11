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

    /** Stress hard-cap. Crossing this triggers an instant heart-attack death
     *  (Sprint 9+ B2 stress lock). */
    public static final int STRESS_MAX = 200;
    /** First threshold — crossing this for the first time triggers the
     *  Affliction/Virtue 70/30 roll via {@code AfflictionResolver}. */
    public static final int AFFLICTION_THRESHOLD = 100;
    /** Heart-attack threshold — crossing this kills the hero instantly. */
    public static final int HEART_ATTACK_THRESHOLD = STRESS_MAX;

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
    /** Sprint 9+ B2 stress system: latched to true the first time stress crosses
     *  {@link #AFFLICTION_THRESHOLD} until {@link #consumePendingAfflictionRoll()}
     *  reads it. CombatController polls this after a stress-damage hit and rolls
     *  an Affliction/Virtue trait via {@code AfflictionResolver}. */
    private boolean pendingAfflictionRoll = false;
    /** Sprint 9+ B2: latched once the hero has resolved their Affliction/Virtue
     *  roll for this run — prevents re-rolling if stress drops below 100 and rises again. */
    private boolean afflictionResolved = false;
    /** Sprint 9+ B2: latched once stress hits {@link #HEART_ATTACK_THRESHOLD}.
     *  HP is forced to 0 and {@link #isAlive()} flips to false. */
    private boolean heartAttacked = false;
    /** Sprint 11 B2 — Cowardly trait sets this true when stress > 50 and 20% RNG
     *  check passes. Consumed at next {@code executePlayerSkill} call → hero
     *  loses turn. Cleared after consumption. */
    private boolean skipNextAction = false;
    /** Sprint 11 B2 — Bloodthirsty (Affliction) trait stacks +5% dmg per kill,
     *  max 5. Reset to 0 at combat start (CombatEncounter constructor or
     *  start round 1). Applied to effective damage via getter for ConditionResolver
     *  to read. */
    private int bloodthirstyStacks = 0;
    /** Sprint 12 B2 — accumulated XP for Guild level-up. Increments from
     *  combat kills (per-killer + party share) and end-of-stage bonuses.
     *  Consumed (decremented) when player triggers Guild level-up.
     *  Persists across runs via {@link com.trungbui.projectshadow.save.HeroState}. */
    private int currentXp = 0;

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
        int base = data.baseHp() + level * data.levelUpHp();
        // Sprint 12 B1 — Fever (-15%) / Plague (-25%) max HP debuffs (multiplicative).
        // Floor at 1 to keep hero alive at all.
        double mult = com.trungbui.projectshadow.combat.ConditionResolver.maxHpMultiplier(this);
        return Math.max(1, (int) Math.round(base * mult));
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

    /** Sprint 12 B1 — apply Hopeless (-10) + Blindness (-20) on top of the
     *  default effective-accuracy (which factors active flat effects).
     *  Floored at 0. */
    @Override
    public int effectiveAccuracy() {
        int base = Combatant.super.effectiveAccuracy();
        int debuff = com.trungbui.projectshadow.combat.ConditionResolver.accuracyDebuff(this);
        return Math.max(0, base - debuff);
    }

    /** Sprint 11 B3 — was hardcoded 0. Now reads HeroData.baseDodge +
     *  level × levelUpDodge. Cursed disease applies -30 on top via
     *  ConditionResolver hook in {@link #effectiveDodge()}. */
    @Override
    public int dodge() {
        double levelGain = level * data.levelUpDodge();
        return Math.max(0, data.baseDodge() + (int) Math.round(levelGain));
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

    /** Sprint 11 B2 — overrides {@link Combatant#effectiveDmgMin()} to apply
     *  the Bloodthirsty (+5% per kill stack) bonus on top of stat effects. */
    @Override
    public int effectiveDmgMin() {
        return applyBloodthirsty(Combatant.super.effectiveDmgMin());
    }

    @Override
    public int effectiveDmgMax() {
        return applyBloodthirsty(Combatant.super.effectiveDmgMax());
    }

    private int applyBloodthirsty(int base) {
        if (bloodthirstyStacks <= 0) return base;
        // 5% per stack, max 5 stacks → max +25% dmg.
        double mult = 1d + 0.05d * bloodthirstyStacks;
        return (int) Math.round(base * mult);
    }

    /** Sprint 12 B1 — Selfish trait (trait_a02): refuses healing. heal() is
     *  a no-op when the hero has the trait. Applied at the heal site so all
     *  healing sources (skills, items, rest options, effects) are blocked
     *  in one place. */
    @Override
    public void heal(int amount) {
        if (traits.contains(com.trungbui.projectshadow.combat.ConditionResolver.TRAIT_SELFISH)) return;
        Combatant.super.heal(amount);
    }

    /** Sprint 11 B3 — Cursed disease (dis_06): -30 dodge. Floor at 0
     *  (can't have negative dodge). Applied on top of {@link Combatant#effectiveDodge()}
     *  (which already factors active stat effects). */
    @Override
    public int effectiveDodge() {
        int base = Combatant.super.effectiveDodge();
        if (diseases.contains("dis_06")) {
            base = Math.max(0, base - 30);
        }
        return base;
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

    /**
     * Apply stress damage. Sprint 9+ B2 stress system:
     * <ul>
     *   <li>Damage is reduced by {@link #stressResist()} then clamped to
     *       {@code [0, STRESS_MAX]}.</li>
     *   <li>First time the hero crosses {@link #AFFLICTION_THRESHOLD} (and hasn't
     *       resolved yet), {@code pendingAfflictionRoll} is latched true so
     *       {@code CombatController} can roll Affliction/Virtue via
     *       {@code AfflictionResolver}.</li>
     *   <li>If stress reaches {@link #HEART_ATTACK_THRESHOLD}, HP is set to 0
     *       (instant death) and {@code heartAttacked} is latched.</li>
     * </ul>
     */
    public void takeStressDamage(int amount) {
        if (amount <= 0) return;
        int reduced = (int) Math.round(amount * (1.0 - stressResist()));
        boolean wasUnderThreshold = currentStress < AFFLICTION_THRESHOLD;
        currentStress = Math.min(STRESS_MAX, currentStress + Math.max(0, reduced));

        if (wasUnderThreshold && currentStress >= AFFLICTION_THRESHOLD && !afflictionResolved) {
            pendingAfflictionRoll = true;
        }
        if (currentStress >= HEART_ATTACK_THRESHOLD && !heartAttacked) {
            heartAttacked = true;
            setCurrentHp(0);
        }
    }

    /**
     * Returns true exactly once per stress-cross-100 event, then resets.
     * Used by {@code CombatController} to know when to invoke
     * {@code AfflictionResolver}. After consumption, {@link #afflictionResolved}
     * is also latched so the same hero won't re-roll in a later combat.
     */
    public boolean consumePendingAfflictionRoll() {
        if (!pendingAfflictionRoll) return false;
        pendingAfflictionRoll = false;
        afflictionResolved = true;
        return true;
    }

    /** True if a heart-attack death has been triggered (stress hit
     *  {@link #HEART_ATTACK_THRESHOLD}). HP is already 0 at this point. */
    public boolean isHeartAttacked() {
        return heartAttacked;
    }

    public boolean afflictionResolved() {
        return afflictionResolved;
    }

    /** Restore the affliction-resolved flag (used by save/load to rehydrate). */
    public void setAfflictionResolved(boolean resolved) {
        this.afflictionResolved = resolved;
    }

    /** Restore the heart-attacked flag (used by save/load to rehydrate). */
    public void setHeartAttacked(boolean attacked) {
        this.heartAttacked = attacked;
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

    // ───── Sprint 11 B2 — Cowardly skip-turn flag ─────

    /** Sprint 11 B2: latched true when Cowardly trait fires (20% chance, stress > 50).
     *  Caller (CombatController) should call {@link #consumeSkipNextAction()} to read
     *  + clear in one shot. */
    public boolean skipNextAction() {
        return skipNextAction;
    }

    /** Set the skip flag (used by ConditionResolver.onTurnStart). */
    public void setSkipNextAction(boolean v) {
        this.skipNextAction = v;
    }

    /** Read + clear in one operation. Returns true if the hero should lose this turn. */
    public boolean consumeSkipNextAction() {
        boolean was = skipNextAction;
        skipNextAction = false;
        return was;
    }

    // ───── Sprint 11 B2 — Bloodthirsty kill stacks ─────

    /** Sprint 11 B2: current Bloodthirsty stack count (0..5). Each kill in this
     *  combat adds 1. Caps at 5. Reset at combat start. Read by
     *  ConditionResolver / effective-dmg accessors to apply +5%/stack bonus. */
    public int bloodthirstyStacks() {
        return bloodthirstyStacks;
    }

    /** Sprint 11 B2: called by ConditionResolver.onKill when this hero has trait_07
     *  Bloodthirsty. Caps at 5. */
    public void addBloodthirstyStack() {
        if (bloodthirstyStacks < 5) bloodthirstyStacks++;
    }

    /** Sprint 11 B2: called at combat start to reset stacks (per-combat counter). */
    public void resetBloodthirstyStacks() {
        bloodthirstyStacks = 0;
    }

    // ───── Sprint 12 B2 — XP accumulator ─────

    public int currentXp() {
        return currentXp;
    }

    /** Adds XP from combat/end-of-stage. Negative deltas ignored (use
     *  {@link #consumeXp(int)} to subtract). */
    public void addXp(int amount) {
        if (amount > 0) currentXp += amount;
    }

    /** Consume {@code amount} XP for Guild level-up. Throws if insufficient. */
    public void consumeXp(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
        if (currentXp < amount) {
            throw new IllegalStateException(
                    "Insufficient XP: need " + amount + ", have " + currentXp);
        }
        currentXp -= amount;
    }

    /** Restore XP from save (used by HeroState.toHero). */
    public void setCurrentXp(int xp) {
        this.currentXp = Math.max(0, xp);
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
