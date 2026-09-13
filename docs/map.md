# Civic Map & Geographic Issue Visualization Architecture (Task 32)

## 1. Executive Summary

Nagrivic provides a production-grade, privacy-first geographic visualization layer enabling citizens to discover, inspect, and filter reported municipal complaints spatially.

The mapping architecture is shared across:
1. **Mobile App**: Integrated into the **Nearby** screen ([`nearby.tsx`](file:///j:/Nagrivic/NagrivicApp/src/app/(tabs)/nearby.tsx)) featuring a seamless **[ Map View | List View ]** segmented control, touch-enabled tile rendering, marker clustering, and compact preview cards.
2. **Public Web**: Accessible at **`/map`** with a responsive split-view (interactive Leaflet map on desktop with an synchronized issue sidebar, and stacked layout on mobile browsers).

---

## 2. Architecture & Authoritative PostGIS

```
                                    ┌────────────────────────────────┐
                                    │      Citizen Frontend          │
                                    │ (Mobile Nearby / Web /map)     │
                                    └───────────────┬────────────────┘
                                                    │
                             GET /api/issues?latitude=...&longitude=...
                             &radiusMeters=...&sort=NEAREST
                                                    │
                                                    ▼
                                    ┌────────────────────────────────┐
                                    │     Spring Boot Backend        │
                                    │    (IssueDiscoveryFilter)      │
                                    └───────────────┬────────────────┘
                                                    │
                     Bounding Box Envelope: ST_Expand(..., radiusDegrees)
                     Geodesic Distance Filter: ST_DWithin(..., radiusMeters)
                     Spatial Index: location_point USING GIST
                                                    │
                                                    ▼
                                    ┌────────────────────────────────┐
                                    │     PostgreSQL + PostGIS       │
                                    │    (Authoritative Geometry)    │
                                    └────────────────────────────────┘
```

1. **Authoritative Backend**: Frontends never compute geographic containment or distance filters client-side. The PostgreSQL + PostGIS engine filters candidates using `ST_DWithin` and the GIST index on `locations.location_point`.
2. **Capped Discovery Radius**: Discovery queries strictly enforce a server-side maximum radius of **50,000 meters (50 km)**. Unrestricted or negative radius values are rejected with `400 BAD_REQUEST`.
3. **Hidden & Duplicate Exclusions**: General map discovery queries strictly exclude issues marked as `moderation_status = 'HIDDEN'` or duplicate reports (`duplicate_of_issue_id IS NOT NULL`).

---

## 3. Map Provider & Tile Infrastructure

- **Open Standard**: Nagrivic uses standard **OpenStreetMap (OSM)** raster tiles, completely eliminating third-party closed tracking SDKs, mandatory proprietary API keys, and vendor lock-in.
- **Configurability**: Tile URLs are completely environment-driven:
  - Web: `NEXT_PUBLIC_MAP_TILE_URL` (Defaults to `https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png`)
  - Mobile: `EXPO_PUBLIC_MAP_TILE_URL` (Defaults to `https://tile.openstreetmap.org/{z}/{x}/{y}.png`)
- **Attribution**: Legally compliant attribution is rendered on both mobile and web (`© OpenStreetMap contributors`).

---

## 4. Default City Context (Ahmedabad Pilot)

Nagrivic's initial municipal deployment is configured for Ahmedabad, Gujarat, India:
- **City Center Landmark**: AMC Headquarters / Central Municipal Ward (`23.0225° N, 72.5714° E`).
- **Default Discovery Radius**: 5,000 meters (5 km).
- **Default Zoom**: Level 13 (covering municipal corporation zone boundaries).
- **Environment Overrides**:
  - `EXPO_PUBLIC_DEFAULT_CITY_LAT` / `EXPO_PUBLIC_DEFAULT_CITY_LNG`
  - `NEXT_PUBLIC_DEFAULT_CITY_LAT` / `NEXT_PUBLIC_DEFAULT_CITY_LNG`
- **Extensible Architecture**: Defined in [`cityConfig.ts`](file:///j:/Nagrivic/NagrivicApp/src/constants/cityConfig.ts) and [`city.ts`](file:///j:/Nagrivic/web/lib/config/city.ts), facilitating expansion to Surat, Vadodara, Rajkot, and additional Indian municipalities without code restructuring.

---

## 5. Marker & Clustering Strategy

1. **Individual Markers**:
   - High-contrast circular pin with a downward-pointing arrow indicating the issue coordinate.
   - Distinct civic category icon (🚧 Roads/Potholes, 🗑️ Garbage, 💡 Streetlights, 🚰 Water, 🌊 Drainage, ⚠️ Other).
   - Priority accent dot (Critical: `#DC2626`, High: `#EA580C`, Medium: `#D97706`, Low: `#16A34A`).
2. **Marker Clustering**:
   - When issues are clustered close together (within 48 screen pixels on mobile or 0.008 degrees on web), dense markers are grouped into a cluster node.
   - Cluster badges display the exact issue count (e.g. `12`).
   - Tapping/clicking a cluster zooms in by 2 zoom levels centered on the cluster centroid, progressively expanding into individual pins.

---

## 6. Compact Issue Preview

Tapping any marker opens a lightweight bottom sheet / preview card:
- **Thumbnail**: Displayed using optimized thumbnail caching with a placeholder icon fallback.
- **Header**: Category name pill, status badge, priority badge, and dismiss button.
- **Metadata**: Civic issue title, distance from user (e.g. `350m away`), support count, and relative age.
- **CTA**: "View Issue" button triggering routing to the canonical Issue Detail page:
  - Mobile: `/issue/[issueId]`
  - Web: `/issues/[issueId]`
- **Payload Efficiency**: Preview card does NOT fetch heavy comments or full audit logs until the user opens the full issue detail.

---

## 7. Performance & Race Condition Guards

1. **Movement Debounce**: Viewport changes (`moveend` / region changes) are debounced by **400ms**, completely preventing API requests on every pan frame.
2. **Stale Request Protection**:
   - Uses sequential request tracking (`requestIdRef`).
   - Uses native `AbortController` cancellation.
   - If Request B finishes before Request A, Request A is discarded and cannot overwrite Request B.
3. **Bounded URL State**: On web, filters and search parameters are synchronized using `window.history.replaceState`, avoiding generation of thousands of browser history entries during map panning.

---

## 8. Mobile Location & "Near Me" Behavior

1. **Foreground Location Only**: Reuses Task 28's `acquireCurrentGpsLocation`. Zero background location tracking or continuous location watchers.
2. **"Near Me" Action**: Acquires one-time high-accuracy GPS coordinates, centers the map, displays a blue pulsating user location indicator, and requests nearby issues within the selected radius.
3. **Graceful Permission Denial**: If location permission is denied or device GPS is disabled:
   - The map remains fully interactive.
   - Automatically falls back to the configured public city center (Ahmedabad).
   - Users can browse issues normally without being blocked.

---

## 9. Fallback & Accessibility Guarantees

1. **Tile Failure Resilience**: If map tiles or network imagery fails:
   - Map displays an accessible "Map Unavailable" fallback card.
   - Provides a one-click button: **"Switch to List View"**.
   - The issue list, search, and category/status/priority filters remain 100% operational.
2. **Screen Reader & Keyboard Accessibility**:
   - Prominent **[ Map View | List View ]** toggle available on both mobile and web.
   - All interactive controls have explicit `accessibilityLabel` or `aria-label` tags.
   - Complete visual and accessible **Map Legend** explaining symbols, clusters, and colors.
3. **Strict Privacy Policy**:
   - Coordinates shown represent municipal public hazard locations.
   - Citizen phone numbers, email addresses, and private residential locations are strictly excluded from map data payloads and DOM elements.
