# Issue Domain Specification

## 1. Domain Purpose

An **Issue** represents a single, citizen-reported civic problem within the Nagrivic platform. Examples include potholes, overflowing garbage dumps, broken streetlights, water pipeline bursts, or clogged storm drainage systems.

The Issue domain acts as the core transactional anchor around which civic accountability, community endorsement, departmental routing, and resolution verification are coordinated.

---

## 2. Core Fields & Data Model (Current Scope)

| Field | Database Column | Type | Nullable | Description |
|---|---|---|---|---|
| `id` | `id` | `UUID` | No | Primary key identifier (UUIDv4). |
| `reportedBy` | `reported_by` | `UUID` | No | Foreign key referencing `users(id)` with `ON DELETE RESTRICT`. |
| `category` | `category_id` | `UUID` | No | Foreign key referencing `categories(id)` with `ON DELETE RESTRICT`. |
| `location` | `location_id` | `UUID` | No | Foreign key referencing `locations(id)` with `ON DELETE RESTRICT`. |
| `title` | `title` | `VARCHAR(255)` | No | Brief human-readable summary of the civic problem (non-empty). |
| `description` | `description` | `TEXT` | Yes | Extended contextual description provided by the citizen. |
| `status` | `status` | `VARCHAR(32)` | No | Machine-friendly status enum value (default: `'REPORTED'`). |
| `duplicateOf` | `duplicate_of_issue_id` | `UUID` | Yes | Self-referencing FK to `issues(id)` with `ON DELETE SET NULL`. NULL indicates a primary canonical issue. |
| `civicBody` | `civic_body_id` | `UUID` | Yes | Foreign key to `civic_bodies(id)` with `ON DELETE SET NULL`. Server-resolved administrative entity. |
| `city` | `city_id` | `UUID` | Yes | Foreign key to `cities(id)` with `ON DELETE SET NULL`. Server-resolved containing city. |
| `ward` | `ward_id` | `UUID` | Yes | Foreign key to `wards(id)` with `ON DELETE SET NULL`. Server-resolved containing ward. |
| `department` | `department_id` | `UUID` | Yes | Foreign key to `departments(id)` with `ON DELETE SET NULL`. Server-resolved responsible department. |
| `responsibilityStatus` | `responsibility_status` | `VARCHAR(30)` | No | Server-controlled resolution status: `UNRESOLVED` or `RESOLVED`. |
| `responsibilityResolvedAt` | `responsibility_resolved_at` | `TIMESTAMPTZ` | Yes | Timestamp when civic responsibility was authoritatively resolved. |
| `responsibilitySource` | `responsibility_source` | `VARCHAR(255)` | Yes | Resolution engine provenance (e.g., `GEOGRAPHIC_POINT_IN_POLYGON`). |
| `createdAt` | `created_at` | `TIMESTAMPTZ` | No | Point-in-time timestamp when the issue was reported (UTC). |
| `updatedAt` | `updated_at` | `TIMESTAMPTZ` | No | Point-in-time timestamp of the last modification (UTC). |

---

## 3. Relationship with User Domain & Authentication (Task 11)

Every civic issue is reported by an authenticated citizen:
- **Server-Derived Identity**: The reporter (`reported_by`) is extracted exclusively from the verified JWT access token via `CurrentUserService`. Client-supplied reporter values in `CreateIssueRequest` are removed and ignored/rejected, preventing user impersonation.
- **Active Account Enforcement**: The reporting user must exist in `users` and have `is_active = TRUE`. Inactive accounts are rejected with `403 Forbidden`.
- **Foreign Key**: `issues.reported_by -> users.id`
- **Referential Integrity**: `ON DELETE RESTRICT` is enforced. Civic issue reports represent public records and municipal accountability history; deleting a user record must not cascade and delete their reported civic issues.
- **Public Read Access**: Viewing issues (`GET /api/issues`, `GET /api/issues/{id}`) remains completely public without requiring login.
- **Privacy Boundary**: Public issue responses expose only safe public fields (`reportedBy` UUID) and **never** leak citizen phone numbers, passwords, auth tokens, or internal security data.

---

## 4. Relationship with Category Domain

Every civic issue is classified under exactly one category:
- **Foreign Key**: `issues.category_id -> categories.id`
- **Referential Integrity**: `ON DELETE RESTRICT` is enforced. Categories with linked issues cannot be hard-deleted.
- **Active Validation**: `IssueService` strictly verifies that the referenced category exists and has `is_active = TRUE` before an issue can be filed.

---

## 5. Relationship with Location Domain

