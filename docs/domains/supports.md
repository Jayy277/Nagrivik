# Support Domain & Issue Endorsement Architecture

## 1. Domain Purpose

Civic change requires collective citizen voice. When multiple residents encounter the same civic problem—such as an unlit road intersection, a dangerous pothole cluster, or irregular garbage collection—each resident's endorsement provides critical signal to municipal authorities and ward councilors regarding community impact and urgency.

The **Support Domain** provides the backend foundation for citizens to endorse ("support") civic issues reported by themselves or fellow citizens.

> [!NOTE]
> **Support is NOT a Social Media "Like"**: Support represents a formal citizen endorsement of a public grievance's validity and urgency. Nagrivic does not implement likes, dislikes, downvotes, or reposts.

---

## 2. Database Schema (`supports`)

Defined in Flyway migration `V13__create_supports_table.sql`:

```sql
CREATE TABLE supports (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_supports_issue_id FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_supports_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uq_supports_issue_user UNIQUE (issue_id, user_id)
);

CREATE INDEX idx_supports_issue_id ON supports(issue_id);
CREATE INDEX idx_supports_user_id ON supports(user_id);
```

### Key Schema Fields
| Field | Column | Type | Constraints | Description |
|---|---|---|---|---|
| `id` | `id` | `UUID` | `PRIMARY KEY` | Globally unique identifier (UUIDv4). |
| `issue` | `issue_id` | `UUID` | `NOT NULL`, `FK` | References `issues(id)` with `ON DELETE CASCADE`. |
| `user` | `user_id` | `UUID` | `NOT NULL`, `FK` | References `users(id)` with `ON DELETE RESTRICT`. |
| `createdAt` | `created_at` | `TIMESTAMPTZ` | `NOT NULL`, default `NOW()` | UTC timestamp when support was recorded. |

### Invariants & Constraint Guarantees
1. **Database-Enforced Uniqueness (`uq_supports_issue_user`)**: A user cannot support the same issue more than once. The constraint is enforced directly by PostgreSQL, preventing duplicate rows even under concurrent race conditions.
2. **Referential Integrity**:
   - Deleting an issue cascades and deletes associated support records (`ON DELETE CASCADE`).
   - Deleting a user who has active support history is restricted (`ON DELETE RESTRICT`) to preserve civic accountability audit trails.
3. **Hard Deletion for Removal**: When a user un-supports an issue, the record is hard-deleted (`DELETE FROM supports WHERE ...`) keeping tables and indexes compact.

---

## 3. REST API Contract

### 3.1 Support an Issue
```http
POST /api/issues/{issueId}/support
Authorization: Bearer <jwt-token>
```

- **Authentication**: Required via Bearer JWT.
- **Success Status**: `201 Created`
- **Duplicate Support**: `409 Conflict` (`CONFLICT` error code).
- **Non-Existent Issue**: `404 Not Found` (`NOT_FOUND` error code).
- **Inactive Citizen**: `403 Forbidden` (`FORBIDDEN` error code).

### 3.2 Remove Support
```http
DELETE /api/issues/{issueId}/support
Authorization: Bearer <jwt-token>
```

- **Authentication**: Required via Bearer JWT.
- **Success Status**: `204 No Content`
- **Support Not Found**: `404 Not Found` (user has not supported this issue).
- **Cross-User Protection**: A user cannot remove another citizen's support.

---

## 4. Issue Response Integration & N+1 Prevention

### 4.1 Response Structure
Issue responses (`IssueResponse`) include support fields:

```json
{
  "id": "b1b689bc-...",
  "reportedBy": "a4d33110-...",
  "category": { "id": "...", "name": "Roads / Potholes", "slug": "roads-potholes" },
  "location": { "latitude": 23.0225, "longitude": 72.5714 },
  "title": "Large pothole near SG Highway",
  "description": "Deep pothole causing vehicle damage",
  "status": "REPORTED",
  "media": [],
  "supportCount": 12,
  "supportedByCurrentUser": true,
  "createdAt": "2026-09-11T16:00:00Z",
  "updatedAt": "2026-09-11T16:00:00Z"
}
```

- `supportCount`: Total number of citizen endorsements.
- `supportedByCurrentUser`: `true` if the authenticated citizen has supported this issue; `false` if not supported or if the request is unauthenticated.
- **Privacy Guarantee**: Individual supporter identities, phone numbers, and names are **never** returned in public issue feeds.

### 4.2 Query Optimization (Zero N+1 Queries)
On `GET /api/issues`, calculating support counts and current-user support status avoids N+1 queries using two batch operations:
1. **Batch Count Aggregation**:
   ```sql
   SELECT s.issue_id, COUNT(s.id) FROM supports s WHERE s.issue_id IN (:issueIds) GROUP BY s.issue_id;
   ```
2. **Batch User Support Check (if authenticated)**:
   ```sql
   SELECT s.issue_id FROM supports s WHERE s.user_id = :currentUserId AND s.issue_id IN (:issueIds);
   ```
Total queries for an entire page of issues remain constant regardless of page size.

---

## 5. Business Rules & Rationale

### 5.1 Self-Support Rule (Allowed)
- **Rule**: A citizen is allowed to support their own reported issue.
- **Rationale**: An issue reported by a citizen represents a genuine civic concern for that citizen. Requiring artificial prevention adds unnecessary complexity, whereas allowing it treats all citizen votes equally.

### 5.2 Concurrency & Race Conditions
When duplicate support requests arrive simultaneously for the same user and issue:
- Both requests pass the preliminary application `existsByIssue_IdAndUser_Id` check.
- The PostgreSQL `uq_supports_issue_user` unique constraint guarantees only one transaction commits.
- The losing transaction triggers `DataIntegrityViolationException`, which the service layer catches and translates into a clean `409 Conflict`.

---

## 6. Deferred Features (Future Scope)

The following features remain deferred to future tasks:
- Sorting by "Most Supported" (`ORDER BY supportCount DESC`)
- Redis cache counters for ultra-high-volume viral issues
- Mobile push notifications when an issue reaches support milestones (10, 50, 100 supports)
- Official authority notification triggers based on threshold support levels
- Mobile support button animations and UI
