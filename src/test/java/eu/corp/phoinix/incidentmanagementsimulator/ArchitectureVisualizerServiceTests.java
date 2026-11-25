package eu.corp.phoinix.incidentmanagementsimulator;

import eu.corp.phoinix.incidentmanagementsimulator.model.ArchitectureGraph;
import eu.corp.phoinix.incidentmanagementsimulator.model.SimulationRequest;
import eu.corp.phoinix.incidentmanagementsimulator.model.SimulationResult;
import eu.corp.phoinix.incidentmanagementsimulator.service.ArchitectureVisualizerService;
import eu.corp.phoinix.incidentmanagementsimulator.service.PaymentArchitectureProvider;
import eu.corp.phoinix.incidentmanagementsimulator.service.PercolationEngine;
import eu.corp.phoinix.incidentmanagementsimulator.service.SimulatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectureVisualizerServiceTests {

    private ArchitectureVisualizerService visualizerService;
    private ArchitectureGraph graph;
    private SimulatorService simulatorService;

    @BeforeEach
    void setUp() {
        visualizerService = new ArchitectureVisualizerService();
        PaymentArchitectureProvider provider = new PaymentArchitectureProvider();
        graph = provider.getPaymentArchitecture();
        simulatorService = new SimulatorService(new PercolationEngine(), provider);
    }

    @Test
    @DisplayName("Generate SVG representation of healthy architecture")
    void testSvgGenerationHealthy() {
        String svg = visualizerService.generateSvgRepresentation(graph, null);
        assertNotNull(svg);
        assertTrue(svg.contains("<svg"), "Must contain SVG tag");
        assertTrue(svg.contains("Order"), "Must contain Order node text");
        assertTrue(svg.contains("Payment Gateway"), "Must contain Payment Gateway node text");
        assertTrue(svg.contains("Fraud Detection"), "Must contain Fraud Detection node text");
        assertTrue(svg.contains("Payment Database"), "Must contain Payment Database node text");
        assertTrue(svg.contains("Notification"), "Must contain Notification node text");
        assertTrue(svg.contains("External Payment Provider"), "Must contain External Payment Provider node text");
    }

    @Test
    @DisplayName("Generate SVG representation with failed nodes")
    void testSvgGenerationWithFailure() {
        SimulationRequest request = SimulationRequest.builder()
                .simulationType("TARGETED")
                .targetedServiceIds(Collections.singletonList("payment-db"))
                .enableCascades(true)
                .build();
        SimulationResult result = simulatorService.runSimulation(request, graph);

        String svg = visualizerService.generateSvgRepresentation(graph, result);
        assertNotNull(svg);
        assertTrue(svg.contains("FAILED (DIRECT)"), "Must render direct failure badge");
        assertTrue(svg.contains("FAILED (CASCADE)"), "Must render cascading failure badge");
    }

    @Test
    @DisplayName("Generate Mermaid diagram")
    void testMermaidGeneration() {
        String mermaid = visualizerService.generateMermaidDiagram(graph, null);
        assertNotNull(mermaid);
        assertTrue(mermaid.startsWith("graph LR"));
        assertTrue(mermaid.contains("payment_gateway"));
    }

    @Test
    @DisplayName("Generate ASCII representation")
    void testAsciiGeneration() {
        String ascii = visualizerService.generateAsciiRepresentation(graph, null);
        assertNotNull(ascii);
        assertTrue(ascii.contains("MICROSERVICE ARCHITECTURE"));
        assertTrue(ascii.contains("payment-gateway"));
    }
}
