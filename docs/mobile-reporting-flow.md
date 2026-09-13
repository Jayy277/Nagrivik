# Mobile Citizen Issue Reporting Flow (Task 29)

This document specifies the end-to-end authenticated citizen issue reporting flow in the Nagrivic mobile application, integrating the React Native Expo client with the Spring Boot PostgreSQL/PostGIS backend.

---

## 1. Architectural Overview & User Flow

The citizen reporting flow is structured as a 6-step guided wizard with complete draft preservation, double-tap protection, advisory duplicate detection, and robust partial-failure handling:

```
[Report Tab]
   │
   ▼
1. Category Selection ──── (Mandatory; fetched from GET /api/categories)
   │
   ▼
2. Photo Capture ───────── (Reuses Task 27 camera/gallery; 1 photo max, preview, replace/remove)
   │
   ▼
3. Issue Details ───────── (Title 5–255 chars, Description 10–1000 chars with live counter)
   │
   ▼
4. GPS Location ────────── (Reuses Task 28 foreground GPS; accuracy check, retry, override)
   │
   ▼
5. Duplicate Check ─────── (POST /api/issues/check-duplicates within 100m)
   │
   ├── [Candidates Found] ──► Advisory Warning
   │                            ├── Action A: "Support Existing Issue" (POST /api/issues/{id}/support) -> Navigate to detail & clear draft
   │                            └── Action B: "Report Anyway" -> Advance to Review
   └── [No Duplicates] ─────► Advance to Review
   │
   ▼
6. Review & Submit ─────── (Summary cards with individual Edit links)
   │
   ▼
[Submission Pipeline]
   │
   ├── Step A: POST /api/issues (Creates issue with coordinates + categoryId; reporter derived from JWT)
   │     └── On Success: Persist createdIssueId in draft
   │     └── On Failure: Retain draft, show citizen-friendly retryable error
   │
   ├── Step B: POST /api/issues/{issueId}/media (Multipart form-data)
   │     └── On Success: Transition to COMPLETED, clear draft, navigate to /issue/{issueId}
   │     └── On Failure (Partial Failure): Retain createdIssueId in draft, allow retrying ONLY media upload
   │
   ▼
7. Success Confirmation ── (Status REPORTED; link to /issue/{issueId})
```

---

## 2. Centralized Draft State (`ReportDraftContext`)

The report draft state is centralized in `src/context/ReportDraftContext.tsx` and wraps the application root.

### State Model
```typescript
export interface ReportDraft {
  categoryId: string | null;
  categoryName: string | null;
  title: string;
  description: string;
  media: DraftMedia | null;
  location: ReportDraftLocation | null;
  duplicateCandidates: DuplicateCandidate[];
  duplicateCheckStatus: 'IDLE' | 'CHECKING' | 'DONE' | 'ERROR';
  submissionState: ReportSubmissionState;
  createdIssueId: string | null;
  submissionError: string | null;
}

export type ReportSubmissionState =
  | 'IDLE'
  | 'CREATING_ISSUE'
  | 'ISSUE_CREATED'
  | 'UPLOADING_MEDIA'
  | 'COMPLETED'
  | 'FAILED_RETRYABLE';
```

### Action Methods
- `setDraftCategory(categoryId, categoryName)`
- `setDraftMedia(media)` / `removeDraftMedia()`
- `setDraftLocation(location)` / `removeDraftLocation()`
- `setDraftDetails(title, description)`
- `setDuplicateCandidates(candidates)` / `clearDuplicateCandidates()`
- `setSubmissionState(state)`
- `setCreatedIssueId(issueId)`
- `setSubmissionError(error)`
- `resetDraft()`

### Discard Protection
Normal back navigation (`chevron-left` in header or hardware back) moves back one step without discarding entered data. Attempting to leave the reporting flow with unsaved content triggers a confirmation dialog (`"Discard Report?"` with `"Continue Editing"` and destructive `"Discard"`).

---

## 3. Backend API Contracts

### A. Category Discovery
- **Endpoint**: `GET /api/categories`
- **Auth**: Public or Authenticated
- **Response**:
```json
[
  {
    "id": "c0000000-0000-0000-0000-000000000001",
    "name": "Roads / Potholes",
    "slug": "roads-potholes",
    "description": "Craters, road cave-ins, damaged asphalt, and resurfacing hazards.",
    "displayOrder": 1
  }
]
```

