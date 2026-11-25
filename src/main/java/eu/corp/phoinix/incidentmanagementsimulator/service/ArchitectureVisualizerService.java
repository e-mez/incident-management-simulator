package eu.corp.phoinix.incidentmanagementsimulator.service;

import eu.corp.phoinix.incidentmanagementsimulator.model.ArchitectureGraph;
import eu.corp.phoinix.incidentmanagementsimulator.model.DependencyEdge;
import eu.corp.phoinix.incidentmanagementsimulator.model.NodeSimulationState;
import eu.corp.phoinix.incidentmanagementsimulator.model.ServiceNode;
import eu.corp.phoinix.incidentmanagementsimulator.model.SimulationResult;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ArchitectureVisualizerService {

    public String generateSvgRepresentation(ArchitectureGraph graph, SimulationResult simResult) {
        StringBuilder svg = new StringBuilder();
        int width = 1000;
        int height = 500;

        Map<String, String> nodeStatusMap = new HashMap<>();
        if (simResult != null && simResult.getNodeStates() != null) {
            for (NodeSimulationState state : simResult.getNodeStates()) {
                nodeStatusMap.put(state.getNodeId(), state.getStatus());
            }
        }

        svg.append(String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %d %d\" width=\"100%%\" height=\"%d\" class=\"architecture-svg\">\n", width, height, height));
        svg.append("  <defs>\n");
        svg.append("    <marker id=\"arrow-normal\" viewBox=\"0 0 10 10\" refX=\"22\" refY=\"5\" markerWidth=\"6\" markerHeight=\"6\" orient=\"auto-start-reverse\">\n");
        svg.append("      <path d=\"M 0 0 L 10 5 L 0 10 z\" fill=\"#64748b\"/>\n");
        svg.append("    </marker>\n");
        svg.append("    <marker id=\"arrow-critical\" viewBox=\"0 0 10 10\" refX=\"22\" refY=\"5\" markerWidth=\"6\" markerHeight=\"6\" orient=\"auto-start-reverse\">\n");
        svg.append("      <path d=\"M 0 0 L 10 5 L 0 10 z\" fill=\"#ef4444\"/>\n");
        svg.append("    </marker>\n");
        svg.append("    <marker id=\"arrow-failed\" viewBox=\"0 0 10 10\" refX=\"22\" refY=\"5\" markerWidth=\"6\" markerHeight=\"6\" orient=\"auto-start-reverse\">\n");
        svg.append("      <path d=\"M 0 0 L 10 5 L 0 10 z\" fill=\"#94a3b8\"/>\n");
        svg.append("    </marker>\n");
        svg.append("    <filter id=\"node-shadow\" x=\"-10%\" y=\"-10%\" width=\"130%\" height=\"130%\">\n");
        svg.append("      <feDropShadow dx=\"0\" dy=\"4\" stdDeviation=\"4\" flood-opacity=\"0.15\"/>\n");
        svg.append("    </filter>\n");
        svg.append("  </defs>\n");

        // Background
        svg.append("  <rect width=\"100%\" height=\"100%\" fill=\"#f8fafc\" rx=\"8\" />\n");

        // Grid lines (subtle)
        svg.append("  <g stroke=\"#e2e8f0\" stroke-width=\"1\" opacity=\"0.5\">\n");
        for (int x = 50; x < width; x += 50) {
            svg.append(String.format("    <line x1=\"%d\" y1=\"0\" x2=\"%d\" y2=\"%d\" />\n", x, x, height));
        }
        for (int y = 50; y < height; y += 50) {
            svg.append(String.format("    <line x1=\"0\" y1=\"%d\" x2=\"%d\" y2=\"%d\" />\n", y, width, y));
        }
        svg.append("  </g>\n");

        // Edges
        svg.append("  <!-- Edges -->\n");
        for (DependencyEdge edge : graph.getEdges()) {
            ServiceNode src = graph.getNode(edge.getSource());
            ServiceNode tgt = graph.getNode(edge.getTarget());
            if (src == null || tgt == null) continue;

            String srcStatus = nodeStatusMap.getOrDefault(src.getId(), "OPERATIONAL");
            String tgtStatus = nodeStatusMap.getOrDefault(tgt.getId(), "OPERATIONAL");
            boolean isEdgeDead = !"OPERATIONAL".equals(srcStatus) || !"OPERATIONAL".equals(tgtStatus);

            String strokeColor;
            String markerId;
            String dashArray = "";

            if (isEdgeDead) {
                strokeColor = "#94a3b8";
                markerId = "arrow-failed";
                dashArray = "stroke-dasharray=\"4,4\"";
            } else if (edge.isCritical()) {
                strokeColor = "#ef4444";
                markerId = "arrow-critical";
            } else {
                strokeColor = "#64748b";
                markerId = "arrow-normal";
                dashArray = "stroke-dasharray=\"6,3\"";
            }

            double x1 = src.getX();
            double y1 = src.getY();
            double x2 = tgt.getX();
            double y2 = tgt.getY();

            // Bezier curve control points
            double cx = (x1 + x2) / 2.0;
            double cy = (y1 + y2) / 2.0;
            if (Math.abs(y1 - y2) < 20) {
                cy += 20; // slight arc for straight horizontal lines
            }

            svg.append(String.format("  <path d=\"M %.1f,%.1f Q %.1f,%.1f %.1f,%.1f\" fill=\"none\" stroke=\"%s\" stroke-width=\"2.5\" %s marker-end=\"url(#%s)\" />\n",
                    x1, y1, cx, cy, x2, y2, strokeColor, dashArray, markerId));

            // Edge protocol label
            svg.append(String.format("  <text x=\"%.1f\" y=\"%.1f\" font-family=\"sans-serif\" font-size=\"10\" fill=\"#64748b\" text-anchor=\"middle\">%s</text>\n",
                    cx, cy - 6, edge.getProtocol()));
        }

        // Nodes
        svg.append("  <!-- Nodes -->\n");
        for (ServiceNode node : graph.getNodes()) {
            String status = nodeStatusMap.getOrDefault(node.getId(), "OPERATIONAL");

            String fillColor;
            String strokeColor;
            String statusBadgeColor;
            String statusText;

            switch (status) {
                case "DIRECT_FAILURE" -> {
                    fillColor = "#fee2e2";
                    strokeColor = "#ef4444";
                    statusBadgeColor = "#dc2626";
                    statusText = "FAILED (DIRECT)";
                }
                case "CASCADING_FAILURE" -> {
                    fillColor = "#ffedd5";
                    strokeColor = "#f97316";
                    statusBadgeColor = "#ea580c";
                    statusText = "FAILED (CASCADE)";
                }
                case "ISOLATED" -> {
                    fillColor = "#f1f5f9";
                    strokeColor = "#94a3b8";
                    statusBadgeColor = "#64748b";
                    statusText = "ISOLATED";
                }
                default -> {
                    fillColor = "#ecfdf5";
                    strokeColor = "#10b981";
                    statusBadgeColor = "#059669";
                    statusText = "HEALTHY";
                }
            }

            double nx = node.getX() - 85;
            double ny = node.getY() - 40;
            double nw = 170;
            double nh = 80;

            svg.append(String.format("  <g transform=\"translate(%.1f, %.1f)\" filter=\"url(#node-shadow)\" class=\"service-node-group\">\n", nx, ny));
            // Node body
            svg.append(String.format("    <rect width=\"%.1f\" height=\"%.1f\" rx=\"10\" fill=\"%s\" stroke=\"%s\" stroke-width=\"2.5\" />\n", nw, nh, fillColor, strokeColor));
            // Header bar
            svg.append(String.format("    <rect width=\"%.1f\" height=\"24\" rx=\"10\" fill=\"%s\" opacity=\"0.2\" />\n", nw, strokeColor));
            // Status pill
            svg.append(String.format("    <rect x=\"%.1f\" y=\"6\" width=\"85\" height=\"14\" rx=\"7\" fill=\"%s\" />\n", nw - 92, statusBadgeColor));
            svg.append(String.format("    <text x=\"%.1f\" y=\"16.5\" font-family=\"sans-serif\" font-size=\"8\" font-weight=\"bold\" fill=\"#ffffff\" text-anchor=\"middle\">%s</text>\n", nw - 49.5, statusText));
            // Node name
            svg.append(String.format("    <text x=\"10\" y=\"16\" font-family=\"sans-serif\" font-size=\"12\" font-weight=\"bold\" fill=\"#1e293b\">%s</text>\n", node.getName()));
            // Type & Tier
            svg.append(String.format("    <text x=\"10\" y=\"45\" font-family=\"sans-serif\" font-size=\"10\" fill=\"#475569\">Type: %s</text>\n", node.getType()));
            svg.append(String.format("    <text x=\"10\" y=\"62\" font-family=\"sans-serif\" font-size=\"9\" fill=\"#64748b\">Tier %d | Rel: %.2f%%</text>\n", node.getCriticalTier(), node.getDefaultReliability() * 100));
            svg.append("  </g>\n");
        }

        // Title and Legend
        svg.append("  <!-- Legend -->\n");
        svg.append("  <g transform=\"translate(20, 20)\">\n");
        svg.append("    <text x=\"0\" y=\"0\" font-family=\"sans-serif\" font-size=\"14\" font-weight=\"bold\" fill=\"#0f172a\">Microservice Topology: Payment Processing Architecture</text>\n");
        svg.append("    <circle cx=\"5\" cy=\"20\" r=\"5\" fill=\"#10b981\" />\n");
        svg.append("    <text x=\"15\" y=\"23\" font-family=\"sans-serif\" font-size=\"10\" fill=\"#334155\">Operational</text>\n");
        svg.append("    <circle cx=\"110\" cy=\"20\" r=\"5\" fill=\"#ef4444\" />\n");
        svg.append("    <text x=\"120\" y=\"23\" font-family=\"sans-serif\" font-size=\"10\" fill=\"#334155\">Direct Failure</text>\n");
        svg.append("    <circle cx=\"220\" cy=\"20\" r=\"5\" fill=\"#f97316\" />\n");
        svg.append("    <text x=\"230\" y=\"23\" font-family=\"sans-serif\" font-size=\"10\" fill=\"#334155\">Cascading Failure</text>\n");
        svg.append("  </g>\n");

        svg.append("</svg>\n");
        return svg.toString();
    }

    public String generateMermaidDiagram(ArchitectureGraph graph, SimulationResult simResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph LR\n");

        Map<String, String> nodeStatusMap = new HashMap<>();
        if (simResult != null && simResult.getNodeStates() != null) {
            for (NodeSimulationState state : simResult.getNodeStates()) {
                nodeStatusMap.put(state.getNodeId(), state.getStatus());
            }
        }

        for (ServiceNode node : graph.getNodes()) {
            String status = nodeStatusMap.getOrDefault(node.getId(), "OPERATIONAL");
            String cleanId = node.getId().replace("-", "_");
            String label = String.format("%s<br/><i>[%s]</i>", node.getName(), status);
            sb.append(String.format("  %s[\"%s\"]\n", cleanId, label));
        }

        for (DependencyEdge edge : graph.getEdges()) {
            String src = edge.getSource().replace("-", "_");
            String tgt = edge.getTarget().replace("-", "_");
            String label = edge.getProtocol();
            if (edge.isCritical()) {
                sb.append(String.format("  %s ==>|%s| %s\n", src, label, tgt));
            } else {
                sb.append(String.format("  %s -.->|%s| %s\n", src, label, tgt));
            }
        }

        // Style classes
        sb.append("  classDef healthy fill:#ecfdf5,stroke:#10b981,stroke-width:2px;\n");
        sb.append("  classDef directFail fill:#fee2e2,stroke:#ef4444,stroke-width:3px;\n");
        sb.append("  classDef cascadeFail fill:#ffedd5,stroke:#f97316,stroke-width:3px;\n");
        sb.append("  classDef isolated fill:#f1f5f9,stroke:#94a3b8,stroke-width:2px;\n");

        for (ServiceNode node : graph.getNodes()) {
            String cleanId = node.getId().replace("-", "_");
            String status = nodeStatusMap.getOrDefault(node.getId(), "OPERATIONAL");
            switch (status) {
                case "DIRECT_FAILURE" -> sb.append(String.format("  class %s directFail;\n", cleanId));
                case "CASCADING_FAILURE" -> sb.append(String.format("  class %s cascadeFail;\n", cleanId));
                case "ISOLATED" -> sb.append(String.format("  class %s isolated;\n", cleanId));
                default -> sb.append(String.format("  class %s healthy;\n", cleanId));
            }
        }

        return sb.toString();
    }

    public String generateAsciiRepresentation(ArchitectureGraph graph, SimulationResult simResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================================\n");
        sb.append(String.format(" MICROSERVICE ARCHITECTURE: %s\n", graph.getName()));
        sb.append("========================================================================================\n\n");

        Map<String, String> statusMap = new HashMap<>();
        if (simResult != null && simResult.getNodeStates() != null) {
            for (NodeSimulationState state : simResult.getNodeStates()) {
                statusMap.put(state.getNodeId(), state.getStatus());
            }
        }

        sb.append("SERVICES (NODES):\n");
        for (ServiceNode node : graph.getNodes()) {
            String status = statusMap.getOrDefault(node.getId(), "OPERATIONAL");
            sb.append(String.format("  * [%-18s] %-26s (Type: %-18s | Status: %s)\n",
                    node.getId(), node.getName(), node.getType(), status));
        }

        sb.append("\nDIRECTED DEPENDENCIES (EDGES):\n");
        for (DependencyEdge edge : graph.getEdges()) {
            sb.append(String.format("  * %-25s --(%s, critical=%b)--> %s\n",
                    edge.getSource(), edge.getProtocol(), edge.isCritical(), edge.getTarget()));
        }

        return sb.toString();
    }
}
