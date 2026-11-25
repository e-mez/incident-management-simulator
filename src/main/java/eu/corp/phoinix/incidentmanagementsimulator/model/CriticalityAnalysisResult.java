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
public class CriticalityAnalysisResult {
    private String architectureName;
    private int totalServices;
    private int totalDependencies;
    private ConnectivityMetrics pristineMetrics;

    /**
     * Ranked services from highest influence to lowest influence
     */
    private List<ServiceCriticality> rankedServices;

    /**
     * Estimated critical percolation threshold p_c
     */
    private double criticalPercolationThreshold;
    private String percolationPhaseDescription;

    /**
     * Services identified as Single Points of Failure
     */
    private List<String> singlePointsOfFailure;

    /**
     * Systemic resilience summary & architectural recommendations
     */
    private List<String> keyFindings;
    private List<String> recommendations;
}
