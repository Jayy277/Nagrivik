# Nagrivic Mobile Citizen Profile & Account Management (Task 35)

## 1. Overview & Architectural Philosophy

The Citizen Profile & Account Management experience connects authenticated citizens to their verified account details, personal settings, and civic activity records. In alignment with Task 33 (Google Authentication) and Task 34 (Notification Center), the profile architecture enforces:

1. **Server-Derived Identity**: The citizen's identity is strictly resolved from the authenticated JWT Bearer token via `GET /api/auth/me` and `GET /api/users/me`. Client-supplied user IDs are never trusted for profile ownership.
2. **Strict Identity Control**: Identity-level attributes—including authenticated Google email, verified phone numbers, citizen roles (`CITIZEN`), and account activation status—are immutable via client self-service.
3. **Editable Public Presentation**: Citizens can customize their public `fullName` via `PATCH /api/users/me` with server-side validation supporting all Indian Unicode scripts and languages while rejecting non-printable control characters.
4. **Immediate State Reactivity**: Profile updates reflect immediately across the mobile app via `AuthContext.updateUser` without requiring an application restart.
5. **Secure Session Lifecycle**: Sign-out performs complete session revocation (`POST /api/auth/logout`), purges device `tokenStorage`, and redirects to `/(auth)/welcome`.

---

## 2. API Contract & Endpoints

| Endpoint | Method | Security | Purpose | Response Format |
|---|---|---|---|---|
| `/api/auth/me` | `GET` | Authenticated | Fetches current user session summary | `UserAuthSummary` |
| `/api/users/me` | `GET` | Authenticated | Fetches user profile for authenticated principal | `UserAuthSummary` |
| `/api/users/me` | `PATCH` | Authenticated | Updates allowed profile fields (`fullName`) | `UserAuthSummary` |
| `/api/auth/logout` | `POST` | Public / Token | Revokes server refresh token session | `{ message: string }` |

### Profile Update Contract

#### Request Payload (`UpdateProfileRequest`)
```json
{
  "fullName": "Aarav Patel"
}
```

#### Validation Rules
- `fullName`: Required, trimmed length between 2 and 100 characters.
- Non-printable control characters (ASCII < 32 or ASCII 127) are strictly rejected.
- Full Unicode support for Indian names (Devanagari, Gujarati, Tamil, Telugu, Gurmukhi, etc.).

#### Safe Response (`UserAuthSummary`)
```json
{
  "id": "c1f7a29e-5b12-4c6e-9821-4f9104fa281b",
  "phoneNumber": "+919876543210",
  "email": "citizen@gmail.com",
  "role": "CITIZEN",
  "fullName": "Aarav Patel",
  "profilePictureUrl": "https://lh3.googleusercontent.com/..."
}
```

> [!IMPORTANT]
> Password hashes, refresh token records, OAuth secrets, moderation internal flags, and database metadata are never exposed in user responses.

---

## 3. Mobile Navigation & Routes

```text
src/app/
├── (tabs)/
│   └── profile.tsx                 # Primary Citizen Profile Screen
├── profile/
│   ├── edit.tsx                    # Dedicated Edit Profile Screen
│   └── reports.tsx                 # Dedicated My Reports Screen (Task 36)
└── notifications/
    └── preferences.tsx             # Dedicated Notification Preferences Screen
```

### 3.1 Primary Profile Screen (`src/app/(tabs)/profile.tsx`)
- **Profile Header & Card**: Displays citizen's name, masked contact info, verified role badge, and identity indicators.
- **Initials Avatar Fallback**: Renders initials (e.g. "AP" for "Aarav Patel") when no profile image exists.
- **Incomplete Profile Banner**: When `fullName` is missing, renders an encouraging "Complete your profile" prompt card with direct navigation to `/profile/edit`.
- **Navigation Entries**:
  - **Edit Profile**: Navigates to `/profile/edit`.
  - **My Reports**: Navigates to `/profile/reports` (Task 36).
  - **Supported Issues**: Placeholder entry prepared for subsequent roadmaps.
  - **Notification Preferences**: Navigates to `/notifications/preferences`.
  - **Privacy & Data Protection**: Explains that phone numbers and emails are never displayed publicly.
  - **Sign Out**: Confirmation alert triggers `authApi.logout`, wipes `tokenStorage`, and navigates to `/(auth)/welcome`.

### 3.2 My Reports Screen (`src/app/profile/reports.tsx`) (Task 36)
- **Dedicated Route**: `/profile/reports` with screen title `"My Reports"`.
- **Real Backend Integration**: Queries `GET /api/issues/my` via `issueApi.getMyReports` with 20 items/page.
- **Server Ownership**: Identity derived purely from JWT; no client user ID parameter is ever transmitted.
- **Full Civic Lifecycle**: Renders `StatusBadge` for `REPORTED` through `NOT_FIXED`.
- **Priority & Department**: Displays authoritative `PriorityBadge` and resolved department name.
- **Duplicate Preservation**: Duplicate-linked issues are preserved with a `[Linked]` badge.
- **Interactive Capabilities**:
  - Infinite scroll pagination (`onEndReached`) with duplicate suppression.
  - Native pull-to-refresh (`RefreshControl`).
  - Filtering and sorting via `FilterModal` (Status, Priority, Sort).
  - Initial loading skeleton via `LoadingState`.
  - Empty state with "Report an Issue" CTA navigating to `/(tabs)/report`.
  - Recoverable error state with "Retry".
  - Card press navigates to `/issue/[id]`.

### 3.3 Edit Profile Screen (`src/app/profile/edit.tsx`)
- Pre-fills current full name.
- Real-time client validation enforcing [2, 100] character length and control character rejection.
- Read-only Google Email card with "Verified Civic Identity" explanation.
- Read-only Phone Number card (when present).
- Save button: disabled when unchanged, loading during flight, and updates `AuthContext` on success.
- Preserves entered form state on network error.

---

## 4. Privacy & Account Deletion Policy

### Account Deletion Status
In strict accordance with project safety guidelines:
- **No unsafe destructive client deletion button is present.**
- Public civic reports, inspection records, and community corroboration must adhere to municipal audit and public record retention laws.
- Account deletion will be implemented in a dedicated privacy phase incorporating content anonymization, disassociating personal contact data while preserving civic report history.

---

## 5. Automated Verification

Automated test suite is located in `scripts/verify-profile-flow.js` and executed via `npm run test:profile` or `npm test`:

- `[Test 1]` Real Authenticated Profile data source & initials avatar fallback.
- `[Test 2]` Incomplete Profile detection and "Complete Profile" onboarding card.
- `[Test 3]` Edit Profile form prefilling and live validation (min 2, max 100, control chars, Unicode).
- `[Test 4]` Save Profile calling `PATCH /api/users/me` and updating `AuthContext`.
- `[Test 5]` Error handling and form value preservation on network failure.
- `[Test 6]` Read-only identity fields for Google email and phone number.
- `[Test 7 & 8]` Secure logout with session revocation, token clearance, and failure resiliency.
- `[Test 9]` Session expiration handling via centralized `ApiClient` token rotation.
- `[Test 10]` Notification Preferences navigation connecting to Task 34 backend endpoints.
- `[Test 11]` My Reports entry preparation without fake data.
- `[Test 12 & 13]` Privacy information declaration and omission of unsafe deletion button.
- `[Test 14]` Route registration in `RootLayout` and accessibility attributes.
