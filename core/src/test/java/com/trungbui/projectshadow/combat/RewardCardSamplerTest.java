package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.data.model.ItemData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 13 B2 — {@link RewardCardSampler} rarity-weighted sampling tests.
 */
class RewardCardSamplerTest {

    /** Build a minimal ItemData with the given id, category, rarity. */
    private static ItemData item(String id, String category, String rarity) {
        return new ItemData(id, "Name " + id, "Name " + id,
                category, null, true, null,
                null, null, null, null, null,
                rarity, 0, true, true, "Desc");
    }

    private static List<ItemData> buildMixedPool() {
        List<ItemData> pool = new ArrayList<>();
        // 6 common, 3 rare, 1 legendary
        for (int i = 0; i < 6; i++) pool.add(item("c" + i, "trinket", "Common"));
        for (int i = 0; i < 3; i++) pool.add(item("r" + i, "trinket", "Rare"));
        pool.add(item("l0", "relic_buff", "Legendary"));
        return pool;
    }

    @Test
    void sample3_returnsExactly3Items() {
        List<ItemData> pool = buildMixedPool();
        List<ItemData> result = RewardCardSampler.sample3(pool, new Random(42));
        assertThat(result).hasSize(3);
    }

    @Test
    void sample3_allItemsDistinct() {
        List<ItemData> pool = buildMixedPool();
        List<ItemData> result = RewardCardSampler.sample3(pool, new Random(77));
        long distinctIds = result.stream().map(ItemData::itemId).distinct().count();
        assertThat(distinctIds).isEqualTo(3);
    }

    @Test
    void sample3_excludesConsumables() {
        List<ItemData> pool = new ArrayList<>(buildMixedPool());
        // Add some consumables that should never appear in results.
        for (int i = 0; i < 5; i++) pool.add(item("cons" + i, "consumable", "Common"));

        for (long seed = 0; seed < 50; seed++) {
            List<ItemData> result = RewardCardSampler.sample3(pool, new Random(seed));
            result.forEach(it ->
                    assertThat(it.category()).as("consumables must not appear in reward cards")
                            .isNotEqualToIgnoringCase("consumable"));
        }
    }

    @Test
    void sample3_distributionRoughly60_30_10() {
        // Use a larger pool (12C/6R/2L) so no bucket is exhausted — a depleted
        // bucket shifts the effective weights and skews the aggregate.
        List<ItemData> pool = new ArrayList<>();
        for (int i = 0; i < 12; i++) pool.add(item("c" + i, "trinket", "Common"));
        for (int i = 0; i < 6; i++) pool.add(item("r" + i, "trinket", "Rare"));
        for (int i = 0; i < 2; i++) pool.add(item("l" + i, "relic_buff", "Legendary"));

        int totalRolls = 3000; // 1000 sample3 calls → 3000 picks
        Map<String, Integer> countByRarity = new HashMap<>();
        countByRarity.put("Common", 0);
        countByRarity.put("Rare", 0);
        countByRarity.put("Legendary", 0);

        // Use non-sequential seeds to avoid java.util.Random's sequential-seed clustering.
        Random seeder = new Random(0xCAFEBABEL);
        for (int i = 0; i < 1000; i++) {
            long seed = seeder.nextLong();
            List<ItemData> result = RewardCardSampler.sample3(pool, new Random(seed));
            for (ItemData it : result) {
                countByRarity.merge(it.rarity(), 1, Integer::sum);
            }
        }

        double commonPct = countByRarity.get("Common") * 100.0 / totalRolls;
        double rarePct = countByRarity.get("Rare") * 100.0 / totalRolls;
        double legendaryPct = countByRarity.get("Legendary") * 100.0 / totalRolls;

        // Allow ±8% tolerance to account for small pool / without-replacement effects.
        assertThat(commonPct).isBetween(52.0, 68.0);
        assertThat(rarePct).isBetween(22.0, 38.0);
        assertThat(legendaryPct).isBetween(2.0, 18.0);
    }

    @Test
    void sample3_smallPool_returnsFewer() {
        // Only 2 items in pool
        List<ItemData> pool = List.of(
                item("x1", "trinket", "Common"),
                item("x2", "trinket", "Rare")
        );
        List<ItemData> result = RewardCardSampler.sample3(pool, new Random(0));
        assertThat(result).hasSize(2);
    }

    @Test
    void sample3_emptyPool_returnsEmptyList() {
        List<ItemData> result = RewardCardSampler.sample3(List.of(), new Random(0));
        assertThat(result).isEmpty();
    }

    @Test
    void sample3_onlyConsumables_returnsEmpty() {
        List<ItemData> pool = List.of(
                item("p1", "consumable", "Common"),
                item("p2", "consumable", "Rare")
        );
        List<ItemData> result = RewardCardSampler.sample3(pool, new Random(0));
        assertThat(result).isEmpty();
    }

    @Test
    void sample3_deterministic_withSameSeed() {
        List<ItemData> pool = buildMixedPool();
        // Use a large seed to avoid java.util.Random sequential-seed clustering.
        long seed = 0xDEADC0DEL;
        List<ItemData> r1 = RewardCardSampler.sample3(pool, new Random(seed));
        List<ItemData> r2 = RewardCardSampler.sample3(pool, new Random(seed));
        assertThat(r1.stream().map(ItemData::itemId).toList())
                .isEqualTo(r2.stream().map(ItemData::itemId).toList());
    }
}
