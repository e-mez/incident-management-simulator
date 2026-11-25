package eu.corp.phoinix.incidentmanagementsimulator.service;

import eu.corp.phoinix.incidentmanagementsimulator.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PercolationEngine {

    /**
     * Compute full connectivity metrics on the given graph with a set of active nodes and edges.
     */
    public ConnectivityMetrics computeConnectivityMetrics(ArchitectureGraph graph,
                                                         Set<String> activeNodeIds,
                                                         Set<String> activeEdgeKeys,
                                                         ConnectivityMetrics baseline) {
        int totalNodes = graph.getNodes().size();
        int activeCount = activeNodeIds.size();
        int failedCount = totalNodes - activeCount;
        int totalEdges = graph.getEdges().size();
        int activeEdgesCount = activeEdgeKeys != null ? activeEdgeKeys.size() : 0;

        if (activeCount == 0) {
            return ConnectivityMetrics.builder()
                    .totalNodes(totalNodes)
                    .activeNodes(0)
                    .failedNodes(totalNodes)
                    .isolatedOrDegradedNodes(0)
                    .totalEdges(totalEdges)
                    .activeEdges(0)
                    .giantComponentSize(0)
                    .relativeGiantComponentSize(0.0)
                    .reachabilityRatio(0.0)
                    .globalEfficiency(0.0)
                    .connectivityLoss(baseline != null && baseline.getReachabilityRatio() > 0 ? 1.0 : 0.0)
                    .stronglyConnectedComponentsCount(0)
                    .weaklyConnectedComponentsCount(0)
                    .build();
        }

        // Build active adjacency for directed and undirected checks
        Map<String, List<String>> activeDirectedAdj = new HashMap<>();
        Map<String, List<String>> activeUndirectedAdj = new HashMap<>();
        for (String nodeId : activeNodeIds) {
            activeDirectedAdj.put(nodeId, new ArrayList<>());
            activeUndirectedAdj.put(nodeId, new ArrayList<>());
        }

        for (DependencyEdge edge : graph.getEdges()) {
            String edgeKey = getEdgeKey(edge);
            if (activeNodeIds.contains(edge.getSource()) && activeNodeIds.contains(edge.getTarget())) {
                if (activeEdgeKeys == null || activeEdgeKeys.contains(edgeKey)) {
                    activeDirectedAdj.get(edge.getSource()).add(edge.getTarget());
                    if (!activeUndirectedAdj.get(edge.getSource()).contains(edge.getTarget())) {
                        activeUndirectedAdj.get(edge.getSource()).add(edge.getTarget());
                    }
                    if (!activeUndirectedAdj.get(edge.getTarget()).contains(edge.getSource())) {
                        activeUndirectedAdj.get(edge.getTarget()).add(edge.getSource());
                    }
                }
            }
        }

        // Weakly connected components (GCC in undirected active subgraph)
        List<Set<String>> wccList = findWeaklyConnectedComponents(activeNodeIds, activeUndirectedAdj);
        int gccSize = wccList.stream().mapToInt(Set::size).max().orElse(0);
        double relativeGccSize = totalNodes > 0 ? (double) gccSize / totalNodes : 0.0;

        // Strongly connected components
        List<Set<String>> sccList = findStronglyConnectedComponents(activeNodeIds, activeDirectedAdj);

        // All-pairs shortest paths for efficiency & reachability (BFS from each active node)
        int reachablePairs = 0;
        double sumInverseDistances = 0.0;
        int totalPossiblePairs = totalNodes > 1 ? totalNodes * (totalNodes - 1) : 1;

        for (String source : activeNodeIds) {
            Map<String, Integer> distances = shortestPathDistances(source, activeDirectedAdj);
            for (String target : activeNodeIds) {
                if (!source.equals(target)) {
                    if (distances.containsKey(target)) {
                        reachablePairs++;
                        int dist = distances.get(target);
                        if (dist > 0) {
                            sumInverseDistances += (1.0 / dist);
                        }
                    }
                }
            }
        }

        double reachabilityRatio = (double) reachablePairs / totalPossiblePairs;
        double globalEfficiency = sumInverseDistances / totalPossiblePairs;

        int isolatedCount = 0;
        for (String nodeId : activeNodeIds) {
            if (activeUndirectedAdj.get(nodeId).isEmpty()) {
                isolatedCount++;
            }
        }

        double connectivityLoss = 0.0;
        if (baseline != null && baseline.getReachabilityRatio() > 0) {
            connectivityLoss = Math.max(0.0, 1.0 - (reachabilityRatio / baseline.getReachabilityRatio()));
        }

        return ConnectivityMetrics.builder()
                .totalNodes(totalNodes)
                .activeNodes(activeCount)
                .failedNodes(failedCount)
                .isolatedOrDegradedNodes(isolatedCount)
                .totalEdges(totalEdges)
                .activeEdges(activeEdgesCount)
                .giantComponentSize(gccSize)
                .relativeGiantComponentSize(relativeGccSize)
                .reachabilityRatio(reachabilityRatio)
                .globalEfficiency(globalEfficiency)
                .connectivityLoss(connectivityLoss)
                .stronglyConnectedComponentsCount(sccList.size())
                .weaklyConnectedComponentsCount(wccList.size())
                .build();
    }

    public ConnectivityMetrics computeBaselineMetrics(ArchitectureGraph graph) {
        Set<String> allNodes = new HashSet<>(graph.getNodeIds());
        Set<String> allEdges = new HashSet<>();
        for (DependencyEdge edge : graph.getEdges()) {
            allEdges.add(getEdgeKey(edge));
        }
        return computeConnectivityMetrics(graph, allNodes, allEdges, null);
    }

    /**
     * Propagate cascading failures along critical dependencies.
     * If node v is failed, any node u that has a critical dependency u -> v fails.
     */
    public Set<String> resolveCascadingFailures(ArchitectureGraph graph,
                                                Set<String> directlyFailedNodes,
                                                boolean enableCascades) {
        Set<String> allFailed = new HashSet<>(directlyFailedNodes);
        if (!enableCascades) {
            return allFailed;
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (DependencyEdge edge : graph.getEdges()) {
                if (edge.isCritical()) {
                    String caller = edge.getSource();
                    String downstreamDep = edge.getTarget();
                    if (allFailed.contains(downstreamDep) && !allFailed.contains(caller)) {
                        allFailed.add(caller);
                        changed = true;
                    }
                }
            }
        }
        return allFailed;
    }

    /**
     * Compute Betweenness Centrality for all nodes using Brandes Algorithm.
     */
    public Map<String, Double> computeBetweennessCentrality(ArchitectureGraph graph) {
        List<String> nodes = graph.getNodeIds();
        Map<String, Double> betweenness = new HashMap<>();
        for (String v : nodes) {
            betweenness.put(v, 0.0);
        }

        Map<String, List<String>> adj = graph.getOutgoingAdjacency();

        for (String s : nodes) {
            Stack<String> stack = new Stack<>();
            Map<String, List<String>> predecessors = new HashMap<>();
            Map<String, Integer> sigma = new HashMap<>();
            Map<String, Integer> d = new HashMap<>();

            for (String w : nodes) {
                predecessors.put(w, new ArrayList<>());
                sigma.put(w, 0);
                d.put(w, -1);
            }

            sigma.put(s, 1);
            d.put(s, 0);

            Queue<String> queue = new LinkedList<>();
            queue.add(s);

            while (!queue.isEmpty()) {
                String v = queue.poll();
                stack.push(v);

                for (String w : adj.getOrDefault(v, Collections.emptyList())) {
                    // w found for the first time?
                    if (d.get(w) < 0) {
                        queue.add(w);
                        d.put(w, d.get(v) + 1);
                    }
                    // shortest path to w via v?
                    if (d.get(w) == d.get(v) + 1) {
                        sigma.put(w, sigma.get(w) + sigma.get(v));
                        predecessors.get(w).add(v);
                    }
                }
            }

            Map<String, Double> delta = new HashMap<>();
            for (String w : nodes) {
                delta.put(w, 0.0);
            }

            // S returns vertices in order of non-increasing distance from s
            while (!stack.isEmpty()) {
                String w = stack.pop();
                for (String v : predecessors.get(w)) {
                    double c = ((double) sigma.get(v) / (double) sigma.get(w)) * (1.0 + delta.get(w));
                    delta.put(v, delta.get(v) + c);
                }
                if (!w.equals(s)) {
                    betweenness.put(w, betweenness.get(w) + delta.get(w));
                }
            }
        }

        // Normalize betweenness
        int n = nodes.size();
        double normFactor = (n > 2) ? ((n - 1) * (n - 2)) : 1.0;
        for (String v : nodes) {
            betweenness.put(v, betweenness.get(v) / normFactor);
        }

        return betweenness;
    }

    /**
     * Compute Closeness Centrality for all nodes.
     */
    public Map<String, Double> computeClosenessCentrality(ArchitectureGraph graph) {
        List<String> nodes = graph.getNodeIds();
        Map<String, Double> closeness = new HashMap<>();
        Map<String, List<String>> adj = graph.getOutgoingAdjacency();
        int n = nodes.size();

        for (String u : nodes) {
            Map<String, Integer> dists = shortestPathDistances(u, adj);
            int reachable = 0;
            int totalDist = 0;
            for (String v : nodes) {
                if (!u.equals(v) && dists.containsKey(v)) {
                    reachable++;
                    totalDist += dists.get(v);
                }
            }
            if (reachable > 0 && totalDist > 0) {
                double rawCloseness = (double) reachable / totalDist;
                double normalized = rawCloseness * ((double) reachable / (n - 1));
                closeness.put(u, normalized);
            } else {
                closeness.put(u, 0.0);
            }
        }
        return closeness;
    }

    private List<Set<String>> findWeaklyConnectedComponents(Set<String> activeNodes,
                                                            Map<String, List<String>> undirectedAdj) {
        List<Set<String>> components = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (String node : activeNodes) {
            if (!visited.contains(node)) {
                Set<String> comp = new HashSet<>();
                Queue<String> queue = new LinkedList<>();
                queue.add(node);
                visited.add(node);

                while (!queue.isEmpty()) {
                    String curr = queue.poll();
                    comp.add(curr);
                    for (String neighbor : undirectedAdj.getOrDefault(curr, Collections.emptyList())) {
                        if (activeNodes.contains(neighbor) && !visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
                components.add(comp);
            }
        }
        return components;
    }

    private List<Set<String>> findStronglyConnectedComponents(Set<String> activeNodes,
                                                             Map<String, List<String>> directedAdj) {
        // Tarjan's algorithm for SCC
        List<Set<String>> sccList = new ArrayList<>();
        Map<String, Integer> index = new HashMap<>();
        Map<String, Integer> lowlink = new HashMap<>();
        Stack<String> stack = new Stack<>();
        Set<String> onStack = new HashSet<>();
        int[] currentIndex = {0};

        for (String node : activeNodes) {
            if (!index.containsKey(node)) {
                tarjanScc(node, directedAdj, activeNodes, index, lowlink, stack, onStack, currentIndex, sccList);
            }
        }
        return sccList;
    }

    private void tarjanScc(String u,
                           Map<String, List<String>> adj,
                           Set<String> activeNodes,
                           Map<String, Integer> index,
                           Map<String, Integer> lowlink,
                           Stack<String> stack,
                           Set<String> onStack,
                           int[] currentIndex,
                           List<Set<String>> sccList) {
        index.put(u, currentIndex[0]);
        lowlink.put(u, currentIndex[0]);
        currentIndex[0]++;
        stack.push(u);
        onStack.add(u);

        for (String v : adj.getOrDefault(u, Collections.emptyList())) {
            if (!activeNodes.contains(v)) continue;
            if (!index.containsKey(v)) {
                tarjanScc(v, adj, activeNodes, index, lowlink, stack, onStack, currentIndex, sccList);
                lowlink.put(u, Math.min(lowlink.get(u), lowlink.get(v)));
            } else if (onStack.contains(v)) {
                lowlink.put(u, Math.min(lowlink.get(u), index.get(v)));
            }
        }

        if (Objects.equals(lowlink.get(u), index.get(u))) {
            Set<String> scc = new HashSet<>();
            while (true) {
                String v = stack.pop();
                onStack.remove(v);
                scc.add(v);
                if (v.equals(u)) break;
            }
            sccList.add(scc);
        }
    }

    private Map<String, Integer> shortestPathDistances(String start, Map<String, List<String>> adj) {
        Map<String, Integer> dist = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        dist.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            String u = queue.poll();
            int d = dist.get(u);
            for (String v : adj.getOrDefault(u, Collections.emptyList())) {
                if (!dist.containsKey(v)) {
                    dist.put(v, d + 1);
                    queue.add(v);
                }
            }
        }
        return dist;
    }

    public static String getEdgeKey(DependencyEdge edge) {
        return edge.getSource() + "->" + edge.getTarget();
    }
}
