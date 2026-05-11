package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.EnemyData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.stage.BossNode;
import com.trungbui.projectshadow.stage.BossReward;
import com.trungbui.projectshadow.stage.CombatNode;
import com.trungbui.projectshadow.stage.DropEntry;
import com.trungbui.projectshadow.stage.EliteNode;
import com.trungbui.projectshadow.stage.MinibossNode;
import com.trungbui.projectshadow.stage.StageNode;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Sprint 9 — combat reward system.
 *
 * <p>Rolls rewards after a combat victory:</p>
 * <ul>
 *   <li><strong>Gold</strong>: scaled by enemy count and variant
 *       (Base 5–10g, Tank/Special/Assassin ×1.2, Elite 15–25g/enemy).
 *       Boss nodes use the pre-rolled {@link BossReward#gold()} from stage JSON.</li>
 *   <li><strong>Item drops</strong>: each enemy rolls its
 *       {@link EnemyData#dropItem()} at {@link EnemyData#dropChance()}.
 *       Pseudo-IDs in {@code KNOWN_TODO_REFS} (e.g. {@code item_gold}) convert
 *       to bonus gold instead of an inventory item.</li>
 *   <li><strong>Stress relief</strong>: -3 stress for one random alive hero
 *       (positive feedback loop, per 2026-05-11 design lock).</li>
 * </ul>
 *
 * <p>For boss nodes, also drops the items pre-rolled in {@link BossReward#drops()}.</p>
 */
public final class CombatRewardRoller {

    /** Bonus gold value when an enemy "drops" the pseudo-id {@code item_gold}. */
    private static final int ITEM_GOLD_VALUE = 20;

    /** Per-victory stress relief amount (positive feedback design lock). */
    private static final int VICTORY_STRESS_RELIEF = 3;

    private CombatRewardRoller() {
    }

    public static CombatReward roll(
            StageNode node,
            List<String> defeatedEnemyIds,
            List<Hero> aliveHeroes,
            GameData gd,
            RandomGenerator rng
    ) {
        int gold = 0;
        List<String> items = new ArrayList<>();

        // Per-enemy rewards (gold scaling + drop_item roll)
        if (node instanceof CombatNode || node instanceof EliteNode || node instanceof MinibossNode) {
            boolean elite = node instanceof EliteNode;
            for (String enemyId : defeatedEnemyIds) {
                EnemyData ed = gd.enemies().get(enemyId);
                if (ed == null) continue;
                gold += rollEnemyGold(ed, elite, rng);

                if (ed.dropItem() != null && rng.nextDouble() < ed.dropChance()) {
                    String drop = ed.dropItem();
                    if ("item_gold".equals(drop)) {
                        gold += ITEM_GOLD_VALUE;
                    } else if (gd.items().containsKey(drop)) {
                        items.add(drop);
                    }
                }
            }
            // Elite bonus gold from JSON
            if (node instanceof EliteNode en) {
                gold += en.goldBonus();
            }
        }

        // Boss flat gold + pre-rolled drops
        if (node instanceof BossNode boss) {
            BossReward br = boss.reward();
            gold += br.gold();
            for (DropEntry d : br.drops()) {
                resolveDropToItem(d, items);
            }
        }

        // Stress relief: pick a random alive hero
        String stressHeroId = null;
        if (!aliveHeroes.isEmpty()) {
            Hero target = aliveHeroes.get(rng.nextInt(aliveHeroes.size()));
            stressHeroId = target.id();
        }

        return new CombatReward(gold, List.copyOf(items),
                stressHeroId, stressHeroId == null ? 0 : VICTORY_STRESS_RELIEF);
    }

    /** Base 5–10g/enemy, Tank/Special/Assassin ×1.2, Elite 15–25g/enemy. */
    private static int rollEnemyGold(EnemyData ed, boolean elite, RandomGenerator rng) {
        if (elite) {
            return 15 + rng.nextInt(11); // 15..25
        }
        int base = 5 + rng.nextInt(6); // 5..10
        String variant = ed.variantType() == null ? "" : ed.variantType();
        if ("Tank".equals(variant) || "Special".equals(variant) || "Assassin".equals(variant)) {
            return (int) Math.round(base * 1.2);
        }
        return base;
    }

    /** Convert a {@link DropEntry} (from boss drop_table) to an inventory item ID. */
    private static void resolveDropToItem(DropEntry d, List<String> items) {
        if (d == null || d.type() == null) return;
        switch (d.type()) {
            case "item" -> {
                if (d.itemId() != null) items.add(d.itemId());
            }
            case "item_random" -> {
                // Category-based random — for MVP we just add a placeholder marker so the
                // popup can show "?" until item-by-category lookup is wired (TODO Sprint 10).
                if (d.category() != null) items.add(d.category());
            }
            case "gold" -> {
                // Gold drops in boss drop_table are already counted via BossReward.gold().
                // Nothing to do here.
            }
            default -> {
                // Unknown drop type — silently ignore for forward compat.
            }
        }
    }
}
