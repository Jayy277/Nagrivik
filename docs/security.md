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
The platform defines four role tiers:

| Role | Permissions |
|---|---|
| `ROLE_CITIZEN` | Report issues, upload photos, upvote ("Support") issues, post comments, confirm resolution, submit moderation reports (`POST /api/moderation/reports`). |
| `ROLE_MODERATOR` | Access admin overview dashboard (`/admin`), moderation summary metrics, filterable moderation queue, inspect report targets safely without PII, execute moderation actions (`NO_ACTION`, `HIDE_CONTENT`, `RESTORE_CONTENT`, `REMOVE_COMMENT`), review/resolve/dismiss reports. |
| `ROLE_OFFICER` | View assigned department queue, update ticket progress, upload resolution proof photos, mark resolved; also authorized for operational moderation actions. |
| `ROLE_ADMIN` | Full moderation authority including user restrictions (`RESTRICT_USER`, `UNRESTRICT_USER`), reassign wards, manage authority users, view city-wide audit logs; exclusive authority to manage civic bodies, cities, wards, departments, and responsibility mappings. |

#### Privileged Moderation & Geography Boundaries
- Citizen Report Creation: `POST /api/moderation/reports` requires authenticated `ROLE_CITIZEN`, `ROLE_MODERATOR`, `ROLE_OFFICER`, or `ROLE_ADMIN`.
- Queue, Detail, Review, Resolve, Dismiss, Hide/Restore: `/api/moderation/**` and `/api/admin/**` require `hasAnyRole('MODERATOR', 'ADMIN', 'OFFICER')`.
- User Restriction Enforcement: `/api/moderation/users/{id}/restrict` and `/unrestrict` strictly require `hasAnyRole('ADMIN', 'OFFICER')`. Normal citizens and standard moderators cannot apply user restrictions.
- Civic Geography & Responsibility Management: `/api/admin/geography/**` strictly requires `hasRole('ADMIN')`. Normal citizens and moderators are rejected with `HTTP 403 Forbidden`. All mutations are protected by JPA optimistic locking (`@Version` $\rightarrow$ `HTTP 409 Conflict` on concurrent edits) and logged to append-only audit tables.
- Authority Operations & Jurisdictional Scoping (Task 43): `/api/authority/**` strictly requires `hasAnyRole('OFFICER', 'ADMIN')`. Normal citizens and moderators receive `HTTP 403 Forbidden`. Requests are protected by server-side scope enforcement (`AuthorityScopeService`), preventing direct-object access attacks (IDOR) on issues outside an officer's assigned ward/department. Unresolved issues cannot be operated on. Authorities are forbidden from setting citizen-verification states (`CITIZEN_VERIFIED`, `NOT_FIXED`). Transitions enforce optimistic concurrency control (`@Version` $\rightarrow$ `HTTP 409 Conflict`), mandatory resolution reasons, and zero-PII exposure.

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

---

## 17. Resolution Evidence & Media Storage Security (Task 44)

### 17.1 Authority Scope Enforcement (IDOR Protection)
- **Scoped Upload**: `POST /api/authority/issues/{issueId}/resolution-evidence` strictly validates that the authenticated authority user (`ROLE_OFFICER` or `ROLE_ADMIN`) possesses an active `AuthorityAssignmentEntity` matching the issue's civic responsibility.
- **IDOR Defense**: Any attempt to attach evidence to an issue outside the officer's jurisdiction returns `403 Forbidden`.
- **State Enforcement**: Evidence upload is permitted only when an issue is in `IN_PROGRESS` or `RESOLVED` status; any other status results in `400 Bad Request`.

