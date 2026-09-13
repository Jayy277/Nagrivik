# Nagrivic Security Architecture & Principles

> **Notice**: This document outlines security principles and architectural requirements for Nagrivic. Authentication, authorization, and rate limiting mechanisms described here are design specifications and must not be implemented until their designated security implementation phases.

---

## 1. Core Security Principles

1. **Zero Hardcoded Secrets**:
   - Secrets, database credentials, API keys, and signing keys must never be committed to Git.
   - All sensitive configurations must be injected via environment variables or cloud secret managers.
   - `.env` files are strictly excluded via `.gitignore`. A `.env.example` file must provide placeholder documentation only.

2. **Defense in Depth & Input Validation**:
   - Never trust input from the client (mobile app or web).
   - Validate all user input both on the client (for user experience) and strictly on the Spring Boot backend using Jakarta Bean Validation (`@Valid`).
   - Sanitize all text fields before persistence to prevent XSS and SQL injection.

3. **Least Privilege Principle**:
   - The backend database user must only possess permissions required for its schema (no superuser access).
   - Internal API endpoints enforce role-based access control (RBAC), granting users only the minimum permissions necessary for their role.

---

## 2. Authentication & Authorization

### Primary Citizen Authentication (Task 33: Google Sign-In)
- **Primary Provider**: Google Sign-In is the primary authentication provider across mobile (React Native + Expo) and web (Next.js).
- **Immutable Subject Identity**: Users are mapped via `user_auth_identities (provider='GOOGLE', provider_subject=<sub-claim>)`. Email alone is never used as an immutable identifier.
- **Cryptographic Token Verification**: The backend validates Google-issued ID tokens with Google's official client SDK:
  - Cryptographic signature check against Google public keys.
  - Issuer validation (`accounts.google.com` or `https://accounts.google.com`).
  - Audience allowlist verification against configured client IDs (`GOOGLE_CLIENT_IDS`).
  - Expiry and non-blank subject claim verification.
- **Zero Client Trust**: Claims supplied by client JSON payloads (such as email, name, role) are ignored. Identity is derived solely from the verified token.
- **Role Escalation Prevention**: Google sign-ins unconditionally auto-provision/load with role `CITIZEN`. Elevated roles (`ADMIN`, `OFFICER`) can never be self-assigned.
- **Email Matching Safety**: Accounts are never silently merged solely because a Google email matches an existing phone user's email.
- **Concurrent Login Protection**: Database unique constraints on `(provider, provider_subject)` combined with transactional conflict recovery safely handle parallel first-time login attempts.
- **Web Session Security**: Web tokens are stored strictly in secure HTTP-only cookies (`nagrivic_access_token`, `nagrivic_refresh_token`) with SameSite=Lax and Secure attributes. Client secrets are never exposed to browser bundles.
- **Mobile Session Security**: Mobile tokens are stored exclusively in Expo `SecureStore` (iOS Keychain / Android Keystore).

### Secondary & Historical Auth (Task 10: Phone OTP)
- Phone OTP infrastructure (`OtpService`, `DevOtpProvider`) remains supported in the backend for automated testing and future secondary fallback, but is deferred from primary citizen UI.

### Authenticated Issue Reporting (Task 11)
- **Server-Derived Identity**: Authenticated user identity is server-derived from the JWT. The client-supplied `reportedBy` mechanism from Task 9 is completely removed.
- **Impersonation Prevention**: Clients cannot report issues on behalf of another user. The server assigns `Issue.reportedBy` exclusively from the verified JWT principal.
- **Active User Verification**: Only active users (`is_active = TRUE`) can create civic issues. Inactive accounts receive `403 Forbidden`.
- **Public Read Access**: Viewing issues (`GET /api/issues`, `GET /api/issues/{id}`) remains public and requires no authentication.
- **PII Scrubbing**: Public issue responses expose only the reporter UUID and never leak phone numbers, full names, or tokens.

### Future Authorization & Roles
- **Municipal Authorities & Admins**:
  - Secure credential login with bcrypt-hashed passwords.
  - Multi-Factor Authentication (MFA) required for admin and municipal department officer logins.

### Role-Based Access Control (RBAC)
The platform defines three primary role tiers:

| Role | Permissions |
|---|---|
| `ROLE_CITIZEN` | Report issues, upload photos, upvote ("Support") issues, post comments, confirm resolution. |
| `ROLE_OFFICER` | View assigned department queue, update ticket progress, upload resolution proof photos, mark resolved. |
| `ROLE_ADMIN` | Moderate content, reassign wards, manage authority users, view city-wide audit logs. |

