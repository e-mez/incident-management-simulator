package eu.corp.phoinix.incidentmanagementsimulator.controller;

import eu.corp.phoinix.incidentmanagementsimulator.model.ArchitectureGraph;
import eu.corp.phoinix.incidentmanagementsimulator.model.CriticalityAnalysisResult;
import eu.corp.phoinix.incidentmanagementsimulator.model.PercolationCurveResult;
import eu.corp.phoinix.incidentmanagementsimulator.service.PaymentArchitectureProvider;
import eu.corp.phoinix.incidentmanagementsimulator.service.SimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final SimulatorService simulatorService;
    private final PaymentArchitectureProvider architectureProvider;

    @GetMapping(value = "/critical-dependencies", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CriticalityAnalysisResult> getCriticalDependencyAnalysis() {
        return ResponseEntity.ok(simulatorService.analyzeCriticalDependencies());
    }

    @GetMapping(value = "/percolation-curve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PercolationCurveResult> getPercolationCurve(
            @RequestParam(defaultValue = "20") int steps,
            @RequestParam(defaultValue = "100") int iterations) {
        ArchitectureGraph graph = architectureProvider.getPaymentArchitecture();
        return ResponseEntity.ok(simulatorService.generatePercolationCurve(graph, steps, iterations));
    }
}
