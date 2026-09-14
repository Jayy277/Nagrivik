# Resolution Evidence & Verifiable Issue Resolution Domain (`authorities / resolution-evidence`)

## 1. Domain Purpose & Philosophy

In municipal governance, declaring a civic problem "resolved" without concrete, inspectable verification undermines citizen trust. Historically, tickets are closed administratively while physical defects remain untouched on the ground.

The **Resolution Evidence Domain** establishes an auditable, verifiable evidence trail for civic issues. When authorized municipal officers work on or conclude repairs, they can attach verifiable proof—such as completion photos, before/after comparative media, and operational completion notes.

### Core Architectural Principles

1. **Independent Citizen Verification**:
   - Authority resolution evidence represents the municipal team's operational claim of completion.
   - Attaching resolution evidence **does NOT** mark an issue as `CITIZEN_VERIFIED`, nor does it bypass citizen verification.
   - The original reporting citizen retains the independent prerogative to inspect the site and confirm (`CITIZEN_VERIFIED`) or contest (`NOT_FIXED`) the resolution.
2. **Strict Authority Jurisdictional Scoping**:
   - Only authenticated officers (`ROLE_OFFICER` and `ROLE_ADMIN`) with server-enforced civic responsibility over the issue's geography (ward, department, or civic body) can attach resolution evidence.
   - Direct-access attacks (IDOR) via UUID manipulation are rejected with `403 Forbidden`.
3. **State Machine Integrity**:
   - Evidence can only be attached to issues that are actively being remediated (`IN_PROGRESS`) or marked completed (`RESOLVED`).
   - Early lifecycle states (`REPORTED`, `VERIFIED`, `ACKNOWLEDGED`) and post-verification states (`CITIZEN_VERIFIED`, `NOT_FIXED`) reject evidence upload with `400 Bad Request`.
4. **Binary / Database Separation & Storage Hygiene**:
   - Raw binary files are **never** stored in PostgreSQL. Media binaries are stored via `MediaStorageService` using secure, non-guessable, server-generated storage keys (`resolution-evidence/{issueId}/{uuid}.{ext}`).
   - PostgreSQL stores only structured relational metadata (`resolution_evidence` table).
   - In accordance with architecture guidelines, existing local disk / deferred storage infrastructure is reused without premature S3 / Task 49 implementation.
5. **Privacy & Public Safety**:
   - Resolution evidence is viewable by the public to foster transparency.
   - Evidence metadata DTOs expose zero PII (no uploader phone numbers, email addresses, or internal officer IDs).
   - EXIF/metadata stripping and magic-byte mime inspection are enforced prior to storage.
6. **Append-Only Activity Stream Audit**:
   - Every evidence addition creates an immutable audit event (`RESOLUTION_EVIDENCE_ADDED`) on the public issue activity timeline.

---

## 2. Evidence Types

| Evidence Type | File Required | Note Required | Description & Use Case |
|---|:---:|:---:|---|
| `COMPLETION_PHOTO` | Yes (Image &le; 10MB) | Optional (max 1000 chars) | Photo taken by field crews showing the completed repair (e.g., filled pothole, repaired street lamp). |
| `COMPLETION_NOTE` | No (File disallowed) | Yes (1–1000 chars) | Operational text report detailing materials used, contractor notes, or asphalt curing timelines. |
| `BEFORE_AFTER_PHOTO` | Yes (Image &le; 10MB) | Optional (max 1000 chars) | Comparative visual evidence illustrating the defect prior to and following municipal intervention. |

---

## 3. Database Schema (`resolution_evidence`)

Flyway migration `V29__create_resolution_evidence_table.sql`:

```sql
CREATE TABLE resolution_evidence (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    uploaded_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    evidence_type VARCHAR(32) NOT NULL,
    storage_key VARCHAR(512),
    original_filename VARCHAR(255),
    content_type VARCHAR(100),
    file_size_bytes BIGINT,
    note VARCHAR(1000),
    captured_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_resolution_evidence_type CHECK (
        evidence_type IN ('COMPLETION_PHOTO', 'COMPLETION_NOTE', 'BEFORE_AFTER_PHOTO')
    ),
    CONSTRAINT chk_resolution_evidence_file_or_note CHECK (
        storage_key IS NOT NULL OR note IS NOT NULL
    )
);

CREATE INDEX idx_resolution_evidence_issue ON resolution_evidence(issue_id);
CREATE INDEX idx_resolution_evidence_uploaded_by ON resolution_evidence(uploaded_by);
CREATE INDEX idx_resolution_evidence_type ON resolution_evidence(evidence_type);
```

---

## 4. API Endpoints

### 4.1 Authority Evidence Upload (Multipart)
- **Method / Path**: `POST /api/authority/issues/{issueId}/resolution-evidence`
- **Security**: Authenticated (`ROLE_OFFICER` or `ROLE_ADMIN`), within server-enforced authority scope.
- **Content-Type**: `multipart/form-data`
- **Form Parameters**:
  - `evidenceType` (string, mandatory): `COMPLETION_PHOTO`, `COMPLETION_NOTE`, or `BEFORE_AFTER_PHOTO`
  - `file` (binary, mandatory for photo types): JPEG, PNG, or WebP up to 10MB.
  - `note` (string, optional for photos, mandatory for `COMPLETION_NOTE`): Max 1000 characters.
  - `capturedAt` (ISO-8601 string, optional): Cannot exceed 15 minutes into the future.
- **Response**: `201 Created` with `ResolutionEvidenceResponse` DTO.

### 4.2 Public Resolution Evidence Inspection
- **Method / Path**: `GET /api/issues/{issueId}/resolution-evidence`
- **Security**: Public / Anonymous.
- **Response**: `200 OK` with JSON array of `ResolutionEvidenceResponse` DTOs ordered chronologically.

### 4.3 Safe Response DTO (`ResolutionEvidenceResponse`)
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "issueId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "evidenceType": "COMPLETION_PHOTO",
  "mediaUrl": "/api/media/resolution-evidence/9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d/photo.jpg",
  "originalFilename": "pothole_repaired.jpg",
  "fileSizeBytes": 1048576,
  "note": "Filled with bituminous cold mix and compacted.",
  "capturedAt": "2026-09-14T12:00:00Z",
  "createdAt": "2026-09-14T12:05:00Z"
}
```

---

## 5. Security & Privacy Guarantees

1. **IDOR Defense**: If an officer attempts to upload evidence for an issue outside their assigned municipal scope, `AuthorityScopeService` raises an `AccessDeniedException` mapped to HTTP `403 Forbidden`.
2. **File Sanitization**: Files are validated against magic bytes using `ImageFileValidator`. Executable files, SVG scripts, and mime-type spoofs are immediately rejected with HTTP `400 Bad Request`.
3. **No PII Leakage**: The `uploadedBy` user ID and officer identity are shielded from public endpoints. The UI attributes evidence neutrally to *"Civic Authority Evidence"*.
4. **Deferred Storage Architecture**: Reuses `MediaStorageService` disk backend without S3 or Task 49 cloud dependencies.
