package com.trungbui.projectshadow.run;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.save.RunState;
import com.trungbui.projectshadow.save.SaveManager;
import com.trungbui.projectshadow.stage.NodeType;
import com.trungbui.projectshadow.stage.StageNode;
import com.trungbui.projectshadow.stage.StageTree;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RunSessionTest {

    @TempDir
    Path tempBaseDir;

    private GameData gd;
    private SaveManager saveManager;

    @BeforeAll
    void loadAll() throws Exception {
        gd = GameData.loadFromDirectory(resolveDataDir());
    }

    @BeforeEach
    void freshSaveDir() {
        saveManager = new SaveManager(tempBaseDir.resolve("saves-" + System.nanoTime()));
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

    private static final List<String> PARTY = List.of("hero_01", "hero_13", "hero_05", "hero_03");

    private RunSession newRun() {
        return RunSession.startNew(gd, saveManager, "stage_1", 42L, PARTY);
    }

    @Test
    void startNew_buildsTreeAndParty() {
        RunSession run = newRun();
        StageTree tree = run.stageTree();
        assertThat(tree.stageId()).isEqualTo("stage_1");
        assertThat(tree.size()).isGreaterThan(0);
        assertThat(tree.bossNode()).isPresent();
        assertThat(run.party()).hasSize(4);
        assertThat(run.party().get(0).id()).isEqualTo("hero_01");
        assertThat(run.state().currentNodeLabel()).isNull();
    }

    @Test
    void startNew_rejectsUnknownStageId() {
        assertThatThrownBy(() -> RunSession.startNew(gd, saveManager, "stage_does_not_exist", 1L, PARTY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stage_does_not_exist");
    }

    @Test
    void startNew_rejectsEmptyParty() {
        assertThatThrownBy(() -> RunSession.startNew(gd, saveManager, "stage_1", 1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reachableNextNodes_atStart_returnsFirstNodeOnly() {
        RunSession run = newRun();
        List<StageNode> next = run.reachableNextNodes();
        assertThat(next).hasSize(1);
        assertThat(next.get(0).label()).isEqualTo(run.stageTree().firstNode().get().label());
    }

    @Test
    void reachableNextNodes_afterFirst_returnsLayer2Nodes() throws IOException {
        RunSession run = newRun();
        String first = run.stageTree().firstNode().get().label();
        run.completeNode(first);
        List<StageNode> next = run.reachableNextNodes();
        assertThat(next).isNotEmpty();
        for (StageNode n : next) {
            assertThat(n.label()).isNotEqualTo(first);
        }
    }

    @Test
    void completeNode_persistsToDisk() throws IOException {
        RunSession run = newRun();
        String first = run.stageTree().firstNode().get().label();
        Path savedFile = run.completeNode(first);
        assertThat(Files.exists(savedFile)).isTrue();
        assertThat(run.state().currentNodeLabel()).isEqualTo(first);
    }

    @Test
    void completeNode_capturesPartyHpDamage() throws IOException {
        RunSession run = newRun();
        Hero firstHero = run.party().get(0);
        int originalHp = firstHero.currentHp();
        firstHero.takeHpDamage(5);

        String first = run.stageTree().firstNode().get().label();
        run.completeNode(first);

        RunState loaded = saveManager.load(run.state().runId());
        assertThat(loaded.party().get(0).currentHp()).isEqualTo(originalHp - 5);
    }

    @Test
    void completeNode_rejectsUnknownLabel() {
        RunSession run = newRun();
        assertThatThrownBy(() -> run.completeNode("LX.Z"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LX.Z");
    }

    @Test
    void currentNode_isNullUntilFirstCompletion() throws IOException {
        RunSession run = newRun();
        assertThat(run.currentNode()).isNull();
        String first = run.stageTree().firstNode().get().label();
        run.completeNode(first);
        assertThat(run.currentNode()).isNotNull();
        assertThat(run.currentNode().label()).isEqualTo(first);
    }

    @Test
    void isPartyDead_falseAtStart_trueAfterAllHpZero() {
        RunSession run = newRun();
        assertThat(run.isPartyDead()).isFalse();
        for (Hero h : run.party()) h.takeHpDamage(99999);
        assertThat(run.isPartyDead()).isTrue();
    }

    @Test
    void isOnBossNode_trueOnlyAfterEnteringBoss() throws IOException {
        RunSession run = newRun();
        assertThat(run.isOnBossNode()).isFalse();

        StageNode boss = run.stageTree().bossNode().orElseThrow();
        run.completeNode(boss.label());
        assertThat(run.isOnBossNode()).isTrue();
        assertThat(run.currentNode().type()).isEqualTo(NodeType.BOSS);
    }

    @Test
    void archiveOnDeath_marksAndMovesSaveFile() throws IOException {
        RunSession run = newRun();
        String first = run.stageTree().firstNode().get().label();
        run.completeNode(first);

        for (Hero h : run.party()) h.takeHpDamage(99999);
        Path archived = run.archiveOnDeath();

        assertThat(Files.exists(archived)).isTrue();
        assertThat(run.state().archived()).isTrue();
        assertThat(saveManager.listActiveSaves()).doesNotContain(run.state().runId());
        assertThat(saveManager.listArchivedSaves()).contains(run.state().runId());
    }

    @Test
    void archiveOnDeath_archivedSaveContainsArchivedFlag() throws IOException {
        RunSession run = newRun();
        String first = run.stageTree().firstNode().get().label();
        run.completeNode(first);
        run.archiveOnDeath();

        RunState reloaded = saveManager.load(run.state().runId());
        assertThat(reloaded.archived()).isTrue();
    }
}
