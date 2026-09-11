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

### Core Endpoint Conventions (Future Specifications)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/issues` | List issues with filtering, pagination, and sorting |
| `GET` | `/api/issues/{id}` | Get detailed information for a single issue |
| `POST` | `/api/issues` | Create/report a new civic issue |
| `PATCH` | `/api/issues/{id}` | Partially update issue status, category, or metadata |
| `GET` | `/api/issues/nearby` | Spatial query for issues within a radius (PostGIS `ST_DWithin`) |
| `POST` | `/api/issues/{id}/support` | Add community upvote ("Support") to an issue |
| `DELETE` | `/api/issues/{id}/support` | Remove community upvote from an issue |
| `GET` | `/api/issues/{id}/comments` | List comments and updates on an issue |
| `POST` | `/api/issues/{id}/comments` | Add a comment or resolution evidence to an issue |
| `GET` | `/api/categories` | List active civic categories (Roads, Garbage, etc.) |
| `GET` | `/api/health` | Service health status (`UP`, `nagrivic-backend`) |

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

### Spatial Query Parameters (`/api/issues/nearby`)

| Parameter | Type | Required | Description |
|---|---|---|---|
| `lat` | float (WGS84) | Yes | Latitude (-90 to +90). |
| `lng` | float (WGS84) | Yes | Longitude (-180 to +180). |
| `radius` | integer (meters) | No (default: `500`) | Proximity radius in meters (max `5000`). |

---

## 6. Identifier & Timestamp Standards

1. **Identifiers (IDs)**:
   - Primary resources (Issues, Users, Reports, Comments) use **UUIDv4** strings (e.g., `e3b0c442-98fc-1c14-9afb-4c700203f567`) to prevent sequential enumeration attacks and facilitate distributed key generation.
   - Fixed taxonomies (Categories, Administrative Wards) may use human-readable slug identifiers (e.g., `ROADS_POTHOLES`, `amc-navrangpura`).

2. **Timestamps**:
   - All timestamps transmitted across API boundaries must be in **ISO-8601 UTC format**: `YYYY-MM-DDTHH:mm:ssZ` (e.g., `2026-09-11T11:06:59Z`).
   - Timezones are converted to the citizen's local time (IST - Indian Standard Time) on the client side.
