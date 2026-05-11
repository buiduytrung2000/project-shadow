package com.trungbui.projectshadow.meta;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.HeroData;
import com.trungbui.projectshadow.data.model.ItemData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import com.trungbui.projectshadow.i18n.I18n;
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
    /** Sprint 10 B1 — cost per "Refresh" click on Stagecoach. Closes the save-scum
     *  loophole where players could re-roll offers free until a Legendary appeared. */
    public static final int STAGECOACH_REFRESH_COST = 50;

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

    /**
     * Sprint 10 B2 — deduct {@link #suppliesTax(int)} from meta gold at embark time.
     * Throws {@link HamletException} via i18n if insufficient gold. Non-refundable.
     * Called by {@code ProjectShadowGame.startNewRun}.
     */
    public static MetaState paySuppliesTax(MetaState meta, int stageAct) {
        int tax = suppliesTax(stageAct);
        if (tax <= 0) return meta; // stage 0 or unknown → free
        // Sprint 11 B1 debt model: gold can go negative — player can embark even
        // when broke; supplies become debt repaid by combat reward.
        return meta.withGold(meta.gold() - tax);
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

    // ───── Sprint 10 B2 — Building upgrades ─────

    /**
     * Sprint 10 B2 — upgrade Stagecoach by one tier.
     * Lv1 → Lv2 (300g + 1 heirloom). Lv2 → Lv3 (800g + 3 heirloom).
     * Throws {@link HamletException} if already maxed or insufficient resources.
     */
    public static MetaState upgradeStagecoach(MetaState meta) {
        return upgradeBuilding(meta, MetaState.B_STAGECOACH,
                STAGECOACH_UPGRADE_LV2_GOLD, STAGECOACH_UPGRADE_LV2_HEIRLOOM,
                STAGECOACH_UPGRADE_LV3_GOLD, STAGECOACH_UPGRADE_LV3_HEIRLOOM);
    }

    /** Sprint 10 B2 — upgrade Guild. Lv1→Lv2 250g+1H, Lv2→Lv3 700g+3H. */
    public static MetaState upgradeGuild(MetaState meta) {
        return upgradeBuilding(meta, MetaState.B_GUILD,
                GUILD_UPGRADE_LV2_GOLD, GUILD_UPGRADE_LV2_HEIRLOOM,
                GUILD_UPGRADE_LV3_GOLD, GUILD_UPGRADE_LV3_HEIRLOOM);
    }

    /** Sprint 10 B2 — upgrade Survivalist. Lv1→Lv2 180g+1H, Lv2→Lv3 350g+2H. */
    public static MetaState upgradeSurvivalist(MetaState meta) {
        return upgradeBuilding(meta, MetaState.B_SURVIVALIST,
                SURVIVALIST_UPGRADE_LV2_GOLD, SURVIVALIST_UPGRADE_LV2_HEIRLOOM,
                SURVIVALIST_UPGRADE_LV3_GOLD, SURVIVALIST_UPGRADE_LV3_HEIRLOOM);
    }

    /** Sprint 10 B2 — upgrade Caretaker. Lv1→Lv2 200g+1H, Lv2→Lv3 500g+2H. */
    public static MetaState upgradeCaretaker(MetaState meta) {
        return upgradeBuilding(meta, MetaState.B_CARETAKER,
                CARETAKER_UPGRADE_LV2_GOLD, CARETAKER_UPGRADE_LV2_HEIRLOOM,
                CARETAKER_UPGRADE_LV3_GOLD, CARETAKER_UPGRADE_LV3_HEIRLOOM);
    }

    /** Internal common upgrade logic. Validate gold+heirloom for next tier, return
     *  updated meta with level bumped + costs deducted. */
    private static MetaState upgradeBuilding(MetaState meta, String building,
                                             int lv2Gold, int lv2Heirloom,
                                             int lv3Gold, int lv3Heirloom) {
        int currentLevel = meta.buildingLevel(building);
        if (currentLevel >= 3) {
            throw new HamletException(I18n.t("error.buildingMaxed", building));
        }
        int costGold = currentLevel == 1 ? lv2Gold : lv3Gold;
        int costHeirloom = currentLevel == 1 ? lv2Heirloom : lv3Heirloom;
        if (meta.gold() < costGold) {
            throw new HamletException(I18n.t("error.notEnoughGold", costGold, meta.gold()));
        }
        if (meta.heirloom() < costHeirloom) {
            throw new HamletException(I18n.t("error.notEnoughHeirloom", costHeirloom, meta.heirloom()));
        }
        return meta
                .withGold(meta.gold() - costGold)
                .withHeirloomDelta(-costHeirloom)
                .withBuildingLevel(building, currentLevel + 1);
    }

    /** Sprint 10 B2 — gold cost for the next upgrade tier of a building.
     *  Returns -1 if already maxed (UI shows "Maxed" instead of price). */
    public static int upgradeGoldCost(String building, int currentLevel) {
        if (currentLevel >= 3) return -1;
        boolean lv2 = currentLevel == 1;
        return switch (building) {
            case MetaState.B_STAGECOACH -> lv2 ? STAGECOACH_UPGRADE_LV2_GOLD : STAGECOACH_UPGRADE_LV3_GOLD;
            case MetaState.B_GUILD       -> lv2 ? GUILD_UPGRADE_LV2_GOLD : GUILD_UPGRADE_LV3_GOLD;
            case MetaState.B_SURVIVALIST -> lv2 ? SURVIVALIST_UPGRADE_LV2_GOLD : SURVIVALIST_UPGRADE_LV3_GOLD;
            case MetaState.B_CARETAKER   -> lv2 ? CARETAKER_UPGRADE_LV2_GOLD : CARETAKER_UPGRADE_LV3_GOLD;
            default -> -1;
        };
    }

    /** Sprint 10 B2 — heirloom cost for the next upgrade tier. -1 if maxed. */
    public static int upgradeHeirloomCost(String building, int currentLevel) {
        if (currentLevel >= 3) return -1;
        boolean lv2 = currentLevel == 1;
        return switch (building) {
            case MetaState.B_STAGECOACH -> lv2 ? STAGECOACH_UPGRADE_LV2_HEIRLOOM : STAGECOACH_UPGRADE_LV3_HEIRLOOM;
            case MetaState.B_GUILD       -> lv2 ? GUILD_UPGRADE_LV2_HEIRLOOM : GUILD_UPGRADE_LV3_HEIRLOOM;
            case MetaState.B_SURVIVALIST -> lv2 ? SURVIVALIST_UPGRADE_LV2_HEIRLOOM : SURVIVALIST_UPGRADE_LV3_HEIRLOOM;
            case MetaState.B_CARETAKER   -> lv2 ? CARETAKER_UPGRADE_LV2_HEIRLOOM : CARETAKER_UPGRADE_LV3_HEIRLOOM;
            default -> -1;
        };
    }

    /**
     * Sprint 10 B1 — auto-cull random heroes from {@code meta.roster} until it fits
     * {@link MetaState#SOFT_ROSTER_CAP}. Picked-party heroes are protected from culling.
     * Called by {@code ProjectShadowGame.startNewRun} when {@link MetaState#isRosterOverCap()}
     * is true. Player was warned by HamletScreen UI before this point.
     *
     * @param meta current meta state (may be over cap)
     * @param protectedHeroIds heroes that MUST survive the cull (the embarking party)
     * @return new MetaState with roster trimmed to cap
     */
    public static MetaState autoCullRosterToCap(MetaState meta, List<String> protectedHeroIds) {
        if (!meta.isRosterOverCap()) return meta;
        Set<String> protect = Set.copyOf(protectedHeroIds);
        List<HeroState> keep = new ArrayList<>();
        List<HeroState> cullable = new ArrayList<>();
        for (HeroState rs : meta.roster()) {
            if (protect.contains(rs.heroId())) keep.add(rs);
            else cullable.add(rs);
        }
        int targetSize = MetaState.SOFT_ROSTER_CAP;
        int toKeepFromCullable = Math.max(0, targetSize - keep.size());
        // Shuffle the cullable list with a fresh RNG and keep the first N.
        java.util.Collections.shuffle(cullable, new java.util.Random());
        List<HeroState> survivors = cullable.stream().limit(toKeepFromCullable).toList();
        List<HeroState> newRoster = new ArrayList<>(keep);
        newRoster.addAll(survivors);
        return meta.withRoster(newRoster);
    }

    /**
     * Sprint 10 B1 — paid refresh of Stagecoach offers. Costs {@link #STAGECOACH_REFRESH_COST}
     * gold. Throws if player can't afford. Returns updated MetaState (gold deducted).
     * Caller is responsible for re-calling {@link #rollStagecoachOffers} to get the new
     * offer list (kept separate so the offer roll itself stays free of side effects).
     */
    public static MetaState payStagecoachRefresh(MetaState meta) {
        // Sprint 11 B1 debt model: gold can go negative (see hireHero comment).
        return meta.withGold(meta.gold() - STAGECOACH_REFRESH_COST);
    }

    public static MetaState hireHero(MetaState meta, String heroId, GameData gd) {
        if (meta.hasInRoster(heroId)) {
            throw new HamletException(I18n.t("error.heroDuplicate", heroId));
        }
        HeroData data = gd.heroes().get(heroId);
        if (data == null) throw new HamletException(I18n.t("error.unknownHero", heroId));
        int cost = hireCost(heroId, gd);
        // Sprint 11 B1 debt model: gold can go negative (player accumulates
        // "supplies debt"). Combat reward pays it down. Lock from 2026-05-11:
        // softlock-fix decision = allow negative gold.
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
                .orElseThrow(() -> new HamletException(I18n.t("error.heroNotInRoster", heroId)));
        if (rs.level() >= GUILD_MAX_LEVEL) {
            throw new HamletException(I18n.t("error.maxLevel", GUILD_MAX_LEVEL));
        }
        int cost = levelUpCost(rs.level());
        if (meta.gold() < cost) {
            throw new HamletException(I18n.t("error.notEnoughGold", cost, meta.gold()));
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
            throw new HamletException(I18n.t("error.notEnoughGold", SURVIVALIST_CRAFT_COST, meta.gold()));
        }
        List<String> trinkets = new ArrayList<>();
        for (ItemData it : gd.items().values()) {
            String cat = it.category();
            if (cat != null && cat.toLowerCase().contains("trinket")) {
                trinkets.add(it.itemId());
            }
        }
        if (trinkets.isEmpty()) {
            throw new HamletException(I18n.t("error.noTrinket"));
        }
        String picked = trinkets.get(rng.nextInt(trinkets.size()));
        List<String> newInv = new ArrayList<>(meta.trinketInventory());
        newInv.add(picked);
        return meta.withGold(meta.gold() - SURVIVALIST_CRAFT_COST).withTrinketInventory(newInv);
    }

    // ---------- Caretaker ----------

    public static MetaState cureDisease(MetaState meta, String heroId, String diseaseId, GameData gd) {
        HeroState rs = meta.heroInRoster(heroId)
                .orElseThrow(() -> new HamletException(I18n.t("error.heroNotInRoster", heroId)));
        if (!rs.diseases().contains(diseaseId)) {
            throw new HamletException(I18n.t("error.noDisease", diseaseId));
        }
        // Sprint 10 B2 — cost varies by Caretaker level (30g → 25g → 20g per design lock).
        int caretakerLevel = meta.buildingLevel(MetaState.B_CARETAKER);
        int cost = CARETAKER_CURE_COST_BY_LEVEL[caretakerLevel];
        if (meta.gold() < cost) {
            throw new HamletException(I18n.t("error.notEnoughGold", cost, meta.gold()));
        }
        // Sprint 10 B2 — slot tracking. Slots reset at end-of-run (per design lock).
        int slotLimit = CARETAKER_CURE_SLOTS_BY_LEVEL[caretakerLevel];
        if (meta.cureSlotsUsedThisVisit() >= slotLimit) {
            throw new HamletException(I18n.t("error.cureSlotsExhausted",
                    meta.cureSlotsUsedThisVisit(), slotLimit));
        }
        Hero h = rs.toHero(gd);
        h.removeDisease(diseaseId);
        return meta
                .withGold(meta.gold() - cost)
                .withRoster(replaceInRoster(meta.roster(), HeroState.from(h)))
                .withCureSlotConsumed();
    }

    /** Reduces the hero's stress by {@link #CARETAKER_STRESS_RELIEF_BLOCK} for one fixed cost. */
    public static MetaState reduceStress(MetaState meta, String heroId, GameData gd) {
        HeroState rs = meta.heroInRoster(heroId)
                .orElseThrow(() -> new HamletException(I18n.t("error.heroNotInRoster", heroId)));
        if (rs.currentStress() <= 0) {
            throw new HamletException(I18n.t("error.noStress"));
        }
        if (meta.gold() < CARETAKER_STRESS_RELIEF_COST) {
            throw new HamletException(I18n.t("error.notEnoughGold", CARETAKER_STRESS_RELIEF_COST, meta.gold()));
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
        // Sprint 10 B2 — reset cure slots at end-of-run (locked design 2026-05-11).
        // One run-cycle = one Hamlet visit for cure-slot purposes.
        return meta.withGold(newGold).withRoster(newRoster).withCureSlotsReset();
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
