# Nagrivic Moderation & Abuse Prevention Domain

## 1. Overview & Core Principles

Nagrivic is a public civic reporting and accountability platform. Its moderation and abuse prevention foundation protects public civic discourse, prevents spam, and mitigates harassment while strictly upholding the following foundational principles:

### A. Political Neutrality
> **"Political criticism or disagreement is not, by itself, a moderation violation."**

Nagrivic citizens have a fundamental right to report infrastructure failures, criticize municipal inaction, question public representatives, and express dissatisfaction with civic governance. Under no circumstances may political disagreement, criticism of a political party, or disapproval of a government authority be categorized as a moderation violation.

### B. Distinction Between Content Safety and Factual Verification
> **"Moderation does not establish whether a civic allegation is factually true."**

Moderation addresses **content safety** (harassment, doxxing, hate speech, spam, sexually explicit material, abusive behavior, and mechanical platform abuse). It does **not** evaluate whether a citizen's complaint about a pothole, pipeline leak, or waste accumulation is factually accurate. Factual disputes and on-site verification belong exclusively to the authoritative civic status workflow (`VERIFIED`, `NOT_FIXED`, `RESOLVED`), not the moderation system.

### C. Reversible Soft Moderation & Audit Integrity
Content that violates safety standards is **soft-hidden** (`HIDDEN`), never hard-deleted. Soft-hiding removes content from public search, feeds, and detail endpoints while preserving the immutable audit trail and historical record for municipal officers, compliance, and legal audit.

---

## 2. Moderation Status Model

Content entities (`IssueEntity` and `CommentEntity`) maintain a controlled moderation state:

| Status | Meaning | Public API Behavior |
| :--- | :--- | :--- |
| `VISIBLE` | Normal healthy standing. | Fully visible in public feeds, searches, and detail endpoints. |
| `UNDER_REVIEW` | Subject to pending or ongoing moderator review. | Remains publicly accessible unless explicitly hidden by a moderator. |
| `HIDDEN` | Soft-hidden due to confirmed or suspected policy violation. | Excluded from public listings, searches, and queries; detail requests return HTTP `404 Not Found`. Preserved in internal audit history. |

---

## 3. Moderation Reports Model

Citizens can report problematic content using a unified, auditable reporting queue (`moderation_reports` table).

### Database Schema

```sql
CREATE TABLE moderation_reports (
    id UUID PRIMARY KEY,
    reporter_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type VARCHAR(32) NOT NULL, -- 'ISSUE', 'COMMENT', 'USER'
    target_id UUID NOT NULL,
    reason VARCHAR(64) NOT NULL,
    description VARCHAR(1000) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN', -- 'OPEN', 'IN_REVIEW', 'RESOLVED', 'DISMISSED'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ NULL,
    resolved_by_user_id UUID NULL REFERENCES users(id) ON DELETE SET NULL
);
```

### Controlled Moderation Reasons

1. `SPAM` — Unsolicited commercial promotions, repeated promotional links, or bot postings.
2. `ABUSIVE_OR_HARASSING` — Threatening language, targeted harassment, or bullying directed at individuals.
3. `HATEFUL_CONTENT` — Hate speech targeting religion, caste, gender, ethnicity, or marginalized communities.
4. `SEXUAL_OR_EXPLICIT` — Obscene, pornographic, or sexually suggestive imagery or descriptions.
5. `PERSONAL_INFORMATION` — Unauthorized posting of private personal data (doxxing: private phone numbers, home addresses, Aadhaar numbers).
6. `MISLEADING_OR_MANIPULATIVE` — Fabricated media, forged timestamps, or deliberate geographic deception.
7. `DUPLICATE_CONTENT` — Intentional flooding of the exact same problem report.
8. `IRRELEVANT` — Non-civic content (personal blog posts, memes, commercial reviews).
9. `OTHER` — Other policy violations accompanied by a required explanation.

*Note: Political criticism or disagreement with civic administration is strictly excluded from valid reasons.*

### Report Lifecyle States

```
OPEN ──► IN_REVIEW ──► RESOLVED (Moderation action executed)
                   └──► DISMISSED (Evaluated as non-violating)
```

- **OPEN**: Citizen report submitted and awaiting assignment in the moderation queue.
- **IN_REVIEW**: Moderator is actively investigating the content.
- **RESOLVED**: Moderator confirmed a violation and executed a moderation action (e.g. `HIDE_CONTENT`).
- **DISMISSED**: Moderator evaluated the report and concluded no safety or policy violation occurred.

---

## 4. Duplicate Report Protection & Self-Reporting Rules

1. **Duplicate Report Idempotency**: If an authenticated citizen submits a report for a target (`ISSUE` or `COMMENT`) for which they already have an unresolved report (`OPEN` or `IN_REVIEW`), the API rejects the submission with HTTP `409 Conflict` (`CONFLICT`: "A report for this content has already been submitted by you and is under review").
2. **Self-Reporting Prevention**: Citizens cannot report their own issues or comments. Attempting to do so returns HTTP `400 Bad Request` ("You cannot report your own issue/comment").
3. **Reporting Already Hidden Content**: Content that is already soft-deleted or hidden cannot be reported again, returning HTTP `400 Bad Request` ("Content has already been removed or hidden").

