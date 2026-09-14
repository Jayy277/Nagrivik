# Nagrivic REST API Conventions

> **Notice**: This document defines the API conventions and design standards for Nagrivic. The endpoints described here are design specifications and must not be implemented until their respective implementation phases.

---

## 1. Base Path & Versioning Strategy

- **Base Path**: `/api`
- **Versioning Strategy**:
  - For MVP and initial launch, all public endpoints are served under `/api/...`.
  - Breaking changes across major releases will adopt URL path versioning: `/api/v1/...`, `/api/v2/...`.
  - Non-breaking changes (such as adding optional fields to request/response DTOs) are released additively without path changes.

---

## 2. Resource URI Design Principles

- URIs represent **resources (nouns)**, not actions (verbs).
- Use lowercase alphanumeric characters with hyphens for multi-word segments (kebab-case): e.g., `/api/ward-boundaries`.
- Use plural nouns for collections: `/api/issues`, `/api/categories`.
- Nested resources represent hierarchy: `/api/issues/{id}/comments`, `/api/issues/{id}/support`.

### Core Endpoint Conventions

| Method | Endpoint | Status | Description |
|---|---|---|---|
| `POST` | `/api/issues` | **Implemented** | Report a new civic issue (`title`, `description`, `categoryId`, `locationId`; reporter server-derived from authenticated JWT) |
| `GET` | `/api/issues/{id}` | **Implemented** | Get detailed information for a single issue |
| `GET` | `/api/issues` | **Implemented** | List issues with pagination (`page`, `size`), sorting (`createdAt DESC`), and filters (`status`, `categoryId`, `reportedBy`) |
| `GET` | `/api/health` | **Implemented** | Service health status (`UP`, `nagrivic-backend`) |
| `PATCH` | `/api/issues/{id}` | *Deferred* | Partially update issue status, category, or metadata |
| `GET` | `/api/issues/nearby` | *Deferred* | Spatial query for issues within a radius (PostGIS `ST_DWithin`) |
| `POST` | `/api/issues/{id}/support` | *Deferred* | Add community upvote ("Support") to an issue |
| `DELETE` | `/api/issues/{id}/support` | *Deferred* | Remove community upvote from an issue |
| `GET` | `/api/issues/{id}/comments` | *Deferred* | List comments and updates on an issue |
| `POST` | `/api/issues/{id}/comments` | *Deferred* | Add a comment or resolution evidence to an issue |
| `GET` | `/api/categories` | *Deferred* | List active civic categories (Roads, Garbage, etc.) |
| `GET` | `/api/admin/geography/overview` | **Implemented** | Get high-level summary counters and metrics for civic geography (ADMIN only) |
| `GET` | `/api/admin/geography/civic-bodies` | **Implemented** | List civic bodies with active filter (ADMIN only) |
| `POST` | `/api/admin/geography/civic-bodies` | **Implemented** | Create civic body with provenance (ADMIN only) |
| `PUT` | `/api/admin/geography/civic-bodies/{id}` | **Implemented** | Update civic body with optimistic lock (ADMIN only) |
| `PATCH` | `/api/admin/geography/civic-bodies/{id}/active` | **Implemented** | Safe active/inactive toggle (ADMIN only) |
| `GET` | `/api/admin/geography/cities` | **Implemented** | List cities with active filter (ADMIN only) |
| `POST` | `/api/admin/geography/cities` | **Implemented** | Create city under civic body (ADMIN only) |
| `PUT` | `/api/admin/geography/cities/{id}` | **Implemented** | Update city with optimistic lock (ADMIN only) |
| `PATCH` | `/api/admin/geography/cities/{id}/active` | **Implemented** | Safe city active toggle (ADMIN only) |
| `GET` | `/api/admin/geography/wards` | **Implemented** | Paginated search of wards by city, name, code, active status (ADMIN only) |
| `POST` | `/api/admin/geography/wards` | **Implemented** | Create authoritative ward with provenance (ADMIN only) |
| `PUT` | `/api/admin/geography/wards/{id}` | **Implemented** | Update ward metadata and boundary geometry (ADMIN only) |
| `PATCH` | `/api/admin/geography/wards/{id}/active` | **Implemented** | Safe ward active toggle (ADMIN only) |
| `POST` | `/api/admin/geography/wards/validate` | **Implemented** | Validate all ward boundary polygons for SRID 4326, valid MultiPolygon topology (ADMIN only) |
| `GET` | `/api/admin/geography/departments` | **Implemented** | Paginated search of departments with active filter (ADMIN only) |
| `POST` | `/api/admin/geography/departments` | **Implemented** | Create department under civic body (ADMIN only) |
| `PUT` | `/api/admin/geography/departments/{id}` | **Implemented** | Update department with optimistic lock (ADMIN only) |
| `PATCH` | `/api/admin/geography/departments/{id}/active` | **Implemented** | Safe department active toggle (ADMIN only) |
| `GET` | `/api/admin/geography/category-department-mappings` | **Implemented** | Paginated list of category routing mappings (ADMIN only) |
| `POST` | `/api/admin/geography/category-department-mappings` | **Implemented** | Create category -> department mapping rule (ADMIN only) |
| `PATCH` | `/api/admin/geography/category-department-mappings/{id}/active` | **Implemented** | Safe category mapping active toggle (ADMIN only) |
| `GET` | `/api/admin/geography/ward-department-mappings` | **Implemented** | Paginated list of ward-specific routing mappings (ADMIN only) |
| `POST` | `/api/admin/geography/ward-department-mappings` | **Implemented** | Create ward -> department mapping rule (ADMIN only) |
| `PATCH` | `/api/admin/geography/ward-department-mappings/{id}/active` | **Implemented** | Safe ward mapping active toggle (ADMIN only) |
| `POST` | `/api/admin/geography/re-resolve` | **Implemented** | Safely re-resolve responsibility for existing issues in bounded batches (ADMIN only) |
| `GET` | `/api/admin/geography/audits` | **Implemented** | Append-only audit history of administrative geography changes (ADMIN only) |
| `GET` | `/api/authority/dashboard` | **Implemented** | High-level metrics & active jurisdiction scopes for authority officers (OFFICER, ADMIN) |
| `GET` | `/api/authority/issues` | **Implemented** | Paginated search of scoped civic issues within officer's jurisdiction (OFFICER, ADMIN) |
| `GET` | `/api/authority/issues/{id}` | **Implemented** | Detailed issue inspection with allowed transitions & sanitized comments (OFFICER, ADMIN) |
| `POST` | `/api/authority/issues/{id}/status` | **Implemented** | Execute operational state transition with optimistic locking & mandatory reason (OFFICER, ADMIN) |
| `POST` | `/api/authority/issues/{id}/resolution-evidence` | **Implemented** | Upload resolution evidence (completion photo, note, before/after) with authority scope enforcement (OFFICER, ADMIN) |
| `GET` | `/api/issues/{id}/resolution-evidence` | **Implemented** | Public inspection of resolution evidence attached to an issue |
| `GET` | `/api/public/accountability` | **Implemented** | Public Civic Accountability Dashboard aggregate metrics and breakdowns across Ahmedabad (no auth required) |
| `POST` | `/api/issues/check-duplicates` | **Implemented** | Pre-creation duplicate check with deterministic spatial and AI similarity signals |
| `GET` | `/api/admin/duplicates/suggestions` | **Implemented** | List pending AI duplicate suggestions for review (MODERATOR, OFFICER, ADMIN) |
| `POST` | `/api/admin/duplicates/suggestions/{id}/link` | **Implemented** | Link suggestion as duplicate with canonical normalization (MODERATOR, OFFICER, ADMIN) |
| `POST` | `/api/admin/duplicates/suggestions/{id}/dismiss` | **Implemented** | Dismiss duplicate suggestion with optional reason (MODERATOR, OFFICER, ADMIN) |
| `POST` | `/api/admin/duplicates/scan/{issueId}` | **Implemented** | Trigger duplicate candidate scan for existing issue (MODERATOR, OFFICER, ADMIN) |
| `POST` | `/api/issues/{id}/media/{mediaId}/analyze` | **Implemented** | Trigger AI image understanding analysis on an issue photo (Reporter, MODERATOR, OFFICER, ADMIN) |
| `GET` | `/api/issues/{id}/media/{mediaId}/analysis` | **Implemented** | Fetch AI image understanding analysis results for an issue photo (Reporter, MODERATOR, OFFICER, ADMIN) |

