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

    public static final int STAGECOACH_HIRE_COST = 50;
    public static final int STAGECOACH_OFFER_COUNT = 3;
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
        if (meta.gold() < STAGECOACH_HIRE_COST) {
            throw new HamletException("Không đủ gold (cần " + STAGECOACH_HIRE_COST + ", có " + meta.gold() + ")");
        }
        Hero fresh = new Hero(data, Position.POS_1, gd.effects());
        List<HeroState> newRoster = new ArrayList<>(meta.roster());
        newRoster.add(HeroState.from(fresh));
        return meta.withGold(meta.gold() - STAGECOACH_HIRE_COST).withRoster(newRoster);
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
