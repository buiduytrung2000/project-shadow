package com.trungbui.projectshadow.data;

import com.trungbui.projectshadow.data.model.EnemyData;
import com.trungbui.projectshadow.data.model.EnemySkillData;
import com.trungbui.projectshadow.data.model.HeroData;
import com.trungbui.projectshadow.data.model.ItemData;
import com.trungbui.projectshadow.data.model.SkillData;
import com.trungbui.projectshadow.data.model.StageConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class DataLoaderDemo {

    public static final Set<String> KNOWN_TODO_REFS = Set.of(
            "item_gold", "random_boss_pool", "random_uncommon_trinket"
    );

    private DataLoaderDemo() {
    }

    public static void main(String[] args) throws Exception {
        Path dataDir = Path.of(args.length > 0 ? args[0] : "assets/data");
        System.out.println("Loading from: " + dataDir.toAbsolutePath());

        GameData gd = GameData.loadFromDirectory(dataDir);

        String summary = String.format(
                "Loaded %d heroes, %d skills, %d effects, %d items, %d enemies, %d enemy skills, %d events, %d diseases/traits, %d stages",
                gd.heroes().size(),
                gd.skills().size(),
                gd.effects().size(),
                gd.items().size(),
                gd.enemies().size(),
                gd.enemySkills().size(),
                gd.events().size(),
                gd.diseasesTraits().size(),
                gd.stages().size()
        );

        List<String> violations = validateForeignKeys(gd);
        if (violations.isEmpty()) {
            System.out.println(summary + " — all FK valid ✅");
        } else {
            System.out.println(summary);
            System.err.println("❌ " + violations.size() + " FK violations:");
            violations.forEach(v -> System.err.println("  - " + v));
            System.exit(1);
        }
    }

    public static List<String> validateForeignKeys(GameData gd) {
        List<String> v = new ArrayList<>();

        for (HeroData hero : gd.heroes().values()) {
            for (String s : hero.defaultSkills()) {
                if (!gd.skills().containsKey(s)) {
                    v.add("Hero '" + hero.heroId() + "' default skill not found: " + s);
                }
            }
            for (String s : hero.availableSkills()) {
                if (!gd.skills().containsKey(s)) {
                    v.add("Hero '" + hero.heroId() + "' available skill not found: " + s);
                }
            }
        }

        for (SkillData sk : gd.skills().values()) {
            if (sk.primaryEffectId() != null && !gd.effects().containsKey(sk.primaryEffectId())) {
                v.add("Skill '" + sk.skillId() + "' primaryEffectId not found: " + sk.primaryEffectId());
            }
        }

        for (ItemData it : gd.items().values()) {
            if (it.effectId() != null && !gd.effects().containsKey(it.effectId())) {
                v.add("Item '" + it.itemId() + "' effectId not found: " + it.effectId());
            }
            if (it.secondaryEffectId() != null && !gd.effects().containsKey(it.secondaryEffectId())) {
                v.add("Item '" + it.itemId() + "' secondaryEffectId not found: " + it.secondaryEffectId());
            }
        }

        for (EnemyData en : gd.enemies().values()) {
            if (!gd.enemySkills().containsKey(en.skill1())) {
                v.add("Enemy '" + en.enemyId() + "' skill1 not found: " + en.skill1());
            }
            if (en.skill2() != null && !gd.enemySkills().containsKey(en.skill2())) {
                v.add("Enemy '" + en.enemyId() + "' skill2 not found: " + en.skill2());
            }
            if (en.specialSkill() != null && !gd.enemySkills().containsKey(en.specialSkill())) {
                v.add("Enemy '" + en.enemyId() + "' specialSkill not found: " + en.specialSkill());
            }
            if (en.dropItem() != null
                    && !KNOWN_TODO_REFS.contains(en.dropItem())
                    && !gd.items().containsKey(en.dropItem())) {
                v.add("Enemy '" + en.enemyId() + "' dropItem not found: " + en.dropItem());
            }
        }

        for (EnemySkillData esk : gd.enemySkills().values()) {
            if (esk.effectId() != null && !gd.effects().containsKey(esk.effectId())) {
                v.add("EnemySkill '" + esk.skillId() + "' effectId not found: " + esk.effectId());
            }
        }

        for (StageConfig stage : gd.stages().values()) {
            StageConfig.ValidationRefs refs = stage.validationRefs();
            if (refs == null) continue;
            checkRefs(v, "Stage '" + stage.id() + "' enemiesUsed",
                    refs.enemiesUsed(), gd.enemies().keySet());
            checkRefs(v, "Stage '" + stage.id() + "' enemySkillsUsed",
                    refs.enemySkillsUsed(), gd.enemySkills().keySet());
            checkRefs(v, "Stage '" + stage.id() + "' eventsUsed",
                    refs.eventsUsed(), gd.events().keySet());
            checkRefs(v, "Stage '" + stage.id() + "' itemsReferenced",
                    refs.itemsReferenced(), gd.items().keySet());
        }

        return v;
    }

    private static void checkRefs(List<String> violations, String label, List<String> refs, Set<String> validKeys) {
        if (refs == null) return;
        for (String id : refs) {
            if (!validKeys.contains(id) && !KNOWN_TODO_REFS.contains(id)) {
                violations.add(label + " not found: " + id);
            }
        }
    }
}
