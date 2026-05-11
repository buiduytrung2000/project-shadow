package com.trungbui.projectshadow.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DataLoader {

    private static final ObjectMapper JSON = new ObjectMapper();

    private DataLoader() {
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static Map<String, Integer> readHeader(CSVReader csv) throws IOException, CsvException {
        String[] header = csv.readNext();
        if (header == null) {
            throw new IllegalStateException("CSV is empty (missing header row)");
        }
        // Strip UTF-8 BOM from first column if present
        if (header.length > 0 && header[0] != null && !header[0].isEmpty() && header[0].charAt(0) == '﻿') {
            header[0] = header[0].substring(1);
        }
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            if (header[i] != null) {
                map.put(header[i].trim(), i);
            }
        }
        return map;
    }

    private static String cell(String[] row, Map<String, Integer> headers, String column) {
        Integer idx = headers.get(column);
        if (idx == null) {
            throw new IllegalStateException("Missing column in CSV header: '" + column + "'");
        }
        if (idx >= row.length) return "";
        String v = row[idx];
        return v == null ? "" : v.trim();
    }

    private static boolean isBlankRow(String[] row) {
        if (row.length == 0) return true;
        for (String c : row) {
            if (c != null && !c.isBlank()) return false;
        }
        return true;
    }

    private static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static int parseInt(String s) {
        if (s == null || s.isBlank()) return 0;
        String t = s.trim();
        if (t.startsWith("+")) t = t.substring(1);
        return Integer.parseInt(t);
    }

    private static Integer parseIntegerNullable(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        if (t.startsWith("+")) t = t.substring(1);
        return Integer.parseInt(t);
    }

    private static double parseDouble(String s) {
        if (s == null || s.isBlank()) return 0d;
        String t = s.trim();
        if (t.startsWith("+")) t = t.substring(1);
        return Double.parseDouble(t);
    }

    private static boolean parseBool(String s) {
        if (s == null) return false;
        String t = s.trim();
        return t.equalsIgnoreCase("Yes") || t.equalsIgnoreCase("True") || t.equals("1");
    }

    private static List<String> parseStringList(String s) {
        if (s == null || s.isBlank()) return List.of();
        String[] parts = s.split(",");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return List.copyOf(out);
    }

    // ─── CSV loaders ─────────────────────────────────────────────────────────

    public static List<HeroData> loadHeroes(Reader reader) throws IOException, CsvException {
        try (CSVReader csv = new CSVReaderBuilder(reader).build()) {
            Map<String, Integer> h = readHeader(csv);
            List<HeroData> out = new ArrayList<>();
            String[] row;
            while ((row = csv.readNext()) != null) {
                if (isBlankRow(row)) continue;
                // Sprint 11 B3 — Base Dodge / LevelUp Dodge are new columns.
                // Backward-compatible: if missing in CSV, derive a sensible default
                // from Position (Front=0, Back=2) so existing data continues working
                // without editing every row. Designer can override per-hero by
                // adding the columns later.
                String position = cell(row, h, "Position");
                int defaultBaseDodge = "Back".equalsIgnoreCase(position) ? 2 : 0;
                int baseDodge = h.containsKey("Base Dodge")
                        ? parseInt(cell(row, h, "Base Dodge"))
                        : defaultBaseDodge;
                double levelUpDodge = h.containsKey("LevelUp Dodge")
                        ? parseDouble(cell(row, h, "LevelUp Dodge"))
                        : 0d;
                out.add(new HeroData(
                        cell(row, h, "Rarity"),
                        cell(row, h, "Name (VN)"),
                        cell(row, h, "Name (EN)"),
                        cell(row, h, "Hero ID"),
                        cell(row, h, "Role"),
                        position,
                        parseInt(cell(row, h, "Base HP")),
                        parseInt(cell(row, h, "Base DMG Min")),
                        parseInt(cell(row, h, "Base DMG Max")),
                        parseInt(cell(row, h, "Base Accuracy")),
                        parseDouble(cell(row, h, "Base Crit")),
                        parseInt(cell(row, h, "Speed")),
                        parseDouble(cell(row, h, "Base Stress Resist")),
                        parseInt(cell(row, h, "LevelUp HP")),
                        parseInt(cell(row, h, "LevelUp DMG")),
                        parseDouble(cell(row, h, "LevelUp Crit")),
                        parseDouble(cell(row, h, "LevelUp Stress Resist")),
                        baseDodge,
                        levelUpDodge,
                        parseStringList(cell(row, h, "Default Skills")),
                        parseStringList(cell(row, h, "Available Skills")),
                        cell(row, h, "Notes")
                ));
            }
            return List.copyOf(out);
        }
    }

    public static List<SkillData> loadSkills(Reader reader) throws IOException, CsvException {
        try (CSVReader csv = new CSVReaderBuilder(reader).build()) {
            Map<String, Integer> h = readHeader(csv);
            List<SkillData> out = new ArrayList<>();
            String[] row;
            while ((row = csv.readNext()) != null) {
                if (isBlankRow(row)) continue;
                out.add(new SkillData(
                        cell(row, h, "Skill ID"),
                        cell(row, h, "Name (VN)"),
                        cell(row, h, "Name (EN)"),
                        cell(row, h, "Hero Class (VN)"),
                        cell(row, h, "Hero Class (EN)"),
                        parseBool(cell(row, h, "Is Offensive")),
                        cell(row, h, "Target Type"),
                        parseDouble(cell(row, h, "Damage Multiplier")),
                        parseInt(cell(row, h, "Accuracy Modifier")),
                        parseInt(cell(row, h, "Cooldown")),
                        nullIfBlank(cell(row, h, "Primary Effect ID")),
                        nullIfBlank(cell(row, h, "Effect Magnitude")),
                        nullIfBlank(cell(row, h, "Effect Duration")),
                        nullIfBlank(cell(row, h, "Effect Notes")),
                        parseInt(cell(row, h, "Stress Damage")),
                        cell(row, h, "Rarity"),
                        cell(row, h, "Description (VN)")
                ));
            }
            return List.copyOf(out);
        }
    }

    public static List<EffectData> loadEffects(Reader reader) throws IOException, CsvException {
        try (CSVReader csv = new CSVReaderBuilder(reader).build()) {
            Map<String, Integer> h = readHeader(csv);
            List<EffectData> out = new ArrayList<>();
            String[] row;
            while ((row = csv.readNext()) != null) {
                if (isBlankRow(row)) continue;
                out.add(new EffectData(
                        cell(row, h, "Effect ID"),
                        cell(row, h, "Name (VN)"),
                        cell(row, h, "Name (EN)"),
                        cell(row, h, "Category"),
                        parseBool(cell(row, h, "Is Removable")),
                        nullIfBlank(cell(row, h, "Removal Condition")),
                        cell(row, h, "Duration Type"),
                        nullIfBlank(cell(row, h, "Default Duration")),
                        cell(row, h, "Stat Affected"),
                        cell(row, h, "Modifier Type"),
                        nullIfBlank(cell(row, h, "Modifier Value")),
                        parseBool(cell(row, h, "Can Stack")),
                        parseIntegerNullable(cell(row, h, "Max Stacks")),
                        cell(row, h, "Trigger"),
                        nullIfBlank(cell(row, h, "Source Type")),
                        nullIfBlank(cell(row, h, "Source Examples")),
                        nullIfBlank(cell(row, h, "Visual Cue")),
                        nullIfBlank(cell(row, h, "Balance Notes")),
                        cell(row, h, "Description (VN)")
                ));
            }
            return List.copyOf(out);
        }
    }

    public static List<ItemData> loadItems(Reader reader) throws IOException, CsvException {
        try (CSVReader csv = new CSVReaderBuilder(reader).build()) {
            Map<String, Integer> h = readHeader(csv);
            List<ItemData> out = new ArrayList<>();
            String[] row;
            while ((row = csv.readNext()) != null) {
                if (isBlankRow(row)) continue;
                out.add(new ItemData(
                        cell(row, h, "Item ID"),
                        cell(row, h, "Name (VN)"),
                        cell(row, h, "Name (EN)"),
                        cell(row, h, "Category"),
                        cell(row, h, "Subcategory"),
                        parseBool(cell(row, h, "Is Removable")),
                        cell(row, h, "Restricted To Class"),
                        nullIfBlank(cell(row, h, "Effect ID")),
                        nullIfBlank(cell(row, h, "Effect Value")),
                        nullIfBlank(cell(row, h, "Effect Duration")),
                        nullIfBlank(cell(row, h, "Secondary Effect ID")),
                        nullIfBlank(cell(row, h, "Secondary Effect Value")),
                        cell(row, h, "Rarity"),
                        parseInt(cell(row, h, "Price (Gold)")),
                        parseBool(cell(row, h, "Can Drop")),
                        parseBool(cell(row, h, "Can Buy")),
                        cell(row, h, "Description (VN)")
                ));
            }
            return List.copyOf(out);
        }
    }

    public static List<EnemyData> loadEnemies(Reader reader) throws IOException, CsvException {
        try (CSVReader csv = new CSVReaderBuilder(reader).build()) {
            Map<String, Integer> h = readHeader(csv);
            List<EnemyData> out = new ArrayList<>();
            String[] row;
            while ((row = csv.readNext()) != null) {
                if (isBlankRow(row)) continue;
                out.add(new EnemyData(
                        cell(row, h, "Name"),
                        cell(row, h, "Enemy ID"),
                        parseInt(cell(row, h, "Base HP")),
                        parseInt(cell(row, h, "Damage Min")),
                        parseInt(cell(row, h, "Damage Max")),
                        parseDouble(cell(row, h, "Critical Chance")),
                        parseDouble(cell(row, h, "Stress Resist")),
                        parseInt(cell(row, h, "Accuracy")),
                        cell(row, h, "Skill 1"),
                        nullIfBlank(cell(row, h, "Skill 2")),
                        nullIfBlank(cell(row, h, "Drop Item")),
                        parseDouble(cell(row, h, "Drop Chance")),
                        nullIfBlank(cell(row, h, "Special Skill")),
                        cell(row, h, "Notes"),
                        cell(row, h, "Variant Type")
                ));
            }
            return List.copyOf(out);
        }
    }

    public static List<EnemySkillData> loadEnemySkills(Reader reader) throws IOException, CsvException {
        try (CSVReader csv = new CSVReaderBuilder(reader).build()) {
            Map<String, Integer> h = readHeader(csv);
            List<EnemySkillData> out = new ArrayList<>();
            String[] row;
            while ((row = csv.readNext()) != null) {
                if (isBlankRow(row)) continue;
                out.add(new EnemySkillData(
                        cell(row, h, "Skill ID"),
                        cell(row, h, "Name (VN)"),
                        cell(row, h, "Name (EN)"),
                        cell(row, h, "User Type"),
                        parseBool(cell(row, h, "Is Offensive")),
                        cell(row, h, "Target Type"),
                        parseDouble(cell(row, h, "Damage Multiplier")),
                        parseInt(cell(row, h, "Accuracy Modifier")),
                        parseInt(cell(row, h, "Cooldown")),
                        nullIfBlank(cell(row, h, "Effect ID")),
                        nullIfBlank(cell(row, h, "Effect Value")),
                        parseInt(cell(row, h, "Stress Damage")),
                        cell(row, h, "Description (VN)")
                ));
            }
            return List.copyOf(out);
        }
    }

    public static List<EventData> loadEvents(Reader reader) throws IOException, CsvException {
        try (CSVReader csv = new CSVReaderBuilder(reader).build()) {
            Map<String, Integer> h = readHeader(csv);
            List<EventData> out = new ArrayList<>();
            String[] row;
            while ((row = csv.readNext()) != null) {
                if (isBlankRow(row)) continue;
                out.add(new EventData(
                        cell(row, h, "Event ID"),
                        cell(row, h, "Name (VN)"),
                        cell(row, h, "Name (EN)"),
                        cell(row, h, "Stage"),
                        cell(row, h, "Trigger Type"),
                        cell(row, h, "Rarity"),
                        cell(row, h, "Description (VN)"),
                        cell(row, h, "Choice 1 Text (VN)"),
                        cell(row, h, "Choice 1 Outcomes"),
                        cell(row, h, "Choice 2 Text (VN)"),
                        cell(row, h, "Choice 2 Outcomes"),
                        nullIfBlank(cell(row, h, "Choice 3 Text (VN)")),
                        nullIfBlank(cell(row, h, "Choice 3 Outcomes")),
                        cell(row, h, "Status"),
                        cell(row, h, "Notes")
                ));
            }
            return List.copyOf(out);
        }
    }

    public static List<DiseaseTraitData> loadDiseasesTraits(Reader reader) throws IOException, CsvException {
        try (CSVReader csv = new CSVReaderBuilder(reader).build()) {
            Map<String, Integer> h = readHeader(csv);
            List<DiseaseTraitData> out = new ArrayList<>();
            String[] row;
            while ((row = csv.readNext()) != null) {
                if (isBlankRow(row)) continue;
                out.add(new DiseaseTraitData(
                        cell(row, h, "Name"),
                        cell(row, h, "Type"),
                        cell(row, h, "Disease/Trait ID"),
                        cell(row, h, "Effect Stat"),
                        cell(row, h, "Effect Value"),
                        nullIfBlank(cell(row, h, "Trigger")),
                        nullIfBlank(cell(row, h, "Condition")),
                        parseInt(cell(row, h, "Remove Cost")),
                        cell(row, h, "Description"),
                        cell(row, h, "Severity"),
                        cell(row, h, "Duration"),
                        // "Resolution" column is new (Sprint 9+ B2); legacy CSVs without it
                        // get null which yields neither affliction nor virtue.
                        h.containsKey("Resolution") ? nullIfBlank(cell(row, h, "Resolution")) : null,
                        cell(row, h, "Status")
                ));
            }
            return List.copyOf(out);
        }
    }

    // ─── JSON loader ─────────────────────────────────────────────────────────

    public static StageConfig loadStage(Reader reader) throws IOException {
        return JSON.readValue(reader, StageConfig.class);
    }
}
