# Civic Accountability Domain (`accountability`)

## 1. Domain Purpose & Philosophy

Nagrivic is founded on the principle that civic infrastructure improves when reporting, remediation, and resolution verification are transparent, auditable, and accessible to every citizen.

The **Civic Accountability Domain** aggregates real, verified issue data across cities and municipal wards to answer fundamental civic questions:
- *What civic defects are being reported across urban areas?*
- *Where are defects geographically concentrated?*
- *How many reported issues are actively being remediated by municipal crews?*
- *How many authority-claimed resolutions have been independently confirmed by reporting citizens?*
- *How long have unresolved defects remained in open queues?*
- *What percentage of reported issues possess resolved civic responsibility mapping?*

### Core Architectural Principles & Neutrality Guarantees

1. **Non-Partisan & Objective Fact-Finding**:
   - The dashboard strictly avoids political rankings, elected official scoring, political party attribution, or subjective blame allocation.
   - Metrics describe operational defect reports, department assignments, and resolution milestones exclusively.
2. **Anti-Double Counting (Canonical Deduplication)**:
   - When multiple citizens report the same physical defect, duplicate submissions are linked to a single canonical primary issue (`duplicate_of_issue_id`).
   - The accountability engine excludes linked duplicate reports (`duplicateOf IS NULL`) so physical problems are counted exactly once.
3. **Strict Moderation Visibility Rules**:
   - Content hidden by moderation (`moderation_status = 'HIDDEN'`) is completely excluded from public metrics to maintain community safety standards and prevent abuse leakage.
4. **Analytical Age Distribution vs Statutory SLAs**:
   - Age buckets (`0–1d`, `2–7d`, `8–30d`, `31–90d`, `90+d`) measure the elapsed calendar time of open reports on Nagrivic.
   - They are explicitly identified as **analytical distributions**, not statutory government Service Level Agreements (SLAs).
5. **Independent Citizen Verification**:
   - Authority resolution claims (work finished by field crews) and citizen verifications (independent confirmation on site) are tracked and displayed as distinct milestones.
   - An authority evidence upload does not imply or guarantee that an issue is fixed until confirmed by the citizen.
6. **Zero Citizen or Officer PII**:
   - Aggregate statistics expose zero citizen phone numbers, emails, passwords, user IDs, or private street addresses.
   - Officer names, employee IDs, and internal assignment notes are strictly excluded from public responses.

---

## 2. API Contract

### 2.1 Public Accountability Overview
- **Path**: `GET /api/public/accountability`
- **Security**: Public / Anonymous (No authentication required).
- **Query Parameters**:
  - `cityId` (`UUID`, optional): Defaults to Ahmedabad if available.
  - `wardId` (`UUID`, optional): Restricts metrics to a specific authoritative ward.
  - `categoryId` (`UUID`, optional): Restricts metrics to a specific civic category.
  - `range` (`string`, optional, default `"30d"`): Time window (`"7d"`, `"30d"`, `"90d"`, or `"all"`).

