package eu.corp.phoinix.incidentmanagementsimulator;

import eu.corp.phoinix.incidentmanagementsimulator.model.CriticalityAnalysisResult;
import eu.corp.phoinix.incidentmanagementsimulator.model.PercolationCurveResult;
import eu.corp.phoinix.incidentmanagementsimulator.model.SimulationRequest;
import eu.corp.phoinix.incidentmanagementsimulator.model.SimulationResult;
import eu.corp.phoinix.incidentmanagementsimulator.service.PaymentArchitectureProvider;
import eu.corp.phoinix.incidentmanagementsimulator.service.PercolationEngine;
import eu.corp.phoinix.incidentmanagementsimulator.service.SimulatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SimulatorServiceTests {

    private SimulatorService simulatorService;

    @BeforeEach
    void setUp() {
        PercolationEngine engine = new PercolationEngine();
        PaymentArchitectureProvider provider = new PaymentArchitectureProvider();
        simulatorService = new SimulatorService(engine, provider);
    }

    @Test
    @DisplayName("Run random percolation failure simulation with zero failure probability")
    void testRandomSimulationZeroProb() {
        SimulationRequest request = SimulationRequest.builder()
                .simulationType("RANDOM")
                .failureProbability(0.0)
                .build();

        SimulationResult result = simulatorService.runSimulation(request);
        assertNotNull(result);
        assertEquals(0, result.getDirectlyFailedNodes().size());
        assertEquals(6, result.getOperationalNodes().size());
        assertEquals(6, result.getSimulatedMetrics().getGiantComponentSize());
        assertEquals(0.0, result.getSimulatedMetrics().getConnectivityLoss());
    }

    @Test
    @DisplayName("Run targeted failure simulation on Payment Gateway")
    void testTargetedFailurePaymentGateway() {
        SimulationRequest request = SimulationRequest.builder()
                .simulationType("TARGETED")
                .targetedServiceIds(Collections.singletonList("payment-gateway"))
                .enableCascades(true)
                .build();

        SimulationResult result = simulatorService.runSimulation(request);
        assertNotNull(result);
        assertTrue(result.getDirectlyFailedNodes().contains("payment-gateway"));
        assertTrue(result.getCascadingFailedNodes().contains("order-service"), "Order service should cascade fail when Payment Gateway fails");
        assertTrue(result.getSimulatedMetrics().getConnectivityLoss() > 0.0, "Connectivity should drop significantly");
    }

    @Test
    @DisplayName("Run Monte Carlo repeated random simulation")
    void testMonteCarloSimulation() {
        SimulationRequest request = SimulationRequest.builder()
                .simulationType("MONTE_CARLO")
                .failureProbability(0.3)
                .iterations(50)
                .seed(12345L)
                .build();

        SimulationResult result = simulatorService.runSimulation(request);
        assertNotNull(result);
        assertEquals("MONTE_CARLO", result.getSimulationType());
        assertEquals(50, result.getIterations());
        assertNotNull(result.getMonteCarloStatistics());
        assertTrue(result.getMonteCarloStatistics().containsKey("meanGccSize"));
    }

    @Test
    @DisplayName("Run critical dependency and influence analysis")
    void testCriticalDependencyAnalysis() {
        CriticalityAnalysisResult analysis = simulatorService.analyzeCriticalDependencies();
        assertNotNull(analysis);
        assertEquals(6, analysis.getRankedServices().size());
        assertFalse(analysis.getSinglePointsOfFailure().isEmpty());
        assertTrue(analysis.getCriticalPercolationThreshold() > 0.0);
        assertFalse(analysis.getKeyFindings().isEmpty());
        assertFalse(analysis.getRecommendations().isEmpty());

        // First ranked service should have highest influence score
        double topScore = analysis.getRankedServices().get(0).getInfluenceScore();
        double lastScore = analysis.getRankedServices().get(5).getInfluenceScore();
        assertTrue(topScore >= lastScore, "Services must be sorted in descending order of influence");
    }

    @Test
    @DisplayName("Generate percolation curve across failure probabilities")
    void testPercolationCurve() {
        PaymentArchitectureProvider provider = new PaymentArchitectureProvider();
        PercolationCurveResult curve = simulatorService.generatePercolationCurve(provider.getPaymentArchitecture(), 10, 20);
        assertNotNull(curve);
        assertEquals(11, curve.getPoints().size()); // 0 to 10 inclusive
        assertEquals(0.0, curve.getPoints().get(0).getFailureProbability());
        assertEquals(1.0, curve.getPoints().get(10).getFailureProbability());
        assertTrue(curve.getCriticalThresholdEstimated() >= 0.0 && curve.getCriticalThresholdEstimated() <= 1.0);
    }
}
