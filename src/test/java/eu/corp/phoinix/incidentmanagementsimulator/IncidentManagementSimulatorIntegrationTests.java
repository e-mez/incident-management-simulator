package eu.corp.phoinix.incidentmanagementsimulator;

import eu.corp.phoinix.incidentmanagementsimulator.controller.AnalysisController;
import eu.corp.phoinix.incidentmanagementsimulator.controller.ArchitectureController;
import eu.corp.phoinix.incidentmanagementsimulator.controller.SimulationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.*;

@SpringBootTest
class IncidentManagementSimulatorIntegrationTests {

    @Autowired
    private ArchitectureController architectureController;

    @Autowired
    private SimulationController simulationController;

    @Autowired
    private AnalysisController analysisController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(architectureController, simulationController, analysisController).build();
    }

    @Test
    @DisplayName("GET /api/architecture returns payment processing architecture JSON")
    void testGetArchitecture() throws Exception {
        mockMvc.perform(get("/api/architecture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", containsString("Payment Processing Microservice Architecture")))
                .andExpect(jsonPath("$.nodes", hasSize(6)))
                .andExpect(jsonPath("$.nodes[?(@.id == 'order-service')].name").value("Order"))
                .andExpect(jsonPath("$.nodes[?(@.id == 'payment-gateway')].name").value("Payment Gateway"))
                .andExpect(jsonPath("$.nodes[?(@.id == 'fraud-detection')].name").value("Fraud Detection"))
                .andExpect(jsonPath("$.nodes[?(@.id == 'payment-db')].name").value("Payment Database"))
                .andExpect(jsonPath("$.nodes[?(@.id == 'notification-service')].name").value("Notification"))
                .andExpect(jsonPath("$.nodes[?(@.id == 'external-payment-provider')].name").value("External Payment Provider"));
    }

    @Test
    @DisplayName("GET /api/architecture/visual returns SVG visual representation")
    void testGetVisualSvg() throws Exception {
        mockMvc.perform(get("/api/architecture/visual"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"))
                .andExpect(content().string(containsString("<svg")));
    }

    @Test
    @DisplayName("POST /api/simulation/random runs random failure simulation")
    void testPostSimulationRandom() throws Exception {
        mockMvc.perform(post("/api/simulation/random")
                        .param("probability", "0.3")
                        .param("iterations", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulatedMetrics.giantComponentSize").exists())
                .andExpect(jsonPath("$.simulatedMetrics.relativeGiantComponentSize").exists());
    }

    @Test
    @DisplayName("POST /api/simulation/targeted runs targeted service failure testing")
    void testPostSimulationTargeted() throws Exception {
        mockMvc.perform(post("/api/simulation/targeted")
                        .param("serviceId", "payment-gateway")
                        .param("enableCascades", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directlyFailedNodes", hasItem("payment-gateway")))
                .andExpect(jsonPath("$.cascadingFailedNodes", hasItem("order-service")));
    }

    @Test
    @DisplayName("GET /api/analysis/critical-dependencies calculates connectivity impact and influence ranking")
    void testGetCriticalDependencies() throws Exception {
        mockMvc.perform(get("/api/analysis/critical-dependencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rankedServices", hasSize(6)))
                .andExpect(jsonPath("$.rankedServices[0].influenceScore").isNumber())
                .andExpect(jsonPath("$.criticalPercolationThreshold").isNumber())
                .andExpect(jsonPath("$.singlePointsOfFailure").isArray());
    }

    @Test
    @DisplayName("GET /api/analysis/percolation-curve computes percolation phase transition")
    void testGetPercolationCurve() throws Exception {
        mockMvc.perform(get("/api/analysis/percolation-curve")
                        .param("steps", "10")
                        .param("iterations", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", hasSize(11)))
                .andExpect(jsonPath("$.criticalThresholdEstimated").isNumber());
    }
}
