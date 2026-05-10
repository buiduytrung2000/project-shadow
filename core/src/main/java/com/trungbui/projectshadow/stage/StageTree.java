package com.trungbui.projectshadow.stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StageTree {

    private final String stageId;
    private final long seed;
    private final Map<String, StageNode> nodes = new LinkedHashMap<>();
    private final Map<String, List<String>> edges = new LinkedHashMap<>();

    public StageTree(String stageId, long seed) {
        if (stageId == null || stageId.isBlank()) {
            throw new IllegalArgumentException("stageId must not be blank");
        }
        this.stageId = stageId;
        this.seed = seed;
    }

    public String stageId() {
        return stageId;
    }

    public long seed() {
        return seed;
    }

    public void addNode(StageNode node) {
        if (nodes.containsKey(node.label())) {
            throw new IllegalArgumentException("Duplicate node label: " + node.label());
        }
        nodes.put(node.label(), node);
        edges.put(node.label(), new ArrayList<>());
    }

    public void replaceNode(String label, StageNode replacement) {
        if (!nodes.containsKey(label)) {
            throw new IllegalArgumentException("Node not found: " + label);
        }
        if (!label.equals(replacement.label())) {
            throw new IllegalArgumentException(
                    "Replacement label must match. expected=" + label + ", got=" + replacement.label());
        }
        nodes.put(label, replacement);
    }

    public void addEdge(String from, String to) {
        if (!nodes.containsKey(from)) {
            throw new IllegalArgumentException("From-node not found: " + from);
        }
        if (!nodes.containsKey(to)) {
            throw new IllegalArgumentException("To-node not found: " + to);
        }
        List<String> outs = edges.get(from);
        if (!outs.contains(to)) outs.add(to);
    }

    public Optional<StageNode> getNode(String label) {
        return Optional.ofNullable(nodes.get(label));
    }

    public Map<String, StageNode> nodes() {
        return Collections.unmodifiableMap(nodes);
    }

    public List<String> edgesFrom(String label) {
        return Collections.unmodifiableList(edges.getOrDefault(label, List.of()));
    }

    public int size() {
        return nodes.size();
    }

    public List<StageNode> nodesByType(NodeType type) {
        List<StageNode> out = new ArrayList<>();
        for (StageNode n : nodes.values()) {
            if (n.type() == type) out.add(n);
        }
        return out;
    }

    public Optional<StageNode> firstNode() {
        return nodes.values().stream().findFirst();
    }

    public Optional<StageNode> bossNode() {
        return nodesByType(NodeType.BOSS).stream().findFirst();
    }

    public boolean hasPath(String fromLabel, String toLabel) {
        if (!nodes.containsKey(fromLabel) || !nodes.containsKey(toLabel)) return false;
        if (fromLabel.equals(toLabel)) return true;
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();
        java.util.HashSet<String> visited = new java.util.HashSet<>();
        queue.add(fromLabel);
        visited.add(fromLabel);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            for (String next : edges.getOrDefault(cur, List.of())) {
                if (next.equals(toLabel)) return true;
                if (visited.add(next)) queue.add(next);
            }
        }
        return false;
    }

    public List<List<String>> allPaths(String fromLabel, String toLabel) {
        List<List<String>> result = new ArrayList<>();
        if (!nodes.containsKey(fromLabel) || !nodes.containsKey(toLabel)) return result;
        List<String> current = new ArrayList<>();
        current.add(fromLabel);
        dfsPaths(fromLabel, toLabel, current, result);
        return result;
    }

    private void dfsPaths(String cur, String target, List<String> path, List<List<String>> result) {
        if (cur.equals(target)) {
            result.add(new ArrayList<>(path));
            return;
        }
        for (String next : edges.getOrDefault(cur, List.of())) {
            if (path.contains(next)) continue;
            path.add(next);
            dfsPaths(next, target, path, result);
            path.remove(path.size() - 1);
        }
    }

    public int countOnPath(List<String> path, NodeType type) {
        int c = 0;
        for (String label : path) {
            StageNode n = nodes.get(label);
            if (n != null && n.type() == type) c++;
        }
        return c;
    }
}