---

## 5. Moderation Actions & Audit Trail

All actions executed by moderators are recorded in an append-only audit log (`moderation_actions` table). This audit trail is separate from the public Issue Activity Timeline and is accessible only to authorized municipal officers and platform administrators.

### Action Types

- `NO_ACTION` — Case dismissed without altering content or user standing.
- `HIDE_CONTENT` — Content soft-hidden (`HIDDEN`) from public endpoints.
- `RESTORE_CONTENT` — Content restored to `VISIBLE` standing.
- `REMOVE_COMMENT` — Comment hidden from public view.
- `RESTRICT_USER` — Citizen temporarily or indefinitely restricted from contributing.
- `UNRESTRICT_USER` — User restored to `ACTIVE` standing.

### Audit Record Schema

```sql
CREATE TABLE moderation_actions (
    id UUID PRIMARY KEY,
    report_id UUID NULL REFERENCES moderation_reports(id) ON DELETE SET NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id UUID NOT NULL,
    moderator_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(64) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    notes TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 6. User Restriction Foundation

The `users` table is extended with three lightweight moderation fields:
- `moderation_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'` (`ACTIVE`, `RESTRICTED`)
- `restricted_until TIMESTAMPTZ NULL`
- `restriction_reason VARCHAR(500) NULL`

When a user is restricted (`moderationStatus == RESTRICTED` and `restrictedUntil` is null or in the future):
- **Blocked Actions**: Creating issues, submitting comments, adding issue support, and submitting moderation reports are rejected with HTTP `403 Forbidden` ("Your account is currently restricted from performing this action due to moderation").
- **Permitted Actions**: Read-only public access to civic issues, comments, categories, and public maps remains fully permitted.
- **Reversibility**: Moderators can lift restrictions at any time via `unrestrictUser(...)`.

---

## 7. Rate Limiting Foundation

High-risk public and authenticated endpoints are protected by application-level rate limiting via the `RateLimiter` interface and `InMemoryRateLimiter` implementation.

### Configured Default Limits (`application.yml`)

```yaml
nagrivic:
  moderation:
    rate-limit:
      comment-per-minute: 5
      issue-per-hour: 10
      report-per-hour: 10
      support-per-minute: 20
      otp-per-minute: 3
```

### Rate Limit Response
When a limit is exceeded, the server returns HTTP `429 Too Many Requests` with a standard `Retry-After: <seconds>` HTTP response header:

```json
{
  "timestamp": "2026-09-11T18:19:15.000Z",
  "status": 429,
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded. Please try again in 3600 seconds.",
  "path": "/api/moderation/reports"
}
```

### Production Architecture & Limitations of In-Memory Limiting
> [!WARNING]
> The current `InMemoryRateLimiter` maintains sliding windows in JVM memory (`ConcurrentHashMap`). In a horizontally scaled multi-instance deployment behind a load balancer, instances do not share state. For multi-node production clusters, replace `InMemoryRateLimiter` with a distributed store implementation (such as Redis Sliding Window or Redisson RateLimiter) implementing the identical `RateLimiter` interface.

---

## 8. Content Abuse Protection

### Comment Abuse Filtering
Before persisting a comment, `ContentAbuseValidator` executes deterministic safety checks:
1. **Empty / Blank Validation**: Comments consisting only of whitespace are rejected with HTTP `400 Bad Request`.
2. **Excessive Repeated Characters**: Content containing 10 or more consecutive identical characters (e.g. `aaaaaaaaaa`, `!!!!!!!!!!`) is rejected with HTTP `400 Bad Request`.
3. **Rapid Duplicate Submission**: If an author submits the identical comment text on the same issue within 60 seconds, it is rejected with HTTP `409 Conflict`.

### Issue Abuse Filtering
1. **Accidental Double-Click Protection**: If an author submits an issue with the identical title in the exact same category and location within 10 seconds, it is rejected with HTTP `409 Conflict`.
2. **Duplicate Detection Integration**: Legitimate nearby civic complaints are evaluated by the existing spatial duplicate detection system (`checkDuplicates`), never automatically discarded or merged.

---

## 9. Privacy & Security Safeguards

1. **Zero PII Exposure**: Moderation reports, internal moderator notes, reporter identity, and investigation details are strictly excluded from public Issue and Activity APIs.
2. **HTML & Script Sanitization**: User-supplied report descriptions are stripped of all HTML and script tags using regex normalization.
3. **Strict RBAC Separation**: Normal citizens can only submit reports (`POST /api/moderation/reports`). All administrative moderation operations (`/api/moderation/**`) require `OFFICER` or `ADMIN` roles.
