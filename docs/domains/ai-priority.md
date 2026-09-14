# AI Priority Assistance Domain

## 1. Domain Purpose & Philosophy

The Nagrivic civic reporting platform processes thousands of civic defects across municipal wards. To ensure rapid response to urgent civic hazards (e.g., deep road craters, live exposed electrical wires, contaminated drinking water, overflowing sewage), municipal officers need clear, explainable signals when triaging reports.

Task 48 introduces **AI Priority Assistance**, an assistive recommendation layer that analyzes civic issue descriptions, categories, and Task 47 visual observations to provide bounded advisory recommendations.

```
                  Citizen Issue Report & Media
                               │
                               ▼
               Task 20: Deterministic Priority Engine
                     (Authoritative Baseline)
                               │
                ┌──────────────┴──────────────┐
                ▼                             ▼
       Deterministic Score            Task 48: AI Assessment
          (0 - 100)                      (Advisory Signal)
                │                             │
                │        ┌────────────────────┴────────────────────┐
                │        ▼                                         ▼
                │   ADVISORY Mode (Default)                   BLENDED Mode (Optional)
                │   Display recommendations                   Calculate bounded delta:
                │   in Authority UI for human                  - Severity: max ±5 pts
                │   review & operational triage               - Impact:   max ±4 pts
                │                                             - Safety:   max ±4 pts
                │                                             - Total:    max ±10 pts
                │                                             Anti-CRITICAL Ceiling:
                │                                             Baseline < 75 -> Blended ≤ 74
                │                                                          │
                └─────────────────────────────┬────────────────────────────┘
                                              ▼
                                Final Operational Priority
```

---

## 2. Core Non-Negotiable Rules & Invariants

> [!IMPORTANT]
> **Authoritative Foundation**:
> 1. The deterministic priority system from Task 20 remains the authoritative foundation.
> 2. AI is **ADVISORY ONLY** by default (`AI_PRIORITY_ENABLED=false`, `influence-mode=ADVISORY`).
> 3. AI must **NEVER** replace deterministic priority calculations.
> 4. **ANTI-CRITICAL SAFEGUARD**: AI alone can **never** force an issue into `CRITICAL` (score ≥ 75). If baseline deterministic score was < 75, the blended score is strictly capped at 74 (`HIGH` maximum).
> 5. **BOUNDED ADJUSTMENT CAP**: In optional `BLENDED` mode, total AI adjustment is strictly capped at maximum `±10` points overall (`±5` severity, `±4` impact, `±4` safety).
> 6. **IMMUTABLE DETERMINISTIC FACTORS**: Issue age (0–10) and citizen support count (0–10) are 100% deterministic and are **NEVER** altered or scaled by AI.
> 7. **ZERO PII**: AI requests contain zero citizen PII (no phone, email, reporter ID, JWT, or private residence address).
> 8. **TASK 47 SYNERGY**: Priority AI consumes structured visual observations (`visualProblemTypes`, `visualSeveritySignals`, `safetyConcern`) directly from the database without re-uploading media.

---

## 3. Component Bounds & Mathematical Blending Formula

The priority system maintains strict component scoring bounds:

| Component | Scoring Range | Deterministic Baseline Bands | Max AI Blended Delta |
| :--- | :--- | :--- | :--- |
| **Severity** | 0 – 30 | LOW (5), MEDIUM (15), HIGH (25), CRITICAL (30) | `±5` points |
| **Impact** | 0 – 25 | LOW (5), MEDIUM (15), HIGH (25) | `±4` points |
| **Safety** | 0 – 25 | NONE (0), LOW (5), MEDIUM (15), HIGH (20), CRITICAL (25) | `±4` points |
| **Age** | 0 – 10 | Computed based on days elapsed | **0 (Untouched)** |
| **Support** | 0 – 10 | Computed based on citizen support count | **0 (Untouched)** |
| **Total** | 0 – 100 | LOW (0–24), MEDIUM (25–49), HIGH (50–74), CRITICAL (75–100) | **Max ±10 points** |

### Mathematical Blending Formula (When in BLENDED Mode)

When `nagrivic.ai.priority.influence-mode=BLENDED` and AI confidence $\ge$ `confidence-threshold` (default 70%):

$$\Delta_{\text{sev}} = \text{clamp}\left(\text{round}\left((S_{\text{ai}} - S_{\text{det}}) \times \frac{C_{\text{ai}}}{100}\right), -5, +5\right)$$

$$\Delta_{\text{imp}} = \text{clamp}\left(\text{round}\left((I_{\text{ai}} - I_{\text{det}}) \times \frac{C_{\text{ai}}}{100}\right), -4, +4\right)$$

