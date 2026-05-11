package com.trungbui.projectshadow.meta;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.HeroData;
import com.trungbui.projectshadow.data.model.ItemData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import com.trungbui.projectshadow.run.RunSession;
import com.trungbui.projectshadow.save.HeroState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/**
 * Sprint 8 — pure logic for the 4 Hamlet buildings + run-end meta progression.
 *
 * <p>All operations are functional: {@code (oldState, params) → newState}. No I/O.
 * Throws {@link HamletException} when the player can't afford / state is invalid —
 * the caller (UI screen) is responsible for catching and reporting.</p>
 */
public final class HamletService {

    // ───── Tier 1 (default) hire costs by rarity ─────
    public static final int STAGECOACH_HIRE_COST = 50;          // legacy default (Common tier)
    public static final int STAGECOACH_HIRE_COST_COMMON = 50;
    public static final int STAGECOACH_HIRE_COST_RARE = 80;
    public static final int STAGECOACH_HIRE_COST_LEGENDARY = 150;
    public static final int STAGECOACH_OFFER_COUNT = 3;

    // ───── 3-tier building upgrade costs (2026-05-11 design lock) ─────
    // Stagecoach Lv1 (default) → Lv2 → Lv3
    public static final int STAGECOACH_UPGRADE_LV2_GOLD = 300;
    public static final int STAGECOACH_UPGRADE_LV2_HEIRLOOM = 1;
    public static final int STAGECOACH_UPGRADE_LV3_GOLD = 800;
    public static final int STAGECOACH_UPGRADE_LV3_HEIRLOOM = 3;
    public static final int STAGECOACH_OFFERS_BY_LEVEL[] = { 0, 3, 4, 5 }; // index = lvl
    public static final double STAGECOACH_LV3_HIRE_DISCOUNT = 0.20;       // -20% at Lv3

    // Guild
    public static final int GUILD_UPGRADE_LV2_GOLD = 250;
    public static final int GUILD_UPGRADE_LV2_HEIRLOOM = 1;
    public static final int GUILD_UPGRADE_LV3_GOLD = 700;
    public static final int GUILD_UPGRADE_LV3_HEIRLOOM = 3;
    public static final int GUILD_MAX_LEVEL_BY_TIER[] = { 0, 5, 6, 7 };
    public static final double GUILD_COST_DISCOUNT_BY_TIER[] = { 0.0, 0.0, 0.20, 0.40 };

    // Survivalist (prices adjusted per user 2026-05-11: Lv2=180g, Lv3=350g)
    public static final int SURVIVALIST_UPGRADE_LV2_GOLD = 180;
    public static final int SURVIVALIST_UPGRADE_LV2_HEIRLOOM = 1;
    public static final int SURVIVALIST_UPGRADE_LV3_GOLD = 350;
    public static final int SURVIVALIST_UPGRADE_LV3_HEIRLOOM = 2;

    // Caretaker — slot system: Lv1=1, Lv2=2, Lv3=4 cure slots per Hamlet visit
    public static final int CARETAKER_UPGRADE_LV2_GOLD = 200;
    public static final int CARETAKER_UPGRADE_LV2_HEIRLOOM = 1;
    public static final int CARETAKER_UPGRADE_LV3_GOLD = 500;
    public static final int CARETAKER_UPGRADE_LV3_HEIRLOOM = 2;
    public static final int CARETAKER_CURE_SLOTS_BY_LEVEL[] = { 0, 1, 2, 4 };
    public static final int CARETAKER_CURE_COST_BY_LEVEL[] = { 0, 30, 25, 20 };

    // ───── Supplies tax (Option C — gold persistence) ─────
    public static final int SUPPLIES_TAX_STAGE_1 = 100;
    public static final int SUPPLIES_TAX_STAGE_2 = 200;
    public static final int SUPPLIES_TAX_STAGE_3 = 400;

    /** Tax paid up-front when entering a stage; non-refundable on defeat. */
    public static int suppliesTax(int stageAct) {
        return switch (stageAct) {
            case 1 -> SUPPLIES_TAX_STAGE_1;
            case 2 -> SUPPLIES_TAX_STAGE_2;
            case 3 -> SUPPLIES_TAX_STAGE_3;
            default -> 0;
        };
    }

    // ───── Heirloom drops ─────
    public static final int HEIRLOOM_BOSS_STAGE_1 = 1;
    public static final int HEIRLOOM_BOSS_STAGE_2 = 2;
    public static final int HEIRLOOM_BOSS_STAGE_3 = 4;

