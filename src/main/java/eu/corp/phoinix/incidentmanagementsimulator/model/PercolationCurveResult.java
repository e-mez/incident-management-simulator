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
public class PercolationCurveResult {
    private String architectureName;
    private int iterationsPerPoint;
    private double criticalThresholdEstimated;
    private List<PercolationPoint> points;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PercolationPoint {
        private double failureProbability; // p
        private double averageGiantComponentFraction; // <P_inf>
        private double averageReachabilityRatio;
        private double averageEfficiency;
        private double susceptibility; // variance in giant component / standard deviation
    }
}
