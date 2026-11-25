package eu.corp.phoinix.incidentmanagementsimulator.service;

import eu.corp.phoinix.incidentmanagementsimulator.model.ArchitectureGraph;
import eu.corp.phoinix.incidentmanagementsimulator.model.DependencyEdge;
import eu.corp.phoinix.incidentmanagementsimulator.model.ServiceNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentArchitectureProvider {

    public ArchitectureGraph getPaymentArchitecture() {
        List<ServiceNode> nodes = new ArrayList<>();
        nodes.add(ServiceNode.builder()
                .id("order-service")
                .name("Order")
                .description("Handles incoming customer orders, checkout workflows, and basket authorization.")
                .type("ENTRYPOINT_API")
                .criticalTier(1)
                .defaultReliability(0.999)
                .x(100)
                .y(200)
                .build());

        nodes.add(ServiceNode.builder()
                .id("payment-gateway")
                .name("Payment Gateway")
                .description("Central payment orchestration service routing transactions, capturing charges, and managing state.")
                .type("CORE_GATEWAY")
                .criticalTier(1)
                .defaultReliability(0.999)
                .x(350)
                .y(200)
                .build());

        nodes.add(ServiceNode.builder()
                .id("fraud-detection")
                .name("Fraud Detection")
                .description("Real-time risk scoring, AML compliance checks, and transaction fraud evaluation.")
                .type("SECURITY_ANALYSIS")
                .criticalTier(2)
                .defaultReliability(0.995)
                .x(600)
                .y(100)
                .build());

        nodes.add(ServiceNode.builder()
                .id("external-payment-provider")
                .name("External Payment Provider")
                .description("Third-party payment processor / acquirer network (e.g. Visa/Mastercard/Stripe).")
                .type("EXTERNAL_INTEGRATION")
                .criticalTier(1)
                .defaultReliability(0.990)
                .x(600)
                .y(300)
                .build());

        nodes.add(ServiceNode.builder()
                .id("payment-db")
                .name("Payment Database")
                .description("High-consistency transactional ledger and payment state database.")
                .type("PERSISTENCE")
                .criticalTier(1)
                .defaultReliability(0.9999)
                .x(850)
                .y(150)
                .build());

        nodes.add(ServiceNode.builder()
                .id("notification-service")
                .name("Notification")
                .description("Asynchronous customer notification dispatch (emails, SMS, webhooks).")
                .type("COMMUNICATION")
                .criticalTier(3)
                .defaultReliability(0.990)
                .x(350)
                .y(380)
                .build());

        List<DependencyEdge> edges = new ArrayList<>();
        // Order -> Payment Gateway (critical)
        edges.add(DependencyEdge.builder()
                .source("order-service")
                .target("payment-gateway")
                .protocol("HTTP_REST")
                .isCritical(true)
                .weight(1.0)
                .description("Synchronous payment authorization call")
                .build());

        // Order -> Notification (non-critical / async)
        edges.add(DependencyEdge.builder()
                .source("order-service")
                .target("notification-service")
                .protocol("ASYNC_MQ")
                .isCritical(false)
                .weight(0.5)
                .description("Order creation event notification")
                .build());

        // Payment Gateway -> Fraud Detection (critical)
        edges.add(DependencyEdge.builder()
                .source("payment-gateway")
                .target("fraud-detection")
                .protocol("GRPC")
                .isCritical(true)
                .weight(0.9)
                .description("Synchronous risk scoring evaluation")
                .build());

        // Payment Gateway -> External Payment Provider (critical)
        edges.add(DependencyEdge.builder()
                .source("payment-gateway")
                .target("external-payment-provider")
                .protocol("HTTPS_EXTERNAL")
                .isCritical(true)
                .weight(1.0)
                .description("Outbound charge settlement & authorization")
                .build());

        // Payment Gateway -> Payment Database (critical)
        edges.add(DependencyEdge.builder()
                .source("payment-gateway")
                .target("payment-db")
                .protocol("DATABASE_TCP")
                .isCritical(true)
                .weight(1.0)
                .description("ACID transaction logging and state persist")
                .build());

        // Payment Gateway -> Notification (non-critical / async)
        edges.add(DependencyEdge.builder()
                .source("payment-gateway")
                .target("notification-service")
                .protocol("ASYNC_MQ")
                .isCritical(false)
                .weight(0.4)
                .description("Payment receipt / status notifications")
                .build());

        // Fraud Detection -> Payment Database (critical for history lookup)
        edges.add(DependencyEdge.builder()
                .source("fraud-detection")
                .target("payment-db")
                .protocol("DATABASE_TCP")
                .isCritical(true)
                .weight(0.8)
                .description("Velocity rules & historical fraud lookup")
                .build());

        return ArchitectureGraph.builder()
                .name("Payment Processing Microservice Architecture")
                .description("Hypothetical payment processing cluster consisting of Order, Payment Gateway, Fraud Detection, Payment Database, Notification, and External Payment Provider.")
                .nodes(nodes)
                .edges(edges)
                .build();
    }
}
