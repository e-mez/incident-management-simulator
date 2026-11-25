package eu.corp.phoinix.incidentmanagementsimulator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyEdge {
    private String source;
    private String target;
    private String protocol; // "HTTP_REST", "GRPC", "DATABASE_TCP", "ASYNC_MQ"
    private boolean isCritical; // If true, downstream failure directly stops upstream service
    private double weight;
    private String description;
}
