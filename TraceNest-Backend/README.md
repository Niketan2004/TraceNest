# TraceNest — Backend API Service

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21_LTS-orange.svg)](https://openjdk.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue.svg)](https://www.postgresql.org/)
[![OpenPDF](https://img.shields.io/badge/OpenPDF-1.3.43-yellow.svg)](https://github.com/LibrePDF/OpenPDF)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](../LICENSE)

The **TraceNest Backend** is a high-performance RESTful API built on Spring Boot 3.4.3 and Java 21 LTS. It powers the TraceNest platform by providing robust authentication, report lifecycle management, geospatial search, a 20KM Haversine radius notification engine, multi-channel alerting (in-app, email, SMS), Cloudinary image handling, and dynamic PDF poster generation.

---

## Table of Contents

1. [Architecture & Tech Stack](#architecture--tech-stack)
2. [Project Structure](#project-structure)
3. [Prerequisites](#prerequisites)
4. [Configuration & Environment Variables](#configuration--environment-variables)
5. [Database Setup](#database-setup)
6. [Building & Running](#building--running)
7. [Running Automated Tests](#running-automated-tests)
8. [API Reference](#api-reference)
9. [Core System Implementations](#core-system-implementations)
   - [20KM Haversine Proximity Engine](#1-20km-haversine-proximity-engine)
   - [Automated PDF Poster Generation](#2-automated-pdf-poster-generation)
   - [Cloudinary Media Storage](#3-cloudinary-media-storage)
   - [Multi-Channel Notification Dispatch](#4-multi-channel-notification-dispatch)
   - [Future AI Pipeline Hook](#5-future-ai-pipeline-hook)
10. [Security & Authorization](#security--authorization)

---

## Architecture & Tech Stack

- **Framework**: Spring Boot 3.4.3
- **Language / Runtime**: Java 21 LTS (Java HotSpot 64-bit)
- **Database & Persistence**: PostgreSQL 14+ with Spring Data JPA & Hibernate 6
- **Security**: Spring Security with Stateless JWT (Bearer token authentication, BCrypt hashing)
- **Document Generation**: OpenPDF 1.3.43 (LibrePDF)
- **Cloud Storage**: Cloudinary HTTP44
- **Communications**: Spring Mail (SMTP/Gmail) & Twilio SDK (SMS alerts)
- **Code Generation & Mapping**: MapStruct 1.5.5.Final & Project Lombok 1.18.48
- **Testing**: JUnit 5, Mockito (Subclass MockMaker), Spring Test

---

## Project Structure

```
TraceNest-Backend/
├── pom.xml                                   # Maven dependencies and build plugins
├── src/
│   ├── main/
│   │   ├── java/com/tracenest/backend/
│   │   │   ├── BackendApplication.java       # Main Spring Boot application entry point
│   │   │   │
│   │   │   ├── Auth/                         # Authentication & Security Module
│   │   │   │   ├── controller/
│   │   │   │   │   └── AuthController.java   # Auth REST endpoints (register, login, refresh, password reset)
│   │   │   │   ├── dto/                      # AuthRequest, AuthResponse, LoginRequest, RegisterRequest
│   │   │   │   ├── security/                 # WebSecurityConfig, JwtUtils, JwtFilter, UserDetails
│   │   │   │   └── service/
│   │   │   │       └── AuthService.java      # Auth & password-reset token lifecycle logic
│   │   │   │
│   │   │   ├── Common/                       # Shared Utilities & Cross-Cutting Concerns
│   │   │   │   ├── config/AsyncConfig.java   # @EnableAsync configuration for non-blocking notifications
│   │   │   │   ├── dto/                      # PaginatedResponse<T>, MessageResponse
│   │   │   │   └── exception/                # GlobalExceptionHandler, CustomException
│   │   │   │
│   │   │   ├── Notification/                 # In-App & Multi-Channel Alert Module
│   │   │   │   ├── controller/               # NotificationController (CRUD & read flags)
│   │   │   │   ├── model/Notification.java   # In-app notification JPA entity
│   │   │   │   ├── repository/               # NotificationRepository (paginated user queries)
│   │   │   │   └── service/
│   │   │   │       ├── NotificationService.java         # In-app notification persistence & state
│   │   │   │       └── NotificationDeliveryService.java # Async Email (Spring Mail) & SMS (Twilio)
│   │   │   │
│   │   │   ├── Report/                       # Core Report Domain
│   │   │   │   ├── controller/
│   │   │   │   │   ├── DashboardController.java  # Aggregate counts & recent author reports
│   │   │   │   │   └── ReportController.java     # CRUD, search, map, stats, poster endpoint
│   │   │   │   ├── dto/                      # CreateReportRequest, ReportDto, DashboardSummaryDto
│   │   │   │   ├── mapper/ReportMapper.java  # MapStruct entity-to-DTO mapper
│   │   │   │   ├── model/                    # Report, Location, ReportDetails, ReportStatus, ReportType
│   │   │   │   ├── repository/               # ReportRepository (JPA Specification Executor)
│   │   │   │   └── service/
│   │   │   │       ├── ReportService.java        # Core business logic, status transitions, geo-filtering
│   │   │   │       ├── PosterService.java        # OpenPDF A4 poster rendering
│   │   │   │       └── AiProcessingService.java  # Async hook prepared for next semester AI
│   │   │   │
│   │   │   ├── Subscription/                 # Geographic Alert Subscriptions
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AlertController.java         # Authenticated user subscription CRUD
│   │   │   │   │   └── SubscriptionController.java  # Public OTP-verified subscription endpoints
│   │   │   │   ├── dto/                      # SubscriptionDto, CreateSubscriptionRequest, OTP DTOs
│   │   │   │   ├── mapper/                   # SubscriptionMapper
│   │   │   │   ├── model/                    # Subscription (auth users), Subscriber (public)
│   │   │   │   ├── repository/               # SubscriptionRepository, SubscriberRepository
│   │   │   │   └── service/
│   │   │   │       ├── SubscriptionService.java     # Haversine 20KM matching & subscriber dispatcher
│   │   │   │       └── OTPService.java              # In-memory OTP cache & verification
│   │   │   │
│   │   │   ├── Upload/                       # Cloudinary Media Storage
│   │   │   │   ├── config/CloudinaryConfig.java
│   │   │   │   ├── controller/UploadController.java # Multipart image validation & upload
│   │   │   │   └── service/CloudinaryService.java   # Upload / delete image assets
│   │   │   │
│   │   │   └── User/                         # User Profile Module
│   │   │       ├── controller/UserController.java
│   │   │       ├── dto/                      # UserDto, UpdateUserRequest, ChangePasswordRequest
│   │   │       ├── mapper/UserMapper.java
│   │   │       ├── model/User.java, Role.java
│   │   │       ├── repository/UserRepository.java
│   │   │       └── service/UserService.java  # Profile edits, avatar upload, cascade deletion
│   │   │
│   │   └── resources/
│   │       └── application.properties        # Server port, database, JWT, Mail, Twilio configs
│   │
│   └── test/
│       ├── java/com/tracenest/backend/       # Automated Unit & Integration Tests (15 tests)
│       │   ├── Auth/service/AuthServiceTest.java
│       │   ├── Report/service/PosterServiceTest.java
│       │   ├── Report/service/ReportServiceTest.java
│       │   └── Subscription/service/SubscriptionServiceTest.java
│       └── resources/
│           └── mockito-extensions/org.mockito.plugins.MockMaker # Java 25 subclass mockmaker
```

---

## Prerequisites

1. **JDK 21 LTS** (Eclipse Temurin, Oracle JDK, or OpenJDK 21)
2. **Apache Maven 3.9+** (installed and added to `PATH`)
3. **PostgreSQL 14+** (running on `localhost:5432`)

---

## Configuration & Environment Variables

All settings reside in [`src/main/resources/application.properties`](file:///d:/koyan/Documents/OneDrive/Desktop/TraceNest/TraceNest-Backend/src/main/resources/application.properties). They can be overridden via environment variables or `.env`:

| Property | Environment Variable | Default Value | Description |
|---|---|---|---|
| `server.port` | `PORT` | `8080` | Port the backend listens on |
| `app.cors.allowed-origins` | `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,...` | Allowed CORS origins |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/tracenest` | PostgreSQL connection string |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `postgres` | Database username |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | `postgres` | Database password |
| `app.jwt.secret` | `APP_JWT_SECRET` | Base64 Secret | 256-bit HMAC secret |
| `app.jwt.expiration-ms` | `APP_JWT_EXPIRATION_MS` | `900000` (15 min) | Access token validity |
| `app.jwt.refresh-expiration-ms` | `APP_JWT_REFRESH_EXPIRATION_MS` | `2592000000` (30 days) | Refresh token validity |
| `cloudinary.cloud-name` | `CLOUDINARY_CLOUD_NAME` | `demo` | Cloudinary cloud identifier |
| `cloudinary.api-key` | `CLOUDINARY_API_KEY` | `mock_key` | Cloudinary API Key |
| `cloudinary.api-secret` | `CLOUDINARY_API_SECRET` | `mock_secret` | Cloudinary API Secret |
| `spring.mail.username` | `MAIL_USERNAME` | `your_email@gmail.com` | SMTP username |
| `spring.mail.password` | `MAIL_PASSWORD` | `your_app_password` | SMTP App password |
| `twilio.account-sid` | `TWILIO_ACCOUNT_SID` | `mock_sid` | Twilio Account SID |
| `twilio.auth-token` | `TWILIO_AUTH_TOKEN` | `mock_token` | Twilio Auth Token |
| `twilio.phone-number` | `TWILIO_PHONE_NUMBER` | `+15733633511` | Twilio Phone Number |

> **Development Note**: Fallback defaults are configured for Cloudinary, Twilio, and Spring Mail. The backend boots and runs unit tests out-of-the-box without requiring live third-party credentials.

---

## Database Setup

1. Start your local PostgreSQL server on port `5432`.
2. Ensure user credentials match `application.properties` (default: user `postgres`, password `postgres`).
3. Create the database `tracenest` using `psql` or `pgAdmin`:
   ```sql
   CREATE DATABASE tracenest;
   ```
4. Hibernate's `ddl-auto=update` manages table schema creation and non-destructive column updates automatically on first boot.

---

## Building & Running

### 1. Compile the Code
```bash
mvn clean compile
```

### 2. Run the Application
```bash
mvn spring-boot:run
```
The API starts on `http://localhost:8080`.

---

## Running Automated Tests

The test suite contains 15 unit and integration tests executing under Java 25:

```bash
mvn test
```

### Test Coverage Breakdown:
- **`AuthServiceTest`** (5 tests):
  - User registration with unique email validation & BCrypt hashing.
  - Conflict handling (`409 EMAIL_TAKEN`) on duplicate registration.
  - Password reset token generation & 1-hour expiry setting.
  - Password reset consumption and hash update.
  - Expired token rejection (`TOKEN_EXPIRED`).
- **`PosterServiceTest`** (2 tests):
  - Binary PDF generation verifying size and `%PDF-` magic byte header.
  - Category-specific formatting (pet vs stolen vehicle).
- **`ReportServiceTest`** (5 tests):
  - Report detail retrieval and 404 handling.
  - Status updates (`MISSING` -> `FOUND` -> `RESOLVED`).
  - Owner authorization enforcement (`403 NOT_OWNER`).
  - Aggregate statistics calculation across types and statuses.
- **`SubscriptionServiceTest`** (3 tests):
  - Authenticated alert subscription persistence.
  - Haversine 20KM distance matching triggering in-app and email dispatch.
  - Far-away location filtering preventing false-positive alerts.

---

## API Reference

### Authentication (`/api/v1/auth`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | None | Register account (`name`, `email`, `password`, `phone`, `city`) |
| `POST` | `/api/v1/auth/login` | None | Authenticate with email/password; returns JWT access + refresh tokens |
| `POST` | `/api/v1/auth/refresh` | None | Exchange valid refresh token for a new access token |
| `POST` | `/api/v1/auth/logout` | Bearer | Invalidate refresh token |
| `POST` | `/api/v1/auth/forgot-password` | None | Generate UUID reset token and send reset link |
| `POST` | `/api/v1/auth/reset-password` | None | Validate reset token and update password |
| `GET` | `/api/v1/auth/me` | Bearer | Fetch authenticated user details |

### User Profile (`/api/v1/users`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/users/me` | Bearer | Get profile |
| `PATCH` | `/api/v1/users/me` | Bearer | Update name, phone, city |
| `POST` | `/api/v1/users/me/avatar` | Bearer | Upload profile picture to Cloudinary |
| `POST` | `/api/v1/users/me/change-password` | Bearer | Validate current password and set new password |
| `DELETE` | `/api/v1/users/me` | Bearer | Delete user, associated reports, and Cloudinary media |

### Reports (`/api/v1/reports`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/reports` | None | Search reports (`q`, `type`, `status`, `city`, `lat`, `lng`, `radiusKm`, `sort`, `page`, `size`) |
| `GET` | `/api/v1/reports/{id}` | None | Retrieve single report detail |
| `POST` | `/api/v1/reports` | Bearer | Multipart report creation with JSON `request` + `images` files (1–5 files, ≤5MB) |
| `PATCH` | `/api/v1/reports/{id}` | Bearer | Update report details (owner only) |
| `PATCH` | `/api/v1/reports/{id}/status` | Bearer | Update status (`missing`, `found`, `resolved`) |
| `DELETE` | `/api/v1/reports/{id}` | Bearer | Delete report, remove images from Cloudinary, delete notifications |
| `GET` | `/api/v1/reports/mine` | Bearer | Paginated list of reports filed by current user |
| `GET` | `/api/v1/reports/map` | None | Compact geo-coordinate list for Leaflet map markers |
| `GET` | `/api/v1/reports/stats` | None | Global platform statistics |
| `GET` | `/api/v1/reports/{id}/poster` | None | **Generate printable A4 PDF poster** (`application/pdf`) |

### Subscriptions & Alerts (`/api/v1/subscriptions` & `/api/v1/subscribe`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/subscriptions` | Bearer | Create geographic alert preference (`city`, `types`, `radiusKm`, `lat`, `lng`) |
| `GET` | `/api/v1/subscriptions` | Bearer | List current user's subscriptions |
| `PATCH` | `/api/v1/subscriptions/{id}` | Bearer | Update subscription criteria |
| `DELETE` | `/api/v1/subscriptions/{id}` | Bearer | Delete alert subscription |
| `POST` | `/api/v1/subscribe/send-otp` | None | Send 6-digit verification code to mobile number |
| `POST` | `/api/v1/subscribe/verify-otp` | None | Verify phone number code |
| `POST` | `/api/v1/subscribe` | None | Register public alert subscriber with location & contact |

### In-App Notifications (`/api/v1/notifications`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/notifications` | Bearer | List user notifications (`unreadOnly`, `page`, `size`) |
| `POST` | `/api/v1/notifications/{id}/read` | Bearer | Mark single notification as read |
| `POST` | `/api/v1/notifications/read-all` | Bearer | Mark all notifications as read |

### Dashboard (`/api/v1/dashboard/summary`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/dashboard/summary` | Bearer | Returns personal stats (`total`, `active`, `found`, `resolved`), community stats, and top 5 recent cases |

---

## Core System Implementations

### 1. 20KM Haversine Proximity Engine
When a report is filed, `SubscriptionService` evaluates both registered user subscriptions and public subscribers using the Haversine spherical formula:

$$\Delta\sigma = 2 \cdot \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta\phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta\lambda}{2}\right)}\right)$$
$$d = R \cdot \Delta\sigma \quad (R = 6371\text{ km})$$

- If distance $\le 20\text{ km}$ (or configured `radiusKm`) or if the report is flagged as city-wide:
  - An in-app `Notification` entity is created for registered users.
  - An email is queued asynchronously via `NotificationDeliveryService`.
  - An SMS is queued asynchronously via Twilio.

### 2. Automated PDF Poster Generation
[`PosterService.java`](file:///d:/koyan/Documents/OneDrive/Desktop/TraceNest/TraceNest-Backend/src/main/java/com/tracenest/backend/Report/service/PosterService.java) dynamically builds an A4 printable document using OpenPDF:
- High-visibility banner (e.g. `MISSING ALERT`, `LOST PET ALERT`, `STOLEN VEHICLE ALERT`).
- Case title, status, and category.
- Primary Cloudinary image embedded with scale fitting.
- Two-column grid detailing last-seen coordinates and type-specific attributes (breed, color, registration number, FIR number).
- Reward highlight banner (if offered).
- High-contrast immediate contact action box.
- TraceNest footer with unique report ID and platform link.

### 3. Cloudinary Media Storage
Image uploads in [`UploadController.java`](file:///d:/koyan/Documents/OneDrive/Desktop/TraceNest/TraceNest-Backend/src/main/java/com/tracenest/backend/Upload/controller/UploadController.java) and [`UserService.java`](file:///d:/koyan/Documents/OneDrive/Desktop/TraceNest/TraceNest-Backend/src/main/java/com/tracenest/backend/User/service/UserService.java) validate:
- Allowed MIME types: `image/jpeg`, `image/png`, `image/webp`, `image/gif`.
- Max file size: 5MB per image (up to 5 images per request).
- When a report or user is deleted, all referenced public IDs are destroyed on Cloudinary to prevent orphaned storage.

### 4. Multi-Channel Notification Dispatch
- **Async Execution**: `NotificationDeliveryService` methods are annotated with `@Async("asyncExecutor")` backed by `ThreadPoolTaskExecutor` (10 core, 50 max threads) defined in [`AsyncConfig.java`](file:///d:/koyan/Documents/OneDrive/Desktop/TraceNest/TraceNest-Backend/src/main/java/com/tracenest/backend/Common/config/AsyncConfig.java).
- Slow email or SMS network I/O never blocks report creation or API latency.

### 5. Future AI Pipeline Hook
[`AiProcessingService.java`](file:///d:/koyan/Documents/OneDrive/Desktop/TraceNest/TraceNest-Backend/src/main/java/com/tracenest/backend/Report/service/AiProcessingService.java) provides an isolated asynchronous post-processing entry point invoked upon report creation. This structure allows future modules (CLIP vision embedding, SBERT semantic search, OCR plate scanning) to be integrated seamlessly next semester without altering core CRUD or returning synthetic AI data.

---

## Security & Authorization

1. **Password Protection**: Passwords encoded with BCrypt (`strength = 10`).
2. **Stateless JWT**: Access tokens signed using HMAC-SHA256 containing `sub` (userId) and `role` claims.
3. **Endpoint Access Control**:
   - Public: `/api/v1/auth/**`, `/api/v1/subscribe/**`, `GET /api/v1/reports/**` (search, detail, map, stats, poster).
   - Authenticated: `GET /api/v1/reports/mine`, `POST /api/v1/reports`, `PATCH /api/v1/reports/**`, `DELETE /api/v1/reports/**`, `/api/v1/subscriptions/**`, `/api/v1/notifications/**`, `/api/v1/users/**`.
4. **Ownership Verification**: Mutating operations on reports, subscriptions, and notifications strictly verify that `entity.getReporter().getId().equals(userId)` before permitting changes.