---

## 3. Privacy & Citizen Protection

Civic reporting must safeguard citizen safety and privacy:

- **PII Redaction on Public Feeds**:
  - Public issue detail pages and maps must **never** expose citizen phone numbers, email addresses, or full real names without explicit user opt-in.
  - Display citizen reports anonymously or with masked initials (e.g., `"Reported by Citizen A.K."`).
- **Location Precision vs. Privacy**:
  - Public issues display the location of the civic hazard.
  - User profile screens must never reveal private citizen home locations or movement history.
- **EXIF Metadata Scrubbing**:
  - Strip camera device serial numbers, personal metadata, and embedded EXIF data during the media upload pipeline before storing publicly accessible image URLs.

---

## 4. Rate Limiting & Abuse Prevention

To protect municipal operations from spam and denial of service:

1. **OTP Endpoints**:
   - Rate limit OTP generation requests by phone number and IP address (e.g., maximum 3 attempts per 10 minutes) to prevent SMS toll fraud.
2. **Issue Creation Throttle**:
   - Enforce cooldown periods between issue submissions per device/user to block scripted flood reporting.
3. **Upvoting / Support Throttling**:
   - Limit support toggles to one vote per user per issue; prevent rapid vote manipulation.

---

## 5. Media & File Upload Security (Implemented in Task 12)

### 5.1 Defense-in-Depth File Validation
The backend enforces comprehensive, multi-layer validation before accepting any uploaded image:
- **Magic-Byte Inspection**: Server inspects the actual binary stream signature (magic bytes) to verify format validity before processing. JPEG (`FF D8 FF`), PNG (`89 50 4E 47 0D 0A 1A 0A`), and WebP (`RIFF`...`WEBP`) are verified. Spoofed files (e.g., shell scripts or HTML files masquerading with image MIME headers) are rejected immediately (`400 Bad Request`).
- **MIME & Extension Enforcement**: Only `image/jpeg`, `image/png`, and `image/webp` are permitted. File extensions, when present, must match the declared MIME type.
- **Strict File Size Limits**: Enforced at the Spring Boot multipart layer (10 MB per file, 15 MB request) and configurable via `nagrivic.media.max-file-size-mb` (default: 10 MB). Oversized uploads are rejected prior to persistence.
- **No Script/Executable Uploads**: Executable formats, SVGs (which may carry embedded JavaScript), PDFs, and documents are strictly prohibited.

### 5.2 Path Traversal & Storage Security
- **Server-Controlled Storage Keys**: Clients are never permitted to provide or influence storage paths. Keys are strictly generated on the server using UUIDs: `issues/{issueId}/{uuid}.{ext}`.
- **Path Traversal Defense**: The storage service normalizes paths and validates that the resolved storage path remains strictly inside the designated root directory, blocking `../` directory escape attacks.
- **Private Storage by Default**: No public filesystem directories are exposed via Spring static resource handlers (e.g. no `GET /uploads/**`). Uploaded files cannot be accessed by guessing filenames.
- **Information Hiding**: Internal storage keys, server directories, raw bytes, and storage infrastructure details are never returned in public API responses.

### 5.3 Media Ownership Authorization
- **Reporter Source of Truth**: Only the authenticated issue creator (`issue.reporter`) is authorized to attach media.
- **Cross-User Tampering Blocked**: Upload attempts to issues owned by other citizens are rejected with `403 Forbidden`.
- **Inactive Account Prevention**: Suspended/inactive citizens cannot attach media (`403 Forbidden`).

### 5.4 Future Enhancements
- **Pre-signed URLs / Direct-to-Storage**: Migration to cloud object storage (S3/Cloudflare R2/MinIO) using short-lived pre-signed upload and download URLs.
- **EXIF Stripping**: Automated removal of GPS, camera serials, and privacy metadata upon upload.
- **Asynchronous Malware/Virus Scanning**: ClamAV or cloud virus scanners for asynchronous payload scanning.

---

## 6. Issue Support Security (Implemented in Task 13)

### 6.1 Supporter Identity Integrity
- **JWT-Derived Identity**: The supporter identity is derived strictly from the verified JWT principal via `CurrentUserService`. No client-supplied `userId` or `supporterId` is accepted.
- **Active Account Requirement**: Suspended or inactive citizens cannot endorse issues (`403 Forbidden`).

