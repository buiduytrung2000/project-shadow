package com.trungbui.projectshadow.save;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.HeroData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record HeroState(
        String heroId,
        int level,
        int currentHp,
        int currentStress,
        int positionRank,
        List<String> equippedSkills,
        List<String> traits,
        List<String> diseases,
        Map<String, Integer> skillCooldowns,
        /** Sprint 12 B2 — accumulated XP. Missing field on legacy saves
         *  Jackson-defaults to 0; compact constructor floors at 0. */
        int currentXp
) {
    public HeroState {
        equippedSkills = equippedSkills == null ? List.of() : List.copyOf(equippedSkills);
        traits = traits == null ? List.of() : List.copyOf(traits);
        diseases = diseases == null ? List.of() : List.copyOf(diseases);
        skillCooldowns = skillCooldowns == null ? Map.of() : Map.copyOf(skillCooldowns);
        if (currentXp < 0) currentXp = 0;
    }

    /** Sprint 12 B2 — legacy 9-arg constructor for tests and callers that
     *  predate the {@code currentXp} field. Defaults XP to 0. */
    public HeroState(String heroId, int level, int currentHp, int currentStress, int positionRank,
                     List<String> equippedSkills, List<String> traits,
                     List<String> diseases, Map<String, Integer> skillCooldowns) {
        this(heroId, level, currentHp, currentStress, positionRank,
                equippedSkills, traits, diseases, skillCooldowns, 0);
    }

    public static HeroState from(Hero hero) {
        return new HeroState(
                hero.id(),
                hero.level(),
                hero.currentHp(),
                hero.currentStress(),
                hero.position().rank(),
                hero.equippedSkills(),
                new ArrayList<>(hero.traits()),
                new ArrayList<>(hero.diseases()),
                hero.skillCooldownsMap(),
                hero.currentXp()
        );
    }

    public Hero toHero(GameData gameData) {
        HeroData data = gameData.heroes().get(heroId);
        if (data == null) {
            throw new IllegalStateException("Unknown heroId in save: " + heroId);
        }
        Hero hero = new Hero(data, level, Position.ofRank(positionRank), equippedSkills, gameData.effects());
        hero.setCurrentHp(currentHp);
        hero.setCurrentStress(currentStress);
        hero.setCurrentXp(currentXp);
        for (String t : traits) hero.addTrait(t);
        for (String d : diseases) hero.addDisease(d);
        for (Map.Entry<String, Integer> e : skillCooldowns.entrySet()) {
            hero.putOnCooldown(e.getKey(), e.getValue());
        }
        return hero;
    }
}