### 17.2 Binary Storage Security & Mime Inspection
- **Magic Bytes Validation**: Uploaded media binaries are verified via `ImageFileValidator` by inspecting magic bytes rather than trusting client-declared `Content-Type`. Allowed types are strictly JPEG, PNG, and WebP.
- **File Size Caps**: Binary files are capped at 10MB; oversized files are rejected immediately.
- **Non-Guessable Storage Keys**: Files are written to server-generated UUID paths (`resolution-evidence/{issueId}/{uuid}.{ext}`). User-supplied filenames are sanitized and stored as metadata only.
- **No Binaries in PostgreSQL**: PostgreSQL stores only structured relational metadata in `resolution_evidence`. Raw binaries are handled via `MediaStorageService` on disk.

### 17.3 Public Transparency with Zero PII
- **Public Inspectability**: `GET /api/issues/{issueId}/resolution-evidence` is publicly queryable to ensure government transparency.
- **Zero PII**: The uploader's personal phone number, email address, and internal user ID are excluded from `ResolutionEvidenceResponse`. The UI presents evidence with neutral civic attribution (*"Civic Authority Evidence"*).
- **Citizen Independence**: Authority evidence is recorded as the municipality's claim of completion. It does not alter citizen verification rights or automatically transition issue states.

---

## 18. Public Civic Accountability Dashboard Security & Privacy (Task 45)

### 18.1 Unauthenticated Public Access with Safety Controls
- **PermitAll Access**: `GET /api/public/accountability` is explicitly whitelisted in `SecurityConfig` to ensure universal accessibility without authentication.
- **Read-Only Aggregation**: The dashboard endpoint performs read-only analytical aggregations across issues, categories, wards, and departments. It exposes no state-modifying operations.
- **Defensive Parameter Validation**: All query parameters (`cityId`, `wardId`, `categoryId`, `range`) are strictly validated against UUID syntax and an allowlist of valid range tokens (`7d`, `30d`, `90d`, `all`). Malformed inputs are rejected with `400 Bad Request`.

### 18.2 Absolute Zero PII Guarantee
- **No Individual Identifiers**: Aggregate payloads contain exclusively count metrics, percentages, and civic reference names. No citizen names, phone numbers, email addresses, user IDs, or reporter UUIDs are ever included in the response.
- **No Private Officer Identity**: Authority performance is grouped at the departmental level (`Water Supply`, `Roads & Buildings`, etc.). Individual authority officer identities, usernames, or internal IDs are strictly withheld.
- **No Exact Coordinate Leakage**: Spatial aggregations are partitioned strictly by authoritative administrative polygons (`ward_id`, `city_id`). Raw GPS points, street-level addresses, or private doorstep locations are never aggregated or leaked.

### 18.3 Canonical Issue Integrity & Moderation Filtering
- **Physical Defect Deduplication**: To prevent distorted civic reporting where multiple citizens flag the same physical defect, aggregate metrics strictly filter `duplicate_of_issue_id IS NULL`. Duplicate issues linked to a primary issue are excluded from total defect counts.
- **Moderation Compliance**: Content marked as hidden by moderators (`moderation_status = 'HIDDEN'`) is filtered out across all aggregate queries, ensuring abusive, spammy, or defamatory reports do not influence civic accountability metrics.

### 18.4 SQL Injection Defense & Aggregate Query Safety
- **Typed Parameter Binding**: All SQL and JPQL aggregate queries utilize typed parameterized queries via Spring Data JPA and EntityManager (`:cityId`, `:wardId`, `:categoryId`, `:since`). Dynamic string concatenation in SQL/JPQL is strictly prohibited.
- **Parameterized Groupings**: All dimensional breakdowns (status, priority, ward, category, department, aging) execute bounded queries with explicit result limits to eliminate denial-of-service via unbounded cardinality.

---

## 19. AI Duplicate Detection Security, Privacy & Safety (Task 46)

### 19.1 Zero PII Guarantee for AI Requests
- **Scrubbed Payloads**: Prompts and candidate comparisons receive strictly sanitized civic defect data (issue title, problem description, category name, approximate distance in meters).
- **Prohibited Data**: Citizen names, phone numbers, email addresses, reporter UUIDs, authentication JWTs, passwords, exact doorstep residential addresses, supporter identities, and moderation reporter details are strictly excluded from AI processing.

