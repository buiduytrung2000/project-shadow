package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.ItemData;
import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.run.RunSession;

/**
 * Sprint 12 B3 — apply a consumable item's effect in combat.
 *
 * <p>Phase 1 scope (Sprint 12): {@code eff_heal} and {@code eff_stress_reduce}
 * only. Other consumable effects (eff_burn, eff_dmg_buff, eff_absorb, etc.)
 * log "not supported" and still consume the action turn — they'll wire up in
 * Sprint 13 when ActiveEffects buff application via items lands. Trinket and
 * relic items remain passive.</p>
 */
public final class ItemUseHandler {

    /** Distinguishes successful application from unsupported / no-op. The caller
     *  (combat controller) advances the turn regardless — items always cost an action. */
    public record AppliedSummary(
            String itemId,
            boolean applied,
            int healedAmount,
            int totalStressReduced,
            String notes
    ) {}

    private ItemUseHandler() {}

    /**
     * Apply {@code itemId}'s primary effect to {@code target} within the run.
     * The item is NOT removed from inventory here — the caller (CombatController)
     * removes after applying so the unit test can assert pure logic in isolation.
     */
    public static AppliedSummary apply(String itemId, Combatant user, Combatant target,
                                       RunSession session, GameData gd) {
        ItemData data = gd.items().get(itemId);
        if (data == null) {
            return new AppliedSummary(itemId, false, 0, 0,
                    "Unknown item: " + itemId);
        }
        if (!"consumable".equalsIgnoreCase(data.category())) {
            return new AppliedSummary(itemId, false, 0, 0,
                    "Item is not consumable (category=" + data.category() + ")");
        }
        String eff = data.effectId() == null ? "" : data.effectId();
        return switch (eff) {
            case "eff_heal" -> applyHeal(data, target);
            case "eff_stress_reduce" -> applyStressReduce(data, session);
            default -> new AppliedSummary(itemId, false, 0, 0,
                    "Effect " + eff + " not yet supported (Sprint 13+)");
        };
    }

    private static AppliedSummary applyHeal(ItemData data, Combatant target) {
        int amount = parseLeadingInt(data.effectValue());
        if (amount <= 0 || target == null || !target.isAlive()) {
            return new AppliedSummary(data.itemId(), false, 0, 0,
                    "Heal failed: invalid target or amount=" + amount);
        }
        int hpBefore = target.currentHp();
        target.heal(amount);
        int healed = target.currentHp() - hpBefore;
        return new AppliedSummary(data.itemId(), true, healed, 0,
                "Healed " + healed + " HP on " + target.id());
    }

    private static AppliedSummary applyStressReduce(ItemData data, RunSession session) {
        // effectValue example: "-20 stress all allies". We take the leading
        // integer magnitude regardless of sign.
        int magnitude = Math.abs(parseLeadingInt(data.effectValue()));
        if (magnitude <= 0 || session == null) {
            return new AppliedSummary(data.itemId(), false, 0, 0,
                    "Stress reduce failed: invalid magnitude or session");
        }
        int totalReduced = 0;
        for (Hero h : session.party()) {
            if (!h.isAlive()) continue;
            int before = h.currentStress();
            h.reduceStress(magnitude);
            totalReduced += (before - h.currentStress());
        }
        return new AppliedSummary(data.itemId(), true, 0, totalReduced,
                "Stress -" + magnitude + " party-wide; total reduced=" + totalReduced);
    }

    /**
     * Parse the leading integer from a free-form effect value like {@code "15 HP"},
     * {@code "-20 stress all allies"}, {@code "+50% dmg next attack"}. Returns 0
     * if no leading number is found.
     */
    static int parseLeadingInt(String s) {
        if (s == null) return 0;
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return 0;
        int i = 0;
        int sign = 1;
        if (trimmed.charAt(0) == '+') { i = 1; }
        else if (trimmed.charAt(0) == '-') { sign = -1; i = 1; }
        int start = i;
        while (i < trimmed.length() && Character.isDigit(trimmed.charAt(i))) i++;
        if (i == start) return 0;
        try {
            return sign * Integer.parseInt(trimmed.substring(start, i));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
