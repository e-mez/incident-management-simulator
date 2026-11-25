package eu.corp.phoinix.incidentmanagementsimulator;

import eu.corp.phoinix.incidentmanagementsimulator.model.ArchitectureGraph;
import eu.corp.phoinix.incidentmanagementsimulator.model.ServiceNode;
import eu.corp.phoinix.incidentmanagementsimulator.service.PaymentArchitectureProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectureModelTests {

    private PaymentArchitectureProvider provider;

    @BeforeEach
    void setUp() {
        provider = new PaymentArchitectureProvider();
    }

    @Test
    @DisplayName("Verify that all 6 required payment services exist in the architecture")
    void testRequiredServicesExist() {
        ArchitectureGraph graph = provider.getPaymentArchitecture();
        assertNotNull(graph);
        assertEquals(6, graph.getNodes().size(), "Must contain exactly the 6 required services");

        List<String> nodeNames = graph.getNodes().stream().map(ServiceNode::getName).toList();
        assertTrue(nodeNames.contains("Order"), "Must contain Order service");
        assertTrue(nodeNames.contains("Payment Gateway"), "Must contain Payment Gateway");
        assertTrue(nodeNames.contains("Fraud Detection"), "Must contain Fraud Detection");
        assertTrue(nodeNames.contains("Payment Database"), "Must contain Payment Database");
        assertTrue(nodeNames.contains("Notification"), "Must contain Notification");
        assertTrue(nodeNames.contains("External Payment Provider"), "Must contain External Payment Provider");
    }

    @Test
    @DisplayName("Verify directed dependencies and connectivity")
    void testDependenciesStructure() {
        ArchitectureGraph graph = provider.getPaymentArchitecture();
        assertFalse(graph.getEdges().isEmpty());

        // Verify Order depends on Payment Gateway
        boolean orderToGateway = graph.getEdges().stream()
                .anyMatch(e -> "order-service".equals(e.getSource()) && "payment-gateway".equals(e.getTarget()));
        assertTrue(orderToGateway, "Order must connect to Payment Gateway");

        // Verify Payment Gateway depends on Fraud Detection, DB, and External Provider
        boolean gatewayToFraud = graph.getEdges().stream()
                .anyMatch(e -> "payment-gateway".equals(e.getSource()) && "fraud-detection".equals(e.getTarget()));
        boolean gatewayToDb = graph.getEdges().stream()
                .anyMatch(e -> "payment-gateway".equals(e.getSource()) && "payment-db".equals(e.getTarget()));
        boolean gatewayToExternal = graph.getEdges().stream()
                .anyMatch(e -> "payment-gateway".equals(e.getSource()) && "external-payment-provider".equals(e.getTarget()));

        assertTrue(gatewayToFraud);
        assertTrue(gatewayToDb);
        assertTrue(gatewayToExternal);
    }
}