### 19.2 Anti-Hallucination & Non-Authoritative Safeguards
- **Advisory Role Only**: AI provides assistive similarity scores (0–100) and explainable signal strings. Under no circumstances does AI automatically link, merge, delete, hide, or alter an issue.
- **Human-in-the-Loop Verification**: Final duplicate linking requires explicit privileged review (`ROLE_OFFICER` or `ROLE_ADMIN`) using canonical tree flattening and cycle prevention rules.
- **Non-Destructive Operations**: Linking an issue preserves the complete original record, citizen authorship, media attachments, and comments.

### 19.3 Prompt Injection Defense & Data Sanitization
- **Data vs Instruction Separation**: Citizen titles and descriptions are treated purely as text payload data for lexical/semantic comparison. They are never concatenated directly into instruction prompts or interpreted as operational commands.
- **Structured Output Validation**: Model outputs are strictly mapped to strongly-typed DTOs (`score`, `confidence`, `signals`). Any malformed values, scores outside 0–100, or unknown candidate IDs are rejected by the server.

### 19.4 Transaction Boundary & Latency Isolation
- **Outside DB Transactions**: External AI provider calls and semantic analysis are executed outside database transactions, preventing database connection starvation or lock contention.
- **Configurable Timeouts & Fallback**: Calls are subject to a strict timeout (`nagrivic.ai.duplicates.timeout-ms`, default 3000ms). If the AI provider times out, fails, or is disabled, the system gracefully falls back to deterministic PostGIS candidates without impeding citizen reporting.

---

## 20. AI Image Understanding Privacy, Security & Anti-Surveillance Safeguards (Task 47)

### 20.1 Strict Anti-Surveillance Principles
- **No Facial Recognition**: The AI image pipeline is strictly prohibited from running face detection, face recognition, or biometric identification algorithms. No facial vectors or biometric hashes are generated or retained.
- **No Vehicle / License Plate Tracking**: The image understanding models extract purely civic problem classifications (e.g. `POTHOLE`, `GARBAGE_PILE`, `DAMAGED_LIGHT_POLE`); license plate recognition and vehicle registration identification are completely barred.
- **No Citizen Profiling**: Visual content is evaluated solely for civic infrastructure defects and physical hazard assessment. Images are never used to track, profile, or grade citizens.

### 20.2 Zero Citizen PII in Analysis Requests
- **Sanitized Payloads**: Image analysis requests transmit exclusively the media binary, MIME type, file size, issue title, issue description, and category slug.
- **Strictly Excluded**: Citizen names, phone numbers, email addresses, reporter UUIDs, JWT tokens, and private address coordinates are entirely omitted from AI requests.

### 20.3 Advisory-Only Non-Mutation Invariant
- **Non-Destructive Observations**: AI image analysis produces advisory visual signals (`likelyCategory`, `visualProblemTypes`, `imageQuality`, `relevance`, `visualSeveritySignals`, `safetyConcern`).
- **Zero Automatic Mutations**: AI **never** automatically rejects a citizen report, hides or deletes an issue, changes category (`issue.category_id`), changes status, changes priority, or alters assigned civic responsibility (`ward_id`, `department_id`).
- **Human Authority**: Human officials and server-controlled product rules remain authoritative at all times.

### 20.4 Isolated Execution & Fallback Resilience
- **Non-Blocking & Asynchronous**: Visual analysis runs outside database transactions in dedicated virtual threads with configurable timeouts (`nagrivic.ai.image.timeout-ms`).
- **Default Disabled**: AI image understanding is disabled by default (`nagrivic.ai.image.enabled: false`). When disabled, core issue reporting, media uploading, and operational triage function normally with zero dependency on AI.

---

## 21. AI Priority Assistance Privacy, Non-Escalation & Safety Safeguards (Task 48)