### 6.2 Duplicate Support & Concurrency Defense
- **Database Unique Constraint**: `CONSTRAINT uq_supports_issue_user UNIQUE (issue_id, user_id)` guarantees at the database engine level that no citizen can endorse an issue more than once.
- **Race Condition Handling**: Simultaneous/concurrent requests that bypass application checks trigger a database unique constraint violation, which is intercepted and mapped to `409 Conflict` (`CONFLICT`).

### 6.3 Authorization on Support Removal
- **Ownership of Endorsement**: When invoking `DELETE /api/issues/{issueId}/support`, the server deletes strictly the authenticated citizen's support record. A citizen cannot remove another citizen's endorsement.

### 6.4 Privacy & Identity Protection
- **No Supporter Enumeration**: Public issue endpoints expose only the aggregate `supportCount` and the requester's own boolean `supportedByCurrentUser`. Individual supporter profiles, names, and phone numbers are never returned in public API payloads.

---

## 7. Issue Comments Security (Implemented in Task 14)

### 7.1 Plain-Text Invariant & Injection Protection
- **Untrusted Plain Text**: Comments are treated strictly as plain text. The backend never evaluates, parses, or stores user comments as trusted HTML or Markdown markup, preventing stored XSS attacks.
- **Length Boundaries**: Content is strictly bounded between 1 non-whitespace character and 1000 characters. Whitespace-only submissions and empty strings are rejected (`400 Bad Request`).

### 7.2 Author Identity & Ownership
- **JWT-Derived Author**: The comment author is derived strictly from the validated Bearer JWT token via `CurrentUserService`. Clients cannot supply `userId` or `authorId`.
- **Inactive Account Protection**: Suspended or inactive citizens cannot submit or delete comments (`403 Forbidden`).

### 7.3 Soft Deletion & Data Privacy
- **Audit Retention**: Comment deletions are recorded via `deleted_at` timestamps, maintaining historical records for administrative accountability and municipal grievance auditing.
- **Public Masking**: Soft-deleted comments return `content: "[Comment deleted]"` and `author.displayName: "Citizen"`. The original text is never exposed through the public API.
- **Author-Only Deletion**: Only the author of a comment can delete it. Attempting to delete another citizen's comment is rejected with `403 Forbidden`.

---

### 8. Audit Logging & Administrative Accountability (Implemented in Task 17)

To maintain public trust and civic integrity:
- **Audit Trails**: All status transitions (including initial issue creation) record an immutable row in `status_history`:
  - `id`: Globally unique identifier.
  - `issue_id`: The target issue.
  - `from_status` and `to_status`: State transition record (`NULL` for initial creation).
  - `changed_by_user_id`: The actor who performed the transition (`ON DELETE SET NULL`).
  - `reason`: Optional or mandatory transition explanation.
  - `created_at`: Point-in-time immutable UTC timestamp.
- **Immutable History**: Issue activity logs are append-only; update and delete operations are prohibited.

---

## 9. Duplicate Detection & Linking Security (Tasks 15 & 16)

### 9.1 Citizen Duplicate-Check Protection
- **Authenticated Identity Required**: `POST /api/issues/check-duplicates` enforces valid Bearer JWT authentication (`401 Unauthorized` on missing/invalid token).
- **Active Account Enforcement**: Inactive or suspended citizens are blocked from checking duplicates (`403 Forbidden`).
- **Data Minimization in Candidate Feeds**: Duplicate candidate responses expose strictly public issue metadata (issue ID, title, category, status, distance in meters, and support count). Citizen reporter phone numbers, emails, passwords, JWTs, and internal storage paths are completely omitted.
- **Side-Effect Free**: The duplicate-check operation is strictly read-only and idempotent with respect to persistent issue records. Clients cannot abuse it to trigger side effects.

### 9.2 Privileged Duplicate Merging / Linking
- **Privileged Action**: Linking a reported issue as a duplicate of another issue (`POST /api/issues/{issueId}/duplicate`) is a privileged administrative operation.
- **Ordinary Citizen Prohibition**: Ordinary citizens (`ROLE_CITIZEN`) are strictly prohibited from linking or merging issues (`403 Forbidden`). Only municipal officers (`ROLE_OFFICER`) or administrators (`ROLE_ADMIN`) have permission.
- **Server-Derived Identity**: The server extracts actor identity exclusively from the validated JWT claims. Client-supplied `userId`, `adminId`, or `reporterId` headers or payload fields are ignored.
- **Integrity & Anti-Tampering Constraints**:
  - Self-duplicate is rejected (`chk_issues_not_self_duplicate`).
  - Circular relationships are mathematically prevented at the service hierarchy traversal layer.
  - Deleting an issue never cascades to delete linked reports (`ON DELETE SET NULL`).

