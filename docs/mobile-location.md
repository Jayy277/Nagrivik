# Nagrivic Mobile Location & GPS Foundation

> [!IMPORTANT]
> **Scope & Authority Declarations:**
> - Task 28 implements one-time foreground device GPS acquisition for civic issue reporting. Continuous/background location tracking is intentionally not implemented.
> - The backend remains authoritative for civic geography and responsibility resolution.

---

## 1. Overview & Core Mission

Accurate location is crucial for resolving municipal hazards. Whether citizens are reporting dangerous potholes, broken streetlights, or sewage overflows, ward officers and civic engineers require precise geographic coordinates to inspect and dispatch maintenance crews.

The mobile location foundation provides an intuitive, trustworthy, and privacy-preserving GPS acquisition experience within the Nagrivic report flow:

```text
Report Issue
    ↓
Confirm Location (Step 3)
    ├── No location added (Initial state)
    │      ↓
    │   User taps "Use Current Location"
    │      ↓
    │   Foreground Location Permission Check / Request
    │      ↓
    │   Location Services (GPS) Enabled Check
    │      ↓
    │   One-Time GPS Fix (Balanced street-level accuracy, 15s timeout)
    │      ↓
    │   Coordinate & Accuracy Validation (-90..90 lat, -180..180 lon, <= 100m)
    │      ↓
    │   Location Confirmation Card ("Location captured", "Accuracy: about 18 m")
    │      ↓
    │   Update Location (Refresh) OR Continue
    │      ↓
    └── Location Preserved in Report Draft (ReportDraftContext)
```

---

## 2. Location Package & Expo Configuration

- **Package**: `expo-location: ~57.0.17` (strictly matching Expo SDK 57).
- **Configuration (`app.json`)**: Configured with the official Expo plugin and user-facing permission rationale:
  ```json
  [
    "expo-location",
    {
      "locationWhenInUsePermission": "Allow Nagrivic to access your location to pinpoint civic issue reports."
    }
  ]
  ```
- **No Third-Party SDKs**: No Google Maps, Mapbox, or proprietary location trackers were installed.

---

## 3. Contextual Permission Flow

Location permission is requested **strictly in context**:
1. **Never on App Launch**: Permissions are never requested when the user opens Nagrivic or navigates feeds.
2. **Explicit User Initiation**: Permission is requested only after the citizen explicitly taps **"Use Current Location"** on Step 3 of the report flow.
3. **Safe State Handling**:
   - `GRANTED`: Proceeds immediately to position acquisition.
   - `DENIED`: Displays safe message: *"Location access is needed to identify where the civic issue was reported."* with a **"Try Again"** button.
   - `BLOCKED`: Displays the message with an **"Open Settings"** action (`Linking.openSettings()`).
   - No infinite prompt loops.

---

## 4. Location Services State Detection

Having app permission does not guarantee that the device GPS hardware is active. The service checks `Location.hasServicesEnabledAsync()` prior to acquiring coordinates:
- If location services are disabled: displays *"Location services are turned off."* with a direct recovery action to open settings.
- Avoids unhandled crashes or hung promises when GPS hardware is powered down.

---

## 5. Accuracy Strategy & Thresholds

Civic infrastructure reporting requires street-level resolution rather than continent- or city-level approximations:
- **Default Accuracy Option**: `Location.Accuracy.Balanced` delivers street-level accuracy (~10–30 meters) without aggressive battery consumption.
- **Accuracy Threshold**: `LOCATION_MAX_ACCEPTABLE_ACCURACY_METERS = 100`.
- **Behavior**:
  - `accuracy <= 100m`: Accepted as a valid street-level fix.
  - `accuracy > 100m`: Triggers a low-accuracy warning: *"Your location isn't accurate enough yet."* Provides citizens with **"Try Again"** (to acquire a better GPS lock) and **"Use Anyway"** (to prevent trapping users in bad satellite reception areas).
  - `accuracy === null`: If the platform omits accuracy, the client accepts the fix without fabricating an accuracy figure.
- **Display Formatting**: Uses human terms (e.g. *"Accuracy: about 18 m"* or *"Accuracy unavailable"*), never raw jargon (`horizontalAccuracy = 18.234`).