### 2.2 Response Schema (`PublicAccountabilityResponse`)
```json
{
  "cityId": "ca000000-0000-0000-0000-000000000001",
  "cityName": "Ahmedabad",
  "wardId": null,
  "wardName": null,
  "categoryId": null,
  "categoryName": null,
  "range": "30d",
  "summary": {
    "totalPublicIssues": 1250,
    "actionableCount": 420,
    "reportedCount": 85,
    "verifiedCount": 110,
    "acknowledgedCount": 95,
    "inProgressCount": 130,
    "resolvedCount": 310,
    "citizenVerifiedCount": 480,
    "notFixedCount": 40,
    "highPriorityCount": 210,
    "criticalPriorityCount": 65,
    "unresolvedResponsibilityCount": 85
  },
  "statusBreakdown": {
    "reported": 85,
    "verified": 110,
    "acknowledged": 95,
    "inProgress": 130,
    "resolved": 310,
    "citizenVerified": 480,
    "notFixed": 40
  },
  "priorityBreakdown": {
    "low": 340,
    "medium": 635,
    "high": 210,
    "critical": 65
  },
  "categoryBreakdown": [
    {
      "categoryId": "cat-uuid-1",
      "name": "Roads / Potholes",
      "slug": "roads-potholes",
      "total": 520,
      "openActionable": 180,
      "inProgress": 70,
      "resolved": 140,
      "citizenVerified": 190,
      "notFixed": 10
    }
  ],
  "wardBreakdown": [
    {
      "wardId": "ward-uuid-1",
      "name": "Navrangpura",
      "wardNumber": "014",
      "wardCode": "NAV",
      "total": 95,
      "openActionable": 28,
      "inProgress": 12,
      "resolved": 22,
      "citizenVerified": 42,
      "notFixed": 3,
      "highOrCriticalCount": 18
    }
  ],
  "agingBreakdown": {
    "zeroToOneDay": 45,
    "twoToSevenDays": 110,
    "eightToThirtyDays": 145,
    "thirtyOneToNinetyDays": 80,
    "overNinetyDays": 40
  },
  "verificationSummary": {
    "resolvedByAuthority": 830,
    "citizenVerified": 480,
    "citizenReportedNotFixed": 40,
    "verificationPending": 310,
    "verificationRate": 57.8
  },
  "responsibilitySummary": {
    "resolvedCount": 1165,
    "unresolvedCount": 85,
    "coveragePercentage": 93.2,
    "departmentBreakdown": [
      {
        "departmentId": "dept-uuid-1",
        "name": "Engineering - Roads",
        "code": "ENG-ROADS",
        "totalAssigned": 520,
        "openActionable": 180,
        "resolved": 140,
        "citizenVerified": 190
      }
    ]
  },
  "trend": [
    {
      "date": "2026-09-01",
      "reportedCount": 24,
      "resolvedCount": 18,
      "citizenVerifiedCount": 15
    }
  ],
  "lastUpdated": "2026-09-14T12:00:00Z"
}
```

---

## 3. Calculation Methodology & Metric Definitions

### 3.1 Total Public Reports
$$\text{Total Public Reports} = \sum_{\text{issues}} 1 \quad [\text{moderation\_status} \neq \text{'HIDDEN'} \land \text{duplicate\_of\_issue\_id IS NULL}]$$

### 3.2 Open / Actionable Count
Issues currently requiring municipal triage or active remediation:
$$\text{Actionable} = \text{REPORTED} + \text{VERIFIED} + \text{ACKNOWLEDGED} + \text{IN\_PROGRESS} + \text{NOT\_FIXED}$$

### 3.3 Citizen Verification Rate
The verification rate evaluates how many issues reported as resolved by authorities have been confirmed fixed by reporting citizens:
$$\text{Resolved by Authority} = \text{RESOLVED} + \text{CITIZEN\_VERIFIED} + \text{NOT\_FIXED}$$
$$\text{Verification Rate} = \frac{\text{CITIZEN\_VERIFIED}}{\text{Resolved by Authority}} \times 100\%$$
*(If $\text{Resolved by Authority} = 0$, rate is $0.0\%$.)*

### 3.4 Responsibility Coverage Percentage
$$\text{Coverage Rate} = \frac{\text{Responsibility Resolved}}{\text{Responsibility Resolved} + \text{Responsibility Unresolved}} \times 100\%$$

---

## 4. Web Frontend Architecture

- **Route**: `/accountability`
- **Component Structure**:
  - `Header` & `Footer` integration for prominent public discoverability.
  - `AccountabilityFilters`: Interactive filter controls for City, Ward, Category, and Time Range (`7d`, `30d`, `90d`, `all`), synchronized with URL query parameters for bookmarkable, shareable states.
  - Summary cards displaying executive counts.
  - Operational lifecycle funnel showing visual issue progression.
  - Accessible data tables with direct drill-down links to `/issues?category=...` and `/issues?wardId=...`.
  - Non-SLA disclaimers and methodology transparency section.