---

## 10. Issue Status & Resolution Workflow Security (Task 17)

### 10.1 Role-Based Operational Transitions
- **Restricted Access**: `POST /api/issues/{issueId}/status` requires `ROLE_OFFICER` or `ROLE_ADMIN`.
- **Citizen Tampering Prevention**: Ordinary citizens attempting to invoke operational status changes receive `403 Forbidden`.
- **State Machine Enforcement**: Transitions must strictly obey the state machine graph. Illegal jumps (e.g. `REPORTED -> RESOLVED` without inspection) return `400 Bad Request` (`INVALID_STATUS_TRANSITION`).

### 10.2 Citizen Resolution Verification Authorization
- **Reporter Source of Truth**: Only the original reporting user (`issue.reporter`) can verify resolution via `POST /api/issues/{issueId}/verify-resolution`.
- **Unauthorized Third Parties**: Any other user (including other citizens or officers without reporter ownership) receives `403 Forbidden`.
- **Precondition Check**: Verification is only permitted when the issue is in `RESOLVED` status.
- **Validation Guard**: If the citizen indicates the issue was not fixed (`fixed: false`), a non-empty `reason` is strictly mandatory. Blank or missing reasons return `400 Bad Request`.

### 10.3 Public Transparency & Privacy
- **Public Audit Log**: `GET /api/issues/{issueId}/status-history` is accessible to the public without requiring authentication.
- **Zero PII Exposure**: The actor summary (`changedBy`) exposes only safe public fields (`id`, `name`). Phone numbers, emails, passwords, and security tokens are strictly excluded.

---

---

## 12. Issue Civic Responsibility Enrichment Security (Task 19)

### 12.1 Server-Controlled Responsibility Assignment
- **Zero Client Trust**: Civic responsibility is server-resolved metadata. The client must NEVER be allowed to choose or override `civicBodyId`, `cityId`, `wardId`, or `departmentId`.
- **Spoofing Defense**: Even if an attacker explicitly submits `civicBodyId`, `cityId`, `wardId`, or `departmentId` in `POST /api/issues`, `CreateIssueRequest` strictly does not accept them, and the server resolves responsibility exclusively using:
  $$\text{Issue Location} + \text{Issue Category} + \text{Server-Side Civic Geography / Mapping Rules}$$
- **Automated Spoofing Verification**: Verified through automated tests (`testClientSpoofingDefense`) asserting that arbitrary client UUIDs are rejected/ignored.

### 12.2 Graceful Unresolved Non-Blocking Behavior
- **"Unresolved Does Not Mean Invalid"**: Missing ward boundaries or unmapped departments never destroy an issue or cause 500 errors. The issue is persisted normally with `responsibility_status = 'UNRESOLVED'`.
- **Deterministic Precedence**: The server resolves departments unambiguously. Conflicting mappings without deterministic precedence mark the issue as `UNRESOLVED` rather than guessing.

### 12.3 Data Minimization in Public Responses
- Public issue responses expose `civicResponsibility` using safe DTO records (`CivicResponsibilityDto`), exposing only `{ id, name }` pairs for administrative bodies.
- Raw PostGIS geometry, internal SQL audit trails, employee personal phone numbers, and sensitive internal comments are strictly barred from public serialization.

---

## 13. Issue Priority & Civic Impact Security (Task 20)

### 13.1 Server-Controlled Priority Calculation & Anti-Spoofing
- **Zero Client Trust on Priority**:
  - The composite score (0-100), categorical priority level (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), and component score breakdowns (`severityScore`, `impactScore`, `safetyScore`, `ageScore`, `supportScore`) are computed strictly server-side.
  - Clients cannot submit or alter priority scores or levels. If a client injects fields such as `priorityLevel: "CRITICAL"` or `score: 99` into `POST /api/issues`, the server's Jackson deserializer ignores unrecognized fields, and the service strictly computes authoritative scores.
  - Advisory Signal: `CreateIssueRequest` accepts an optional `severity` enum (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) as an advisory signal only.
- **Automated Spoofing Verification**: Verified through automated integration test `testClientSpoofingDefense` and live API verification script.