### B. Duplicate Check
- **Endpoint**: `POST /api/issues/check-duplicates`
- **Auth**: Authenticated (Bearer JWT)
- **Request Body**:
```json
{
  "title": "Deep crater on crossroads",
  "description": "Damaged asphalt causing accidents",
  "categoryId": "c0000000-0000-0000-0000-000000000001",
  "latitude": 23.0225,
  "longitude": 72.5714,
  "radiusMeters": 100.0
}
```
- **Response**:
```json
{
  "hasPotentialDuplicates": true,
  "candidates": [
    {
      "issueId": "e1111111-1111-1111-1111-111111111111",
      "title": "Large pothole near crossroad signal",
      "category": {
        "id": "c0000000-0000-0000-0000-000000000001",
        "name": "Roads / Potholes",
        "slug": "roads-potholes"
      },
      "distanceMeters": 34.2,
      "status": "REPORTED",
      "supportCount": 4
    }
  ]
}
```

### C. Issue Creation with Location Coordinates
- **Endpoint**: `POST /api/issues`
- **Auth**: Authenticated (Bearer JWT)
- **Request Body**:
```json
{
  "title": "Large pothole on highway",
  "description": "Deep crater near the intersection causing difficulty for two-wheelers.",
  "categoryId": "c0000000-0000-0000-0000-000000000001",
  "location": {
    "latitude": 23.0225,
    "longitude": 72.5714,
    "accuracyMeters": 14.5
  }
}
```
- **Server Responsibilities**:
  - `reportedBy` is strictly extracted from the authenticated JWT user principal. Client-supplied user IDs are rejected or ignored.
  - Validates latitude in `[-90, 90]` and longitude in `[-180, 180]`.
  - Transactionally creates a `LocationEntity` with PostGIS point geometry.
  - Executes server-side civic responsibility resolution (municipality, ward, department) based on geometry and category. Clients cannot override jurisdiction.
  - Assigns initial status `REPORTED`.
- **Response**:
```json
{
  "id": "a0000000-0000-0000-0000-000000000099",
  "title": "Large pothole on highway",
  "description": "Deep crater near the intersection causing difficulty for two-wheelers.",
  "category": {
    "id": "c0000000-0000-0000-0000-000000000001",
    "name": "Roads / Potholes",
    "slug": "roads-potholes"
  },
  "status": "REPORTED",
  "priority": { "score": 25, "level": "MEDIUM" },
  "location": {
    "latitude": 23.0225,
    "longitude": 72.5714
  },
  "supportCount": 0
}
```

### D. Media Upload
- **Endpoint**: `POST /api/issues/{issueId}/media`
- **Auth**: Authenticated (must be the issue reporter)
- **Content-Type**: `multipart/form-data`
- **Form Field**: `file` (binary payload with MIME type `image/jpeg`, `image/png`, or `image/webp`)
- **Response**:
```json
{
  "id": "m0000000-0000-0000-0000-000000000001",
  "issueId": "a0000000-0000-0000-0000-000000000099",
  "storageKey": "uploads/a0000000/media-uuid.jpg",
  "mimeType": "image/jpeg",
  "displayOrder": 1
}
```

### E. Support Existing Issue
- **Endpoint**: `POST /api/issues/{issueId}/support`
- **Auth**: Authenticated (Bearer JWT)
- **Response**: `201 Created`

---

## 4. Partial Failure Handling & Idempotency Safeguards

The report submission process is split into two asynchronous network operations:
1. Issue creation (`POST /api/issues`)
2. Media attachment (`POST /api/issues/{issueId}/media`)

### Failure Matrix
| Failure Scenario | State | App Behavior |
|---|---|---|
| Issue creation fails | `FAILED_RETRYABLE` | No issue created on backend. Entire draft is preserved. User is shown a retryable error ("Could not connect to server..."). Retry invokes `POST /api/issues`. |
| Issue creation succeeds, Media upload fails | `FAILED_RETRYABLE` | Issue exists on backend. `createdIssueId` is saved into `ReportDraftContext`. Button transforms to **"Retry Photo Upload"**. Retrying calls `POST /api/issues/{createdIssueId}/media` directly and **NEVER** calls `POST /api/issues` again, preventing duplicate issues. |
| Both succeed | `COMPLETED` | Success confirmation is rendered. Draft is cleared (`resetDraft()`). Navigation advances to `/issue/{issueId}`. |

---

## 5. Security & Privacy Protections

1. **Zero Client-Side Trust for Jurisdiction**: Civic body, ward, city, and department are strictly resolved by backend PostGIS boundaries.
2. **Reporter Identity Derived from JWT**: `reporter` is strictly set from the verified `UserEntity` in the Spring Security context.
3. **No Sensitive PII Exposure**: Duplicate candidate DTOs exclude phone numbers, internal storage keys, or moderator notes.
4. **Zero Base64 in Draft State**: Photos are stored locally as file URIs until multipart streaming.
