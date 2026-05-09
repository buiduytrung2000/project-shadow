package com.trungbui.projectshadow.data;

import com.opencsv.exceptions.CsvException;
import com.trungbui.projectshadow.data.model.DiseaseTraitData;
import com.trungbui.projectshadow.data.model.EffectData;
import com.trungbui.projectshadow.data.model.EnemyData;
import com.trungbui.projectshadow.data.model.EnemySkillData;
import com.trungbui.projectshadow.data.model.EventData;
import com.trungbui.projectshadow.data.model.HeroData;
import com.trungbui.projectshadow.data.model.ItemData;
import com.trungbui.projectshadow.data.model.SkillData;
import com.trungbui.projectshadow.data.model.StageConfig;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record GameData(
        Map<String, HeroData> heroes,
        Map<String, SkillData> skills,
        Map<String, EffectData> effects,
        Map<String, ItemData> items,
        Map<String, EnemyData> enemies,
        Map<String, EnemySkillData> enemySkills,
        Map<String, EventData> events,
        Map<String, DiseaseTraitData> diseasesTraits,
        Map<String, StageConfig> stages
) {
    @FunctionalInterface
    private interface CsvLoaderFn<T> {
        List<T> load(Reader r) throws IOException, CsvException;
    }

    public static GameData loadFromDirectory(Path dataDir) throws IOException, CsvException {
        Map<String, HeroData> heroes = readCsvAsMap(
                dataDir.resolve("heroes.csv"), DataLoader::loadHeroes, HeroData::heroId);
        Map<String, SkillData> skills = readCsvAsMap(
                dataDir.resolve("skills.csv"), DataLoader::loadSkills, SkillData::skillId);
        Map<String, EffectData> effects = readCsvAsMap(
                dataDir.resolve("effects.csv"), DataLoader::loadEffects, EffectData::effectId);
        Map<String, ItemData> items = readCsvAsMap(
                dataDir.resolve("items.csv"), DataLoader::loadItems, ItemData::itemId);
        Map<String, EnemyData> enemies = readCsvAsMap(
                dataDir.resolve("enemies.csv"), DataLoader::loadEnemies, EnemyData::enemyId);
        Map<String, EnemySkillData> enemySkills = readCsvAsMap(
                dataDir.resolve("enemy_skills.csv"), DataLoader::loadEnemySkills, EnemySkillData::skillId);
        Map<String, EventData> events = readCsvAsMap(
                dataDir.resolve("events.csv"), DataLoader::loadEvents, EventData::eventId);
        Map<String, DiseaseTraitData> diseasesTraits = readCsvAsMap(
                dataDir.resolve("diseases_traits.csv"), DataLoader::loadDiseasesTraits, DiseaseTraitData::id);

        Map<String, StageConfig> stages = new LinkedHashMap<>();
        Path stagesDir = dataDir.resolve("stages");
        try (Stream<Path> jsonFiles = Files.list(stagesDir)) {
            List<Path> sorted = jsonFiles
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
            for (Path jsonFile : sorted) {
                try (Reader r = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
                    StageConfig sc = DataLoader.loadStage(r);
                    stages.put(sc.id(), sc);
                }
            }
        }

        return new GameData(
                heroes, skills, effects, items,
                enemies, enemySkills, events, diseasesTraits,
                Map.copyOf(stages)
        );
    }

    private static <T> Map<String, T> readCsvAsMap(
            Path file,
            CsvLoaderFn<T> loader,
            Function<T, String> keyFn
    ) throws IOException, CsvException {
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return loader.load(r).stream()
                    .collect(Collectors.toUnmodifiableMap(keyFn, Function.identity()));
        }
    }
}