---

## 3. HTTP Methods & Status Codes

| HTTP Method | Usage | Expected Success Code |
|---|---|---|
| `GET` | Retrieve resource(s) (idempotent, safe) | `200 OK` |
| `POST` | Create a new resource or trigger an action | `201 Created` or `200 OK` |
| `PUT` | Full replacement of a resource | `200 OK` |
| `PATCH` | Partial update of a resource | `200 OK` |
| `DELETE` | Remove a resource | `204 No Content` or `200 OK` |

### Standard HTTP Status Codes

| Status Code | Reason | Usage in Nagrivic |
|---|---|---|
| `200 OK` | Success | Standard response for successful `GET`, `PUT`, or `PATCH`. |
| `201 Created` | Resource Created | Successful `POST` creating an issue, comment, or support vote. |
| `204 No Content` | No Content | Successful `DELETE` or action returning no body. |
| `400 Bad Request` | Malformed Request | Syntax errors, invalid JSON body, missing required headers. |
| `401 Unauthorized` | Unauthenticated | Missing, expired, or invalid JWT token or OTP. |
| `403 Forbidden` | Access Denied | Authenticated user lacks permission (e.g., citizen calling admin API). |
| `404 Not Found` | Resource Not Found | Requested issue, user, or ward ID does not exist. |
| `409 Conflict` | Conflict | Duplicate vote, duplicate submission, or concurrent modification conflict. |
| `422 Unprocessable Entity` | Validation Failure | Request body well-formed JSON, but business/field constraints failed. |
| `429 Too Many Requests` | Rate Limited | OTP request limit exceeded, or anti-spam report throttling triggered. |
| `500 Internal Server Error` | Server Failure | Unhandled backend exception (never leak stack trace to client). |

