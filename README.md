# Incident Management Simulator

An incident management and architectural resilience simulator for microservice architectures, founded on **Graph Percolation Theory** and network science.

The simulator models microservice systems as directed dependency graphs, simulates random node/edge failures and targeted cascading outages, calculates systemic connectivity degradation, and identifies critical services and Single Points of Failure (SPOFs).

---

## 📌 Mathematical Foundation: Percolation Theory

Percolation theory analyzes the behavior of connected clusters in a graph when nodes (services) or edges (communication channels) are randomly or deliberately removed:

- **Site & Bond Percolation:** Models random service outages (site percolation with failure probability $p$) and network link failures (bond percolation with probability $p_{edge}$).
- **Giant Connected Component ($P_\infty = S / N$):** Measures the relative size of the largest functioning service subnetwork.
- **Critical Percolation Threshold ($p_c$):** The critical failure probability above which the global network abruptly collapses from a connected system into isolated micro-fragments (phase transition).
- **Pairwise Reachability Ratio:** The fraction of ordered service pairs $(u, v)$ where communication paths remain viable.
- **Global Network Efficiency ($E_{glob} = \frac{1}{N(N-1)} \sum_{u \neq v} \frac{1}{d(u,v)}$):** Quantifies information and transaction propagation efficiency based on harmonic shortest path distances.
- **Centrality & Criticality Metrics:** Incorporates Brandes betweenness centrality, closeness centrality, and cascading blast radius along critical synchronous dependency chains.

---

## 🏛️ Hypothetical Payment Architecture Model

The prototype includes a realistic payment-processing microservice architecture composed of 6 core services:

```
                  ┌───────────────┐
                  │ Order Service │
                  └───────┬───────┘
                          │ (HTTP/REST - Critical)
                          ▼
             ┌─────────────────────────┐
             │ Payment Gateway Service │
             └────┬───────────────┬────┘
                  │               │
  (gRPC - Critical)│               │ (gRPC - Critical)
                  ▼               ▼
┌─────────────────────────┐     ┌────────────────────────────┐
│ Fraud Detection Service │     │ External Payment Provider  │
└─────────────────────────┘     └──────────────┬─────────────┘
                                               │ (TCP - Critical)
                                               ▼
                                ┌────────────────────────────┐
                                │  Payment Database Service  │
                                └──────────────┬─────────────┘
                                               │ (Async MQ - Non-Critical)
                                               ▼
                                ┌────────────────────────────┐
                                │    Notification Service    │
                                └────────────────────────────┘
```

### Services Overview
1. **Order Service (`order-service`):** High-level orchestration for order lifecycles and checkout flows.
2. **Payment Gateway Service (`payment-gateway`):** Core routing hub handling transaction authorization, fraud evaluation, and settlement.
3. **Fraud Detection Service (`fraud-detection`):** Real-time risk scoring, AML screening, and anti-fraud heuristics.
4. **External Payment Provider (`external-provider`):** Upstream acquirer and card network settlement gateway (Visa/Mastercard/Stripe).
5. **Payment Database Service (`payment-db`):** High-availability transactional ledger and persistence store for payments.
6. **Notification Service (`notification-service`):** Asynchronous customer alerting via Email/SMS/Push notifications.

---

## ⚙️ Key Operations

### 1. Architecture Modelling & Visualisation
- **Dynamic SVG Vector Rendering:** Renders color-coded status diagrams displaying operational, directly failed, cascadingly failed, and isolated service nodes with directional dependency arrows.
- **Mermaid.js Integration:** Generates Mermaid graph syntax for documentation and live client-side rendering.
- **ASCII Topology Export:** Generates clean terminal/text representation of the architecture.

### 2. Failure Simulation
- **Random Failure Simulation:** Evaluates percolation behavior across varying node failure probabilities $p \in [0.0, 1.0]$.
- **Targeted Failure Testing:** Simulates targeted outages of specific critical services (e.g., simulating `payment-gateway` or `payment-db` failure).
- **Cascading Failure Propagation:** Evaluates how outages cascade upstream along synchronous, non-isolated dependencies.
- **Monte Carlo Simulations:** Executes repeated simulations (e.g., 50–500 iterations) and aggregates mean, median, standard deviation, and min/max network health statistics.

