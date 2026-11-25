package eu.corp.phoinix.incidentmanagementsimulator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeSimulationState {
    private String nodeId;
    private String nodeName;
    /**
     * Status: "OPERATIONAL", "DIRECT_FAILURE", "CASCADING_FAILURE", "ISOLATED"
     */
    private String status;
    private boolean operational;
    private String failureReason;
    private int inDegree;
    private int outDegree;
    private int reachableNodesCount;
    private double centralityScore;
}
