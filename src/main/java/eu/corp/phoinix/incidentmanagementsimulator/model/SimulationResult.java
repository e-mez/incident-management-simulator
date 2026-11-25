package eu.corp.phoinix.incidentmanagementsimulator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResult {
    private String simulationId;
    private String simulationType; // "RANDOM", "TARGETED", "MONTE_CARLO"
    private double configuredFailureProbability;
    private int iterations;
    private List<String> targetedServices;

    private ConnectivityMetrics baselineMetrics;
    private ConnectivityMetrics simulatedMetrics;

    private List<NodeSimulationState> nodeStates;
    private List<String> directlyFailedNodes;
    private List<String> cascadingFailedNodes;
    private List<String> operationalNodes;

    // Monte Carlo aggregated statistics (when iterations > 1)
    private Map<String, Object> monteCarloStatistics;

    // Critical impact summary
    private String summaryText;
}
