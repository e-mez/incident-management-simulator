package eu.corp.phoinix.incidentmanagementsimulator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceNode {
    private String id;
    private String name;
    private String description;
    private String type; // e.g., "CORE", "GATEWAY", "DATABASE", "SECURITY", "NOTIFICATION", "EXTERNAL"
    private int criticalTier; // 1 = Critical, 2 = High, 3 = Medium
    private double defaultReliability; // e.g. 0.99
    private double x; // layout coordinate
    private double y; // layout coordinate
}
