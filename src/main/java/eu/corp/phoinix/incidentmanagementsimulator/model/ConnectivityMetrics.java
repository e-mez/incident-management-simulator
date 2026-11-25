package eu.corp.phoinix.incidentmanagementsimulator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectivityMetrics {
    private int totalNodes;
    private int activeNodes;
    private int failedNodes;
    private int isolatedOrDegradedNodes;
    private int totalEdges;
    private int activeEdges;

    /**
     * Giant Connected Component size (S): number of nodes in largest connected component
     */
    private int giantComponentSize;

    /**
     * Relative Giant Component Size P_inf = S / N
     */
    private double relativeGiantComponentSize;

    /**
     * Pairwise reachability ratio: fraction of ordered pairs (u, v) where path exists
     */
    private double reachabilityRatio;

    /**
     * Global network efficiency E_glob = (1 / N(N-1)) * sum(1 / d(u,v))
     */
    private double globalEfficiency;

    /**
     * Connectivity drop compared to pristine/baseline graph (percentage [0.0 - 1.0])
     */
    private double connectivityLoss;

    /**
     * Number of strongly connected components
     */
    private int stronglyConnectedComponentsCount;

    /**
     * Number of weakly connected components
     */
    private int weaklyConnectedComponentsCount;
}
