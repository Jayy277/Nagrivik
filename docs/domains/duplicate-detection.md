# AI Duplicate Detection & Suggestion Domain

## 1. Domain Purpose & Philosophy

Multiple citizens frequently report the same physical civic defect (e.g., a pothole, broken streetlight, or garbage pile) using different words, varying levels of detail, or slightly different camera angles.

Task 46 introduces an **AI-assisted duplicate detection layer** to augment the existing deterministic PostGIS spatial detection foundation (Task 15/16).

```
                        Report Details (Title, Description, Category, Location)
                                                  │
                                                  ▼
                                     POST /api/issues/check-duplicates
                                                  │
                         ┌────────────────────────┴────────────────────────┐
                         ▼                                                 ▼
        1. Deterministic PostGIS Filter                   2. Canonical Filter
           (Same category, within 100m,                      (Primary issues only:
            GiST index, max 20 candidates)                    duplicate_of_issue_id IS NULL)
                         │
                         ▼
        3. DuplicateAiProvider Abstraction
           ├── If AI_DUPLICATE_ENABLED=false -> Deterministic match type (DISABLED)
           └── If AI_DUPLICATE_ENABLED=true  -> Semantic & Proximity Scoring
                   │
                   ├── Text Semantic Similarity (Token Jaccard & character n-grams)
                   ├── Spatial Proximity Decay (0 to 100m)
                   └── Category Verification (Exact match)
                   │
                   ▼
        4. Composite Score & Explainable Signals
           ├── Score: 0 - 100
           ├── Confidence: HIGH (85+), LIKELY (70-84), POSSIBLE (40-69), LOW (<40)
           └── Signals: ["Same category: Roads", "Location 24m away", "Similar description"]
```

---

## 2. Critical Safety & Anti-Hallucination Guardrails

> [!IMPORTANT]
> **AI is an Assistive Signal Only**:
> - AI **NEVER** automatically merges issues.
> - AI **NEVER** automatically deletes issues.
> - AI **NEVER** automatically links `duplicate_of_issue_id`.
> - AI **NEVER** modifies issue status or priority.
> - AI **NEVER** modifies civic responsibility.
> - AI **NEVER** makes moderation decisions.
> - AI **NEVER** overrides citizen agency.

Final duplicate linking remains a controlled, privileged server-side operation requiring explicit administrative or officer review.

---

## 3. Provider Abstraction & No Vendor Lock-In

The backend decouples duplicate detection from any specific AI vendor via the `DuplicateAiProvider` interface:

```java
public interface DuplicateAiProvider {
    DuplicateAiAnalysisResult analyze(DuplicateAiRequest request);
    String getProviderName();
    String getModelName();
    boolean isAvailable();
}
```

### Supported Providers
1. **`DisabledDuplicateAiProvider`**: Active when `AI_DUPLICATE_ENABLED=false` (the default). The application operates 100% normally without external network requests or AI latency.
2. **`LocalSemanticDuplicateAiProvider`**: In-memory, zero-dependency semantic text similarity provider utilizing token Jaccard similarity, character 3-gram overlaps, spatial decay weighting, and category verification. Always available for testing and self-contained deployments.
3. **Pluggable External Providers**: Extensible for external LLM or vector embedding APIs via environment configuration.

---

## 4. Scoring Model & Confidence Brackets

The composite similarity score is computed strictly on a **0–100** normalized integer scale:

$$\text{Score} = \left( 0.45 \times \text{TextSimilarity} + 0.35 \times \text{SpatialProximity} + 0.20 \times \text{CategoryMatch} \right) \times 100$$

### Confidence Levels
| Score Range | Confidence Level | Citizen Display Label | Admin Review Priority |
|---|---|---|---|
| **85 – 100** | `HIGH` | "High Match Nearby" | High Attention |
| **70 – 84** | `LIKELY` | "Likely Duplicate" | Normal Review |
| **40 – 69** | `POSSIBLE` | "Possible Related Report" | Low Attention |
| **0 – 39** | `LOW` | Not highlighted | Filtered by default |

---

## 5. Explainable Signals

AI duplicate recommendations must never present a black-box percentage. The system generates concise, human-understandable factors:
- `"Same category: Roads & Potholes"`
- `"Immediate proximity (<5m)"` or `"Location 24m away"`
- `"High semantic text similarity"`
- `"Similar problem description"`
- `"Similar issue title"`

Raw model prompts, chain-of-thought tokens, and internal model parameters are **never** exposed to clients.

---

## 6. Privacy & Zero-PII Guarantees

AI inputs are strictly sanitized to include only physical problem attributes:
- **Included**: Issue title, problem description, category name, approximate distance in meters.
- **Excluded**: Citizen full name, phone number, email address, reporter UUID, auth JWT, doorstep private address, supporter identities, moderation history.

Descriptions and titles are treated purely as **data**, preventing prompt injection attacks from altering server behavior.

---

## 7. Administrative Duplicate Review Workflow

Privileged reviewers (`ROLE_MODERATOR`, `ROLE_OFFICER`, `ROLE_ADMIN`) inspect pending suggestions at `/admin/duplicates`:

1. **Side-by-Side Inspection**: Compare reported issue and candidate primary issue side-by-side (titles, descriptions, categories, distances, and detected signals).
2. **Link as Duplicate**:
   - Executes `POST /api/admin/duplicates/suggestions/{id}/link`.
   - Reuses canonical duplicate linking (`DuplicateDetectionService.linkDuplicate`).
   - Normalizes to canonical root primary, reparents children, prevents cycles.
   - Marks suggestion status as `LINKED`.
   - Preserves all citizen comments, media, and supporters.
3. **Not a Duplicate (Dismiss)**:
   - Executes `POST /api/admin/duplicates/suggestions/{id}/dismiss`.
   - Accepts an optional reason (e.g., *"Inspected on-site: two distinct physical potholes"*).
   - Marks suggestion status as `DISMISSED` and records reviewer identity.
   - Prevents re-prompting for the same pair.