### 3. Critical Dependency & Resilience Analysis
- **Service Criticality Ranking:** Scores every service on a 0–100 influence index based on Giant Component drop, reachability drop, efficiency drop, cascading blast radius, and graph betweenness centrality.
- **Single Point of Failure (SPOF) Identification:** Highlights single services whose failure causes catastrophic connectivity collapse.
- **Percolation Phase Transition Curve:** Generates full sweep percolation curves ($p = 0.0 \to 1.0$) plotting Giant Component size and reachability degradation to pinpoint the tipping point ($p_c$).

---

## 🚀 Getting Started

### Prerequisites
- **Java 21** or later (OpenJDK 21 / 25)
- Maven 3.9+ (or use the included Maven Wrapper `./mvnw`)

### Build and Run

```bash
# Build the project
./mvnw clean package

# Run the unit and integration tests
./mvnw test

# Start the application
./mvnw spring-boot:run
```

Once started, open your web browser at:
👉 **`http://localhost:8080/`**

---

## 🌐 Interactive Dashboard

The built-in web dashboard (`/`) provides an interactive GUI to:
- Inspect the microservice topology and dependency matrix.
- Trigger random percolation simulations with configurable failure probabilities and Monte Carlo iterations.
- Run targeted single or multi-service failure experiments.
- View real-time visual graph diagrams with dynamic failure coloring.
- Analyze connectivity metrics (Giant Component $P_\infty$, Pairwise Reachability, Global Efficiency $E_{glob}$).
- Explore ranked service criticality, blast radii, SPOF alerts, and architectural recommendations.
- Plot percolation curve phase transitions across varying failure thresholds.

---

## 📡 REST API Reference

### Architecture Endpoints
- `GET /api/architecture` - Returns the complete architecture graph (nodes and dependency edges).
- `GET /api/architecture/visualize?format=svg` - Renders an SVG vector diagram of the baseline architecture.
- `GET /api/architecture/visualize?format=mermaid` - Returns Mermaid.js diagram syntax.
- `GET /api/architecture/visualize?format=ascii` - Returns an ASCII visualization of the graph.

### Simulation Endpoints
- `POST /api/simulation/run` - Runs a failure simulation (Random or Targeted).
  ```json
  {
    "simulationType": "RANDOM",
    "failureProbability": 0.3,
    "iterations": 1,
    "enableCascades": true
  }
  ```
- `POST /api/simulation/monte-carlo` - Executes repeated Monte Carlo percolation simulations.
  ```json
  {
    "simulationType": "MONTE_CARLO",
    "failureProbability": 0.25,
    "iterations": 100,
    "enableCascades": true
  }
  ```
- `POST /api/simulation/targeted/{serviceId}` - Runs a targeted failure test against a specific service.
- `POST /api/simulation/visualize` - Runs a simulation and returns the SVG diagram with node health highlights.

### Analysis Endpoints
- `GET /api/analysis/criticality` - Calculates and returns systemic criticality rankings, SPOFs, and recommendations.
- `GET /api/analysis/percolation-curve?steps=20&samplesPerStep=50` - Computes the percolation curve and critical threshold $p_c$.

---

## 🧪 Testing

The test suite covers domain modeling, percolation mathematics, simulation flows, visualizers, and web APIs:

```bash
./mvnw test
```

- `PercolationEngineTests`: Mathematical validation of GCC, reachability, efficiency, betweenness, and cascades.
- `SimulatorServiceTests`: Random, targeted, Monte Carlo simulations, and criticality ranking.
- `ArchitectureVisualizerServiceTests`: SVG, Mermaid, and ASCII diagram generation.
- `IncidentManagementSimulatorIntegrationTests`: End-to-end Spring Web MVC REST API tests.