---

## 6. Coordinate Validation & Privacy

Before storing coordinates in draft state, `validateCoordinates()` enforces:
1. **Finite Numbers**: Strictly rejects `null`, `undefined`, `NaN`, `Infinity`, and non-number types.
2. **WGS84 Bounds**:
   - Latitude: `-90.0 <= lat <= 90.0`
   - Longitude: `-180.0 <= lon <= 180.0`
3. **Coordinate Privacy**: Exact raw coordinates (e.g. `23.0225, 72.5714`) are **never displayed as primary UI content**. The citizen sees *"Location captured"* and an accuracy badge. Coordinates remain internal to the report draft.

---

## 7. Report Draft Data Model & Lifetime

- **Model (`src/types/location.ts`)**:
  ```typescript
  export type LocationSource = 'DEVICE_GPS';

  export interface ReportDraftLocation {
    latitude: number;
    longitude: number;
    accuracyMeters: number | null;
    capturedAt: string; // ISO 8601 string
    source: LocationSource; // strictly 'DEVICE_GPS'
  }
  ```
- **Context Integration**: Stored in `ReportDraftContext.draft.location`.
- **Lifetime**: Strictly in-memory for the active reporting session. Location is not persisted across app restart.
- **No Address Stored**: Full addresses or private reverse-geocoded data are not stored in the GPS model.

---

## 8. Lifecycle, Concurrency & Timeout Handling

- **Timeout Protection**: Enforces a 15-second timeout (`GPS_TIMEOUT_MS = 15000`). If a GPS fix cannot be obtained in 15 seconds, it returns a safe failure: *"We couldn't get your location in time."* with a retry button.
- **Double-Tap Protection**: `isLoadingLocation` locks detection buttons and displays *"Getting your location..."* with a spinner to prevent concurrent GPS calls.
- **Screen Unmount**: Avoids background leaks by only using one-time acquisition (`getCurrentPositionAsync`).

---

## 9. Non-Goals & Architectural Constraints

- **No Background Location**: Continuous tracking, background watchers (`watchPositionAsync`), location history, and geofencing are strictly prohibited and not implemented.
- **No Reverse Geocoding**: Mobile client does not query Google Geocoding, Mapbox, or Nominatim for addresses.
- **No Client Civic Geography**: Ward, zone, and municipal boundaries are never calculated on mobile.
- **No Map SDK**: Map rendering (Google Maps/Mapbox) is intentionally deferred.
- **No Premature API Calls**: `POST /api/issues` is NOT called in Task 28.

---

## 10. Verification & Test Coverage

Automated testing is verified via `scripts/verify-location-foundation.js`:
- **Run Command**: `npm run test:location` or `npm test`
- **Criteria (Tests A through Z)**:
  - Screen rendering & titles (Tests A, B)
  - Permission denied & services disabled handling (Tests C, D)
  - Coordinate validation: valid, out-of-range, NaN, Infinity (Tests E–J)
  - Accuracy storage, formatting, and low accuracy threshold (Tests K–M)
  - Retry, Update Location, and Continue behavior (Tests N–P)
  - Back navigation & double-tap protection (Tests Q, R)
  - Timeout and error recovery (Test S)
  - Zero premature backend API calls & zero background tracking (Tests T–V)
  - Photo, auth, and navigation regression tests (Tests W–Y)
  - TypeScript compilation check (Test Z)

---

## 11. Future Backend Mapping Architecture

When issue submission is connected in a future task, the authoritative flow will be:

```text
MOBILE CLIENT
  - latitude: 23.0225
  - longitude: 72.5714
  - accuracyMeters: 18
  - capturedAt: '2026-09-12T01:30:00Z'
      ↓
POST /api/issues
      ↓
BACKEND SERVICE
      ↓
PostGIS Point: ST_SetSRID(ST_MakePoint(72.5714, 23.0225), 4326)
      ↓
CIVIC GEOGRAPHY RESOLVER (Point-in-Polygon)
      ↓
Authoritative Assignment:
  - City: Ahmedabad
  - Ward: Navrangpura
  - Civic Body: AMC (Ahmedabad Municipal Corporation)
  - Department: Engineering / Roads
```