    public static int heirloomFromBoss(int stageAct) {
        return switch (stageAct) {
            case 1 -> HEIRLOOM_BOSS_STAGE_1;
            case 2 -> HEIRLOOM_BOSS_STAGE_2;
            case 3 -> HEIRLOOM_BOSS_STAGE_3;
            default -> 0;
        };
    }

    /** Cost to hire a specific hero based on rarity tier (Common/Rare/Legendary). */
    public static int hireCost(String heroId, GameData gd) {
        HeroData data = gd.heroes().get(heroId);
        if (data == null) return STAGECOACH_HIRE_COST_COMMON;
        return switch (data.rarity()) {
            case "Legendary" -> STAGECOACH_HIRE_COST_LEGENDARY;
            case "Rare" -> STAGECOACH_HIRE_COST_RARE;
            default -> STAGECOACH_HIRE_COST_COMMON;
        };
    }
    public static final int GUILD_LEVEL_COST_BASE = 100;
    public static final int GUILD_MAX_LEVEL = 5;
    public static final int SURVIVALIST_CRAFT_COST = 80;
    public static final int CARETAKER_DISEASE_CURE_COST = 30;
    public static final int CARETAKER_STRESS_RELIEF_BLOCK = 10;
    public static final int CARETAKER_STRESS_RELIEF_COST = 30;

    private HamletService() {
    }

    // ---------- Stagecoach ----------

    /** Returns up to {@link #STAGECOACH_OFFER_COUNT} hero IDs not already in the roster. */
    public static List<String> rollStagecoachOffers(MetaState meta, GameData gd, RandomGenerator rng) {
        Set<String> taken = meta.roster().stream().map(HeroState::heroId).collect(Collectors.toSet());
        List<String> candidates = new ArrayList<>();
        for (String id : gd.heroes().keySet()) {
            if (!taken.contains(id)) candidates.add(id);
        }
        java.util.Collections.shuffle(candidates, new java.util.Random(rng.nextLong()));
        int take = Math.min(STAGECOACH_OFFER_COUNT, candidates.size());
        return List.copyOf(candidates.subList(0, take));
    }

    public static MetaState hireHero(MetaState meta, String heroId, GameData gd) {
        if (meta.hasInRoster(heroId)) {
            throw new HamletException("Hero đã có trong roster: " + heroId);
        }
        HeroData data = gd.heroes().get(heroId);
        if (data == null) throw new HamletException("Unknown heroId: " + heroId);
        int cost = hireCost(heroId, gd);
        if (meta.gold() < cost) {
            throw new HamletException("Không đủ gold (cần " + cost + " cho " + data.rarity() + ", có " + meta.gold() + ")");
        }
        Hero fresh = new Hero(data, Position.POS_1, gd.effects());
        List<HeroState> newRoster = new ArrayList<>(meta.roster());
        newRoster.add(HeroState.from(fresh));
        return meta.withGold(meta.gold() - cost).withRoster(newRoster);
    }

    // ---------- Guild ----------

    /** Cost to advance from {@code currentLevel} to {@code currentLevel + 1}. */
    public static int levelUpCost(int currentLevel) {
        return GUILD_LEVEL_COST_BASE * (currentLevel + 1);
    }

    public static MetaState levelUpHero(MetaState meta, String heroId, GameData gd) {
        HeroState rs = meta.heroInRoster(heroId)
                .orElseThrow(() -> new HamletException("Hero không có trong roster: " + heroId));
        if (rs.level() >= GUILD_MAX_LEVEL) {
            throw new HamletException("Hero đã đạt level tối đa (" + GUILD_MAX_LEVEL + ")");
        }
        int cost = levelUpCost(rs.level());
        if (meta.gold() < cost) {
            throw new HamletException("Không đủ gold (cần " + cost + ", có " + meta.gold() + ")");
        }
        Hero h = rs.toHero(gd);
        h.setLevel(rs.level() + 1);
        return meta
                .withGold(meta.gold() - cost)
                .withRoster(replaceInRoster(meta.roster(), HeroState.from(h)));
    }

    // ---------- Survivalist ----------