### 13.2 Anti-Brigading: Support Cap & Hazard Primacy
- **Popularity Brigading Defense**:
  - Upvoting or social media campaigns must not manipulate civic queue triage. Support contribution is mathematically capped at a maximum of 10 points out of 100.
- **Physical Hazard Primacy**:
  - Severe physical hazards (e.g. exposed live electric cables, arterial sinkholes) contribute up to 55 points independently (`severity` 30 + `safety_impact` 25).
  - High hazards naturally fall into HIGH or CRITICAL priority bands even with 0 public supports.
  - Conversely, low-hazard aesthetic defects can never surpass 24 points without severity, preventing them from displacing urgent hazards.

### 13.3 Idempotency & Canonical Integrity
- **One Priority Record Per Canonical Issue**: Enforced by a `UNIQUE` database constraint on `issue_id` in `issue_priorities`.
- **Duplicate Protection**: Recalculating priority on a duplicate issue redirects calculation to the canonical issue, ensuring duplicate reports do not create duplicate or skewed priority entries.
- **Range & Consistency Database Constraints**: Every score component is checked at the database level (`score BETWEEN 0 AND 100`, component bounds, and valid enum values).

---

## 15. Moderation Security, Privacy, and Abuse Prevention

### 15.1 Political Neutrality Principle
- Moderation rules strictly separate **content safety** from **political expression or factual verification**.
- Disagreements with civic policy, criticism of municipal authorities, or complaints regarding public infrastructure failures are explicitly NOT treated as abuse or spam.
- Moderation is constrained to defined safety categories: `SPAM`, `ABUSIVE_OR_HARASSING`, `HATEFUL_CONTENT`, `SEXUAL_OR_EXPLICIT`, `PERSONAL_INFORMATION`, `MISLEADING_OR_MANIPULATIVE`, `DUPLICATE_CONTENT`, `IRRELEVANT`, and `OTHER`.

### 15.2 Privacy & Information Segregation
- **Operational Audit vs. Public Timeline**: Moderation reports and moderator actions are stored in internal tables (`moderation_reports` and `moderation_actions`) and are never exposed via the public Issue Activity timeline or public issue APIs.
- **Zero PII in Responses**: Reporter and moderator phone numbers, emails, passwords, and internal investigation notes are never leaked through any client response.
- **Input Sanitization**: User-supplied report descriptions are stripped of all executable markup and HTML tags prior to persistence.

### 15.3 Soft Moderation and Audit Immutability
- Offending content is soft-hidden (`HIDDEN`), never permanently deleted.
- Soft-hidden content is excluded from public search and feed endpoints, and detail requests return HTTP 404.
- All moderator actions (`HIDE_CONTENT`, `RESTORE_CONTENT`, `RESTRICT_USER`, etc.) create an append-only, immutable audit record with timestamp, moderator identity, and resolution rationale.

### 15.4 User Restrictions & Contributing Lockout
- When a user is restricted (`RESTRICTED`), they are locked out from active contribution (creating issues, comments, support, or reports) returning HTTP 403 Forbidden.
- Read-only public access to civic issues, maps, and categories remains available.

### 15.5 Application-Level Rate Limiting
- High-risk endpoints are protected by sliding-window rate limiting (`RateLimiter`).
- Breaches result in HTTP 429 Too Many Requests accompanied by the standard `Retry-After` HTTP header.

---

## 16. Notifications Security, Privacy, and Idempotency (Task 23)

### 16.1 Strict Ownership & Authorization
- **JWT Identity Ownership**: Access to notification endpoints (`GET /api/notifications`, `GET /api/notifications/unread-count`, `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/read-all`, `GET/PATCH /api/notifications/preferences`) strictly enforces ownership based on the authenticated user's JWT principal.
- **Cross-User Protection**: Attempting to view or mark read another user's notifications returns `403 Forbidden`. The API does not accept `userId` as a request body parameter to select target users.

### 16.2 Zero Exposure of Private Data & Supporter Anonymity
- **No PII Leakage**: Notification titles, bodies, and metadata are generated server-side using safe, neutral language. PII (phone numbers, email addresses, exact private addresses, moderation notes) is strictly excluded.
- **Supporter Anonymity**: Supporter user IDs are queried internally for notification fan-out but are never exposed in public API responses or notification payload DTOs.

### 16.3 Idempotency & Duplicate Suppression
- **Deterministic Event Keys**: Important notifications enforce an idempotency key (`event_key`, indexed per user) to prevent duplicate alerts caused by network retries, repeated service invocations, or transaction retries.



