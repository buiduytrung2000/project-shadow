package com.trungbui.projectshadow.run;

import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.data.model.StageConfig;
import com.trungbui.projectshadow.domain.Hero;
import com.trungbui.projectshadow.domain.Position;
import com.trungbui.projectshadow.save.RunState;
import com.trungbui.projectshadow.save.SaveManager;
import com.trungbui.projectshadow.stage.NodeType;
import com.trungbui.projectshadow.stage.StageGenerator;
import com.trungbui.projectshadow.stage.StageNode;
import com.trungbui.projectshadow.stage.StageTree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Sprint 7 — run loop driver.
 *
 * <p>Owns the live {@link StageTree}, the active {@link Hero} party, and the persistent
 * {@link RunState}. After each node is resolved, {@link #completeNode(String)} updates
 * the run state and persists it via the {@link SaveManager}.</p>
 *
 * <p>Saves only happen at safe-node boundaries (after a node fully resolves), not mid-combat.
 * On party death, callers must invoke {@link #archiveOnDeath()} to honor permadeath.</p>
 */
public class RunSession {

    private final GameData gameData;
    private final SaveManager saveManager;
    private final StageTree stageTree;
    private final List<Hero> party;
    private RunState state;

    private RunSession(GameData gameData, SaveManager saveManager,
                       StageTree stageTree, List<Hero> party, RunState state) {
        this.gameData = gameData;
        this.saveManager = saveManager;
        this.stageTree = stageTree;
        this.party = party;
        this.state = state;
    }

    public static RunSession startNew(GameData gd, SaveManager mgr,
                                      String stageId, long seed, List<String> heroIds) {
        if (gd == null) throw new IllegalArgumentException("gameData must not be null");
        if (mgr == null) throw new IllegalArgumentException("saveManager must not be null");
        StageConfig cfg = gd.stages().get(stageId);
        if (cfg == null) throw new IllegalArgumentException("Unknown stageId: " + stageId);
        StageTree tree = StageGenerator.generate(cfg, seed);
        List<Hero> party = buildParty(gd, heroIds);
        RunState state = RunState.newRun(stageId, seed, party);
        return new RunSession(gd, mgr, tree, party, state);
    }

    private static List<Hero> buildParty(GameData gd, List<String> heroIds) {
        if (heroIds == null || heroIds.isEmpty() || heroIds.size() > 4) {
            throw new IllegalArgumentException("party size must be 1..4, got "
                    + (heroIds == null ? 0 : heroIds.size()));
        }
        List<Hero> heroes = new ArrayList<>(heroIds.size());
        for (int i = 0; i < heroIds.size(); i++) {
            String id = heroIds.get(i);
            var data = gd.heroes().get(id);
            if (data == null) throw new IllegalArgumentException("Unknown heroId: " + id);
            heroes.add(new Hero(data, Position.values()[i], gd.effects()));
        }
        return heroes;
    }

    public GameData gameData() {
        return gameData;
    }

    public SaveManager saveManager() {
        return saveManager;
    }

    public StageTree stageTree() {
        return stageTree;
    }

    public List<Hero> party() {
        return List.copyOf(party);
    }

    public RunState state() {
        return state;
    }

    /** Returns the node the player most recently resolved, or {@code null} if no node yet. */
    public StageNode currentNode() {
        if (state.currentNodeLabel() == null) return null;
        return stageTree.getNode(state.currentNodeLabel()).orElse(null);
    }

    /**
     * Returns the nodes the player can next enter from the current position.
     * If no node has been resolved yet, returns just the first node of the stage.
     */
    public List<StageNode> reachableNextNodes() {
        String cur = state.currentNodeLabel();
        if (cur == null) {
            return stageTree.firstNode().map(List::of).orElse(List.of());
        }
        List<String> labels = stageTree.edgesFrom(cur);
        List<StageNode> out = new ArrayList<>(labels.size());
        for (String l : labels) {
            stageTree.getNode(l).ifPresent(out::add);
        }
        return out;
    }

    /**
     * Marks the given node as just-resolved, syncs party HP/stress/etc. into the
     * snapshot, persists to disk. Returns the save file path.
     */
    public Path completeNode(String label) throws IOException {
        if (label == null || stageTree.getNode(label).isEmpty()) {
            throw new IllegalArgumentException("Unknown node label: " + label);
        }
        state = state.withCurrentNode(label).withParty(party);
        return saveManager.save(state);
    }

    public boolean isPartyDead() {
        return party.stream().allMatch(h -> h.currentHp() <= 0);
    }

    public boolean isOnBossNode() {
        StageNode cur = currentNode();
        return cur != null && cur.type() == NodeType.BOSS;
    }

    /**
     * Permadeath: marks the run archived in the save and moves the file to the
     * archive subdirectory. Returns the archive file path if successful.
     */
    public Path archiveOnDeath() throws IOException {
        state = state.markArchived().withParty(party);
        saveManager.save(state);
        saveManager.archive(state.runId());
        return saveManager.baseDir().resolve("archive").resolve("run_" + state.runId() + ".json");
    }
}
