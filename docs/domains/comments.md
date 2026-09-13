# Comments Domain & Issue Discussion Architecture

## 1. Domain Purpose

Public civic issues frequently require updates, ground observations, and community discussion:
- Citizens reporting whether a problem is ongoing, worsening, or temporarily blocked.
- Residents providing additional landmarks or neighborhood context.
- Future municipal acknowledgments or work updates.

The **Comments Domain** provides the backend foundation for issue-specific comments in Nagrivic.

> [!NOTE]
> **Issue-Specific Discussion, Not a Social Feed**: Comments belong strictly to individual civic issues (`/api/issues/{issueId}/comments`). Nagrivic does not implement generic social networking feeds, hashtags, mentions, or comment reactions.

---

## 2. Database Schema (`comments`)

Defined in Flyway migration `V14__create_comments_table.sql`:

```sql
CREATE TABLE comments (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL,
    user_id UUID NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_comments_issue_id FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_comments_content_not_empty CHECK (LENGTH(TRIM(content)) > 0)
);

CREATE INDEX idx_comments_issue_id ON comments(issue_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_issue_created_at ON comments(issue_id, created_at ASC);
CREATE INDEX idx_comments_deleted_at ON comments(deleted_at);
```

### Key Schema Fields
| Field | Column | Type | Constraints | Description |
|---|---|---|---|---|
| `id` | `id` | `UUID` | `PRIMARY KEY` | Globally unique identifier (UUIDv4). |
| `issue` | `issue_id` | `UUID` | `NOT NULL`, `FK` | References `issues(id)` with `ON DELETE CASCADE`. |
| `user` | `user_id` | `UUID` | `NOT NULL`, `FK` | References `users(id)` with `ON DELETE RESTRICT`. |
| `content` | `content` | `VARCHAR(1000)` | `NOT NULL`, max 1000 | Plain-text comment content (trimmed, non-empty). |
| `createdAt` | `created_at` | `TIMESTAMPTZ` | `NOT NULL`, default `NOW()` | Creation timestamp. |
| `updatedAt` | `updated_at` | `TIMESTAMPTZ` | `NOT NULL`, default `NOW()` | Last update timestamp. |
| `deletedAt` | `deleted_at` | `TIMESTAMPTZ` | Nullable | Soft deletion timestamp. When populated, comment is considered deleted. |

---

## 3. Soft Deletion & Audit Integrity

Nagrivic employs **soft deletion** for comments:
1. When a user deletes their comment, the database row is **never** physically deleted (`deleted_at = NOW()`).
2. **Audit Preservation**: Retaining deleted comment rows preserves historical records for municipal moderation and dispute investigation.
3. **Public Masking**: When served via the public API, soft-deleted comments return:
   ```json
   {
     "id": "c3f8a120-...",
     "issueId": "a9d0337f-...",
     "content": "[Comment deleted]",
     "author": { "id": "...", "displayName": "Citizen" },
     "deleted": true,
     "createdAt": "2026-09-11T16:00:00Z",
     "updatedAt": "2026-09-11T16:05:00Z"
   }
   ```
   The original deleted text is never sent over the wire.

---

## 4. REST API Contract

### 4.1 Create a Comment
```http
POST /api/issues/{issueId}/comments
Content-Type: application/json
Authorization: Bearer <jwt-token>

{
  "content": "This road has been damaged for several weeks."
}
```
- **Auth**: Required via Bearer JWT.
- **Success**: `201 Created` returning `CommentResponse`.
- **Validation**: Rejects empty strings, whitespace-only, or text > 1000 chars (`400 Bad Request`).
- **Target Issue**: Must exist (`404 Not Found`).
- **Author Identity**: Derived exclusively from the validated JWT token; client cannot supply author ID.

### 4.2 Read Comments (Public)
```http
GET /api/issues/{issueId}/comments?page=0&size=20
```
- **Auth**: Public (no login required).
- **Success**: `200 OK` returning `PagedResponse<CommentResponse>`.
- **Sort**: Fixed chronological order (`createdAt ASC`) for conversational clarity.
- **Pagination**: Default `size=20`, max `size=100`.

### 4.3 Delete a Comment
```http
DELETE /api/issues/{issueId}/comments/{commentId}
Authorization: Bearer <jwt-token>
```
- **Auth**: Required via Bearer JWT.
- **Success**: `204 No Content`.
- **Ownership**: Only the comment author can delete (`403 Forbidden` for other users).
- **Issue Association**: Comment must belong to the specified issue (`404 Not Found` if mismatched).
- **Idempotency**: If already deleted, operation safely succeeds without error.

---

## 5. Comment Count & Zero N+1 Optimization

Public issue responses include an active comment count:
```json
{
  "id": "...",
  "supportCount": 12,
  "commentCount": 5,
  "supportedByCurrentUser": false
}
```

- `commentCount` reflects **only active, non-deleted comments** (`deleted_at IS NULL`).
- **N+1 Prevention on `GET /api/issues`**: The backend loads active comment counts for an entire page of issues in a single grouped batch query:
  ```sql
  SELECT c.issue_id, COUNT(c.id) FROM comments c
  WHERE c.issue_id IN (:issueIds) AND c.deleted_at IS NULL
  GROUP BY c.issue_id;
  ```
  Page size does not impact query count.

---

## 6. Security & Abuse Prevention

1. **Plain-Text Treatment**: Comments are strictly treated as plain text. No HTML or Markdown parsing is performed on the server.
2. **Content Length Constraints**: Minimum 1 non-whitespace character, maximum 1000 characters.
3. **No PII Leaks**: Public comment payloads include only author UUID and display name (falling back to `"Citizen"` if `fullName` is unset). Phone numbers are never exposed.
4. **Future Enhancements**: Automated toxicity filtering, rate limits on comment submissions per citizen per hour, and municipal official badges.
