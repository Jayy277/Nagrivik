# Nagrivic System Architecture

## 1. High-Level System Architecture

Nagrivic is architected around the core principle of **one central backend and one central spatial database**, serving all client platforms via a unified REST API:

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

## 2. Layer Responsibilities

### Mobile Application (`NagrivicApp/` or `mobile/`)
- **Primary Citizen Touchpoint**: React Native + Expo + TypeScript.
- **Reporting Civic Issues**: Geotagged camera capture, category selection, and description.
- **Viewing Public Issues**: Interactive feed and map-based visualization of reported problems.
- **Supporting Issues**: Community upvoting to elevate priority issues.
- **Comments & Updates**: Public discussion threads and evidence updates.
- **Map & Location**: User GPS positioning, map browsing, and nearby issue markers.
- **Resolution Verification**: Citizen-driven confirmation of whether an issue was genuinely resolved on the ground.

### Public Website (`web/public`)
- **Public Discovery**: Next.js + React + TypeScript web portal for citizens, journalists, and civic activists.
- **Public Issue Pages**: SEO-optimized, indexable detail pages for every public issue.
- **Civic Map & Transparency**: City-wide map of open, in-progress, and resolved issues.
- **Search & Filtering**: Filter by category, ward, status, date, and support count.
- **Future Web Reporting**: Citizen reporting directly through the web browser.

### Admin & Authority Dashboard (`web/admin`)
- **Administrative Portal**: Next.js + React + TypeScript workspace for municipal engineers, ward officers, and platform moderators.
- **Moderation Queue**: Review incoming reports for spam, inappropriate content, or false submissions.
- **Departmental Routing**: Assigning tickets to the relevant municipal department (e.g., Solid Waste Management, Roads, Water & Drainage, Light).
- **Resolution Tracking & SLAs**: Monitoring resolution deadlines, field worker notes, and resolution evidence.
- **Spatial Analytics**: Ward-level heatmaps, recurring problem zones, and department performance leaderboards.

### Central Backend (`backend/`)
- **Single Spring Boot Deployment**: Modular monolith running Java 23 and Spring Boot.
- **Business Logic**: Enforcement of domain rules, state machines, and workflows.
- **Authentication & Authorization**: Google Sign-In as primary citizen authentication (cryptographic ID token verification via Google API client, multi-client ID allowlist, `user_auth_identities` provider mapping, auto-provisioning `CITIZEN` role), phone OTP retained for test/secondary flows, authority role-based access control (RBAC), stateless JWT access tokens and rotating SHA-256 hashed refresh token sessions.
- **Issue Lifecycle**: State transitions (Submitted → Verified → In-Progress → Resolved → Citizen-Confirmed).
- **User Management**: Profile data, officer assignments, reputation scores.
- **Geographic Mapping & Spatial Queries**: Coordinates, ward polygons, and spatial distance checks.
- **Authority & Representative Routing**: Automatic point-in-polygon mapping to ward councilors and municipal zones.
- **Notifications**: Push notifications (FCM), SMS delivery, and status change alerts.
- **Analytics & Reporting**: Aggregated metrics on resolution times, open issue counts, and citizen participation.

### Database (PostgreSQL)
- **Application Persistence**: Structured relational storage for users, issues, comments, supports, authorities, departments, and audit logs.
- **ACID Transactions**: Consistent, reliable transactions across related entities within the modular monolith.

### PostGIS (Spatial Database Extension)
- **Geographic Coordinates**: Issues stored with native `geography(Point, 4326)` data types.
- **Nearby Issue Queries**: High-speed spatial proximity checks (`ST_DWithin`) to identify nearby duplicates within a 15-meter radius.
- **Ward & Jurisdiction Boundaries**: Stored as `geometry(Polygon, 4326)` or `MultiPolygon`.
- **Geographic Containment (`ST_Contains`)**: Automatic detection of which municipal ward and administrative zone an issue falls within.
- **Spatial Indexing (GiST)**: Sub-millisecond bounding box and nearest-neighbor spatial queries.

### Object Storage (Task 49 Production Foundation)
- **S3-Compatible Cloud Storage**: Provider-independent abstraction (`ObjectStorageProvider`) supporting AWS S3, MinIO, Cloudflare R2, Google Cloud Storage, Local Filesystem, and No-Op storage.
- **Direct & Presigned Media Workflows**: Presigned upload/download URLs for citizen issue photos and resolution evidence, isolating heavy I/O from backend application servers.

---

## 3. Backend Modular Monolith