    /** Crafts a random trinket (item type {@code trinket}) and adds it to inventory. */
    public static MetaState craftRandomTrinket(MetaState meta, GameData gd, RandomGenerator rng) {
        if (meta.gold() < SURVIVALIST_CRAFT_COST) {
            throw new HamletException("Không đủ gold (cần " + SURVIVALIST_CRAFT_COST + ", có " + meta.gold() + ")");
        }
        List<String> trinkets = new ArrayList<>();
        for (ItemData it : gd.items().values()) {
            String cat = it.category();
            if (cat != null && cat.toLowerCase().contains("trinket")) {
                trinkets.add(it.itemId());
            }
        }
        if (trinkets.isEmpty()) {
            throw new HamletException("Không có trinket nào trong items.csv để craft");
        }
        String picked = trinkets.get(rng.nextInt(trinkets.size()));
        List<String> newInv = new ArrayList<>(meta.trinketInventory());
        newInv.add(picked);
        return meta.withGold(meta.gold() - SURVIVALIST_CRAFT_COST).withTrinketInventory(newInv);
    }

    // ---------- Caretaker ----------

    public static MetaState cureDisease(MetaState meta, String heroId, String diseaseId, GameData gd) {
        HeroState rs = meta.heroInRoster(heroId)
                .orElseThrow(() -> new HamletException("Hero không có trong roster: " + heroId));
        if (!rs.diseases().contains(diseaseId)) {
            throw new HamletException("Hero không mắc bệnh: " + diseaseId);
        }
        if (meta.gold() < CARETAKER_DISEASE_CURE_COST) {
            throw new HamletException("Không đủ gold (cần " + CARETAKER_DISEASE_CURE_COST + ", có " + meta.gold() + ")");
        }
        Hero h = rs.toHero(gd);
        h.removeDisease(diseaseId);
        return meta
                .withGold(meta.gold() - CARETAKER_DISEASE_CURE_COST)
                .withRoster(replaceInRoster(meta.roster(), HeroState.from(h)));
    }

    /** Reduces the hero's stress by {@link #CARETAKER_STRESS_RELIEF_BLOCK} for one fixed cost. */
    public static MetaState reduceStress(MetaState meta, String heroId, GameData gd) {
        HeroState rs = meta.heroInRoster(heroId)
                .orElseThrow(() -> new HamletException("Hero không có trong roster: " + heroId));
        if (rs.currentStress() <= 0) {
            throw new HamletException("Hero không có stress");
        }
        if (meta.gold() < CARETAKER_STRESS_RELIEF_COST) {
            throw new HamletException("Không đủ gold (cần " + CARETAKER_STRESS_RELIEF_COST + ", có " + meta.gold() + ")");
        }
        Hero h = rs.toHero(gd);
        h.reduceStress(CARETAKER_STRESS_RELIEF_BLOCK);
        return meta
                .withGold(meta.gold() - CARETAKER_STRESS_RELIEF_COST)
                .withRoster(replaceInRoster(meta.roster(), HeroState.from(h)));
    }

    // ---------- Run end ----------

    /**
     * Apply meta progression after a run ends.
     * <ul>
     *   <li>{@code victory=true}: surviving party heroes update their roster snapshot;
     *       dead party heroes are removed (permadeath); run gold is added to meta wallet.</li>
     *   <li>{@code victory=false}: all dead party heroes are removed; gold is forfeited.</li>
     * </ul>
     */
    public static MetaState applyRunOutcome(MetaState meta, RunSession run, boolean victory) {
        Set<String> partyIds = run.party().stream().map(Hero::id).collect(Collectors.toSet());
        List<HeroState> newRoster = new ArrayList<>();
        for (HeroState rs : meta.roster()) {
            if (partyIds.contains(rs.heroId())) {
                Optional<Hero> matching = run.party().stream()
                        .filter(h -> h.id().equals(rs.heroId()))
                        .findFirst();
                if (matching.isPresent() && matching.get().isAlive()) {
                    newRoster.add(HeroState.from(matching.get()));
                }
                // dead party hero → omit (permadeath)
            } else {
                newRoster.add(rs); // not in party — unchanged
            }
        }
        int newGold = victory ? meta.gold() + run.state().gold() : meta.gold();
        return meta.withGold(newGold).withRoster(newRoster);
    }

    // ---------- helpers ----------

    private static List<HeroState> replaceInRoster(List<HeroState> roster, HeroState updated) {
        List<HeroState> out = new ArrayList<>(roster.size());
        for (HeroState rs : roster) {
            out.add(rs.heroId().equals(updated.heroId()) ? updated : rs);
        }
        return out;
    }

    /** Thrown when a Hamlet operation is invalid (insufficient gold, missing hero, etc.). */
    public static final class HamletException extends RuntimeException {
        public HamletException(String msg) {
            super(msg);
        }
    }
}
