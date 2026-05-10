package com.trungbui.projectshadow.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.trungbui.projectshadow.ProjectShadowGame;
import com.trungbui.projectshadow.run.RunSession;
import com.trungbui.projectshadow.stage.CombatNode;
import com.trungbui.projectshadow.stage.EliteNode;
import com.trungbui.projectshadow.stage.NodeType;
import com.trungbui.projectshadow.stage.StageNode;
import com.trungbui.projectshadow.stage.StageTree;
import com.trungbui.projectshadow.ui.FontFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sprint 7 — stage tree map.
 *
 * <p>Lays out the {@link StageTree} via BFS-distance from the first node, draws edges
 * with {@link ShapeRenderer}, and renders a clickable {@link TextButton} per node.
 * Only nodes reachable from the current run position (i.e. {@code edgesFrom(current)})
 * are interactable; others are visually dimmed.</p>
 */
public class StageMapScreen implements Screen {

    private static final int VIRTUAL_WIDTH = 1920;
    private static final int VIRTUAL_HEIGHT = 1080;
    private static final int FONT_SIZE_PX = 22;

    private static final int NODE_WIDTH = 180;
    private static final int NODE_HEIGHT = 64;

    private static final int LEFT_MARGIN = 120;
    private static final int RIGHT_MARGIN = 120;
    private static final int TOP_MARGIN = 200;
    private static final int BOTTOM_MARGIN = 120;

    private final ProjectShadowGame game;
    private final RunSession run;

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final FontFactory fontFactory;
    private final Skin skin;
    private final Stage uiStage;
    private final ShapeRenderer shapes;

    private final Map<String, Vector2> nodeCenters = new HashMap<>();
    private final Set<String> reachableLabels = new HashSet<>();

    private Label headerLabel;
    private Label partyLabel;

    public StageMapScreen(ProjectShadowGame game) {
        this.game = game;
        this.run = game.runSession();
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        this.fontFactory = new FontFactory(Gdx.files.internal("fonts/BeVietnamPro-Regular.ttf"));
        var vnFont = fontFactory.create(FONT_SIZE_PX);
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        skin.get(Label.LabelStyle.class).font = vnFont;
        skin.get(TextButton.TextButtonStyle.class).font = vnFont;
        this.uiStage = new Stage(viewport);
        this.shapes = new ShapeRenderer();

        for (StageNode n : run.reachableNextNodes()) reachableLabels.add(n.label());

        computeLayout();
        buildHeader();
        buildNodeButtons();
        viewport.apply(true);
    }

    /** BFS-distance layering from the first node, then column layout per layer. */
    private void computeLayout() {
        StageTree tree = run.stageTree();
        String start = tree.firstNode().orElseThrow().label();
        Map<String, Integer> layer = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(start);
        layer.put(start, 0);
        while (!queue.isEmpty()) {
            String n = queue.poll();
            int d = layer.get(n);
            for (String next : tree.edgesFrom(n)) {
                int candidate = d + 1;
                Integer existing = layer.get(next);
                if (existing == null || candidate > existing) {
                    layer.put(next, candidate);
                    queue.add(next);
                }
            }
        }

        // group by layer
        int maxLayer = layer.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        Map<Integer, List<String>> byLayer = new HashMap<>();
        for (var e : layer.entrySet()) {
            byLayer.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }

        int usableW = VIRTUAL_WIDTH - LEFT_MARGIN - RIGHT_MARGIN;
        int usableH = VIRTUAL_HEIGHT - TOP_MARGIN - BOTTOM_MARGIN;
        int xStep = maxLayer > 0 ? usableW / maxLayer : 0;

        for (int l = 0; l <= maxLayer; l++) {
            List<String> labels = byLayer.getOrDefault(l, List.of());
            int count = labels.size();
            if (count == 0) continue;
            int x = LEFT_MARGIN + l * xStep;
            for (int i = 0; i < count; i++) {
                int y = BOTTOM_MARGIN + (count == 1
                        ? usableH / 2
                        : usableH - (i * usableH / Math.max(1, count - 1)));
                nodeCenters.put(labels.get(i), new Vector2(x, y));
            }
        }
    }

