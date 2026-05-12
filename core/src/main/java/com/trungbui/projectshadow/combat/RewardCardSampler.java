package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.model.ItemData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * Sprint 13 B2 — rarity-weighted item sampler for the reward-card picker.
 *
 * <p>Produces a list of 3 distinct items drawn from the supplied pool according
 * to rarity weights: Common 60%, Rare 30%, Legendary 10%. If a rarity bucket
 * runs out of unique items the sampler falls back to the next available rarity
 * (lower first) to guarantee 3 results whenever the pool has ≥ 3 items.</p>
 *
 * <p>Consumables are excluded from the pool by default — the reward card picker
 * is for trinket / relic_buff / cursed_item only.</p>
 */
public final class RewardCardSampler {

    /** Rarity weights used for sampling: Common 60%, Rare 30%, Legendary 10%. */
    static final double WEIGHT_COMMON = 0.60;
    static final double WEIGHT_RARE = 0.30;
    static final double WEIGHT_LEGENDARY = 0.10;

    /** Categories excluded from the reward-card pool (consumables are single-use
     *  items that belong in per-combat drops, not strategic reward picks). */
    static final String CATEGORY_CONSUMABLE = "consumable";

    private RewardCardSampler() {
    }

    /**
     * Sample up to 3 distinct items from {@code pool} using rarity weights.
     *
     * @param pool all available {@link ItemData} (typically from GameData.items().values())
     * @param rng  seeded random — use {@code stageSeed ^ nodeLabel.hashCode()} pattern
     * @return list of up to 3 distinct items (may be < 3 if pool is too small)
     */
    public static List<ItemData> sample3(Collection<ItemData> pool, RandomGenerator rng) {
        // Partition into rarity buckets, excluding consumables.
        List<ItemData> common = new ArrayList<>();
        List<ItemData> rare = new ArrayList<>();
        List<ItemData> legendary = new ArrayList<>();
        for (ItemData item : pool) {
            if (CATEGORY_CONSUMABLE.equalsIgnoreCase(item.category())) continue;
            switch (item.rarity() == null ? "" : item.rarity().toLowerCase()) {
                case "common" -> common.add(item);
                case "rare" -> rare.add(item);
                case "legendary" -> legendary.add(item);
                // unknown rarity: treat as common
                default -> common.add(item);
            }
        }

        List<ItemData> result = new ArrayList<>(3);
        // We sample 3 times, each time using the rarity wheel; already-picked items
        // are removed from their bucket so we get distinct results.
        List<ItemData> commonRemaining = new ArrayList<>(common);
        List<ItemData> rareRemaining = new ArrayList<>(rare);
        List<ItemData> legendaryRemaining = new ArrayList<>(legendary);

        for (int i = 0; i < 3; i++) {
            ItemData pick = pickOne(commonRemaining, rareRemaining, legendaryRemaining, rng);
            if (pick == null) break;
            result.add(pick);
            // remove from whichever bucket contained it
            commonRemaining.remove(pick);
            rareRemaining.remove(pick);
            legendaryRemaining.remove(pick);
        }
        return List.copyOf(result);
    }

    /**
     * Pick one item using the rarity wheel, falling back to any non-empty bucket
     * if the rolled bucket is empty.
     */
    private static ItemData pickOne(List<ItemData> common, List<ItemData> rare,
                                    List<ItemData> legendary, RandomGenerator rng) {
        // Build a combined list weighted by rarity for a single roll.
        // Rather than duplicating entries, we roll a double and map to bucket.
        double allEmpty = (common.isEmpty() ? 0 : WEIGHT_COMMON)
                + (rare.isEmpty() ? 0 : WEIGHT_RARE)
                + (legendary.isEmpty() ? 0 : WEIGHT_LEGENDARY);
        if (allEmpty == 0) return null;

        double roll = rng.nextDouble() * allEmpty;
        double cursor = 0;
        if (!common.isEmpty()) {
            cursor += WEIGHT_COMMON;
            if (roll < cursor) return pickRandom(common, rng);
        }
        if (!rare.isEmpty()) {
            cursor += WEIGHT_RARE;
            if (roll < cursor) return pickRandom(rare, rng);
        }
        if (!legendary.isEmpty()) {
            return pickRandom(legendary, rng);
        }
        return null;
    }

    private static ItemData pickRandom(List<ItemData> bucket, RandomGenerator rng) {
        return bucket.get(rng.nextInt(bucket.size()));
    }
}
