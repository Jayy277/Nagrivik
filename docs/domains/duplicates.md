# Duplicates Domain & Duplicate Detection Foundation

## 1. Domain Purpose & Core Product Principle

In civic management, multiple citizens frequently report the **same physical civic problem** (e.g., a specific crater pothole at an intersection, an overflowing garbage dumpster, or a fallen tree blocking a road). 

Historically in government ticketing portals, each citizen submission generates a separate ticket (e.g., Issue #101, Issue #102, Issue #103). This creates operational chaos:
- Municipal engineers receive redundant work orders for the same physical hazard.
- Community urgency and citizen endorsements are fragmented across multiple tickets.
- Status updates and resolution proofs must be duplicated across multiple records.

Nagrivic’s foundational product principle is:
> **Multiple citizens reporting the SAME physical civic problem should eventually represent ONE public issue rather than many disconnected duplicate issues.**

```
Canonical Primary Issue #101 (Public record, holds aggregate community attention)
  ├── Duplicate Report #102 (Preserves citizen report, description, photo & timestamp)
  └── Duplicate Report #103 (Preserves citizen report, description, photo & timestamp)
```

---

## 2. Primary vs. Duplicate Model

1. **Primary Issue (`duplicate_of_issue_id IS NULL`)**:
   - The canonical, independent public issue.
   - Appears in public feeds, search queries, and municipal triage queues.
   - Accumulates community endorsements and comments.
2. **Duplicate Issue (`duplicate_of_issue_id = <primary_id>`)**:
   - A real, authentic citizen report that has been identified as pointing to an existing primary issue.
   - **Never hard-deleted**: retains its original citizen author (`reported_by`), location (`location_id`), description, media evidence (`media`), comments, and timestamps.
   - Points directly to the canonical primary issue via `duplicate_of_issue_id`.
   - In public API representations, clearly marks `isDuplicate: true` and `primaryIssueId: <UUID>`.

---

## 3. Database Schema & Constraints

Implemented in Flyway migration `V15__add_duplicate_of_to_issues.sql`:

```sql
ALTER TABLE issues
    ADD COLUMN duplicate_of_issue_id UUID NULL,
    ADD CONSTRAINT fk_issues_duplicate_of FOREIGN KEY (duplicate_of_issue_id) REFERENCES issues(id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_issues_not_self_duplicate CHECK (duplicate_of_issue_id IS NULL OR duplicate_of_issue_id <> id);

CREATE INDEX idx_issues_duplicate_of_issue_id ON issues(duplicate_of_issue_id);
```

### Constraints & Integrity Rules:
- **Self-Reference Prevention**: `chk_issues_not_self_duplicate` rejects any record where `duplicate_of_issue_id = id`.
- **No Cascade Deletion**: `ON DELETE SET NULL` guarantees that deleting or retiring an issue never cascades to delete linked reports.
- **Fast Indexed Queries**: B-tree index `idx_issues_duplicate_of_issue_id` optimizes primary-to-duplicate lookups.

---

## 4. Duplicate Relationship Normalization & Cycle Prevention

To prevent multi-hop dependency chains (e.g., `Issue C -> Issue B -> Issue A`), Nagrivic enforces strict tree flattening and cycle rejection at the service layer (`DuplicateDetectionService`):

1. **Normalization to Root Primary**:
   If an issue is marked as a duplicate of an issue that is itself already a duplicate, the relationship is automatically normalized to the root primary:
   $$\text{Target} \rightarrow \text{Root Primary} \implies \text{Source} \rightarrow \text{Root Primary}$$
2. **Child Reparenting**:
   If an existing primary issue that already has linked duplicates is linked to another primary issue, all existing child duplicates are reparented directly to the new root primary.
3. **Cycle Prevention**:
   Traversing the target's hierarchy guarantees that linking does not create circular dependencies (e.g., `A -> B -> A`). Attempts to create a circular link throw `IllegalArgumentException`.

---

## 5. PostGIS Proximity Search & Candidate Detection

Duplicate detection relies entirely on **deterministic geographic and semantic signals**. Nagrivic strictly rejects black-box ML/AI or automatic distance merges.

### Proximity Search Query
Using PostGIS spatial indexing on `locations.location_point` (`geometry(Point, 4326)`):

```sql
SELECT i.id, i.title, c.id, c.name, c.slug, i.status,
       ROUND(CAST(ST_Distance(l.location_point::geography, ST_SetSRID(ST_MakePoint(:targetLon, :targetLat), 4326)::geography) AS numeric), 2) AS distance_meters
FROM issues i
JOIN locations l ON i.location_id = l.id
JOIN categories c ON i.category_id = c.id
WHERE i.category_id = :categoryId
  AND i.duplicate_of_issue_id IS NULL
  AND (:excludeIssueId IS NULL OR i.id <> :excludeIssueId)
  AND l.location_point && ST_Expand(ST_SetSRID(ST_MakePoint(:targetLon, :targetLat), 4326), :radiusDegrees)
  AND ST_DWithin(l.location_point::geography, ST_SetSRID(ST_MakePoint(:targetLon, :targetLat), 4326)::geography, :radiusMeters)
ORDER BY distance_meters ASC
LIMIT :limit;
```

### Deterministic Signals:
1. **GiST Spatial Index Utilization**: `l.location_point && ST_Expand(...)` filters bounding boxes using the existing GiST index without scanning full tables.
2. **PostGIS Distance on Spheroid**: `ST_DWithin` and `ST_Distance` on `geography` compute true surface distances in meters.
3. **Same Category Requirement**: Potholes match potholes, garbage matches garbage. Unrelated categories are never compared.
4. **Primary Issue Filtering**: Only canonical primary issues (`duplicate_of_issue_id IS NULL`) are presented as duplicate candidates.
5. **Configurable Radius**: Configured via `nagrivic.duplicates.detection-radius-meters` (default: 100.0 meters).

---

## 6. Citizen Duplicate Warning Flow (Task 16)

Instead of silently creating duplicate tickets or guessing user intent, Nagrivic employs a **two-step issue creation flow**:

```
Citizen enters issue details (photo, location, category)
                       │
                       ▼
      POST /api/issues/check-duplicates
                       │
                       ├──────────────────────────────────┐
                       ▼                                  ▼
             [Candidates Found]                   [No Candidates]
                       │                                  │
      ┌────────────────┴───────────────┐                  ▼
      ▼                                ▼          POST /api/issues
[Support Existing]             [Create New Issue]  (Created directly)
      │                                │
      ▼                                ▼
POST /api/issues/{id}/support   POST /api/issues
(Zero duplicate issue created)  (New issue created)
```

### Endpoint: `POST /api/issues/check-duplicates`
- **Authentication**: Required (`Bearer <token>`).
- **Request Body**:
  ```json
  {
    "title": "Deep pothole near junction",
    "description": "Hazardous for two-wheelers",
    "categoryId": "488f2881-2292-4f0f-8c38-0c6a51276067",
    "locationId": "73e93b14-8f0c-4976-b997-75982e5b871c"
  }
  ```
- **Response Body**:
  ```json
  {
    "hasPotentialDuplicates": true,
    "candidates": [
      {
        "issueId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
        "title": "Large pothole on main road",
        "category": {
          "id": "488f2881-2292-4f0f-8c38-0c6a51276067",
          "name": "Roads / Potholes",
          "slug": "roads-potholes"
        },
        "distanceMeters": 28.45,
        "status": "REPORTED",
        "supportCount": 12
      }
    ]
  }
  ```
- **Candidate Limit**: Maximum 5 candidates returned to citizens to maintain clean, fast mobile UX.
- **Side-Effect Free**: This endpoint **strictly never persists** any issue record.

---

## 7. Why Geographic Proximity Alone Does NOT Mean Duplicate

Geographic closeness is a candidate signal, **not** proof of identity:
- A 100-meter radius in a dense urban ward in Ahmedabad may contain three distinct potholes, two broken streetlights, and an unrelated drainage blockage on opposite sides of a street.
- Automatic merging based on distance would falsely erase distinct civic problems.
- Therefore, **the citizen always retains ultimate agency**:
  - If an existing issue represents their problem, they tap **Support Existing** (`POST /api/issues/{id}/support`), adding citizen backing without creating clutter.
  - If their problem is separate, they tap **Create New Issue** (`POST /api/issues`), filing an independent report.

---

## 8. Privileged Duplicate Linking (Internal / Moderation)

Normal citizens **cannot** merge or link issues arbitrarily. Duplicate linking is restricted to authorized municipal officers (`ROLE_OFFICER`) or platform administrators (`ROLE_ADMIN`):

- **Endpoint**: `POST /api/issues/{issueId}/duplicate`
- **Request**: `{"primaryIssueId": "<UUID>"}`
- **Security**: Rejects unauthenticated callers with `401 Unauthorized` and ordinary citizens with `403 Forbidden`.
