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
| `title` | `title` | `VARCHAR(255)` | No | Brief human-readable summary of the civic problem (non-empty). |
| `description` | `description` | `TEXT` | Yes | Extended contextual description provided by the citizen. |
| `status` | `status` | `VARCHAR(32)` | No | Machine-friendly status enum value (default: `'REPORTED'`). |
| `createdAt` | `created_at` | `TIMESTAMPTZ` | No | Point-in-time timestamp when the issue was reported (UTC). |
| `updatedAt` | `updated_at` | `TIMESTAMPTZ` | No | Point-in-time timestamp of the last modification (UTC). |

---

## 3. Relationship with User Domain

Every civic issue is reported by an authenticated or verified citizen:
- **Foreign Key**: `issues.reported_by -> users.id`
- **Referential Integrity**: `ON DELETE RESTRICT` is enforced. Civic issue reports represent public records and municipal accountability history; deleting a user record must not cascade and delete their reported civic issues.
- **Privacy Boundary**: The `issues` table stores only the foreign key `reported_by`. It does **not** duplicate sensitive citizen personal data (such as phone numbers, email addresses, or home addresses).

---

## 4. Status Representation

The initial issue lifecycle representation uses stable, machine-friendly enum values:

- `REPORTED`: Initial default status when an issue is created.
- `VERIFIED`: Issue report confirmed as genuine by moderation or community validation.
- `ACKNOWLEDGED`: Recognized by municipal authorities or designated ward engineers.
- `IN_PROGRESS`: Active repair or mitigation work underway on site.
- `RESOLVED`: Field work completed and marked resolved by municipal authorities.
- `CITIZEN_VERIFIED`: Confirmed as genuinely fixed by citizens on the ground.

---

## 5. Database Indexes & Constraints

- **Foreign Key Constraint**: `fk_issues_reported_by` referencing `users(id)` with `ON DELETE RESTRICT`.
- **Title Check Constraint**: `chk_issues_title_not_empty` ensuring `LENGTH(TRIM(title)) > 0`.
- **Indexes**:
  - `idx_issues_reported_by`: Fast lookup of issues reported by a specific citizen.
  - `idx_issues_status`: High-speed filtering by issue status.
  - `idx_issues_created_at`: Chronological timeline queries (`created_at DESC`).

---

## 6. Explicitly Excluded Future Domains

To keep the architecture clean and modular, the following capabilities are **intentionally excluded** from Task 5 and will be designed in their respective dedicated tasks:

- **Category**: Civic taxonomy (`ROADS_POTHOLES`, `GARBAGE`, etc.) will be mapped via a dedicated Category domain.
- **Location & PostGIS**: Latitude, longitude, `geography(Point, 4326)`, and ward boundary polygon containment (`ST_Contains`) belong to the dedicated Location domain.
- **Media**: Evidence attachments, photos, videos, and S3 pre-signed URLs belong to the Media domain (`issue_media`).
- **Supports**: Community upvoting and endorsement counts belong to the Support domain.
- **Comments**: Citizen and official discussion threads belong to the Comments domain.
- **Status History**: State-transition audit trails and SLA tracking belong to the Status History domain.
- **Duplicate Detection**: Spatial proximity clustering (`ST_DWithin`) and visual duplicate matching belong to the Duplicates domain.
- **Authorities & Representatives**: Ward councilors, municipal departments (AMC), and grievance routing belong to the Authorities domain.