### 21.1 Advisory by Default & Non-Replacement Invariant
- **Authoritative Determinism**: The deterministic priority calculation from Task 20 remains the authoritative foundation of Nagrivic. AI cannot replace or override the deterministic priority engine.
- **Default Advisory Mode**: AI priority assistance operates in `ADVISORY` mode by default (`nagrivic.ai.priority.enabled: false`, `nagrivic.ai.priority.influence-mode: ADVISORY`). Recommendations are presented in the authority dashboard as assistive triage signals and do not alter the deterministic score in `issue_priorities`.
- **Untouchable Core Components**: Issue age score (0–10) and citizen support count score (0–10) are strictly deterministic and are never modified, scaled, or influenced by AI.

### 21.2 Anti-Critical Escalation Hard Ceiling
- **AI Alone Cannot Force Critical**: To protect municipal operations against model hallucinations or automated denial-of-service, AI recommendations alone can **never** escalate an issue into the `CRITICAL` band (score $\ge 75$).
- **Hard Ceiling Rule**: If the baseline deterministic score was $< 75$, any blended calculation is strictly capped at $74$ (`HIGH` priority maximum).
- **Critical Baseline Preservation**: If human reports and deterministic signals already placed an issue in `CRITICAL` ($\ge 75$), that critical status is preserved.

### 21.3 Bounded Blending Mathematics & Clamped Adjustments
- **Strict Bounded Deltas**: In optional `BLENDED` mode, AI adjustments are strictly clamped per component:
  - Severity: maximum $\pm 5$ points (within 0–30 bounds)
  - Public Impact: maximum $\pm 4$ points (within 0–25 bounds)
  - Safety Hazard: maximum $\pm 4$ points (within 0–25 bounds)
- **Cumulative Adjustment Cap**: The total net adjustment from all three components combined is strictly bounded to a maximum of $\pm 10$ points overall.
- **Confidence Gating**: Blending applies only when AI confidence reaches or exceeds the configured threshold (`nagrivic.ai.priority.confidence-threshold`, default 70%). Below threshold, AI delta is strictly 0.

### 21.4 Zero Citizen PII in Priority AI Requests
- **Sanitized Request Context**: Priority AI request payloads contain strictly non-personal civic defect data (issue UUID, sanitized title, description, category slug, and Task 47 structured visual signals).
- **Prohibited Data**: Citizen names, phone numbers, email addresses, reporter UUIDs, JWT authorization tokens, passwords, and private residence coordinates are strictly barred from priority AI processing.
- **Zero Media Re-Upload**: Rather than transmitting raw image files or streaming media to AI endpoints, the priority engine consumes only structured metadata (`visualProblemTypes`, `visualSeveritySignals`, `safetyConcern`) already verified and stored in `image_ai_analysis`.

### 21.5 Latency Isolation, Virtual Threads & Graceful Fallback
- **Virtual Thread Concurrency**: AI priority assessment executes on dedicated lightweight virtual threads (`Thread.ofVirtual().start(...)`), completely isolated from the request thread.
- **Outside Database Transactions**: AI model evaluation runs entirely outside database transactions, preventing database connection starvation or lock contention.
- **Strict Timeout**: Network requests are bounded by `nagrivic.ai.priority.timeout-ms` (default 4000ms). If the model times out or encounters errors, the system records `FAILED` or `UNAVAILABLE` without degrading platform performance.

### 21.6 Role-Based Access Control (RBAC) & Immutable Audit Logging
- **Inspection RBAC**: AI priority recommendations (`GET /api/issues/{id}/priority/ai-recommendation`) can be viewed by the issue reporter or authorized municipal staff (`OFFICER`, `AUTHORITY`, `MODERATOR`, `ADMIN`). Other citizens are forbidden (`403 Forbidden`).
- **Trigger RBAC**: On-demand priority assessment (`POST /api/issues/{id}/priority/ai-assess`) is strictly restricted to privileged municipal staff (`OFFICER`, `AUTHORITY`, `MODERATOR`, `ADMIN`). Ordinary citizens cannot trigger assessment (`403 Forbidden`).
- **Audit Trail**: Every completed AI assessment is logged to `issue_activity` with activity type `PRIORITY_AI_ASSESSED`, recording the acting officer ID, model version, suggested scores, and confidence.

