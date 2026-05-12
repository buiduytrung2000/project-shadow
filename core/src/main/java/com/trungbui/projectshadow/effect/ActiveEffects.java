package com.trungbui.projectshadow.effect;

import com.trungbui.projectshadow.data.model.EffectData;
import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.Hero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

public class ActiveEffects {

    private final Map<String, EffectData> catalog;
    private final List<EffectInstance> instances = new ArrayList<>();
    /** Sprint 9+ B2: dedup set for the hybrid tick scheme. An effect that already
     *  ticked this round (via {@link #onTurnStart}) is skipped if {@code onTurnStart}
     *  is called again in the same round (e.g. AoE skills that loop per target).
     *  Cleared at end of each round by {@link #endRoundReset()}. */
    private final Set<String> tickedThisRound = new HashSet<>();

    public ActiveEffects() {
        this(Collections.emptyMap());
    }

    public ActiveEffects(Map<String, EffectData> catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public List<EffectInstance> instances() {
        return List.copyOf(instances);
    }

    public int size() {
        return instances.size();
    }

    public Optional<EffectInstance> find(String effectId) {
        for (EffectInstance ei : instances) {
            if (ei.effectId().equals(effectId)) return Optional.of(ei);
        }
        return Optional.empty();
    }

    public boolean has(String effectId) {
        return find(effectId).isPresent();
    }

    public EffectInstance apply(String effectId, Combatant source, Combatant target, RandomGenerator rng) {
        return apply(effectId, source, target, rng, null, EffectInstance.UNKNOWN_ROUND);
    }

    public EffectInstance apply(
            String effectId,
            Combatant source,
            Combatant target,
            RandomGenerator rng,
            String skillMagnitudeOverride
    ) {
        return apply(effectId, source, target, rng, skillMagnitudeOverride, EffectInstance.UNKNOWN_ROUND);
    }

    /**
     * Apply an effect. Sprint 9+ B2: takes {@code currentRound} so the instance can
     * remember when it was applied; {@link #onTurnStart} uses this to skip the first
     * tick on the same round (an effect just-applied this round shouldn't immediately
     * fire its DoT — it ticks starting next round).
     *
     * <p>On refresh (effect already present), the immediate damage/heal IS re-applied
     * (e.g. casting a heal a second time heals again). Stack count increments if the
     * effect can stack.</p>
     */
    public EffectInstance apply(
            String effectId,
            Combatant source,
            Combatant target,
            RandomGenerator rng,
            String skillMagnitudeOverride,
            int currentRound
    ) {
        EffectData data = lookup(effectId);
        int duration = parseDuration(data);

        Optional<EffectInstance> existing = find(effectId);
        if (existing.isPresent()) {
            EffectInstance ei = existing.get();
            if (data.canStack()) {
                int max = data.maxStacks() != null ? data.maxStacks() : 1;
                ei.addStack(max);
            }
            ei.refreshDuration(duration);
            // Sprint 9+ B2: re-apply immediate damage/heal on refresh so re-casting
            // a heal still heals. Previously only the first apply triggered immediate.
            applyImmediate(ei, data, target, rng, skillMagnitudeOverride);
            // Re-applying in same round → keep effective appliedRound at current to
            // prevent surprise ticks from a stale earlier value.
            return ei;
        }

        EffectInstance fresh = new EffectInstance(
                effectId,
                source != null ? source.id() : null,
                duration,
                1,
                currentRound
        );
        instances.add(fresh);
        applyImmediate(fresh, data, target, rng, skillMagnitudeOverride);
        return fresh;
    }

    public void onTurnStart(Combatant self, RandomGenerator rng) {
        onTurnStart(self, rng, EffectInstance.UNKNOWN_ROUND);
    }

    /**
     * Tick effects with {@code on_turn_start} trigger. Sprint 9+ B2 hybrid tick:
     * <ul>
     *   <li>Each effect ticks at most once per round (deduped via {@link #tickedThisRound}),
     *       even when {@code onTurnStart} is called multiple times in the same round
     *       (e.g. AoE skills looping over targets).</li>
     *   <li>An effect applied during the current round does NOT tick on its first round —
     *       it starts ticking next round (skip if {@code appliedRound == currentRound}).</li>
     *   <li>Duration decrements once per round regardless of how many times {@code onTurnStart}
     *       is called, by the same dedup logic.</li>
     * </ul>
     * {@code currentRound == UNKNOWN_ROUND} disables the just-applied skip (legacy callers).
     */
    public void onTurnStart(Combatant self, RandomGenerator rng, int currentRound) {
        boolean dedup = currentRound != EffectInstance.UNKNOWN_ROUND;
        Iterator<EffectInstance> it = instances.iterator();
        while (it.hasNext()) {
            EffectInstance ei = it.next();
            if (dedup && tickedThisRound.contains(ei.effectId())) {
                // Already processed this round (e.g. AoE multi-target call). Skip silently.
                continue;
            }
            // Just-applied skip: effect applied THIS round → don't tick yet, but still mark
            // as "processed" so duration decrement also waits until next round.
            if (dedup && ei.appliedRound() == currentRound) {
                tickedThisRound.add(ei.effectId());
                continue;
            }
            EffectData data = lookup(ei.effectId());
            if ("on_turn_start".equals(data.trigger())) {
                tickEffect(ei, data, self, rng);
            }
            if (!ei.isPermanent()) {
                ei.tickDuration();
                if (ei.isExpired()) {
                    it.remove();
                }
            }
            if (dedup) tickedThisRound.add(ei.effectId());
        }
    }

    /** Sprint 9+ B2: clear the per-round dedup set. {@link com.trungbui.projectshadow.combat.CombatController}
     *  invokes this on every combatant at end-of-round so the next round's first
     *  per-actor tick is allowed to fire. */
    public void endRoundReset() {
        tickedThisRound.clear();
    }

    public boolean removeFirst(String effectId) {
        Iterator<EffectInstance> it = instances.iterator();
        while (it.hasNext()) {
            if (it.next().effectId().equals(effectId)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public void clear() {
        instances.clear();
    }

    public int sumFlatModifier(StatType stat) {
        int total = 0;
        for (EffectInstance ei : instances) {
            EffectData data = lookup(ei.effectId());
            if (StatType.from(data.statAffected()) != stat) continue;
            if (!"flat".equals(data.modifierType())) continue;
            total += parseFlatValue(data.modifierValue(), null) * ei.stacks();
        }
        return total;
    }

    public double sumPercentModifier(StatType stat) {
        double total = 0d;
        for (EffectInstance ei : instances) {
            EffectData data = lookup(ei.effectId());
            if (StatType.from(data.statAffected()) != stat) continue;
            if (!"percent".equals(data.modifierType())) continue;
            total += parsePercentValue(data.modifierValue()) * ei.stacks();
        }
        return total;
    }

    public boolean isStunned() {
        return has("eff_stun") || has("eff_melancholy");
    }

    public boolean isTaunting() {
        return has("eff_taunt");
    }

    private void applyImmediate(
            EffectInstance ei,
            EffectData data,
            Combatant target,
            RandomGenerator rng,
            String skillMagnitudeOverride
    ) {
        if (target == null) return;

        // Fix I (post-Sprint-13 pack #2) — eff_stress_reset: set stress to 0 and
        // heal 20% max HP. Uses setCurrentHp directly so Selfish trait (which blocks
        // external heal()) does not interfere with this self-recovery skill.
        if ("eff_stress_reset".equals(ei.effectId()) && target instanceof Hero h) {
            h.setCurrentStress(0);
            int healAmount = Math.round(h.maxHp() * 0.20f);
            h.setCurrentHp(Math.min(h.maxHp(), h.currentHp() + healAmount));
            return;
        }

        String cat = data.category();
        String value = resolveValue(data.modifierValue(), skillMagnitudeOverride);

        if ("heal".equals(cat) || ("instant".equals(data.durationType()) && "hp".equals(data.statAffected()))) {
            int amount = Math.abs(parseFlatValue(value, rng));
            if (amount > 0) target.heal(amount);
        } else if ("damage".equals(cat)) {
            int amount = Math.abs(parseFlatValue(value, rng));
            if (amount > 0) target.takeHpDamage(amount);
        }
    }

    private static String resolveValue(String effectModifierValue, String skillMagnitudeOverride) {
        boolean placeholder = effectModifierValue == null
                || effectModifierValue.isBlank()
                || "+".equals(effectModifierValue.trim())
                || "-".equals(effectModifierValue.trim());
        if (placeholder && skillMagnitudeOverride != null && !skillMagnitudeOverride.isBlank()) {
            return skillMagnitudeOverride;
        }
        return effectModifierValue;
    }

    private void tickEffect(EffectInstance ei, EffectData data, Combatant self, RandomGenerator rng) {
        if (!"flat_per_turn".equals(data.modifierType())) return;
        if (!"hp".equals(data.statAffected())) return;
        int perStack = parseFlatValue(data.modifierValue(), rng);
        int amount = perStack * ei.stacks();
        if ("dot".equals(data.category())) {
            self.takeHpDamage(-amount);
        } else if ("hot".equals(data.category())) {
            self.heal(amount);
        }
    }

    private EffectData lookup(String effectId) {
        EffectData data = catalog.get(effectId);
        if (data == null) {
            throw new IllegalArgumentException("Unknown effectId: " + effectId);
        }
        return data;
    }

    private static int parseDuration(EffectData data) {
        String type = data.durationType();
        if ("permanent".equals(type)) return EffectInstance.PERMANENT;
        if ("instant".equals(type)) return 0;
        String d = data.defaultDuration();
        if (d == null || d.isBlank()) return 1;
        try {
            return Integer.parseInt(d.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    static int parseFlatValue(String raw, RandomGenerator rng) {
        if (raw == null || raw.isBlank()) return 0;
        String s = raw.trim().replace("%", "");
        if (s.contains(" to ")) {
            String[] parts = s.split(" to ");
            try {
                int a = Integer.parseInt(stripPlus(parts[0].trim()));
                int b = Integer.parseInt(stripPlus(parts[1].trim()));
                int min = Math.min(a, b);
                int max = Math.max(a, b);
                if (rng != null) {
                    return min + rng.nextInt(max - min + 1);
                }
                return (a + b) / 2;
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        try {
            return Integer.parseInt(stripPlus(s));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static double parsePercentValue(String raw) {
        if (raw == null || raw.isBlank()) return 0d;
        String s = raw.trim().replace("%", "");
        if (s.contains(" to ")) {
            String[] parts = s.split(" to ");
            try {
                double a = Double.parseDouble(stripPlus(parts[0].trim()));
                double b = Double.parseDouble(stripPlus(parts[1].trim()));
                return ((a + b) / 2d) / 100d;
            } catch (NumberFormatException e) {
                return 0d;
            }
        }
        try {
            return Double.parseDouble(stripPlus(s)) / 100d;
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    private static String stripPlus(String s) {
        return s.startsWith("+") ? s.substring(1) : s;
    }
}
