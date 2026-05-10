package com.trungbui.projectshadow.effect;

import com.trungbui.projectshadow.data.model.EffectData;
import com.trungbui.projectshadow.domain.Combatant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

public class ActiveEffects {

    private final Map<String, EffectData> catalog;
    private final List<EffectInstance> instances = new ArrayList<>();

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
        return apply(effectId, source, target, rng, null);
    }

    public EffectInstance apply(
            String effectId,
            Combatant source,
            Combatant target,
            RandomGenerator rng,
            String skillMagnitudeOverride
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
            return ei;
        }

        EffectInstance fresh = new EffectInstance(
                effectId,
                source != null ? source.id() : null,
                duration,
                1
        );
        instances.add(fresh);
        applyImmediate(fresh, data, target, rng, skillMagnitudeOverride);
        return fresh;
    }

    public void onTurnStart(Combatant self, RandomGenerator rng) {
        Iterator<EffectInstance> it = instances.iterator();
        while (it.hasNext()) {
            EffectInstance ei = it.next();
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
        }
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