    private void buildHeader() {
        headerLabel = new Label(headerText(), skin);
        headerLabel.setPosition(LEFT_MARGIN, VIRTUAL_HEIGHT - 60);
        uiStage.addActor(headerLabel);

        partyLabel = new Label(partyText(), skin);
        partyLabel.setPosition(LEFT_MARGIN, VIRTUAL_HEIGHT - 110);
        partyLabel.setColor(Color.LIGHT_GRAY);
        uiStage.addActor(partyLabel);
    }

    private String headerText() {
        StageNode cur = run.currentNode();
        String pos = cur == null ? "khởi đầu" : cur.label() + " (" + describeType(cur.type()) + ")";
        return "Stage: " + run.stageTree().stageId()
                + "  —  Vị trí: " + pos
                + "  —  Gold: " + run.state().gold();
    }

    private String partyText() {
        StringBuilder sb = new StringBuilder("Đội hình:  ");
        boolean first = true;
        for (var h : run.party()) {
            if (!first) sb.append("   |   ");
            first = false;
            sb.append(h.data().nameVn())
                    .append(" HP ").append(h.currentHp()).append("/").append(h.maxHp())
                    .append(" Stress ").append(h.currentStress()).append("/").append(h.maxStress());
        }
        return sb.toString();
    }

    private void buildNodeButtons() {
        StageTree tree = run.stageTree();
        Set<String> visited = new HashSet<>(run.state().visitedNodes());
        String currentLabel = run.state().currentNodeLabel();

        for (var entry : nodeCenters.entrySet()) {
            String label = entry.getKey();
            Vector2 c = entry.getValue();
            StageNode node = tree.getNode(label).orElseThrow();

            String text = label + "\n" + describeNode(node);
            TextButton btn = new TextButton(text, skin);
            btn.setBounds(c.x - NODE_WIDTH / 2f, c.y - NODE_HEIGHT / 2f, NODE_WIDTH, NODE_HEIGHT);

            boolean isCurrent = label.equals(currentLabel);
            boolean isVisited = visited.contains(label);
            boolean isReachable = reachableLabels.contains(label);

            if (isCurrent) {
                btn.setColor(Color.YELLOW);
                btn.setDisabled(true);
            } else if (isVisited) {
                btn.setColor(Color.DARK_GRAY);
                btn.setDisabled(true);
            } else if (isReachable) {
                btn.setColor(Color.CYAN);
                btn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                        game.enterNode(node);
                    }
                });
            } else {
                btn.setColor(Color.GRAY);
                btn.setDisabled(true);
            }
            uiStage.addActor(btn);
        }
    }

    private static String describeType(NodeType t) {
        return switch (t) {
            case COMBAT -> "Chiến đấu";
            case EVENT -> "Sự kiện";
            case REST -> "Nghỉ ngơi";
            case REWARD -> "Phần thưởng";
            case ELITE -> "Elite";
            case MINIBOSS -> "Miniboss";
            case BOSS -> "BOSS";
        };
    }

    private static String describeNode(StageNode n) {
        return switch (n) {
            case CombatNode c -> "Chiến đấu (" + c.enemies().size() + ")";
            case EliteNode e -> "Elite (" + e.enemies().size() + ")";
            default -> describeType(n.type());
        };
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.06f, 0.07f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.45f, 0.45f, 0.55f, 1f);
        StageTree tree = run.stageTree();
        for (String from : tree.nodes().keySet()) {
            Vector2 a = nodeCenters.get(from);
            if (a == null) continue;
            for (String to : tree.edgesFrom(from)) {
                Vector2 b = nodeCenters.get(to);
                if (b == null) continue;
                shapes.line(a.x, a.y, b.x, b.y);
            }
        }
        shapes.end();

        uiStage.act(delta);
        uiStage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(uiStage);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        uiStage.dispose();
        skin.dispose();
        fontFactory.dispose();
        shapes.dispose();
    }
}
