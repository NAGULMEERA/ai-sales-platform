package com.aisales.architecture;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Detects circular dependencies in a directed graph of Maven module artifactIds using a classic
 * depth-first search with a recursion-stack ("in progress") set. This is the platform-level
 * counterpart to the {@code common-testing} ArchUnit package-slice cycle check: it operates on
 * the Maven module graph (the granularity at which a cycle is an actual architecture defect -
 * Maven itself would refuse to build a real one), while the ArchUnit check operates on packages
 * within a single deployable service.
 */
final class CycleDetector {

    private CycleDetector() {
    }

    /**
     * @return the first cycle discovered, expressed as an ordered path of artifactIds where the
     *     first and last elements are the same node (e.g. {@code [a, b, c, a]}), or an empty list
     *     if the graph is acyclic.
     */
    static List<String> findCycle(Map<String, List<String>> graph) {
        Set<String> visited = new HashSet<>();
        Set<String> onStack = new LinkedHashSet<>();

        for (String node : graph.keySet()) {
            if (!visited.contains(node)) {
                List<String> cycle = depthFirstSearch(node, graph, visited, onStack);
                if (!cycle.isEmpty()) {
                    return cycle;
                }
            }
        }
        return List.of();
    }

    private static List<String> depthFirstSearch(
            String node, Map<String, List<String>> graph, Set<String> visited, Set<String> onStack) {
        visited.add(node);
        onStack.add(node);

        for (String neighbor : graph.getOrDefault(node, List.of())) {
            if (onStack.contains(neighbor)) {
                return extractCycle(onStack, neighbor);
            }
            if (!visited.contains(neighbor)) {
                List<String> cycle = depthFirstSearch(neighbor, graph, visited, onStack);
                if (!cycle.isEmpty()) {
                    return cycle;
                }
            }
        }

        onStack.remove(node);
        return List.of();
    }

    private static List<String> extractCycle(Set<String> onStack, String repeatedNode) {
        List<String> path = new ArrayList<>(onStack);
        int start = path.indexOf(repeatedNode);
        List<String> cycle = new ArrayList<>(path.subList(start, path.size()));
        cycle.add(repeatedNode);
        return cycle;
    }
}