The backend is strictly structured as a **Modular Monolith** within a single deployable Spring Boot application.

```
backend/
└── src/
    └── main/
        └── java/
            └── org/nagrivic/
                ├── NagrivicBackendApplication.java
                ├── config/
                ├── health/
                └── modules/
                    ├── auth/
                    ├── users/
                    ├── issues/
                    ├── media/
                    ├── locations/
                    ├── categories/
                    ├── authorities/
                    ├── departments/
                    ├── representatives/
                    ├── supports/
                    ├── comments/
                    ├── status/
                    ├── moderation/
                    ├── notifications/
                    ├── push/
                    ├── ai/
                    └── admin/
```

### Modular Monolith Rules

1. **Self-Contained Business Logic**: Each module owns its business logic, services, and repositories.
2. **Explicit Application Interfaces**: When Module A needs functionality from Module B, it communicates through typed Java service interfaces or Spring domain application events (`ApplicationEventPublisher`). Direct dependency on another module's internal repositories or entities is prohibited.
3. **No Cross-Module Database Coupling**: Modules must not execute direct cross-module joins or manipulate other modules' tables directly.
4. **Single Spring Boot Deployment**: The entire backend compiles into and runs as a single JAR file.
5. **No Microservices**: Microservices introduce distributed transactions, network latency, serialization overhead, and deployment complexity that are unnecessary and harmful at MVP stage.
6. **Single Database**: All modules share one PostgreSQL + PostGIS database instance. Do not create separate databases per module.
7. **Future Extraction Path**: Because module boundaries are strictly enforced via clear domain interfaces and package structures, any module (e.g., `notifications` or `media`) can be extracted into an independent microservice in the future **only if real scale requirements justify it**.

### 3.1 Push Notification Architecture (FCM Delivery Channel)

Push notifications (Task 50) are designed as a non-blocking delivery channel that complements the authoritative in-app notification domain (Task 23):

```
+-------------------------------------------------------------------------------+
|                             DATABASE TRANSACTION                              |
|                                                                               |
|  Status Change / Comment / Assignment                                         |
|         |                                                                     |
|         v                                                                     |
|  NotificationService.createNotification(...)                                  |
|         |                                                                     |
|         +---> Persist NotificationEntity (PostgreSQL)                         |
|         +---> ApplicationEventPublisher.publishEvent(NotificationCreatedEvent)|
|         |                                                                     |
|  COMMIT TRANSACTION                                                           |
+-------------------------------------------------------------------------------+
                                      |
                                      v (AFTER_COMMIT Phase)
+-------------------------------------------------------------------------------+
|                       PUSH DISPATCHER & DELIVERY CHANNEL                      |
|                                                                               |
|  PushNotificationDispatcher.onNotificationCreated(event)                      |
|         |                                                                     |
|         +---> Check User Notification Preferences                             |
|         +---> Lookup Active User Device Tokens (push_devices)                 |
|         +---> PushNotificationProvider.sendMulticast(tokens, message)         |
|         |        |                                                            |
|         |        +---> FcmPushNotificationProvider (Firebase Cloud Messaging) |
|         |        +---> NoOpPushNotificationProvider (Offline / Tests)         |
|         |                                                                     |
|         +---> Record Delivery Audit (notification_push_deliveries)            |
|         +---> Auto-deactivate Invalid/Unregistered Tokens                     |
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                        MOBILE CLIENT (NagrivicApp)                            |
|                                                                               |
|  1. Register Push Token: POST /api/push/devices                               |
|  2. Receive Notification -> Open Deep Link: nagrivicapp://issue/{issueId}     |
|  3. Logout Deactivation: POST /api/push/devices/deactivate                     |
+-------------------------------------------------------------------------------+
```

**Key Architectural Guarantees**:
- **Strict Post-Commit Dispatch**: Push notification dispatch is hooked via `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` with an immutable scalar payload event (`NotificationCreatedEvent`). Database commits NEVER fail or roll back due to push delivery issues.
- **Provider Decoupling**: Application core codes to `PushNotificationProvider` interface. Switching between FCM, APNs, or a mock provider requires only configuration updates.
- **Automatic Token Lifecycle**: Tokens returning `UNREGISTERED` or `INVALID_ARGUMENT` from Firebase are immediately marked inactive (`is_active = FALSE`).
- **Privacy & Minimization**: Push notification data payloads contain only minimal identifiers (`notificationId`, `issueId`, `type`, `deepLink`) and strictly omit personally identifiable information (PII) or user coordinates.

---

