package eu.corp.phoinix.incidentmanagementsimulator;

import eu.corp.phoinix.incidentmanagementsimulator.model.ArchitectureGraph;
import eu.corp.phoinix.incidentmanagementsimulator.model.ConnectivityMetrics;
import eu.corp.phoinix.incidentmanagementsimulator.service.PaymentArchitectureProvider;
import eu.corp.phoinix.incidentmanagementsimulator.service.PercolationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PercolationEngineTests {

    private PercolationEngine engine;
    private ArchitectureGraph graph;

    @BeforeEach
    void setUp() {
        engine = new PercolationEngine();
        graph = new PaymentArchitectureProvider().getPaymentArchitecture();
    }

    @Test
    @DisplayName("Compute baseline connectivity metrics on pristine payment graph")
    void testBaselineMetrics() {
        ConnectivityMetrics metrics = engine.computeBaselineMetrics(graph);
        assertNotNull(metrics);
        assertEquals(6, metrics.getTotalNodes());
        assertEquals(6, metrics.getActiveNodes());
        assertEquals(0, metrics.getFailedNodes());
        assertEquals(6, metrics.getGiantComponentSize());
        assertEquals(1.0, metrics.getRelativeGiantComponentSize());
        assertTrue(metrics.getReachabilityRatio() > 0.0);
        assertTrue(metrics.getGlobalEfficiency() > 0.0);
        assertEquals(0.0, metrics.getConnectivityLoss());
    }

    @Test
    @DisplayName("Cascading failure resolution along critical dependencies")
    void testCascadingFailures() {
        // If Payment DB fails, Fraud Detection (which critically depends on DB) and Payment Gateway (critically depends on DB and Fraud) should cascade fail, and Order (critically depends on Gateway) should cascade fail
        Set<String> direct = Collections.singleton("payment-db");
        Set<String> allFailed = engine.resolveCascadingFailures(graph, direct, true);

        assertTrue(allFailed.contains("payment-db"));
        assertTrue(allFailed.contains("fraud-detection"), "Fraud detection should cascade fail due to DB dependency");
        assertTrue(allFailed.contains("payment-gateway"), "Payment gateway should cascade fail due to DB dependency");
        assertTrue(allFailed.contains("order-service"), "Order service should cascade fail due to Gateway dependency");

        // Non-critical notification service should NOT fail
        assertFalse(allFailed.contains("notification-service"), "Notification service is an asynchronous non-critical dependency");
    }

    @Test
    @DisplayName("Betweenness centrality calculation via Brandes algorithm")
    void testBetweennessCentrality() {
        Map<String, Double> betweenness = engine.computeBetweennessCentrality(graph);
        assertNotNull(betweenness);
        assertEquals(6, betweenness.size());

        // Payment Gateway is central transit hub in the architecture and should have highest or significant betweenness
        double gatewayBet = betweenness.getOrDefault("payment-gateway", 0.0);
        double notifBet = betweenness.getOrDefault("notification-service", 0.0);
        assertTrue(gatewayBet >= notifBet, "Payment Gateway should have higher or equal betweenness compared to leaf services");
    }

    @Test
    @DisplayName("Connectivity with zero active nodes")
    void testZeroActiveNodes() {
        ConnectivityMetrics metrics = engine.computeConnectivityMetrics(graph, Collections.emptySet(), Collections.emptySet(), null);
        assertEquals(0, metrics.getActiveNodes());
        assertEquals(6, metrics.getFailedNodes());
        assertEquals(0, metrics.getGiantComponentSize());
        assertEquals(0.0, metrics.getReachabilityRatio());
    }
}
