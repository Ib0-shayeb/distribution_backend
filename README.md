<div align="center">
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white" />
</div>
# Real-Time Fleet Location Tracking System

<div align="center">
  <img src="./images/demo.gif" alt="Manager Dashboard Demo"  />

  <img src="./images/DriverApp.jpg" alt="Driver Mobile App" width="250" /><img src="./images/DriverAppTracking.jpg" alt="Driver Mobile App" width="250" />
</div>

**[View the Frontend React Repository](https://github.com/Ib0-shayeb/fleet-frontend)**

This repository contains the backend architecture for a full-stack real-time fleet management system designed to track mobile field workers. The core premise was to build a highly responsive mapping dashboard where managers can monitor drivers in real time, with strict multi-tenant isolation ensuring that disparate fleets do not overlap.

## The Tech Stack and Architecture Overview

The backend is a stateless Java Spring Boot REST API connected to a PostgreSQL database. It handles the heavy lifting of user authentication, real-time event broadcasting, and geospatial data ingestion. On the frontend, the ecosystem is split into two applications: a React web dashboard for managers and a React Native mobile application for drivers.

To accelerate development, the frontend interfaces were rapidly generated and orchestrated using AI coding agents including Codium, Roo+, and Headroom. Delegating the frontend boilerplate to AI allowed me to focus my manual engineering efforts purely on the backend architecture, system design, and database tuning. The entire suite is currently deployed to Render for live staging and production testing.

## Security and Multi-Tenancy

Security is handled through Spring Security utilizing JSON Web Tokens. Because the system is designed to host multiple independent companies simultaneously, I implemented custom session management and role-based access control. When a user authenticates, their JWT encodes their specific fleet identifier. The backend intercepts every request and strictly scopes database queries and live socket connections to that identifier, guaranteeing absolute data isolation across disjoint fleets.

## Real-Time Streaming and Protecting the Database

The live tracking engine relies on Server-Sent Events rather than WebSockets. Since location tracking in this context is predominantly unidirectional—drivers send data, managers read it—SSE provided a lighter, native HTTP streaming approach. Inside the Java application, active manager connections are held in an in-memory map tying fleet IDs to lists of active emitter objects.

A major architectural challenge was protecting the PostgreSQL database from write exhaustion. If thousands of drivers pinged their GPS coordinates every few seconds, the database would quickly buckle under the sheer volume of write operations, especially with geospatial indexing in place. To solve this, I shifted the burden to the client. The mobile application implements location-log batching. The driver's phone stores intermediate coordinates locally, calculating distance deltas and skipping network requests if the vehicle hasn't moved significantly. The app then sends a batched payload of historical points alongside the live location, dramatically minimizing backend database stress while maintaining seamless historical playback for managers. To validate this under load, I developed a Python simulation script that mimics concurrent GPS telemetry streams from hundreds of simulated vehicles.

## Lessons Learned: A C++ Developer Navigating the JVM

Coming from a background in C and C++ where memory management and CPU architecture dictate performance, designing a high-throughput system in Java required a shift in perspective. My initial instinct was to apply Data-Oriented Design principles to the SSE broadcasting loop. I wanted to structure the active socket connections and location payloads in flat, contiguous memory arrays to ensure sequential L1 and L2 cache hits during the broadcast iteration.

The reality of the JVM quickly rendered that approach moot. Java's memory model, characterized by scattered heap allocations, object overhead, pointer chasing, and garbage collection pauses, inherently fights against strict cache-line alignment.

More importantly, this project cemented the realization that fighting for nanosecond CPU optimizations is often a misallocation of effort in a web backend. Unlike high-frequency trading engines running kernel-bypass networking over direct fiber optics, this system operates over unpredictable cellular networks. The bottleneck is always network I/O and TCP routing latency, measured in tens or hundreds of milliseconds. That network overhead completely dwarfs the nanoseconds saved by optimal CPU cache usage, proving that architectural patterns matter far more here than instruction-level micro-optimizations.

## Future Scalability: The Horizontal Scaling Dilemma

Currently, the application runs effectively as a single node. Scaling this horizontally introduces the classic stateful connection problem. If a fleet grows and the system requires multiple server instances behind a load balancer, a driver's GPS ping might hit Server A while their manager's live connection is held on Server B.

I evaluated two primary architectural paths to solve this for the future. The first is Channel-Based Pub/Sub using Redis. In this model, the backend remains entirely stateless. Servers publish incoming location data to a Redis channel dedicated to a specific fleet, and any server holding a manager for that fleet subscribes to it. This guarantees delivery and handles server failover seamlessly, but introduces a separate message broker dependency that must be paid for, secured, and maintained.

The alternative is Layer 7 Sticky Routing. By using a smart API gateway to decrypt the JWT and consistently hash the fleet ID, the load balancer can guarantee that all drivers and managers of the same fleet always hit the exact same Java node. This removes the need for Redis entirely, reduces network hops, and keeps the infrastructure incredibly simple. The primary tradeoff with Sticky Routing is the risk of split-brain packet delivery during server crashes. If a node dies, the load balancer must execute a fail-fast circuit breaker pattern, rejecting connections with a 503 status until the internal routing table converges on a new server. Because the React Native app is already built with robust local batching, it can gracefully catch those rejections, store the coordinates offline, and flush the queue once the new server mapping is established.

However, if the constraint of small individual fleet sizes is removed, the Sticky Routing architecture becomes a liability due to the risk of server hotspotting. If a single enterprise client scales to thousands of concurrent drivers and managers, routing them all to the exact same Java node based on their fleet ID would overwhelm that specific server's CPU and memory, leaving other nodes in the cluster underutilized. In an unbounded growth scenario, the system must abandon Sticky Routing and adopt the Redis Pub/Sub model. This allows a standard, dumb load balancer to distribute a massive fleet's incoming HTTP requests evenly across the entire server cluster, relying on Redis to act as the high-throughput message broker to synchronize state between the nodes.

## Getting Started (Local Setup)

### Prerequisites
* Java 21
* Maven
* PostgreSQL 15+

### Running the Spring Boot Backend
1. Clone the repository:
   ```bash
   git clone [https://github.com/YourUsername/fleet-backend.git](https://github.com/YourUsername/fleet-backend.git)
   cd fleet-backend
   ```
2. Before running the application, ensure you have a running instance of PostgreSQL. Create a database for the application (e.g., `fleet_db`) and configure your `src/main/resources/application.properties` file with your specific connection credentials and security keys:
    ```properties
    SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/fleet_db
    SPRING_DATASOURCE_USERNAME=your_postgres_user
    SPRING_DATASOURCE_PASSWORD=your_postgres_password
    JWT_SECRET=your_super_secret_jwt_signing_key_here
    JWT_EXPIRATION_MS=86400000
    ```
3. Build and run the application:
    ```bash
   ./mvnw spring-boot:run
   ```
