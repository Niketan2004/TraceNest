# TraceNest — Community Lost & Found Intelligence Platform

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21_LTS-orange.svg)](https://openjdk.org/)
[![React](https://img.shields.io/badge/React-19.2.0-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.8.3-blue.svg)](https://www.typescriptlang.org/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4.2.1-38bdf8.svg)](https://tailwindcss.com/)
[![Leaflet](https://img.shields.io/badge/Leaflet-1.9.4-brightgreen.svg)](https://leafletjs.com/)
[![OpenPDF](https://img.shields.io/badge/OpenPDF-1.3.43-yellow.svg)](https://github.com/LibrePDF/OpenPDF)
[![License: MIT](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)
[![CI Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](.github/workflows/ci.yml)

**TraceNest** is a full-stack, community-driven emergency intelligence and recovery platform designed to report, track, and locate missing persons, lost pets, and stolen or misplaced vehicles.

It unites a reactive **Spring Boot 3.4.3 (Java 21 LTS)** REST micro-service architecture with a modern **React 19 / TanStack Router** frontend, providing interactive OpenStreetMap geolocation, an automated **20KM Haversine spherical radius notification engine**, multi-channel alert dispatch (in-app alerts, SMTP email, Twilio SMS), Cloudinary media management, and on-demand printable **A4 PDF poster generation**.

---

## Table of Contents

1. [Sub-Project Documentation](#sub-project-documentation)
2. [System Architecture](#system-architecture)
3. [Repository Structure](#repository-structure)
4. [Quick Start with Docker Compose](#quick-start-with-docker-compose-one-command-run-)
5. [Local Development Setup](#local-development-setup)
6. [Environment Variables & Configuration](#environment-variables--configuration)
7. [End-to-End User Journeys](#end-to-end-user-journeys)
8. [Key Technical Highlights](#key-technical-highlights)
9. [Contributing & Security](#contributing--security)
10. [License](#license)

---

## Sub-Project Documentation

For component-specific architecture and setup guides, refer to:
- 📖 [**Backend Documentation (`TraceNest-Backend/README.md`)**](TraceNest-Backend/README.md)
- 🎨 [**Frontend Documentation (`TraceNet-Frontend/README.md`)**](TraceNet-Frontend/README.md)

---

## System Architecture

```
                               +-------------------------------------+
                               |         TRACENEST PLATFORM          |
                               +-------------------------------------+
                                                  |
            +--------------------------------------+--------------------------------------+
            |                                                                             |
            v                                                                             v
+------------------------+                                                   +------------------------+
|   TraceNet-Frontend    |  <-- REST API (JSON / Multipart / Bearer JWT) --> |   TraceNest-Backend    |
| (React 19 / TanStack)  |                                                   | (Spring Boot / Java 21)|
+------------------------+                                                   +------------------------+
            |                                                                             |
            |-- TanStack Router & Nitro SSR                                               |-- Spring Security + JWT
            |-- Tailwind CSS 4 Design Tokens                                              |-- Spring Data JPA / Hibernate
            |-- Interactive Leaflet Maps (GPS)                                            |-- 20KM Haversine Proximity Matcher
            |-- Category Forms (Zod Validation)                                           |-- OpenPDF Poster Engine
            |-- Case Management & Status Toggles                                          |-- Async Notifications (Email/SMS)
            |-- Real-Time In-App Alert Feed                                               +------------------------+
            |                                                                             |            |
            +-----------------------------------------------------------------------------+            |
                                                                                         |            |
                                                                                         v            v
                                                                                +------------+ +-------------+
                                                                                | Cloudinary | |PostgreSQL DB|
                                                                                | (Media CDN)| | (JPA Store) |
                                                                                +------------+ +-------------+
```

---

## Repository Structure

```
TraceNest/
├── .github/
│   ├── workflows/ci.yml                # Automated GitHub Actions CI workflow
│   ├── ISSUE_TEMPLATE/                 # Bug report and feature request templates
│   └── pull_request_template.md        # Pull request guidelines
├── .dockerignore                       # Excludes secrets, temporary files, and build outputs
├── .gitignore                          # Comprehensive multi-tier git ignore rules
├── docker-compose.yml                  # Unified Docker Compose orchestration (DB, API, Web)
├── CONTRIBUTING.md                     # Contribution guidelines and coding conventions
├── SECURITY.md                         # Security and vulnerability reporting policy
├── LICENSE                             # MIT License
├── README.md                           # Master Project Documentation (This File)
├── datasets/                           # Scaffolding for computer vision datasets
│
├── TraceNest-Backend/                  # Spring Boot 3.4.3 & Java 21 LTS REST API
│   ├── .env.example                    # Backend environment configuration template
│   ├── README.md                       # Backend Architecture, API Specs, and Setup Guide
│   ├── pom.xml                         # Maven dependencies (OpenPDF, Cloudinary, Twilio, JWT, PostgreSQL)
│   └── src/
│       ├── main/java/com/tracenest/backend/
│       │   ├── Auth/                   # JWT Security, BCrypt, AuthController & AuthService
│       │   ├── Common/                 # Global exception handler, async config, DTO wrappers
│       │   ├── Notification/           # In-app notifications & Async Email/SMS delivery
│       │   ├── Report/                 # CRUD, search, map, stats, OpenPDF Poster, AI hook
│       │   ├── Subscription/           # Authenticated & public geo-subscriptions, Haversine 20KM
│       │   ├── Upload/                 # Cloudinary image upload and deletion
│       │   └── User/                   # Profile updates, password changes, avatar upload
│       └── test/java/                  # Automated unit test suite (15 tests passing)
│
└── TraceNet-Frontend/                  # React 19, TanStack Router & Tailwind CSS 4 Web App
    ├── .env.example                    # Frontend environment configuration template
    ├── README.md                       # Frontend Architecture, Routes, and Setup Guide
    ├── package.json                    # Node dependencies and scripts
    ├── vite.config.ts                  # Vite + TanStack Start configuration
    ├── backend.md                      # Complete API Contract Specification
    └── src/
        ├── components/                 # Maps (Leaflet), ReportCard, Navbar, Radix UI primitives
        ├── lib/                        # Auth context, geocoding helpers, report utilities
        ├── routes/                     # File-based routes (Public, Dashboard, My Reports, Edit, etc.)
        └── services/                   # Axios API client, typed DTOs, notification & report services
```

---

## Quick Start with Docker Compose (One-Command Run) 🚀

Run the entire platform — PostgreSQL Database, Spring Boot Backend, and React Frontend — with a single command:

```bash
# 1. Clone the repository
git clone https://github.com/your-username/TraceNest.git
cd TraceNest

# 2. Configure environment variables (optional, sensible defaults included)
cp TraceNest-Backend/.env.example TraceNest-Backend/.env
cp TraceNet-Frontend/.env.example TraceNet-Frontend/.env

# 3. Spin up all services
docker-compose up -d --build
```

*(Or `docker compose up -d --build` if using Docker Compose v2 CLI plugin)*

Once the containers start:
- 🌐 **Frontend Web UI**: [http://localhost:5173](http://localhost:5173)
- ⚙️ **Backend REST API**: [http://localhost:8080/api/v1](http://localhost:8080/api/v1)
- 🩺 **Actuator Health Check**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- 🗄️ **PostgreSQL Database**: `localhost:5432` (database: `tracenest`, user: `postgres`, password: `postgres`)

To view real-time logs across all services:
```bash
docker-compose logs -f
```

To stop all services:
```bash
docker-compose down
```

---

## Local Development Setup

### Prerequisites
- **Java**: JDK 21 LTS installed and configured (`java -version`)
- **Maven**: Apache Maven 3.9+ (`mvn -v`)
- **Node.js**: Node v20.x or v22.x LTS (`node -v`)
- **Database**: PostgreSQL 14+ running on port `5432`

---

### 1. Database Initialization
Ensure PostgreSQL is active and create the database `tracenest`:
```sql
CREATE DATABASE tracenest;
```

---

### 2. Backend API Service
```bash
cd TraceNest-Backend
cp .env.example .env
mvn clean compile
mvn spring-boot:run
```
The REST API will boot on: **`http://localhost:8080`**

To execute automated unit and integration tests:
```bash
mvn test
```

---

### 3. Frontend Web Application
```bash
cd TraceNet-Frontend
cp .env.example .env
npm install
npm run dev
```
The web application will launch at: **`http://localhost:5173`**

To build the production bundle:
```bash
npm run build
```

---

## Environment Variables & Configuration

TraceNest provides pre-configured `.env.example` templates in both `TraceNest-Backend/` and `TraceNet-Frontend/`.

| Variable | Description | Default / Example | Required for MVP |
|---|---|---|:---:|
| `PORT` | Backend server port | `8080` | No |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/tracenest` | No |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL Username | `postgres` | Yes |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL Password | `postgres` | Yes |
| `APP_JWT_SECRET` | 256-bit Base64 JWT Secret Key | Base64 HMAC Key | No |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary Cloud Name | `demo` *(fallback enabled)* | Optional |
| `CLOUDINARY_API_KEY` | Cloudinary API Key | `mock_key` *(fallback enabled)* | Optional |
| `CLOUDINARY_API_SECRET` | Cloudinary API Secret | `mock_secret` *(fallback enabled)* | Optional |
| `MAIL_USERNAME` | SMTP / Gmail Address | `your_email@gmail.com` | Optional |
| `MAIL_PASSWORD` | SMTP App Password | `your_app_password` | Optional |
| `TWILIO_ACCOUNT_SID` | Twilio Account SID | `mock_sid` *(mock logger enabled)* | Optional |
| `TWILIO_AUTH_TOKEN` | Twilio Auth Token | `mock_token` *(mock logger enabled)* | Optional |
| `TWILIO_PHONE_NUMBER` | Twilio Phone Number | `+1234567890` | Optional |
| `VITE_API_BASE_URL` | Frontend API Base URL | `http://localhost:8080/api/v1` | No |

---

## End-to-End User Journeys

### 1. Case Owner Journey
1. **Sign Up & Profile**: User registers via `/register`, logs in via `/login`, and accesses their `/profile` to upload an avatar.
2. **File Report**: Navigates to `/reports/new`, selects a category (`person`, `pet`, `lost-vehicle`, `stolen-vehicle`), uploads 1–5 photos, uses the **GPS** button to auto-detect coordinates (or drops a pin on the Leaflet map), enters identifying attributes, and submits.
3. **Notification Fan-out**: The backend saves the report, uploads photos to Cloudinary, runs the 20KM Haversine radius engine, and automatically notifies matching community subscribers via in-app alert, email, and SMS.
4. **Case Management**:
   - Opens `/my-reports` to monitor all active cases.
   - Downloads an official A4 printable PDF poster (`/reports/:id/poster`) for physical search flyers.
   - Edits report details or adjustments to the location pin (`/reports/:id/edit`).
   - Updates status to `Found` and then `Resolved` via immediate status toggles.
   - Deletes the report when resolved (automatically pruning Cloudinary images and notifications).

### 2. Community Member Journey
1. **Browse & Filter**: Opens `/reports` to search cases by keywords (`q`), category tabs, status, and city.
2. **Map View**: Navigates to `/reports/map` to view active cases plotted as color-coded pins (Blue = Missing, Green = Found, Gold = Resolved) with click-to-preview cards.
3. **Report Dossier**: Views full report (`/reports/:id`) with photo gallery, description, details grid, and Google Maps directions link.
4. **Direct Contact**: Contacts the reporter via one-click phone dialer or email.

### 3. Local Alert Subscriber Journey
1. **Subscribe**: Community member visits `/subscribe` without creating an account.
2. **Location & Contact**: Enters their name, email, phone number, and drops a home/work location pin.
3. **SMS Verification**: Enters the 6-digit OTP sent via Twilio to verify mobile alerts.
4. **Emergency Alerts**: Whenever a missing person, lost pet, or stolen vehicle is filed within 20KM of their pin, they receive immediate email and SMS alerts containing the title, description, and direct link.

---

## Key Technical Highlights

1. **20KM Haversine Geofence**: High-precision spherical distance calculation filtering subscribers in realtime without requiring specialized PostGIS extensions.
2. **Automated PDF Poster Generation**: Integrated OpenPDF rendering printable A4 posters complete with high-contrast alert banners, embedded Cloudinary photos, attribute grids, reward callouts, and platform QR/links.
3. **Stateless Scalable Security**: Bearer JWT tokens with refresh rotation, BCrypt encryption, and strict owner authorization checks protecting every mutation.
4. **Zero Production Mocks**: All mock services (`mock-data.ts`, fake notification arrays, stub subscription endpoints) have been replaced with live database-backed operations.
5. **Next Semester AI Readiness**: An asynchronous `AiProcessingService` hook runs post-creation without injecting synthetic results, establishing clean boundaries for future CLIP vision embeddings, SBERT vector search, and OCR plate recognition.

---

## Contributing & Security

- Contributions are welcome! Please read [**CONTRIBUTING.md**](CONTRIBUTING.md) for branch naming conventions, PR guidelines, and style guides.
- To report security vulnerabilities, review our [**SECURITY.md**](SECURITY.md) policy.

---


