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
public class SimulationRequest {
    /**
     * Type of simulation: "RANDOM" or "TARGETED" or "MONTE_CARLO"
     */
    @Builder.Default
    private String simulationType = "RANDOM";

    /**
     * Node failure probability p in [0.0, 1.0] for random percolation
     */
    @Builder.Default
    private double failureProbability = 0.2;

    /**
     * Edge failure probability (bond percolation) in [0.0, 1.0]
     */
    @Builder.Default
    private double edgeFailureProbability = 0.0;

    /**
     * Number of Monte Carlo iterations for repeated random simulation
     */
    @Builder.Default
    private int iterations = 1;

    /**
     * Specific service ID or list of IDs for targeted failure testing
     */
    private List<String> targetedServiceIds;

    /**
     * Enable cascading failure propagation along critical dependencies
     */
    @Builder.Default
    private boolean enableCascades = true;

    /**
     * Optional random seed for reproducible simulations
     */
    private Long seed;
}