Every civic issue is anchored to an authoritative geographic point:
- **Foreign Key**: `issues.location_id -> locations.id`
- **Referential Integrity**: `ON DELETE RESTRICT` is enforced. Deleting a location linked to an active civic issue is prohibited.
- **PostGIS Point**: Location domain manages the single source of truth (`geometry(Point, 4326)`). Coordinates are not duplicated in the `issues` table.

---

## 6. Relationship with Media Domain

An Issue can have multiple associated visual evidence attachments (photographs):
- **Foreign Key**: `media.issue_id -> issues.id`
- **Referential Integrity**: `ON DELETE CASCADE` is enforced. If an issue is deleted, its associated media metadata records are purged automatically to prevent orphaned metadata.
- **Ordered Galleries**: Display order (`display_order ASC`) enables consistent presentation of multiple issue photos.
- **Binary Separation**: Binary file data resides in object storage; only file metadata is stored in PostgreSQL.

---

## 7. Status Representation & State Lifecycle (Task 17)

The issue lifecycle uses stable, machine-friendly enum values with enforced state transitions:

- `REPORTED`: Initial default status when an issue is created. Initial status history record created atomically (`NULL -> REPORTED`).
- `VERIFIED`: Issue report confirmed as genuine by moderation or field officer inspection.
- `ACKNOWLEDGED`: Recognized by municipal authorities or designated ward engineers.
- `IN_PROGRESS`: Active repair or mitigation work underway on site.
- `RESOLVED`: Field work completed and marked resolved by municipal authorities.
- `CITIZEN_VERIFIED`: Confirmed as genuinely fixed by the original reporting citizen.
- `NOT_FIXED`: Rejected by original reporting citizen during resolution verification, immediately reopening to `IN_PROGRESS`.

---

## 8. Database Indexes & Constraints

- **Foreign Key Constraints**:
  - `fk_issues_reported_by` referencing `users(id)` with `ON DELETE RESTRICT`.
  - `fk_issues_category_id` referencing `categories(id)` with `ON DELETE RESTRICT`.
  - `fk_issues_location_id` referencing `locations(id)` with `ON DELETE RESTRICT`.
- **Title Check Constraint**: `chk_issues_title_not_empty` ensuring `LENGTH(TRIM(title)) > 0`.
- **Concurrency Protection**: `version BIGINT NOT NULL DEFAULT 0` column for optimistic locking across status updates.
- **Indexes**:
  - `idx_issues_reported_by`: Fast lookup of issues reported by a specific citizen.
  - `idx_issues_category_id`: Fast filtering of issues by category.
  - `idx_issues_location_id`: Fast join / navigation to location.
  - `idx_issues_status`: High-speed filtering by issue status.
  - `idx_issues_created_at`: Chronological timeline queries (`created_at DESC`).

---

## 9. Explicitly Excluded Future Domains

To keep the architecture clean and modular, the following capabilities are **intentionally excluded** and will be designed in their respective dedicated tasks:

- **Authorities & Representatives**: Ward councilors, municipal departments (AMC), and grievance routing belong to the Authorities domain.

---

## 10. Issue Media & Visual Evidence (Task 12)

### 10.1 Ownership & Attachment Rules
Civic issues support attaching photographic evidence under strict ownership rules:
- **Authenticated Issue Creator Only**: Only the authenticated citizen who created the issue (`issue.reporter`) is authorized to attach media via `POST /api/issues/{issueId}/media`.
- **Public & Unauthenticated Access Blocked**: Public users cannot upload media to any issue (`401 Unauthorized`).
- **Cross-User Tampering Prevented**: Attempting to attach media to another citizen's issue is strictly rejected with `403 Forbidden`.
- **Active Account Requirement**: Suspended or inactive accounts cannot attach media (`403 Forbidden`).
- **Server-Derived Source of Truth**: The reporter identity is extracted exclusively from the validated Bearer JWT token; no client-provided uploader identifiers are accepted.

---

## 11. Issue Support System (Task 13)

### 11.1 Endorsement Model
Citizens can formally back / endorse an issue to signal urgency and public importance to municipal authorities:
- **Endpoints**:
  - `POST /api/issues/{issueId}/support` (201 Created, 409 Conflict on duplicate)
  - `DELETE /api/issues/{issueId}/support` (204 No Content, 404 if not supported)
- **Response Integration**: All issue responses (`GET /api/issues/{id}` and `GET /api/issues`) expose:
  - `supportCount`: Total count of citizen endorsements.
  - `supportedByCurrentUser`: Boolean flag indicating if the authenticated user has endorsed the issue (`false` for unauthenticated requests).
- **Self-Support Allowed**: Citizens can support their own reported issues.
- **Supporter Privacy**: Public feeds never expose individual supporter phone numbers or identities.
- **Zero N+1 Query Execution**: `GET /api/issues` batches support counts and current user status in single grouped queries.

