# Nagrivic

> Civic issue reporting, transparency, and accountability platform empowering citizens and local governance.

---

## 1. What Nagrivic Is

**Nagrivic** is a civic tech platform designed to bridge the gap between citizens and municipal authorities. When citizens encounter urban problems in their neighborhoods—such as hazardous potholes, overflowing garbage dumps, broken streetlights, water pipeline leaks, or clogged drainage systems—they can quickly report them with:

- Geotagged high-resolution photos
- Precise GPS coordinates
- Structured civic category
- Contextual description

Nagrivic tracks each issue through its entire lifecycle: verifying reports, detecting duplicates, mapping to the responsible local authority/ward, enabling community support and discussion, tracking resolution progress in real time, and giving citizens the power to verify whether the problem was genuinely fixed on the ground.

---

## 2. Current MVP Scope

- **Initial Launch Location**: Ahmedabad, Gujarat, India (starting with a pilot ward).
- **Initial Civic Categories**:
  1. **Roads / Potholes** (craters, uneven resurfacing, cave-ins)
  2. **Garbage** (open garbage dumps, overflowing community bins, uncollected waste)
  3. **Streetlights** (non-functional lights, damaged poles, dark accident zones)
  4. **Water** (pipeline bursts, contaminated supply, low pressure)
  5. **Drainage** (overflowing gutters, waterlogging, broken manhole covers)

---

## 3. Technology Stack

| Layer | Technology | Status |
|---|---|---|
| **Mobile Application** | React Native, Expo, Expo Router, TypeScript | Foundation Active (`NagrivicApp/`) |
| **Public Website** | Next.js, React, TypeScript | Foundation Established (`web/`) |
| **Admin & Authority Dashboard** | Next.js, React, TypeScript | Foundation Established (`web/`) |
| **Backend API** | Java 23, Spring Boot 4 (Modular Monolith) | Skeleton Active (`backend/`, `/api/health` UP) |
| **Database** | PostgreSQL with PostGIS extension | Configured with env variables |
| **Cache (Future)** | Redis (optional for MVP) | Planned |
| **Object Storage (Future)** | S3-compatible cloud object storage | Planned |
| **Notifications (Future)** | Firebase Cloud Messaging (FCM) & SMS | Planned |
| **AI / ML Service (Future)** | Python microservice (computer vision & duplicate detection) | Planned (Post-MVP) |

---

## 4. High-Level Architecture

Nagrivic is built on the core principle of **one central backend and one central spatial database**.

```
                    NAGRIVIC
                       |
        +--------------+--------------+
        |              |              |
      MOBILE          WEB          ADMIN
        |              |              |
        +--------------+--------------+
                       |
                    REST API
                       |
                SPRING BOOT
                       |
        +--------------+--------------+
        |                             |
   PostgreSQL                       PostGIS
        |
   Object Storage
   (future)
```

---

## 5. Architectural & Standards Documentation

Detailed technical specifications and design conventions are located in `docs/`:

- [System Architecture (docs/architecture.md)](docs/architecture.md): Layer responsibilities, modular monolith rules, mobile/web structures, PostGIS rationale.
- [REST API Conventions (docs/api-conventions.md)](docs/api-conventions.md): Base path, endpoint designs, HTTP codes, standard error envelope, pagination, spatial parameters.
- [Database Conventions (docs/database-conventions.md)](docs/database-conventions.md): PostgreSQL + PostGIS standards, table/column naming, spatial types (`geography` vs `geometry`), GiST indexing.
- [Coding Standards (docs/coding-standards.md)](docs/coding-standards.md): TypeScript strict standards, Spring Boot layered architecture (Controller → Service → Repository), DTO records, Git conventions.
- [Security Architecture (docs/security.md)](docs/security.md): Zero-secrets policy, auth roadmap, citizen privacy, rate limiting, media upload security, audit logging.

---

## 6. Core Development Principles

