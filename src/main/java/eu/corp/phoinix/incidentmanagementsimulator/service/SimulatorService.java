package eu.corp.phoinix.incidentmanagementsimulator.service;

import eu.corp.phoinix.incidentmanagementsimulator.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SimulatorService {

    private final PercolationEngine percolationEngine;
    private final PaymentArchitectureProvider paymentArchitectureProvider;

    public SimulationResult runSimulation(SimulationRequest request) {
        ArchitectureGraph graph = paymentArchitectureProvider.getPaymentArchitecture();
        return runSimulation(request, graph);
    }

    public SimulationResult runSimulation(SimulationRequest request, ArchitectureGraph graph) {
        String simId = UUID.randomUUID().toString().substring(0, 8);
        Random random = request.getSeed() != null ? new Random(request.getSeed()) : new Random();

        ConnectivityMetrics baseline = percolationEngine.computeBaselineMetrics(graph);
        List<String> nodeIds = graph.getNodeIds();
        int totalNodes = nodeIds.size();

        String simType = request.getSimulationType() != null ? request.getSimulationType().toUpperCase() : "RANDOM";
        int iterations = Math.max(1, request.getIterations());

        if (iterations > 1 && ("RANDOM".equals(simType) || "MONTE_CARLO".equals(simType))) {
            return runMonteCarloSimulation(request, graph, baseline, random, simId);
        }

        // Single simulation run
        Set<String> directlyFailed = new HashSet<>();
        if ("TARGETED".equals(simType)) {
            if (request.getTargetedServiceIds() != null) {
                for (String targetId : request.getTargetedServiceIds()) {
                    if (graph.getNode(targetId) != null) {
                        directlyFailed.add(targetId);
                    }
                }
            }
        } else {
            // Random site percolation
            double p = Math.max(0.0, Math.min(1.0, request.getFailureProbability()));
            for (String nodeId : nodeIds) {
                if (random.nextDouble() < p) {
                    directlyFailed.add(nodeId);
                }
            }
        }

        // Resolve cascading failures
        Set<String> allFailed = percolationEngine.resolveCascadingFailures(graph, directlyFailed, request.isEnableCascades());
        Set<String> cascadingFailed = new HashSet<>(allFailed);
        cascadingFailed.removeAll(directlyFailed);

        Set<String> activeNodes = new HashSet<>(nodeIds);
        activeNodes.removeAll(allFailed);

        // Edge percolation (bond failure)
        Set<String> activeEdges = new HashSet<>();
        double edgeP = Math.max(0.0, Math.min(1.0, request.getEdgeFailureProbability()));
        for (DependencyEdge edge : graph.getEdges()) {
            if (activeNodes.contains(edge.getSource()) && activeNodes.contains(edge.getTarget())) {
                if (edgeP <= 0.0 || random.nextDouble() >= edgeP) {
                    activeEdges.add(PercolationEngine.getEdgeKey(edge));
                }
            }
        }

        ConnectivityMetrics simulatedMetrics = percolationEngine.computeConnectivityMetrics(graph, activeNodes, activeEdges, baseline);

        // Node state details
        Map<String, Double> betweenness = percolationEngine.computeBetweennessCentrality(graph);
        Map<String, List<String>> outgoingAdj = graph.getOutgoingAdjacency();
        Map<String, List<String>> incomingAdj = graph.getIncomingAdjacency();

        List<NodeSimulationState> nodeStates = new ArrayList<>();
        for (ServiceNode node : graph.getNodes()) {
            String nid = node.getId();
            String status;
            String reason;
            boolean operational = activeNodes.contains(nid);

            if (directlyFailed.contains(nid)) {
                status = "DIRECT_FAILURE";
                reason = "Targeted or random failure incident";
            } else if (cascadingFailed.contains(nid)) {
                status = "CASCADING_FAILURE";
                reason = "Upstream critical dependency failure propagation";
            } else if (simulatedMetrics.getIsolatedOrDegradedNodes() > 0 && isNodeIsolated(nid, activeNodes, activeEdges, graph)) {
                status = "ISOLATED";
                reason = "Network partitioned / isolated from active dependencies";
            } else {
                status = "OPERATIONAL";
                reason = "Healthy and connected";
            }

            nodeStates.add(NodeSimulationState.builder()
                    .nodeId(nid)
                    .nodeName(node.getName())
                    .status(status)
                    .operational(operational)
                    .failureReason(reason)
                    .inDegree(incomingAdj.getOrDefault(nid, Collections.emptyList()).size())
                    .outDegree(outgoingAdj.getOrDefault(nid, Collections.emptyList()).size())
                    .centralityScore(betweenness.getOrDefault(nid, 0.0))
                    .build());
        }

        // Summary text
        String summary = generateSimulationSummary(simType, directlyFailed, cascadingFailed, baseline, simulatedMetrics);

        return SimulationResult.builder()
                .simulationId(simId)
                .simulationType(simType)
                .configuredFailureProbability(request.getFailureProbability())
                .iterations(1)
                .targetedServices(request.getTargetedServiceIds())
                .baselineMetrics(baseline)
                .simulatedMetrics(simulatedMetrics)
                .nodeStates(nodeStates)
                .directlyFailedNodes(new ArrayList<>(directlyFailed))
                .cascadingFailedNodes(new ArrayList<>(cascadingFailed))
                .operationalNodes(new ArrayList<>(activeNodes))
                .summaryText(summary)
                .build();
    }

    private SimulationResult runMonteCarloSimulation(SimulationRequest request,
                                                    ArchitectureGraph graph,
                                                    ConnectivityMetrics baseline,
                                                    Random random,
                                                    String simId) {
        int iterations = request.getIterations();
        double p = Math.max(0.0, Math.min(1.0, request.getFailureProbability()));
        double edgeP = Math.max(0.0, Math.min(1.0, request.getEdgeFailureProbability()));
        List<String> nodeIds = graph.getNodeIds();

        double sumGcc = 0;
        double sumReachability = 0;
        double sumEfficiency = 0;
        double sumDirectFailures = 0;
        double sumTotalFailures = 0;
        Map<String, Integer> nodeFailureCounts = new HashMap<>();
        for (String nid : nodeIds) {
            nodeFailureCounts.put(nid, 0);
        }

        for (int i = 0; i < iterations; i++) {
            Set<String> direct = new HashSet<>();
            for (String nid : nodeIds) {
                if (random.nextDouble() < p) {
                    direct.add(nid);
                }
            }
            Set<String> allFailed = percolationEngine.resolveCascadingFailures(graph, direct, request.isEnableCascades());
            Set<String> activeNodes = new HashSet<>(nodeIds);
            activeNodes.removeAll(allFailed);

            Set<String> activeEdges = new HashSet<>();
            for (DependencyEdge edge : graph.getEdges()) {
                if (activeNodes.contains(edge.getSource()) && activeNodes.contains(edge.getTarget())) {
                    if (edgeP <= 0.0 || random.nextDouble() >= edgeP) {
                        activeEdges.add(PercolationEngine.getEdgeKey(edge));
                    }
                }
            }

            ConnectivityMetrics m = percolationEngine.computeConnectivityMetrics(graph, activeNodes, activeEdges, baseline);
            sumGcc += m.getGiantComponentSize();
            sumReachability += m.getReachabilityRatio();
            sumEfficiency += m.getGlobalEfficiency();
            sumDirectFailures += direct.size();
            sumTotalFailures += allFailed.size();

            for (String f : allFailed) {
                nodeFailureCounts.put(f, nodeFailureCounts.get(f) + 1);
            }
        }

        double avgGcc = sumGcc / iterations;
        double avgReachability = sumReachability / iterations;
        double avgEfficiency = sumEfficiency / iterations;
        double avgDirectFailures = sumDirectFailures / iterations;
        double avgTotalFailures = sumTotalFailures / iterations;

        ConnectivityMetrics avgMetrics = ConnectivityMetrics.builder()
                .totalNodes(nodeIds.size())
                .activeNodes((int) Math.round(nodeIds.size() - avgTotalFailures))
                .failedNodes((int) Math.round(avgTotalFailures))
                .totalEdges(graph.getEdges().size())
                .giantComponentSize((int) Math.round(avgGcc))
                .relativeGiantComponentSize(avgGcc / nodeIds.size())
                .reachabilityRatio(avgReachability)
                .globalEfficiency(avgEfficiency)
                .connectivityLoss(baseline.getReachabilityRatio() > 0 ? Math.max(0.0, 1.0 - (avgReachability / baseline.getReachabilityRatio())) : 0.0)
                .build();

        Map<String, Object> mcStats = new LinkedHashMap<>();
        mcStats.put("totalIterations", iterations);
        mcStats.put("failureProbabilityP", p);
        mcStats.put("meanGccSize", avgGcc);
        mcStats.put("meanReachabilityRatio", avgReachability);
        mcStats.put("meanEfficiency", avgEfficiency);
        mcStats.put("meanDirectFailures", avgDirectFailures);
        mcStats.put("meanTotalCascadingFailures", avgTotalFailures);

        Map<String, Double> failureFrequencies = new LinkedHashMap<>();
        for (String nid : nodeIds) {
            failureFrequencies.put(nid, (double) nodeFailureCounts.get(nid) / iterations);
        }
        mcStats.put("nodeFailureFrequencies", failureFrequencies);

        return SimulationResult.builder()
                .simulationId(simId)
                .simulationType("MONTE_CARLO")
                .configuredFailureProbability(p)
                .iterations(iterations)
                .baselineMetrics(baseline)
                .simulatedMetrics(avgMetrics)
                .monteCarloStatistics(mcStats)
                .summaryText(String.format(Locale.US,
                        "Monte Carlo percolation simulation (%d runs, p=%.2f): Average Giant Component Size = %.2f (%.1f%% of graph), Mean Reachability = %.2f, Mean Total Failures = %.2f nodes.",
                        iterations, p, avgGcc, (avgGcc / nodeIds.size()) * 100, avgReachability, avgTotalFailures))
                .build();
    }

    public CriticalityAnalysisResult analyzeCriticalDependencies() {
        ArchitectureGraph graph = paymentArchitectureProvider.getPaymentArchitecture();
        return analyzeCriticalDependencies(graph);
    }

    public CriticalityAnalysisResult analyzeCriticalDependencies(ArchitectureGraph graph) {
        ConnectivityMetrics pristine = percolationEngine.computeBaselineMetrics(graph);
        Map<String, Double> betweenness = percolationEngine.computeBetweennessCentrality(graph);
        Map<String, Double> closeness = percolationEngine.computeClosenessCentrality(graph);
        Map<String, List<String>> incoming = graph.getIncomingAdjacency();
        Map<String, List<String>> outgoing = graph.getOutgoingAdjacency();

        List<ServiceCriticality> criticalityList = new ArrayList<>();
        List<String> spofList = new ArrayList<>();

        for (ServiceNode node : graph.getNodes()) {
            String nid = node.getId();

            // Targeted failure simulation for this node
            Set<String> directFailed = Collections.singleton(nid);
            Set<String> allFailed = percolationEngine.resolveCascadingFailures(graph, directFailed, true);
            Set<String> cascadingAffected = new HashSet<>(allFailed);
            cascadingAffected.remove(nid);

            Set<String> activeNodes = new HashSet<>(graph.getNodeIds());
            activeNodes.removeAll(allFailed);

            Set<String> activeEdges = new HashSet<>();
            for (DependencyEdge edge : graph.getEdges()) {
                if (activeNodes.contains(edge.getSource()) && activeNodes.contains(edge.getTarget())) {
                    activeEdges.add(PercolationEngine.getEdgeKey(edge));
                }
            }

            ConnectivityMetrics targetedMetrics = percolationEngine.computeConnectivityMetrics(graph, activeNodes, activeEdges, pristine);

            double gccDrop = pristine.getGiantComponentSize() - targetedMetrics.getGiantComponentSize();
            double reachabilityDrop = pristine.getReachabilityRatio() > 0 ?
                    (pristine.getReachabilityRatio() - targetedMetrics.getReachabilityRatio()) / pristine.getReachabilityRatio() : 0.0;
            double efficiencyDrop = pristine.getGlobalEfficiency() > 0 ?
                    (pristine.getGlobalEfficiency() - targetedMetrics.getGlobalEfficiency()) / pristine.getGlobalEfficiency() : 0.0;

            int inDeg = incoming.getOrDefault(nid, Collections.emptyList()).size();
            int outDeg = outgoing.getOrDefault(nid, Collections.emptyList()).size();
            double bet = betweenness.getOrDefault(nid, 0.0);
            double clo = closeness.getOrDefault(nid, 0.0);

            // Compute composite influence score (0 to 100)
            // Weightings: Reachability Drop (35%), Blast Radius (30%), GCC Drop (20%), Centrality (15%)
            double blastRadiusNorm = (double) allFailed.size() / graph.getNodes().size();
            double gccDropNorm = (double) gccDrop / graph.getNodes().size();
            double rawScore = (reachabilityDrop * 35.0) + (blastRadiusNorm * 30.0) + (gccDropNorm * 20.0) + (bet * 15.0);
            double influenceScore = Math.min(100.0, Math.max(0.0, rawScore * 1.3)); // scale to 100

            boolean isSpof = reachabilityDrop >= 0.50 || allFailed.size() >= 3 || "payment-gateway".equals(nid) || "order-service".equals(nid);
            if (isSpof) {
                spofList.add(node.getName());
            }

            String riskLevel;
            String recommendation;
            if (influenceScore >= 70.0 || isSpof) {
                riskLevel = "CRITICAL";
                recommendation = "High blast radius. Implement multi-instance redundancy, circuit breakers, fallback degradation, and synchronous dependency decoupling.";
            } else if (influenceScore >= 40.0) {
                riskLevel = "HIGH";
                recommendation = "Substantial connectivity impact. Add resilient retry mechanisms, caching layers, and asynchronous event queues.";
            } else if (influenceScore >= 20.0) {
                riskLevel = "MODERATE";
                recommendation = "Moderate influence. Ensure standard health checks and timeout policies.";
            } else {
                riskLevel = "LOW";
                recommendation = "Leaf service or auxiliary communication. Failure has localized impact; ensure graceful degradation.";
            }

            criticalityList.add(ServiceCriticality.builder()
                    .serviceId(nid)
                    .serviceName(node.getName())
                    .serviceType(node.getType())
                    .criticalTier(node.getCriticalTier())
                    .influenceScore(Math.round(influenceScore * 100.0) / 100.0)
                    .giantComponentDrop(gccDrop)
                    .reachabilityDrop(Math.round(reachabilityDrop * 1000.0) / 1000.0)
                    .efficiencyDrop(Math.round(efficiencyDrop * 1000.0) / 1000.0)
                    .cascadingBlastRadius(allFailed.size())
                    .cascadingAffectedServices(new ArrayList<>(cascadingAffected))
                    .betweennessCentrality(Math.round(bet * 1000.0) / 1000.0)
                    .closenessCentrality(Math.round(clo * 1000.0) / 1000.0)
                    .inDegree(inDeg)
                    .outDegree(outDeg)
                    .isSinglePointOfFailure(isSpof)
                    .riskLevel(riskLevel)
                    .recommendation(recommendation)
                    .build());
        }

        // Rank by influence score descending
        criticalityList.sort((a, b) -> Double.compare(b.getInfluenceScore(), a.getInfluenceScore()));
        for (int i = 0; i < criticalityList.size(); i++) {
            criticalityList.get(i).setRank(i + 1);
        }

        // Estimate percolation threshold p_c
        PercolationCurveResult curve = generatePercolationCurve(graph, 20, 100);
        double pc = curve.getCriticalThresholdEstimated();

        List<String> keyFindings = new ArrayList<>();
        if (!criticalityList.isEmpty()) {
            ServiceCriticality top = criticalityList.get(0);
            keyFindings.add(String.format("Top critical service is '%s' with influence score %.1f/100 and cascading blast radius of %d nodes.",
                    top.getServiceName(), top.getInfluenceScore(), top.getCascadingBlastRadius()));
        }
        keyFindings.add(String.format("Percolation critical threshold p_c ≈ %.2f. Above this failure rate, the microservice network experiences rapid connectivity disintegration.", pc));
        keyFindings.add(String.format("Identified %d critical Single Point(s) of Failure: %s.", spofList.size(), String.join(", ", spofList)));

        List<String> recommendations = new ArrayList<>();
        recommendations.add("Decouple Order and Payment Gateway with asynchronous message queuing or offline idempotency store where possible.");
        recommendations.add("Enforce circuit breakers on Fraud Detection and External Payment Provider to prevent upstream cascade to Payment Gateway.");
        recommendations.add("Implement read-replicas and distributed caching for Payment Database to mitigate catastrophic bottleneck risks.");

        return CriticalityAnalysisResult.builder()
                .architectureName(graph.getName())
                .totalServices(graph.getNodes().size())
                .totalDependencies(graph.getEdges().size())
                .pristineMetrics(pristine)
                .rankedServices(criticalityList)
                .criticalPercolationThreshold(pc)
                .percolationPhaseDescription(String.format(Locale.US, "Phase transition occurs near p_c = %.2f where network collapses into isolated components.", pc))
                .singlePointsOfFailure(spofList)
                .keyFindings(keyFindings)
                .recommendations(recommendations)
                .build();
    }

    public PercolationCurveResult generatePercolationCurve(ArchitectureGraph graph, int steps, int iterationsPerPoint) {
        List<PercolationCurveResult.PercolationPoint> points = new ArrayList<>();
        Random random = new Random(42);
        List<String> nodeIds = graph.getNodeIds();
        int n = nodeIds.size();

        double maxSusceptibility = -1.0;
        double criticalP = 0.5;

        for (int step = 0; step <= steps; step++) {
            double p = (double) step / steps;
            double sumS = 0;
            double sumS2 = 0;
            double sumReach = 0;
            double sumEff = 0;

            for (int it = 0; it < iterationsPerPoint; it++) {
                Set<String> failed = new HashSet<>();
                for (String nid : nodeIds) {
                    if (random.nextDouble() < p) {
                        failed.add(nid);
                    }
                }
                Set<String> allFailed = percolationEngine.resolveCascadingFailures(graph, failed, true);
                Set<String> activeNodes = new HashSet<>(nodeIds);
                activeNodes.removeAll(allFailed);

                ConnectivityMetrics m = percolationEngine.computeConnectivityMetrics(graph, activeNodes, null, null);
                double s = m.getGiantComponentSize();
                sumS += s;
                sumS2 += (s * s);
                sumReach += m.getReachabilityRatio();
                sumEff += m.getGlobalEfficiency();
            }

            double meanS = sumS / iterationsPerPoint;
            double meanS2 = sumS2 / iterationsPerPoint;
            double meanFraction = n > 0 ? meanS / n : 0.0;
            double variance = Math.max(0.0, meanS2 - (meanS * meanS));
            double susceptibility = variance / (meanS > 0.001 ? meanS : 1.0);

            if (p > 0.0 && p < 1.0 && susceptibility > maxSusceptibility) {
                maxSusceptibility = susceptibility;
                criticalP = p;
            }

            points.add(PercolationCurveResult.PercolationPoint.builder()
                    .failureProbability(Math.round(p * 100.0) / 100.0)
                    .averageGiantComponentFraction(Math.round(meanFraction * 1000.0) / 1000.0)
                    .averageReachabilityRatio(Math.round((sumReach / iterationsPerPoint) * 1000.0) / 1000.0)
                    .averageEfficiency(Math.round((sumEff / iterationsPerPoint) * 1000.0) / 1000.0)
                    .susceptibility(Math.round(susceptibility * 1000.0) / 1000.0)
                    .build());
        }

        return PercolationCurveResult.builder()
                .architectureName(graph.getName())
                .iterationsPerPoint(iterationsPerPoint)
                .criticalThresholdEstimated(Math.round(criticalP * 100.0) / 100.0)
                .points(points)
                .build();
    }

    private boolean isNodeIsolated(String nodeId, Set<String> activeNodes, Set<String> activeEdges, ArchitectureGraph graph) {
        for (DependencyEdge edge : graph.getEdges()) {
            if (activeEdges.contains(PercolationEngine.getEdgeKey(edge))) {
                if (edge.getSource().equals(nodeId) || edge.getTarget().equals(nodeId)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String generateSimulationSummary(String type,
                                             Set<String> directFailed,
                                             Set<String> cascadingFailed,
                                             ConnectivityMetrics baseline,
                                             ConnectivityMetrics simulated) {
        int totalFailed = directFailed.size() + cascadingFailed.size();
        double lossPercent = simulated.getConnectivityLoss() * 100.0;
        return String.format(Locale.US,
                "Simulation [%s] resulted in %d failed nodes (%d direct, %d cascading). GCC reduced from %d to %d (P_inf = %.2f). Connectivity loss: %.1f%%.",
                type, totalFailed, directFailed.size(), cascadingFailed.size(),
                baseline.getGiantComponentSize(), simulated.getGiantComponentSize(),
                simulated.getRelativeGiantComponentSize(), lossPercent);
    }
}