---

## 4. Request & Response Envelopes

### Standard Error Response Format
All errors return a consistent, structured payload adhering to RFC 7807 principles:

```json
{
  "timestamp": "2026-09-11T16:00:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Validation failed for one or more fields",
  "path": "/api/issues",
  "validationErrors": [
    {
      "field": "description",
      "rejectedValue": "",
      "message": "Description must not be blank"
    },
    {
      "field": "location.latitude",
      "rejectedValue": 95.2,
      "message": "Latitude must be between -90 and 90 degrees"
    }
  ]
}
```

### Standard Paginated Collection Response Format
All paginated list queries (`GET /api/issues`, `GET /api/issues/{id}/comments`) return a standard pagination metadata envelope:

```json
{
  "content": [
    {
      "id": "e3b0c442-98fc-1c14-9afb-4c700203f567",
      "title": "Severe pothole near Ring Road junction",
      "category": "ROADS_POTHOLES",
      "status": "SUBMITTED",
      "supportCount": 14,
      "createdAt": "2026-09-11T14:20:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 142,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

---

## 5. Filtering, Sorting & Pagination Parameters

Collections accept standard URL query parameters:

| Parameter | Type | Default | Example | Description |
|---|---|---|---|---|
| `page` | integer | `0` | `page=0` | Zero-based page index. |
| `size` | integer | `20` | `size=20` | Items per page (max `100`). |
| `sort` | string | `createdAt,desc` | `sort=supportCount,desc` | Sort field and direction (`asc` or `desc`). |
| `status` | string | - | `status=IN_PROGRESS` | Filter by issue status. |
| `category` | string | - | `category=ROADS_POTHOLES` | Filter by category key. |
| `wardId` | string | - | `wardId=amc-ward-12` | Filter by municipal ward identifier. |

### Geographic & Map Query Parameters (`GET /api/issues`)

For interactive civic map exploration and nearby discovery:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `latitude` | double (WGS84) | Conditional | Target center latitude (-90.0 to +90.0). Required if `longitude` is supplied. |
| `longitude` | double (WGS84) | Conditional | Target center longitude (-180.0 to +180.0). Required if `latitude` is supplied. |
| `radiusMeters` | double (meters) | No (default: `50000`) | Proximity radius in meters (1 to `50000` / 50km max). |
| `sort` | enum | No | Supports `NEAREST` (orders ascending by physical distance), `NEWEST`, `PRIORITY`, `MOST_SUPPORTED`. |

*Note: For complete map architecture, tile configurations, and clustering, see [docs/map.md](file:///j:/Nagrivic/docs/map.md).*

---

## 6. Identifier & Timestamp Standards

1. **Identifiers (IDs)**:
   - Primary resources (Issues, Users, Reports, Comments) use **UUIDv4** strings (e.g., `e3b0c442-98fc-1c14-9afb-4c700203f567`) to prevent sequential enumeration attacks and facilitate distributed key generation.
   - Fixed taxonomies (Categories, Administrative Wards) may use human-readable slug identifiers (e.g., `ROADS_POTHOLES`, `amc-navrangpura`).

2. **Timestamps**:
   - All timestamps transmitted across API boundaries must be in **ISO-8601 UTC format**: `YYYY-MM-DDTHH:mm:ssZ` (e.g., `2026-09-11T11:06:59Z`).
   - Timezones are converted to the citizen's local time (IST - Indian Standard Time) on the client side.

---

## 7. Implemented Issue Endpoints Specification (Task 9)

### 1. Report Civic Issue (`POST /api/issues`)
- **Status**: `201 Created` with `Location: /api/issues/{id}` header.
- **Request Body**:
  ```json
  {
    "title": "Severe pothole near Ring Road junction",
    "description": "Deep crater on outer lane causing dangerous vehicle swerves.",
    "categoryId": "c0000000-0000-0000-0000-000000000001",
    "locationId": "11111111-1111-1111-1111-111111111111",
    "reportedBy": "22222222-2222-2222-2222-222222222222"
  }
  ```
- **Validation Rules**:
  - `title`: Required, non-blank, maximum 255 characters.
  - `description`: Optional/sensible, maximum 5000 characters.
  - `categoryId`: Required UUID, category must exist and be `is_active = true`.
  - `locationId`: Required UUID, location must exist.
  - `reportedBy`: Required UUID, user must exist. *(Temporary development convenience: will be replaced by authenticated JWT identity in the authentication task).*
  - `status`: Automatically initialized to `REPORTED`. Clients cannot choose or override status.

### 2. Get Issue by ID (`GET /api/issues/{id}`)
- **Status**: `200 OK` on success, `404 Not Found` if nonexistent, `400 Bad Request` if malformed UUID.
- **Response Body**:
  ```json
  {
    "id": "e3b0c442-98fc-1c14-9afb-4c700203f567",
    "reportedBy": "22222222-2222-2222-2222-222222222222",
    "category": {
      "id": "c0000000-0000-0000-0000-000000000001",
      "name": "Roads / Potholes",
      "slug": "roads-potholes"
    },
    "location": {
      "id": "11111111-1111-1111-1111-111111111111",
      "latitude": 23.0225,
      "longitude": 72.5714,
      "accuracyMeters": 5.0
    },
    "title": "Severe pothole near Ring Road junction",
    "description": "Deep crater on outer lane causing dangerous vehicle swerves.",
    "status": "REPORTED",
    "media": [],
    "createdAt": "2026-09-11T14:20:00Z",
    "updatedAt": "2026-09-11T14:20:00Z"
  }
  ```

### 3. List Issues (`GET /api/issues`)
- **Status**: `200 OK`.
- **Query Parameters**:
  - `status`: Optional `IssueStatus` enum (`REPORTED`, `VERIFIED`, `IN_PROGRESS`, etc.).
  - `categoryId`: Optional UUID filtering by civic category.
  - `reportedBy`: Optional UUID filtering by reporter.
  - `page`: Zero-based page index (default: `0`, min: `0`).
  - `size`: Page size (default: `20`, clamped to max: `100`).
- **Sorting**: Default `createdAt DESC` (newest issues first).
- **Paginated Response Envelope**:
  ```json
  {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
  ```

---

## AI Priority Assistance Endpoints (Task 48)

### 1. Inspect AI Priority Recommendation (`GET /api/issues/{issueId}/priority/ai-recommendation`)
- **Authentication**: Required (`Bearer JWT`).
- **Authorization**: Accessible by the issue reporter, or authorized staff (`ROLE_OFFICER`, `ROLE_AUTHORITY`, `ROLE_MODERATOR`, `ROLE_ADMIN`). Unauthorized citizens receive `403 Forbidden`.
- **Response**: `200 OK`
  ```json
  {
    "issueId": "91a8e234-927b-4028-98e9-d7575306ea64",
    "status": "COMPLETED",
    "provider": "LOCAL_HEURISTIC",
    "model": "heuristic-priority-v1",
    "modelVersion": "1.0.0",
    "calculationVersion": "v1",
    "suggestedSeverity": 25,
    "suggestedImpact": 15,
    "suggestedSafety": 20,
    "severityConfidence": 85,
    "impactConfidence": 80,
    "safetyConfidence": 90,
    "confidence": 85,
    "signals": [
      "High severity keywords detected in issue text",
      "Task 47 visual signals: [DEEP_POTHOLE]",
      "Task 47 visual safety concern: HIGH"
    ],
    "appliedToCalculation": false,
    "createdAt": "2026-09-14T14:10:00Z"
  }
  ```

### 2. Trigger AI Priority Assessment (`POST /api/issues/{issueId}/priority/ai-assess`)
- **Authentication**: Required (`Bearer JWT`).
- **Authorization**: Privileged operation. Restricted to `ROLE_OFFICER`, `ROLE_AUTHORITY`, `ROLE_MODERATOR`, `ROLE_ADMIN`. Ordinary citizens receive `403 Forbidden`.
- **Response**: `200 OK` (returns updated `AiPriorityRecommendationResponse`).
- **Audit**: Automatically records a `PRIORITY_AI_ASSESSED` entry in `issue_activity`.