$$\Delta_{\text{safe}} = \text{clamp}\left(\text{round}\left((H_{\text{ai}} - H_{\text{det}}) \times \frac{C_{\text{ai}}}{100}\right), -4, +4\right)$$

$$\Delta_{\text{total}} = \text{clamp}\left(\Delta_{\text{sev}} + \Delta_{\text{imp}} + \Delta_{\text{safe}}, -10, +10\right)$$

$$T_{\text{candidate}} = T_{\text{deterministic}} + \Delta_{\text{total}}$$

### Anti-Critical Safeguard Enforcement

$$\text{Final Score} = \begin{cases} 
\min(74, T_{\text{candidate}}) & \text{if } T_{\text{deterministic}} < 75 \\
\text{clamp}(T_{\text{candidate}}, 75, 100) & \text{if } T_{\text{deterministic}} \ge 75 
\end{cases}$$

---

## 4. Integration with Task 47 Image Understanding

Rather than re-analyzing images or streaming binary data to external models, the priority engine leverages structured visual signals already extracted by Task 47 from `image_ai_analysis`:
- **`visualProblemTypes`**: e.g., `POTHOLE`, `ROAD_CAVE_IN`, `OPEN_MANHOLE`, `SEWAGE_OVERFLOW`
- **`visualSeveritySignals`**: e.g., `DEEP_POTHOLE`, `LARGE_ROAD_OBSTRUCTION`, `FLOODING`
- **`safetyConcern`**: e.g., `HIGH` or `POSSIBLE`

These observations are synthesized alongside category semantics and description keywords to yield explainable factors (e.g., `"Task 47 visual signals: [DEEP_POTHOLE, ROAD_CAVE_IN]"`).

---

## 5. Database Schema & Architecture

Table: `issue_ai_priorities` (`V32__create_issue_ai_priorities_table.sql`)

```sql
CREATE TABLE issue_ai_priorities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    provider VARCHAR(64) NOT NULL,
    model VARCHAR(64) NOT NULL,
    model_version VARCHAR(32) NOT NULL,
    calculation_version VARCHAR(32) NOT NULL DEFAULT 'v1',
    status VARCHAR(32) NOT NULL,
    suggested_severity INT CHECK (suggested_severity IS NULL OR (suggested_severity >= 0 AND suggested_severity <= 30)),
    suggested_impact INT CHECK (suggested_impact IS NULL OR (suggested_impact >= 0 AND suggested_impact <= 25)),
    suggested_safety INT CHECK (suggested_safety IS NULL OR (suggested_safety >= 0 AND suggested_safety <= 25)),
    severity_confidence INT CHECK (severity_confidence IS NULL OR (severity_confidence >= 0 AND severity_confidence <= 100)),
    impact_confidence INT CHECK (impact_confidence IS NULL OR (impact_confidence >= 0 AND impact_confidence <= 100)),
    safety_confidence INT CHECK (safety_confidence IS NULL OR (safety_confidence >= 0 AND safety_confidence <= 100)),
    confidence INT CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 100)),
    signals JSONB,
    applied_to_calculation BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_issue_ai_priority_issue UNIQUE (issue_id)
);
```

---

## 6. API Endpoints & Role-Based Access Control (RBAC)

### 1. `GET /api/issues/{issueId}/priority/ai-recommendation`
- **Access**: Reporter of the issue, or users with role `OFFICER`, `AUTHORITY`, `MODERATOR`, `ADMIN`.
- **Response**:
```json
{
  "issueId": "e1f13ce3-5969-42b7-8d7d-5c8e42f6d2f3",
  "status": "COMPLETED",
  "provider": "LOCAL_HEURISTIC",
  "model": "heuristic-priority-v1",
  "modelVersion": "1.0.0",
  "calculationVersion": "v1",
  "suggestedSeverity": 25,
  "suggestedImpact": 15,
  "suggestedSafety": 20,
  "severityConfidence": 85,
  "impactConfidence": 80,
  "safetyConfidence": 90,
  "confidence": 85,
  "signals": [
    "High severity keywords detected in issue text",
    "Task 47 visual signals: [DEEP_POTHOLE]",
    "Task 47 visual safety concern: HIGH"
  ],
  "appliedToCalculation": false,
  "createdAt": "2026-09-14T14:00:00Z"
}
```

### 2. `POST /api/issues/{issueId}/priority/ai-assess`
- **Access**: Restricted to `OFFICER`, `AUTHORITY`, `MODERATOR`, `ADMIN`. Ordinary citizens receive `403 Forbidden`.
- **Behavior**: Executes analysis outside write transactions using virtual threads with 4000ms timeout. Records audit log `PRIORITY_AI_ASSESSED` in `issue_activity`.
