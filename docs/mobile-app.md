# Nagrivic Mobile App UI Foundation

## 1. Overview & Architectural Philosophy

Nagrivic is a citizen-first civic issue reporting and municipal accountability mobile application built with **React Native**, **Expo Router**, and **TypeScript**.

The application prioritizes:
- **Trustworthy Civic Branding**: Clean, authoritative, accessible design without political party or governmental department co-optation.
- **Immediate Actionability**: Prominent reporting actions, clear hazard visibility, and transparent municipal workflow stages.
- **Privacy & Safety**: Zero public leakage of citizen phone numbers, private emails, or supporter identities.
- **Clean Architecture**: Decoupled design tokens, isolated mock datasets, stateless UI components, and prepared API client interfaces.

> [!IMPORTANT]
> **Mock Data Policy**:
> Mock data is UI-only and must never be treated as production backend data. All mock records live exclusively under `src/mocks/` and are never imported into production API clients.

---

## 2. Navigation & Route Architecture

The mobile application utilizes **Expo Router** file-based routing:

```text
src/app/
├── _layout.tsx                     # Root provider: ThemeProvider + AuthProvider + Stack
├── index.tsx                       # Root redirector based on Auth status
├── (auth)/
│   ├── _layout.tsx                 # Auth stack navigator
│   ├── welcome.tsx                 # Civic brand intro & value pillars
│   ├── phone.tsx                   # Indian (+91) phone number entry
│   └── otp.tsx                     # 6-digit OTP verification & countdown UI
├── (tabs)/
│   ├── _layout.tsx                 # Bottom tab bar with prominent center Report CTA
│   ├── index.tsx                   # Home: Greeting, Area context, CTA, Category pills, Feed
│   ├── nearby.tsx                  # Nearby: Geographic discovery, Ward context, Distance cards
│   ├── report.tsx                  # Report: 5-step complaint wizard (Photo, Cat, Loc, Desc, Submit)
│   ├── notifications.tsx           # Updates: Municipal alerts, status transition feeds
│   └── profile.tsx                 # Citizen profile, engagement statistics, settings
└── issue/
    └── [id].tsx                    # Issue Detail: Media, Responsibility, Timeline, Support CTA
```

---

## 3. Design System & Theming

All tokens and semantic styles are centralized in `src/theme/`:

### 3.1 Colors & Semantics (`src/theme/colors.ts`)
- **Primary Brand**: Civic Blue (`#1D4ED8`) with Dark Indigo variant (`#1E3A8A`) and Light tint (`#EFF6FF`).
- **Neutrals**: Tailwind Slate scale (`#0F172A` to `#F8FAFC`).
- **Semantic Status Badges**:
  - `REPORTED`: Warm Amber (`#D97706`, `#FEF3C7`)
  - `VERIFIED`: Sky Blue (`#0284C7`, `#E0F2FE`)
  - `ACKNOWLEDGED`: Indigo (`#6366F1`, `#EEF2FF`)
  - `IN_PROGRESS`: Blue (`#2563EB`, `#DBEAFE`)
  - `RESOLVED`: Emerald (`#059669`, `#D1FAE5`)
  - `CITIZEN_VERIFIED`: Dark Emerald (`#047857`, `#A7F3D0`)
  - `NOT_FIXED`: Red (`#DC2626`, `#FEE2E2`)
- **Priority Badges**:
  - `LOW`: Slate (`#64748B`, `#F1F5F9`)
  - `MEDIUM`: Amber (`#D97706`, `#FEF3C7`)
  - `HIGH`: Orange (`#EA580C`, `#FFEDD5`)
  - `CRITICAL`: Red (`#DC2626`, `#FEE2E2`)

### 3.2 Typography Scale (`src/theme/typography.ts`)
- `display` (28px/34px, bold)
- `heading` (22px/28px, bold)
- `title` (18px/24px, semi-bold)
- `body` / `bodyBold` (15px/22px)
- `caption` / `captionMedium` (13px/18px)
- `button` (15px/20px, bold)
- `badge` (12px/16px, semi-bold)

### 3.3 Spacing & Layout (`src/theme/spacing.ts`)
- Spacing: `xxs: 2`, `xs: 4`, `sm: 8`, `md: 12`, `lg: 16`, `xl: 20`, `xxl: 24`, `huge: 32`
- Touch targets: Minimum 44px for accessibility.
- Max content width: Clamped to 680px for optimal layout on tablets and web viewports.

---

## 4. Reusable UI Components (`src/components/`)

