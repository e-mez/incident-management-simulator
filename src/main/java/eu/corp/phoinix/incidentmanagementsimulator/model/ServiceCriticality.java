package eu.corp.phoinix.incidentmanagementsimulator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCriticality {
    private int rank;
    private String serviceId;
    private String serviceName;
    private String serviceType;
    private int criticalTier;

    /**
     * Overall Influence Score (normalized [0.0, 100.0])
     */
    private double influenceScore;

    /**
     * Drop in Giant Component Size when this node is removed
     */
    private double giantComponentDrop;

    /**
     * Drop in Global Reachability when this node is removed (fraction 0.0 - 1.0)
     */
    private double reachabilityDrop;

    /**
     * Drop in Global Efficiency when this node is removed
     */
    private double efficiencyDrop;

    /**
     * Cascading Blast Radius: Number of downstream/dependent services crippled if this fails
     */
    private int cascadingBlastRadius;
    private List<String> cascadingAffectedServices;

    /**
     * Graph theoretical centrality metrics
     */
    private double betweennessCentrality;
    private double closenessCentrality;
    private int inDegree;
    private int outDegree;

    /**
     * Single Point of Failure (SPOF) indicator
     */
    private boolean isSinglePointOfFailure;
    private String riskLevel; // "CRITICAL", "HIGH", "MODERATE", "LOW"
    private String recommendation;
}
