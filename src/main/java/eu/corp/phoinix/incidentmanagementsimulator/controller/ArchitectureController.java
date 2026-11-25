package eu.corp.phoinix.incidentmanagementsimulator.controller;

import eu.corp.phoinix.incidentmanagementsimulator.model.ArchitectureGraph;
import eu.corp.phoinix.incidentmanagementsimulator.service.ArchitectureVisualizerService;
import eu.corp.phoinix.incidentmanagementsimulator.service.PaymentArchitectureProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/architecture")
@RequiredArgsConstructor
public class ArchitectureController {

    private final PaymentArchitectureProvider architectureProvider;
    private final ArchitectureVisualizerService visualizerService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArchitectureGraph> getArchitecture() {
        return ResponseEntity.ok(architectureProvider.getPaymentArchitecture());
    }

    @GetMapping(value = "/visual", produces = "image/svg+xml")
    public ResponseEntity<String> getVisualSvg() {
        ArchitectureGraph graph = architectureProvider.getPaymentArchitecture();
        String svg = visualizerService.generateSvgRepresentation(graph, null);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(svg);
    }

    @GetMapping(value = "/mermaid", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getMermaid() {
        ArchitectureGraph graph = architectureProvider.getPaymentArchitecture();
        return ResponseEntity.ok(visualizerService.generateMermaidDiagram(graph, null));
    }

    @GetMapping(value = "/ascii", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getAscii() {
        ArchitectureGraph graph = architectureProvider.getPaymentArchitecture();
        return ResponseEntity.ok(visualizerService.generateAsciiRepresentation(graph, null));
    }
}
