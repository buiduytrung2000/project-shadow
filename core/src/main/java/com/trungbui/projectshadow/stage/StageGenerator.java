package com.trungbui.projectshadow.stage;

import com.fasterxml.jackson.databind.JsonNode;
import com.trungbui.projectshadow.data.model.StageConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class StageGenerator {

    public static StageTree generate(StageConfig config, long seed) {
        Random rng = new Random(seed);
        StageTree tree = new StageTree(config.id(), seed);
        StageRules rules = StageRules.parse(config.rules());
        JsonNode layout = config.layout();
        JsonNode pools = config.pools();

        StageNode firstNode = generateFirstNode(layout.path("first_node_fixed"), rng);
        tree.addNode(firstNode);
        List<StageNode> prevLayer = List.of(firstNode);

        int eliteCount = 0;
        boolean minibossInserted = false;
        JsonNode minibossSpec = layout.path("miniboss_layer");
        boolean hasMiniboss = !minibossSpec.isMissingNode();

        List<LayerSpec> middleLayers = collectMiddleLayers(layout);
        for (LayerSpec layer : middleLayers) {
            if (hasMiniboss && !minibossInserted && expectsMiniboss(layer)) {
                prevLayer = generateMinibossLayer(minibossSpec, prevLayer, tree, rng);
                minibossInserted = true;
            }
            List<StageNode> currentLayer = generateMiddleLayer(
                    layer, prevLayer, tree, pools, rules, rng, eliteCount);
            for (StageNode n : currentLayer) {
                if (n.type() == NodeType.ELITE) eliteCount++;
            }
            prevLayer = currentLayer;
        }

        if (hasMiniboss && !minibossInserted) {
            prevLayer = generateMinibossLayer(minibossSpec, prevLayer, tree, rng);
        }

        BossNode boss = generateBossNode(layout.path("last_node_fixed"));
        tree.addNode(boss);
        for (StageNode prev : prevLayer) {
            tree.addEdge(prev.label(), boss.label());
        }

        applyMaxCombatRule(tree, rules, pools, rng);

        return tree;
    }

    private static StageNode generateFirstNode(JsonNode firstSpec, Random rng) {
        String label = firstSpec.path("label").asText("L1.A");
        return generateCombatNode(label, firstSpec, rng, false);
    }

    private static BossNode generateBossNode(JsonNode bossSpec) {
        String label = bossSpec.path("label").asText("BOSS");
        String bossId = bossSpec.path("boss_id").asText();
        return new BossNode(label, bossId);
    }

    private static List<StageNode> generateMinibossLayer(
            JsonNode minibossSpec, List<StageNode> prevLayer, StageTree tree, Random rng) {
        String label = minibossSpec.path("name").asText("MB");
        JsonNode pool = minibossSpec.path("miniboss_pool");
        String minibossId = pickFromArray(pool, rng);
        if (minibossId == null) minibossId = "miniboss_unknown";
        MinibossNode mb = new MinibossNode(label, minibossId);
        tree.addNode(mb);
        for (StageNode prev : prevLayer) {
            tree.addEdge(prev.label(), mb.label());
        }
        return List.of(mb);
    }

    private static List<StageNode> generateMiddleLayer(
            LayerSpec layer,
            List<StageNode> prevLayer,
            StageTree tree,
            JsonNode pools,
            StageRules rules,
            Random rng,
            int currentEliteCount
    ) {
        List<StageNode> currentLayer = new ArrayList<>(layer.numNodes());
        int localEliteCount = currentEliteCount;
        Integer eliteCap = rules.eliteNodeMaxPerRun();

        for (int i = 0; i < layer.numNodes(); i++) {
            String label = layer.name() + "." + (char) ('A' + i);
            NodeType type = pickWeightedType(layer.weights(), rng);
            type = enforceNoConsecutive(type, prevLayer, rules, rng);

            if (type == NodeType.ELITE && eliteCap != null && localEliteCount >= eliteCap) {
                type = NodeType.COMBAT;
            }

            StageNode node = generateNodeContent(type, label, pools, rng);
            if (type == NodeType.ELITE) localEliteCount++;
            tree.addNode(node);
            currentLayer.add(node);
        }

        for (StageNode prev : prevLayer) {
            for (StageNode cur : currentLayer) {
                tree.addEdge(prev.label(), cur.label());
            }
        }
        return currentLayer;
    }

    private static StageNode generateNodeContent(NodeType type, String label, JsonNode pools, Random rng) {
        return switch (type) {
            case COMBAT -> generateCombatNode(label, pools.path("combat").path("default"), rng, false);
            case EVENT -> generateEventNode(label, pools.path("event"), rng);
            case REST -> generateRestNode(label, pools.path("rest"), rng);
            case REWARD -> generateRewardNode(label, pools.path("reward"), rng);
            case ELITE -> generateEliteNode(label, pools.path("combat").path("elite"), rng);
            default -> throw new IllegalStateException("Cannot generate content for type: " + type);
        };
    }

    private static CombatNode generateCombatNode(String label, JsonNode pool, Random rng, boolean preBoss) {
        int min = pool.path("min_enemies").asInt(1);
        int max = pool.path("max_enemies").asInt(min);
        int n = max > min ? min + rng.nextInt(max - min + 1) : min;
        JsonNode enemies = pool.path("enemies").isMissingNode() ? pool.path("enemy_pool") : pool.path("enemies");
        List<String> picked = new ArrayList<>(n);
        if (enemies.isArray() && !enemies.isEmpty()) {
            for (int i = 0; i < n; i++) {
                picked.add(enemies.get(rng.nextInt(enemies.size())).asText());
            }
        }
        List<String> variants = jsonArrayToList(pool.path("variants_allowed"));
        return new CombatNode(label, picked, variants, preBoss);
    }

    private static EventNode generateEventNode(String label, JsonNode eventPool, Random rng) {
        JsonNode weights = eventPool.path("weights");
        String eventId = pickWeighted(weights, rng);
        if (eventId == null) {
            JsonNode poolList = eventPool.path("stage_1_pool");
            if (poolList.isMissingNode()) poolList = eventPool.path("stage_2_pool");
            if (poolList.isMissingNode()) poolList = eventPool.path("stage_3_pool");
            eventId = pickFromArray(poolList, rng);
        }
        if (eventId == null) eventId = "event_unknown";
        return new EventNode(label, eventId);
    }

    private static RestNode generateRestNode(String label, JsonNode restPool, Random rng) {
        JsonNode optionsArr = restPool.path("options");
        List<RestOption> options = new ArrayList<>();
        if (optionsArr.isArray()) {
            for (JsonNode o : optionsArr) {
                options.add(new RestOption(
                        o.path("label").asText(""),
                        o.path("effect").asText(""),
                        o.path("target").asText(""),
                        o.path("value_min").asInt(0),
                        o.path("value_max").asInt(0)
                ));
            }
        }
        return new RestNode(label, options);
    }

    private static RewardNode generateRewardNode(String label, JsonNode rewardPool, Random rng) {
        int rolls = rewardPool.path("rolls").asInt(1);
        JsonNode dropTable = rewardPool.path("drop_table");
        List<DropEntry> drops = new ArrayList<>();
        for (int i = 0; i < rolls; i++) {
            DropEntry d = pickDrop(dropTable, rng);
            if (d != null) drops.add(d);
        }
        return new RewardNode(label, drops);
    }

    private static EliteNode generateEliteNode(String label, JsonNode elitePool, Random rng) {
        if (elitePool.isMissingNode()) {
            return new EliteNode(label, List.of(), List.of(), 1.5, 1.2, 0, 0d);
        }
        int min = elitePool.path("min_enemies").asInt(1);
        int max = elitePool.path("max_enemies").asInt(min);
        int n = max > min ? min + rng.nextInt(max - min + 1) : min;
        JsonNode enemies = elitePool.path("enemies");
        List<String> picked = new ArrayList<>(n);
        if (enemies.isArray() && !enemies.isEmpty()) {
            for (int i = 0; i < n; i++) {
                picked.add(enemies.get(rng.nextInt(enemies.size())).asText());
            }
        }
        List<String> variants = jsonArrayToList(elitePool.path("variants_allowed"));
        JsonNode statMods = elitePool.path("stat_modifiers");
        JsonNode rewards = elitePool.path("rewards");
        return new EliteNode(
                label, picked, variants,
                statMods.path("hp_mult").asDouble(1.5),
                statMods.path("dmg_mult").asDouble(1.2),
                rewards.path("gold_bonus").asInt(0),
                rewards.path("item_drop_chance").asDouble(0d)
        );
    }

    private static DropEntry pickDrop(JsonNode dropTable, Random rng) {
        if (!dropTable.isArray() || dropTable.isEmpty()) return null;
        int total = 0;
        for (JsonNode entry : dropTable) total += entry.path("weight").asInt(1);
        if (total <= 0) return null;
        int roll = rng.nextInt(total);
        int acc = 0;
        for (JsonNode entry : dropTable) {
            acc += entry.path("weight").asInt(1);
            if (roll < acc) {
                return new DropEntry(
                        entry.path("type").asText(),
                        entry.has("item_id") ? entry.path("item_id").asText() : null,
                        entry.has("category") ? entry.path("category").asText() : null,
                        entry.has("value_min") ? entry.path("value_min").asInt() : null,
                        entry.has("value_max") ? entry.path("value_max").asInt() : null
                );
            }
        }
        return null;
    }

    private static NodeType pickWeightedType(JsonNode weights, Random rng) {
        String picked = pickWeighted(weights, rng);
        if (picked == null) return NodeType.COMBAT;
        return NodeType.parse(picked);
    }

    private static String pickWeighted(JsonNode weights, Random rng) {
        if (weights == null || !weights.isObject() || weights.isEmpty()) return null;
        Map<String, Integer> w = new LinkedHashMap<>();
        int total = 0;
        var iter = weights.fields();
        while (iter.hasNext()) {
            var e = iter.next();
            int wv = e.getValue().asInt(0);
            if (wv > 0) {
                w.put(e.getKey(), wv);
                total += wv;
            }
        }
        if (total <= 0) return null;
        int roll = rng.nextInt(total);
        int acc = 0;
        for (var e : w.entrySet()) {
            acc += e.getValue();
            if (roll < acc) return e.getKey();
        }
        return w.keySet().iterator().next();
    }

    private static String pickFromArray(JsonNode arr, Random rng) {
        if (arr == null || !arr.isArray() || arr.isEmpty()) return null;
        return arr.get(rng.nextInt(arr.size())).asText();
    }

    private static NodeType enforceNoConsecutive(NodeType type, List<StageNode> prev, StageRules rules, Random rng) {
        if (rules.noConsecutiveRest() && type == NodeType.REST
                && prev.stream().allMatch(p -> p.type() == NodeType.REST)) {
            return NodeType.EVENT;
        }
        if (rules.noConsecutiveReward() && type == NodeType.REWARD
                && prev.stream().allMatch(p -> p.type() == NodeType.REWARD)) {
            return NodeType.COMBAT;
        }
        if (rules.noConsecutiveEventUnless30() && type == NodeType.EVENT
                && prev.stream().allMatch(p -> p.type() == NodeType.EVENT)
                && rng.nextDouble() > 0.3) {
            return NodeType.COMBAT;
        }
        return type;
    }

    private static void applyMaxCombatRule(StageTree tree, StageRules rules, JsonNode pools, Random rng) {
        if (rules.maxCombatPerRun() == null) return;
        int max = rules.maxCombatPerRun();
        StageNode start = tree.firstNode().orElse(null);
        StageNode boss = tree.bossNode().orElse(null);
        if (start == null || boss == null) return;

        List<List<String>> paths = tree.allPaths(start.label(), boss.label());
        for (List<String> path : paths) {
            int combatCount = tree.countOnPath(path, NodeType.COMBAT);
            while (combatCount > max) {
                String victim = findReplaceableCombat(tree, path);
                if (victim == null) break;
                NodeType replaceType = rng.nextBoolean() ? NodeType.REST : NodeType.REWARD;
                StageNode replacement = generateNodeContent(replaceType, victim, pools, rng);
                tree.replaceNode(victim, replacement);
                combatCount--;
            }
        }
    }

    private static String findReplaceableCombat(StageTree tree, List<String> path) {
        for (int i = path.size() - 1; i >= 0; i--) {
            String label = path.get(i);
            StageNode n = tree.getNode(label).orElseThrow();
            if (n.type() == NodeType.COMBAT && !(n instanceof CombatNode cn && cn.preBoss())) {
                return label;
            }
        }
        return null;
    }

    private static List<LayerSpec> collectMiddleLayers(JsonNode layout) {
        List<LayerSpec> out = new ArrayList<>();
        JsonNode middle = layout.path("middle_layers");
        if (middle.isArray()) {
            for (JsonNode layer : middle) {
                String name = layer.path("name").asText("L");
                int n = layer.path("num_nodes").asInt(2);
                JsonNode weights = layer.path("node_generation").path("weights");
                String edgesIn = layer.path("edges_in").asText("");
                out.add(new LayerSpec(name, n, weights, edgesIn));
            }
            return out;
        }
        for (int i = 2; ; i++) {
            JsonNode layer = layout.path("layer_" + i);
            if (layer.isMissingNode()) break;
            String name = layer.has("label_prefix")
                    ? layer.path("label_prefix").asText()
                    : "L" + i;
            int n = layer.path("num_nodes").asInt(2);
            JsonNode weights = layer.path("node_generation").path("weights");
            String edgesIn = layer.path("edges_in").asText("");
            out.add(new LayerSpec(name, n, weights, edgesIn));
        }
        return out;
    }

    private static boolean expectsMiniboss(LayerSpec layer) {
        return layer.edgesIn() != null && layer.edgesIn().contains("miniboss");
    }

    private static List<String> jsonArrayToList(JsonNode arr) {
        if (!arr.isArray()) return List.of();
        List<String> out = new ArrayList<>(arr.size());
        for (JsonNode n : arr) out.add(n.asText());
        return out;
    }

    private record LayerSpec(String name, int numNodes, JsonNode weights, String edgesIn) {
    }

    private StageGenerator() {
    }
}
