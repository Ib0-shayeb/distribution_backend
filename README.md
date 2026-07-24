# Fleet Distribution Tracker

A high-throughput, real-time fleet tracking system built with Spring Boot, PostgreSQL, and Leaflet.js.

## Current Features & Architecture
- **Real-Time Location Streaming:** Uses Server-Sent Events (SSE) to push live driver coordinate updates to an interactive Leaflet map.
- **Historical Route Replay:** Allows querying and rendering precise path histories over selected time intervals.
- **Worker Management:** Dynamic registration and roster visibility.

## Tech Stack
- **Backend:** Java, Spring Boot, Spring Data JPA, Server-Sent Events (SSE)
- **Database:** PostgreSQL
- **Frontend:** Vanilla JS, Leaflet.js, CSS3

## Roadmap
-  Core Location History & Streaming API
-  Admin Map Dashboard & Roster UI
-  **Custom Spring Security Implementation (In Progress):** Currently refactoring authentication to use a custom JWT filter chain built from scratch via `OncePerRequestFilter`.