---

## 12. Issue Comments System (Task 14)

### 12.1 Discussion & Observation Model
Citizens and residents can discuss ongoing issues and provide ground updates:
- **Endpoints**:
  - `POST /api/issues/{issueId}/comments` (201 Created, requires authentication)
  - `GET /api/issues/{issueId}/comments` (200 OK, paginated, chronological order, public)
  - `DELETE /api/issues/{issueId}/comments/{commentId}` (204 No Content, author-only soft deletion)
- **Response Integration**: Issue responses expose:
  - `commentCount`: Total count of active, non-deleted comments.
- **Zero N+1 Query Execution**: `GET /api/issues` batches active comment counts using a single grouped count query across page issue IDs.
- **Soft Deletion**: Deleted comments are marked with `deleted_at` and displayed as `[Comment deleted]`, preserving moderation history.

---

## 13. Duplicate Issue Tracking & Citizen Duplicate-Warning Workflow (Tasks 15 & 16)

### 13.1 Two-Stage Issue Creation Flow
To avoid spamming municipal dashboards with redundant complaints when multiple citizens report the same hazard, Nagrivic supports a citizen-first duplicate detection workflow:
1. **Stage 1: Pre-Creation Duplicate Check (`POST /api/issues/check-duplicates`)**:
   - Client sends proposed issue coordinates (`locationId`) and `categoryId`.
   - The backend runs a PostGIS proximity query (`ST_DWithin`, GiST index) within a configurable radius (default: 100 meters).
   - If potential same-category candidates exist, up to 5 candidates are returned with accurate distance in meters and support counts.
   - **Crucial Rule**: The endpoint strictly creates no records in the database.
2. **Stage 2: Citizen Decision**:
   - If a candidate matches: The citizen can support the existing issue via `POST /api/issues/{issueId}/support`, increasing public urgency without clutter.
   - If distinct: The citizen proceeds to create a new issue via `POST /api/issues`, which continues to work directly as before.

### 13.2 Primary vs. Duplicate API Representation
Issue responses (`IssueResponse`) expose duplicate status safely without recursive entity serialization:
```json
{
  "id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "isDuplicate": false,
  "primaryIssueId": null
}
```
For a linked duplicate issue:
```json
{
  "id": "e6a0d4c9-58ec-4475-bf7d-944c2ef6cf27",
  "isDuplicate": true,
  "primaryIssueId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
}
```

---

## 14. Issue Status & Resolution Workflow (Task 17)

Every issue lifecycle is governed by the state machine implemented in the Status History module:
- **Operational Transitions**: `POST /api/issues/{issueId}/status` restricted to `ROLE_OFFICER` and `ROLE_ADMIN`.
- **Citizen Verification**: `POST /api/issues/{issueId}/verify-resolution` restricted to original issue reporter when in `RESOLVED` status.
- **Public Audit History**: `GET /api/issues/{issueId}/status-history` is publicly viewable without authentication.
- **Optimistic Locking**: Handled via `version` column on `issues` table.

