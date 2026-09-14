# AI Image Understanding Domain

## 1. Domain Purpose & Philosophy

Citizen reports in Nagrivic are frequently accompanied by photos of civic defects (e.g., potholes, broken streetlights, overflowing waste bins, water leakages, blocked storm drains).

Task 47 introduces an **AI-assisted image understanding layer** to extract structured visual signals from citizen-submitted media. These visual observations assist civic authorities and moderators in quickly assessing the nature, urgency, and relevance of reports without requiring time-consuming manual triage.

```
                  Citizen Issue Media Upload
                              │
                              ▼
            POST /api/issues/{id}/media/{mediaId}/analyze
                              │
          ┌───────────────────┴───────────────────┐
          ▼                                       ▼
1. RBAC & IDOR Verification             2. Storage Retrieval
   (Issue owner, Moderator,                (Local / Future S3
    or Authority Officer)                   MediaStorageService)
          │                                       │
          └───────────────────┬───────────────────┘
                              ▼
                3. ImageUnderstandingProvider
                   ├── AI_IMAGE_ENABLED=false -> DisabledImageUnderstandingProvider (UNAVAILABLE)
                   └── AI_IMAGE_ENABLED=true  -> LocalHeuristicImageUnderstandingProvider
                              │
                              ├── Likely Civic Category & Confidence (0-100)
                              ├── Visual Problem Types (chips)
                              ├── Image Quality (GOOD, ACCEPTABLE, POOR)
                              ├── Civic Relevance (LIKELY_RELEVANT, UNCERTAIN, LIKELY_IRRELEVANT)
                              ├── Severity Signals (DEEP_POTHOLE, FLOODING, HAZARDS)
                              └── Safety Indicators (NONE, POSSIBLE, HIGH)
                              │
                              ▼
                4. Persisted In image_ai_analysis
                   (Advisory only — never alters issue category,
                    status, priority, or civic responsibility)
```

---

## 2. Critical Safety & Anti-Hallucination Guardrails

> [!IMPORTANT]
> **AI is an Assistive Signal Only**:
> - AI **NEVER** automatically rejects a citizen report.
> - AI **NEVER** automatically deletes or hides an issue.
> - AI **NEVER** changes issue category (`issue.category_id` remains user-selected).
> - AI **NEVER** modifies issue status (`status` remains under operational control).
> - AI **NEVER** modifies priority (`priority` remains governed by Task 20 rules).
> - AI **NEVER** modifies civic responsibility (`ward_id` and `department_id` remain authoritative).
> - AI **NEVER** performs facial recognition, person identification, or license plate tracking.
> - AI **NEVER** makes political or moderation judgments.

Human officials and server-controlled business rules remain authoritative at all times.

---

## 3. Privacy, Anti-Surveillance & Zero PII Architecture

Nagrivic adheres to strict privacy and anti-surveillance principles:
1. **Zero Citizen PII**:
   - The analysis request payload sent to providers contains only the image binary, media MIME type, file size, issue title, issue description, and category slug.
   - Citizen phone numbers, names, email addresses, JWT tokens, user IDs, and private address coordinates are **strictly excluded**.
2. **Anti-Surveillance**:
   - The system is explicitly configured not to perform biometric surveillance, facial recognition, or license plate extraction.
   - The data model includes `sensitiveVisualContentDetected` to alert moderators if non-civic or sensitive content is observed, but does not identify individuals.

---

## 4. Provider Abstraction & Pluggability

The service interacts with providers via the `ImageUnderstandingProvider` interface:

```java
public interface ImageUnderstandingProvider {
    ImageUnderstandingResult analyze(ImageUnderstandingRequest request);
    String getProviderName();
    String getModelName();
    boolean isAvailable();
}
```

Implementations include:
- `DisabledImageUnderstandingProvider`: Returns `UNAVAILABLE` when `nagrivic.ai.image.enabled=false`.
- `LocalHeuristicImageUnderstandingProvider`: High-speed, deterministic, local image inspection using binary format validation and semantic keyword mapping.
- Future LLM/Vision API Providers (Task 48+): Can plug in without database or controller refactoring.

---

## 5. REST API Endpoints

All endpoints enforce strict role-based access control and IDOR checks:

### 5.1 Trigger Image Analysis
- **`POST /api/issues/{issueId}/media/{mediaId}/analyze`**
- **Auth**: Authenticated (Reporter, Moderator, or Authority Officer).
- **Behavior**: Idempotent. Returns existing analysis if already completed; otherwise submits analysis asynchronously outside database transactions.

### 5.2 Retrieve Image Analysis
- **`GET /api/issues/{issueId}/media/{mediaId}/analysis`**
- **Auth**: Authenticated (Reporter, Moderator, or Authority Officer).
- **Behavior**: Returns `200 OK` with analysis results, or `404 Not Found` if no analysis exists.

---

## 6. Frontend Integration

Authority and administrative portals render visual observations via `ImageAiAnalysisCard`:
- Prominent **"Advisory Only"** badge.
- Likely category and confidence percentage.
- Detected visual problem chips (`POTHOLE`, `GARBAGE_PILE`, `DAMAGED_LIGHT_POLE`, etc.).
- Image quality indicator and usability warnings (`TOO_DARK`, `TOO_BLURRY`).
- Relevance rating (`LIKELY_RELEVANT`, `UNCERTAIN`, `LIKELY_IRRELEVANT`).
- Visual severity signals and safety concern indicators.