## 4. Mobile Architecture (React Native + Expo)

Nagrivic uses the existing React Native + Expo + TypeScript foundation in `NagrivicApp/`.

### Directory Structure & Conventions

```
NagrivicApp/ (or mobile/)
└── src/
    ├── app/               # Expo Router file-based routes and screen layouts
    │   ├── _layout.tsx    # Root layout, providers, navigation shell
    │   ├── (tabs)/        # Bottom tab navigation screens (Home, Map, Report, Profile)
    │   ├── issues/        # Issue detail, comments, and resolution verification screens
    │   └── auth/          # OTP login and phone verification screens
    ├── features/          # Feature-scoped business logic and UI
    │   ├── issues/        # Issue cards, issue feed hooks, report form components
    │   ├── map/           # Interactive map view, clustering, location picker
    │   ├── auth/          # Phone input, OTP input, auth context/hook
    │   └── profile/       # User profile, reported issues history, settings
    ├── components/        # Reusable design system UI components
    │   ├── button/        # Primary, secondary, icon buttons
    │   ├── card/          # Issue card, statistic card
    │   ├── input/         # Text input, search input
    │   └── badge/         # Status badge, category tag
    ├── services/          # HTTP API client (Fetch/Axios), token handling
    ├── hooks/             # Generic reusable React hooks (useDebounce, useLocation, useTheme)
    ├── types/             # Shared TypeScript models (Issue, User, Category, Location)
    ├── constants/         # Design tokens (Colors, Typography, Spacing, Layout)
    ├── utils/             # Pure helper functions (formatDate, distanceCalculator, validation)
    ├── navigation/        # Deep linking and navigation configuration
    └── assets/            # Static images, icons, and fonts
```

### Mobile Architectural Guidelines
- **Existing Code Preservation**: The existing Expo structure is preserved. New features will gradually adopt this feature-based organization without disrupting existing functioning screens.
- **Thin Screens, Rich Features**: Screens in `src/app/` act as thin orchestrators; complex UI and business logic reside in `src/features/<feature>/`.
- **Centralized API Client**: All backend HTTP calls route through `src/services/apiClient.ts` to ensure consistent base URLs, headers, and error handling.
- **Offline & Optimistic UI**: Issue browsing should support cached offline data; support/upvoting should reflect optimistically with graceful rollback on network error.

---

## 5. Website Architecture (Next.js)

The web platform in `web/` serves both the public discovery website and the future authority dashboard.

### Directory Structure & Conventions

```
web/
├── app/                   # Next.js App Router (pages, layouts, error boundaries)
│   ├── (public)/          # Public routes (landing, civic map, issue details)
│   │   ├── page.tsx       # Homepage / Civic dashboard
│   │   ├── issues/        # Public issue pages (/issues/[id])
│   │   └── map/           # Fullscreen interactive civic issue map
│   ├── (auth)/            # Login & authentication routes
│   └── (admin)/           # Protected authority & admin dashboard routes
├── components/            # Shared, reusable UI components (Header, Footer, Button, Modal)
├── features/              # Feature-specific components and state
│   ├── issues/            # Issue cards, status filters, public issue view
│   ├── map/               # MapLibre / Leaflet map viewer and marker clusters
│   └── admin/             # Ticket management tables, assignment modals, SLA alerts
├── lib/                   # Third-party library initializations & configurations
├── services/              # API communication layer calling the Spring Boot backend
├── hooks/                 # Custom React hooks
├── types/                 # Shared TypeScript interfaces (IssueDTO, FilterParams, User)
├── utils/                 # Formatting, date helpers, validation utilities
└── public/                # Static assets (favicons, logos, illustrations)
```

### Web Architectural Guidelines
- **Server vs. Client Components**:
  - **Server Components (Default)**: Use for data fetching, static content, and public SEO-indexable pages (e.g., issue details, landing page).
  - **Client Components (`'use client'`)**: Use strictly when user interaction, state (`useState`, `useEffect`), browser APIs, or interactive map rendering are required.
- **Backend Communication**: Next.js communicates strictly via HTTP REST to the Spring Boot backend (`http://localhost:8080/api`). No direct database connections from Next.js.
- **SEO & Social Sharing**: Public issue pages (`/issues/[id]`) generate dynamic OpenGraph metadata with geotags and issue photos for social media sharing.
- **Admin Isolation**: Admin routes (`/(admin)/*`) are guarded by role-based middleware validating authority JWT tokens before rendering.

---

## 6. Development Principles

Nagrivic adheres to 14 core engineering principles:

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