For complete state transitions, matrix, and immutability guarantees, refer to [status-history.md](file:///j:/Nagrivic/docs/domains/status-history.md).

---

## 15. Civic Geography & Responsible Department Enrichment (Task 18)

Every civic issue is anchored to a geographic location point (`Point, 4326`). The backend dynamically enriches public issue representations with governing civic geography:
- **Dynamic Resolution**: Issue location coordinates are queried against PostGIS ward boundaries (`geometry(MultiPolygon, 4326)`) using spatial index acceleration.
- **Responsible Department**: Resolved from the issue's category, governing civic body, and ward service area mappings.
- **Client Immature Overrides Prevented**: Clients cannot submit `wardId`, `cityId`, `civicBodyId`, or `departmentId` in issue creation requests. Responsibility is 100% server-derived.
- **Unmapped Graceful Degradation**: If boundary data is missing or the issue location lies outside mapped polygons, `civicArea` is returned as `null`. Issue creation and retrieval continue normally without failure.

For complete spatial data model and mapping rules, refer to [civic-geography.md](file:///j:/Nagrivic/docs/domains/civic-geography.md) and [departments.md](file:///j:/Nagrivic/docs/domains/departments.md).

---

## 16. Persisted Civic Responsibility Context (Task 19)

To ensure persistent accountability, administrative filtering, and departmental routing, the server persists the resolved civic responsibility context directly on the `issues` table:
- **Server-Controlled Only**: The client has no ability to supply or override `civicBodyId`, `cityId`, `wardId`, or `departmentId`. `CreateIssueRequest` does not bind these fields.
- **Snapshot Persistence**:
  - `civic_body_id`, `city_id`, `ward_id`, `department_id` are nullable foreign keys referencing their respective authoritative tables with `ON DELETE SET NULL`.
  - `responsibility_status` tracks resolution: `RESOLVED` (all components resolved deterministically) or `UNRESOLVED` (missing or conflicting mapping).
  - Audit columns: `responsibility_resolved_at` and `responsibility_source` (`GEOGRAPHIC_POINT_IN_POLYGON`).
- **"Unresolved Does Not Mean Invalid"**: If ward boundary geometry or category-department mappings are absent, issue reporting succeeds smoothly without failure, marked `UNRESOLVED`.
- **Re-Resolution Service**: `IssueResponsibilityService.resolveIssueResponsibility(UUID issueId)` provides an idempotent internal operation to enrich issues as new municipal geography and department mappings are loaded.
- **N+1 Prevention**: Issues queried through `findIssues` fetch-join `civicBody`, `city`, `ward`, and `department` in the primary query.
- **Safe Exposure via `CivicResponsibilityDto`**: Exposed on `IssueResponse` as `civicResponsibility: { status, civicBody, city, ward, department }`.

---

## 17. Issue Priority & Civic Impact Foundation (Task 20)

Every civic issue is evaluated by a multi-dimensional impact scoring engine that balances defect severity, physical safety hazards, public reach, aging, and citizen support:
- **Formula**:
  $$\text{Priority Score } (0-100) = \text{Severity } (0-30) + \text{Public Impact } (0-25) + \text{Safety Impact } (0-25) + \text{Age } (0-10) + \text{Support } (0-10)$$
- **Categorical Bands**: `LOW` (0-24), `MEDIUM` (25-49), `HIGH` (50-74), `CRITICAL` (75-100).
- **Core Principle: Support Count $\neq$ Priority**:
  - Support score is strictly capped at a maximum of 10 points to prevent popularity brigading from suppressing life-safety hazards.
  - Physical hazards independently contribute up to 55 points (`severity` up to 30 + `safety_impact` up to 25).
- **Server-Controlled Only**:
  - Score, breakdown components, and level are computed exclusively on the server.
  - `CreateIssueRequest` accepts an optional advisory `severity` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), treated strictly as an advisory input.
- **Canonical Duplicate Association**:
  - Duplicate issues associate priority with their canonical primary issue.
- **Recalculation Triggers**:
  - Issue creation (`IssueService.createIssue`).
  - Citizen support added or removed (`SupportService.addSupport`, `removeSupport`).
  - Status transitions (`StatusHistoryService.changeStatus`) and citizen `NOT_FIXED` reactivation (`verifyResolution`).
- **N+1 Prevention**:
  - `findIssues` fetch-joins `priority` alongside civic responsibility entities.
  - Public listing supports optional filtering by `?priority=LOW|MEDIUM|HIGH|CRITICAL`.
- **Heuristic Disclaimer**:
  - Priority scores are internal advisory heuristics for triage and workflow prioritization, not statutory legal SLAs.

For comprehensive details, refer to [issue-priority.md](file:///j:/Nagrivic/docs/domains/issue-priority.md).

---

## 18. Citizen Authenticated "My Reports" Experience (`GET /api/issues/my`) (Task 36)

The Nagrivic platform provides an authenticated endpoint for citizens to track all civic reports they have submitted:
- **Endpoint**: `GET /api/issues/my`
- **Security**: Authenticated only (`ROLE_CITIZEN`, `ROLE_OFFICER`, etc.). Unauthenticated requests are rejected with `401 Unauthorized`.
- **Server-Derived Ownership**: The user identity is derived exclusively from the verified JWT access token (`CurrentUserService.getCurrentUser()`). Client-supplied `reportedBy` parameters are ignored to eliminate user impersonation.
- **Duplicate Preservation**: Reports filed by the authenticated citizen that were linked to a primary issue as a duplicate remain visible in the citizen's reports (`includeDuplicates = true`), marked with `isDuplicate: true` and `primaryIssueId`.
- **Moderation Compliance**: Content hidden by moderation (`moderation_status = 'HIDDEN'`) is safely omitted.
- **Filtering & Sorting Parameters**:
  - `status`: Filter by `IssueStatus` enum.
  - `categoryId`: Filter by Category UUID.
  - `priority`: Filter by `PriorityLevel` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
  - `sort`: Order by `NEWEST` (default), `OLDEST`, `PRIORITY`, or `MOST_SUPPORTED`.
  - `page` & `size`: Standard server pagination (default 20, max 100).
- **Safe Response**: Returns `PagedResponse<IssueResponse>` with zero PII leaks (no phone numbers, emails, passwords, or internal security tokens).

