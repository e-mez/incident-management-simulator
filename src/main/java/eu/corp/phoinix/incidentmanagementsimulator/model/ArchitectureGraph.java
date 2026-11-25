package eu.corp.phoinix.incidentmanagementsimulator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchitectureGraph {
    private String name;
    private String description;
    @Builder.Default
    private List<ServiceNode> nodes = new ArrayList<>();
    @Builder.Default
    private List<DependencyEdge> edges = new ArrayList<>();

    public ServiceNode getNode(String id) {
        if (nodes == null) return null;
        return nodes.stream()
                .filter(n -> n.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    public List<String> getNodeIds() {
        if (nodes == null) return Collections.emptyList();
        return nodes.stream().map(ServiceNode::getId).toList();
    }

    public Map<String, List<String>> getOutgoingAdjacency() {
        Map<String, List<String>> adj = new HashMap<>();
        for (ServiceNode node : nodes) {
            adj.put(node.getId(), new ArrayList<>());
        }
        for (DependencyEdge edge : edges) {
            if (adj.containsKey(edge.getSource())) {
                adj.get(edge.getSource()).add(edge.getTarget());
            }
        }
        return adj;
    }

    public Map<String, List<String>> getIncomingAdjacency() {
        Map<String, List<String>> adj = new HashMap<>();
        for (ServiceNode node : nodes) {
            adj.put(node.getId(), new ArrayList<>());
        }
        for (DependencyEdge edge : edges) {
            if (adj.containsKey(edge.getTarget())) {
                adj.get(edge.getTarget()).add(edge.getSource());
            }
        }
        return adj;
    }

    public Map<String, List<String>> getUndirectedAdjacency() {
        Map<String, List<String>> adj = new HashMap<>();
        for (ServiceNode node : nodes) {
            adj.put(node.getId(), new ArrayList<>());
        }
        for (DependencyEdge edge : edges) {
            if (adj.containsKey(edge.getSource()) && adj.containsKey(edge.getTarget())) {
                if (!adj.get(edge.getSource()).contains(edge.getTarget())) {
                    adj.get(edge.getSource()).add(edge.getTarget());
                }
                if (!adj.get(edge.getTarget()).contains(edge.getSource())) {
                    adj.get(edge.getTarget()).add(edge.getSource());
                }
            }
        }
        return adj;
    }
}