---

## 22. Object Storage Foundation & S3 Security Safeguards (Task 49)

### 22.1 Provider Independence & Sandboxed Local Storage
- **Isolated Abstraction**: Domain logic interacts strictly with `ObjectStorageService` / `MediaStorageService`. Domain code never imports `software.amazon.awssdk` directly.
- **Local Storage Path Traversal Defense**: `LocalMediaStorageService` canonicalizes and validates every storage key, strictly preventing path traversal attacks (`../`).
- **Default Local**: Dev and CI use `STORAGE_PROVIDER=local` by default, requiring zero cloud credentials or external cloud infrastructure.

### 22.2 Private Buckets & Controlled Streaming
- **Private Buckets**: S3 buckets are private by default; public read/write ACLs are strictly forbidden.
- **Controlled Streaming**: Public downloads (`/api/issues/{id}/media/{mediaId}` and `/api/media/**`) enforce database verification before streaming bytes from storage.
- **Defensive Headers**: Streams enforce `X-Content-Type-Options: nosniff`, `Content-Disposition: inline`, and strict MIME types (`image/jpeg`, `image/png`, `image/webp`).

---

## 23. Push Notification & FCM Security, Token Privacy, and Lock-Screen Safeguards (Task 50)

### 23.1 Server-Derived Identity & Device Ownership
- **Strict JWT Identity**: Device registration (`POST /api/push/devices`) strictly derives user identity from server-verified JWT claims. Request payloads cannot supply or spoof `userId`.
- **Token Ownership Enforcement**: A device registration can be deactivated (`DELETE /api/push/devices/{id}`) only by its authenticated owner. Unauthorized deactivation attempts trigger `403 Forbidden`.
- **Account Switching Hygiene**: If a device token is re-registered by a new user on the same phone, ownership is safely re-assigned to the new user, preventing cross-user notification leakage.

### 23.2 Lock-Screen Privacy & Minimal Payloads
- **Lock-Screen Privacy**: Push notification titles and bodies contain only high-level, neutral civic updates (e.g. "Your reported issue is in progress").
- **Prohibited Data**: Citizen names, phone numbers, email addresses, reporter identities, private residential addresses, moderation notes, and internal AI scores are strictly barred from push payloads.
- **Controlled Deep Linking**: Deep links in push payloads are strictly restricted to internal routes (`nagrivicapp://issue/{id}` or `nagrivicapp://notifications`). External web links (`https://`), javascript URIs, and arbitrary schemes are rejected.

### 23.3 Transaction Decoupling & Network Isolation
- **Post-Commit Dispatch**: Push notifications are dispatched strictly `AFTER_COMMIT` via `NotificationCreatedEvent`. Network latency, Firebase outages, or FCM timeouts never hold database locks or roll back domain transactions.
- **Token Hygiene**: If FCM reports `UNREGISTERED` or `INVALID_ARGUMENT`, the device token is automatically marked inactive (`is_active = false`), preventing infinite retry loops.

### 23.4 Credential Isolation & Safe Defaults
- **Server-Side Credentials**: Firebase service-account private keys are managed strictly on the server via environment variables or secure secret stores (`FIREBASE_PRIVATE_KEY`, `FIREBASE_CLIENT_EMAIL`, `FIREBASE_PROJECT_ID`). No service account JSON is committed to Git.
- **Offline Development**: When `FCM_ENABLED=false` (the default), `NoOpPushNotificationProvider` skips push delivery cleanly without network calls.