1. **Build the simplest correct solution**: Solve today's civic reporting requirements with clean, direct code.
2. **Do not over-engineer the MVP**: Focus on reporting, tracking, and verifying civic issues in Ahmedabad before building complex abstractions.
3. **Avoid microservices**: Run one modular monolith with one central database. Distributed systems add premature operational debt.
4. **Avoid unnecessary dependencies**: Evaluate every external library; prefer standard language and framework capabilities.
5. **Keep business logic testable**: Write clean domain services with unit and integration tests.
6. **Keep API contracts explicit**: Strict request and response DTOs with validation rules and clear error responses.
7. **Keep location data first-class**: Geographic coordinates and PostGIS spatial capabilities are central to duplicate detection and ward routing.
8. **Protect citizen privacy**: Never expose personal phone numbers or citizen identities publicly without explicit consent.
9. **Maintain auditability for important actions**: All status changes, department reassignments, and administrative actions must be logged.
10. **Prefer maintainability over clever code**: Write clear, self-documenting code with consistent naming.
11. **Do not introduce AI until the core system works**: Establish reliable manual reporting and resolution workflows before layering automated computer vision.
12. **Do not make government integration a day-one dependency**: The platform must deliver value to citizens and ward officers independently through its own dashboards before building direct government API adapters.
13. **Ahmedabad is the initial launch scope**: Design ward structures, categories, and routing for Ahmedabad Municipal Corporation (AMC) as the pilot before scaling to other cities.
14. **Architecture should be scalable without premature complexity**: High-cohesion modular boundaries ensure smooth growth without the friction of distributed microservices.

---

## 7. Repository Structure

```
nagrivic/
├── NagrivicApp/         # Mobile application (React Native + Expo + TypeScript)
├── backend/             # Central backend REST API (Java 23 + Spring Boot Modular Monolith)
├── web/                 # Next.js web portals (public website & admin dashboard)
├── docs/                # Architecture diagrams, API/DB standards, security principles
│   ├── architecture.md
│   ├── api-conventions.md
│   ├── database-conventions.md
│   ├── coding-standards.md
│   └── security.md
├── .gitignore           # Unified monorepo gitignore
└── README.md            # Root documentation
```

*Note: The existing mobile application is maintained inside `NagrivicApp/` preserving all existing dependencies, file-based routing, and assets.*

---

## 8. Local Development Prerequisites

Ensure you have the following installed on your development machine:

1. **Java JDK**: Version 21 or 23 (JDK 23.0.1 verified).
2. **Node.js & npm**: Node.js v20+ (Node v24.14.0 and npm 11.19.1 verified).
3. **Expo Go / Android Studio / Xcode**: For mobile preview.
4. **PostgreSQL 15+ with PostGIS 3.3+**: For future database connection (configured via environment variables).

---

## 9. How to Run the Mobile Application

1. Navigate to the mobile application directory:
   ```bash
   cd NagrivicApp
   ```

2. Verify TypeScript compilation:
   ```bash
   npx tsc --noEmit
   ```

3. Start the Expo development server:
   ```bash
   npx expo start
   ```

4. Run on specific platforms:
   - Press `w` for Web preview
   - Press `a` for Android emulator
   - Scan QR code with the **Expo Go** mobile app on iOS/Android

---

## 10. How to Run the Backend

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```

2. Build the project using the self-contained Gradle wrapper:
   ```powershell
   # Windows PowerShell / CMD
   .\gradlew.bat build

   # macOS / Linux
   ./gradlew build
   ```

3. Run the Spring Boot application:
   ```powershell
   # Windows PowerShell / CMD
   .\gradlew.bat bootRun

   # macOS / Linux
   ./gradlew bootRun
   ```

4. Verify backend health endpoint:
   ```bash
   curl http://localhost:8080/api/health
   ```

   Response:
   ```json
   {
     "status": "UP",
     "service": "nagrivic-backend"
   }
   ```

---

## 11. Development Roadmap & Current Status

- **Phase 1: Project Foundation** - **COMPLETED**
  - Unified monorepo structure, modular monolith backend skeleton, basic `/api/health` endpoint, documentation, Expo web preview.
- **Phase 2: Architecture & Coding Standards** - **COMPLETED (Current)**
  - Comprehensive architectural specifications, layer responsibilities, REST API contracts, PostGIS database conventions, TypeScript & Java coding standards, security principles, and 14 development principles.
- **Phase 3: Spatial Database & PostGIS Setup** - *Next*
  - PostgreSQL + PostGIS schemas, spatial indexes, ward boundary polygons for Ahmedabad pilot ward.
- **Phase 4: Authentication & Core Identity** - *Upcoming*
  - Citizen mobile OTP authentication, authority roles, JWT security.
- **Phase 5: Issue Reporting & Media Pipeline** - *Upcoming*
  - Geotagged camera capture, S3 pre-signed uploads, issue state machine, duplicate proximity query (`ST_DWithin`).
- **Phase 6: Civic Engagement & Community Accountability** - *Upcoming*
  - Issue upvoting ("Support"), discussion threads, citizen resolution verification.
- **Phase 7: Web Portals & Authority Routing** - *Upcoming*
  - Next.js public transparency portal and AMC ward admin dashboard.
- **Phase 8: AI Verification Service** - *Post-MVP*
  - Computer vision for image quality, automated category tagging, and severity scoring.