- **`Icon`**: Cross-platform SVG/vector icon renderer with support for civic symbols.
- **`Button`**: Multi-variant (`primary`, `secondary`, `outline`, `text`, `danger`) with loading and disabled states.
- **`Input`**: Accessible input with floating label, prefix (`+91`), error, and helper states.
- **`StatusBadge`**: Converts `IssueStatus` enum to human-friendly labels with accessible semantic colors.
- **`PriorityBadge`**: Human-readable priority indicators ("Critical Priority", "High Priority", etc.).
- **`CategorySelector`**: Interactive category picker for the 5 authoritative categories.
- **`IssueCard`**: Feed card displaying status, priority, title, ward/location, distance, and support metrics.
- **`NotificationItem`**: Alert cards featuring read/unread indicators and status icons.
- **`LoadingState`, `EmptyState`, `ErrorState`**: Standardized feedback containers with friendly citizen messaging.

---

## 5. Authentication State Architecture (`src/context/AuthContext.tsx`)

A robust, real authentication state machine integrated with backend JWT sessions:
- `UNKNOWN`: Initial state during app launch while reading secure storage.
- `UNAUTHENTICATED`: Citizen redirected to `/(auth)/welcome`.
- `AUTHENTICATED`: Citizen granted access to `/(tabs)` with verified profile.

Features:
- Backed by `expo-secure-store` (and web memory/session storage fallback).
- Auto-attaches `Authorization: Bearer <token>` to API calls.
- Centralized 401 interception, token refresh lock, and token rotation.
- Real phone OTP verification with 6-digit input and cooldown.
- See detailed documentation in [docs/mobile-authentication.md](file:///j:/Nagrivic/docs/mobile-authentication.md).

---

## 6. API Service Client Foundation (`src/services/api/`)

- **`config.ts`**: Handles environment-aware base URLs (`EXPO_PUBLIC_API_BASE_URL` or `EXPO_PUBLIC_API_URL`, Android `10.0.2.2`, Web/iOS `localhost:8080`).
- **`client.ts`**: Standardized HTTP client with bearer token injection, automated 401 refresh, concurrency queue, single retry, and safe citizen error parsing.
- **`authApi.ts`**: Dedicated typed authentication API service (`sendOtp`, `verifyOtp`, `refreshToken`, `logout`, `getCurrentUser`).

---

## 7. Authoritative Categories

Nagrivic defines five standard civic categories:
1. **Roads / Potholes** (`roads-potholes`)
2. **Garbage** (`garbage`)
3. **Streetlights** (`streetlights`)
4. **Water** (`water`)
5. **Drainage** (`drainage`)

---

## 8. Mobile Media Foundation (`src/services/media/`, `src/components/media/`)

Implemented in Task 27:
- **Expo Camera & ImagePicker**: Hardware camera capture and single-photo gallery selection.
- **Draft Media State**: In-memory `ReportDraftContext` holding temporary local URIs (`DraftMedia`) with zero base64 bloat.
- **Client Media Validation**: Max 10 MB file size enforcement, format validation (JPEG, PNG, WebP).
- **Reusable Components**: `ImagePreview` (aspect-ratio preservation, retake, replace, remove, continue) and `CameraModal` (live viewfinder with rear camera default).
- For complete details, see [docs/mobile-media.md](file:///j:/Nagrivic/docs/mobile-media.md).

---

## 9. Mobile GPS Location Foundation (`src/services/location/`, `src/components/location/`)

Implemented in Task 28:
- **Expo Location**: Foreground one-time device GPS location acquisition (`expo-location: ~57.0.17`).
- **Permission & Services Handling**: Explicit user-initiated permission check, safe recovery (Try Again, Open Settings), and location services state detection.
- **Coordinate & Accuracy Validation**: WGS84 range validation (-90..90 lat, -180..180 lon), finite number checks, and road-level accuracy threshold enforcement (`LOCATION_MAX_ACCEPTABLE_ACCURACY_METERS = 100`).
- **Draft Location State**: Integrated into in-memory `ReportDraftContext` as `ReportDraftLocation` with `DEVICE_GPS` tagging.
- **Reusable Components**: `LocationConfirmation` card with empty, detecting, low accuracy, and success confirmation states.
- **Privacy & Non-Goals**: No background location tracking, no watchers, no raw coordinates display, and no client-side reverse geocoding.
- For complete details, see [docs/mobile-location.md](file:///j:/Nagrivic/docs/mobile-location.md).

---

## 10. Intentionally Deferred Items

The following items remain intentionally deferred to subsequent tasks:
- Backend issue media multipart upload (`POST /api/issues/{issueId}/media`)
- Real issue submission API integration (`POST /api/issues`)
- Interactive geographic map rendering & map pin placement
- Server-side PostGIS point-in-polygon civic responsibility resolution
- Comments & Support live API calls
- Push notifications & FCM integration


