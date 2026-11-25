package eu.corp.phoinix.incidentmanagementsimulator.controller;

import eu.corp.phoinix.incidentmanagementsimulator.model.ArchitectureGraph;
import eu.corp.phoinix.incidentmanagementsimulator.model.SimulationRequest;
import eu.corp.phoinix.incidentmanagementsimulator.model.SimulationResult;
import eu.corp.phoinix.incidentmanagementsimulator.service.ArchitectureVisualizerService;
import eu.corp.phoinix.incidentmanagementsimulator.service.PaymentArchitectureProvider;
import eu.corp.phoinix.incidentmanagementsimulator.service.SimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulatorService simulatorService;
    private final PaymentArchitectureProvider architectureProvider;
    private final ArchitectureVisualizerService visualizerService;

    @PostMapping(value = "/run", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimulationResult> runSimulation(@RequestBody SimulationRequest request) {
        return ResponseEntity.ok(simulatorService.runSimulation(request));
    }

    @PostMapping(value = "/random", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimulationResult> runRandomSimulation(
            @RequestParam(defaultValue = "0.2") double probability,
            @RequestParam(defaultValue = "1") int iterations,
            @RequestParam(defaultValue = "true") boolean enableCascades,
            @RequestParam(required = false) Long seed) {

        SimulationRequest request = SimulationRequest.builder()
                .simulationType(iterations > 1 ? "MONTE_CARLO" : "RANDOM")
                .failureProbability(probability)
                .iterations(iterations)
                .enableCascades(enableCascades)
                .seed(seed)
                .build();

        return ResponseEntity.ok(simulatorService.runSimulation(request));
    }

    @PostMapping(value = "/targeted", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimulationResult> runTargetedSimulation(
            @RequestParam String serviceId,
            @RequestParam(defaultValue = "true") boolean enableCascades) {

        SimulationRequest request = SimulationRequest.builder()
                .simulationType("TARGETED")
                .targetedServiceIds(Collections.singletonList(serviceId))
                .enableCascades(enableCascades)
                .build();

        return ResponseEntity.ok(simulatorService.runSimulation(request));
    }

    @PostMapping(value = "/visual", consumes = MediaType.APPLICATION_JSON_VALUE, produces = "image/svg+xml")
    public ResponseEntity<String> getSimulatedVisual(@RequestBody SimulationRequest request) {
        ArchitectureGraph graph = architectureProvider.getPaymentArchitecture();
        SimulationResult result = simulatorService.runSimulation(request, graph);
        String svg = visualizerService.generateSvgRepresentation(graph, result);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(svg);
    }
}
