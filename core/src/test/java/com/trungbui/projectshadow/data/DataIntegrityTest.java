package com.trungbui.projectshadow.data;

import com.trungbui.projectshadow.data.model.EnemyData;
import com.trungbui.projectshadow.data.model.EnemySkillData;
import com.trungbui.projectshadow.data.model.HeroData;
import com.trungbui.projectshadow.data.model.ItemData;
import com.trungbui.projectshadow.data.model.SkillData;
import com.trungbui.projectshadow.data.model.StageConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataIntegrityTest {

    private GameData gd;

    @BeforeAll
    void loadAll() throws Exception {
        Path dataDir = resolveDataDir();
        gd = GameData.loadFromDirectory(dataDir);
    }

    private static Path resolveDataDir() {
        Path[] candidates = {
                Path.of("../assets/data"),
                Path.of("assets/data"),
                Path.of("../../assets/data")
        };
        for (Path p : candidates) {
            if (Files.isDirectory(p)) return p;
        }
        throw new IllegalStateException(
                "Cannot locate assets/data from cwd=" + Path.of("").toAbsolutePath());
    }

    @Test
    void loadCounts_match_expected() {
        assertThat(gd.heroes()).hasSize(14);
        assertThat(gd.skills()).hasSize(140);
        assertThat(gd.effects()).hasSize(134);
        assertThat(gd.items()).hasSize(50);
        assertThat(gd.enemies()).hasSize(11);
        assertThat(gd.enemySkills()).hasSize(9);
        assertThat(gd.events()).hasSize(11);
        assertThat(gd.diseasesTraits()).hasSize(14);
        assertThat(gd.stages()).hasSize(3);
    }

    @Test
    void heroDefaultSkills_referValidSkills() {
        for (HeroData h : gd.heroes().values()) {
            assertThat(h.defaultSkills())
                    .as("Hero %s defaultSkills", h.heroId())
                    .allMatch(s -> gd.skills().containsKey(s));
        }
    }

    @Test
    void heroAvailableSkills_referValidSkills() {
        for (HeroData h : gd.heroes().values()) {
            assertThat(h.availableSkills())
                    .as("Hero %s availableSkills", h.heroId())
                    .allMatch(s -> gd.skills().containsKey(s));
        }
    }

    @Test
    void skillPrimaryEffectIds_referValidEffects() {
        for (SkillData sk : gd.skills().values()) {
            String eid = sk.primaryEffectId();
            if (eid == null) continue;
            assertThat(gd.effects())
                    .as("Skill %s primaryEffectId=%s", sk.skillId(), eid)
                    .containsKey(eid);
        }
    }

    @Test
    void itemEffectIds_referValidEffects() {
        for (ItemData it : gd.items().values()) {
            if (it.effectId() != null) {
                assertThat(gd.effects())
                        .as("Item %s effectId=%s", it.itemId(), it.effectId())
                        .containsKey(it.effectId());
            }
            if (it.secondaryEffectId() != null) {
                assertThat(gd.effects())
                        .as("Item %s secondaryEffectId=%s", it.itemId(), it.secondaryEffectId())
                        .containsKey(it.secondaryEffectId());
            }
        }
    }

    @Test
    void enemySkillRefs_referValidEnemySkills() {
        for (EnemyData en : gd.enemies().values()) {
            assertThat(gd.enemySkills())
                    .as("Enemy %s skill1=%s", en.enemyId(), en.skill1())
                    .containsKey(en.skill1());
            if (en.skill2() != null) {
                assertThat(gd.enemySkills())
                        .as("Enemy %s skill2=%s", en.enemyId(), en.skill2())
                        .containsKey(en.skill2());
            }
            if (en.specialSkill() != null) {
                assertThat(gd.enemySkills())
                        .as("Enemy %s specialSkill=%s", en.enemyId(), en.specialSkill())
                        .containsKey(en.specialSkill());
            }
        }
    }

    @Test
    void enemyDropItems_referValidItems_orPseudo() {
        for (EnemyData en : gd.enemies().values()) {
            String drop = en.dropItem();
            if (drop == null || DataLoaderDemo.KNOWN_TODO_REFS.contains(drop)) continue;
            assertThat(gd.items())
                    .as("Enemy %s dropItem=%s", en.enemyId(), drop)
                    .containsKey(drop);
        }
    }

    @Test
    void enemySkillEffectIds_referValidEffects() {
        for (EnemySkillData esk : gd.enemySkills().values()) {
            if (esk.effectId() == null) continue;
            assertThat(gd.effects())
                    .as("EnemySkill %s effectId=%s", esk.skillId(), esk.effectId())
                    .containsKey(esk.effectId());
        }
    }

    @Test
    void stageEnemyRefs_referValidEnemies_orTodo() {
        for (StageConfig stage : gd.stages().values()) {
            StageConfig.ValidationRefs refs = stage.validationRefs();
            if (refs == null || refs.enemiesUsed() == null) continue;
            for (String enemyId : refs.enemiesUsed()) {
                if (DataLoaderDemo.KNOWN_TODO_REFS.contains(enemyId)) continue;
                assertThat(gd.enemies())
                        .as("Stage %s enemiesUsed: %s", stage.id(), enemyId)
                        .containsKey(enemyId);
            }
        }
    }

    @Test
    void stageEventRefs_referValidEvents() {
        for (StageConfig stage : gd.stages().values()) {
            StageConfig.ValidationRefs refs = stage.validationRefs();
            if (refs == null || refs.eventsUsed() == null) continue;
            for (String eventId : refs.eventsUsed()) {
                assertThat(gd.events())
                        .as("Stage %s eventsUsed: %s", stage.id(), eventId)
                        .containsKey(eventId);
            }
        }
    }

    @Test
    void stageItemRefs_referValidItems() {
        for (StageConfig stage : gd.stages().values()) {
            StageConfig.ValidationRefs refs = stage.validationRefs();
            if (refs == null || refs.itemsReferenced() == null) continue;
            for (String itemId : refs.itemsReferenced()) {
                if (DataLoaderDemo.KNOWN_TODO_REFS.contains(itemId)) continue;
                assertThat(gd.items())
                        .as("Stage %s itemsReferenced: %s", stage.id(), itemId)
                        .containsKey(itemId);
            }
        }
    }

    @Test
    void allValidations_clean() {
        assertThat(DataLoaderDemo.validateForeignKeys(gd))
                .as("Aggregated FK violations from DataLoaderDemo.validateForeignKeys")
                .isEmpty();
    }
}
