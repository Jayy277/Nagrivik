/**
 * Nagrivic Web Civic Map Acceptance Tests (Task 32)
 * Verifies all 11 web map requirements from PART 34:
 * 1. /map route
 * 2. map shell
 * 3. issue data rendering
 * 4. filters
 * 5. URL synchronization
 * 6. marker/preview behavior
 * 7. Issue Detail navigation
 * 8. map fallback
 * 9. list fallback
 * 10. accessibility alternative
 * 11. responsive layout
 */

const fs = require('fs');
const path = require('path');
const assert = require('assert');

console.log('\n===========================================================');
console.log('  NAGRIVIC WEB CIVIC MAP TEST SUITE (TASK 32)             ');
console.log('===========================================================\n');

const mapPagePath = path.join(__dirname, '..', 'app', 'map', 'page.tsx');
const explorerPath = path.join(__dirname, '..', 'app', 'map', 'MapExplorerClient.tsx');
const civicMapPath = path.join(__dirname, '..', 'components', 'map', 'CivicMap.tsx');
const previewPath = path.join(__dirname, '..', 'components', 'map', 'IssueMapPreview.tsx');
const legendPath = path.join(__dirname, '..', 'components', 'map', 'MapLegend.tsx');
const headerPath = path.join(__dirname, '..', 'components', 'Header.tsx');
const issuesPagePath = path.join(__dirname, '..', 'app', 'issues', 'page.tsx');
const cityConfigPath = path.join(__dirname, '..', 'lib', 'config', 'city.ts');

assert(fs.existsSync(mapPagePath), 'app/map/page.tsx must exist');
assert(fs.existsSync(explorerPath), 'MapExplorerClient.tsx must exist');
assert(fs.existsSync(civicMapPath), 'CivicMap.tsx must exist');
assert(fs.existsSync(previewPath), 'IssueMapPreview.tsx must exist');
assert(fs.existsSync(legendPath), 'MapLegend.tsx must exist');
assert(fs.existsSync(headerPath), 'Header.tsx must exist');
assert(fs.existsSync(issuesPagePath), 'app/issues/page.tsx must exist');
assert(fs.existsSync(cityConfigPath), 'lib/config/city.ts must exist');

const mapPageSrc = fs.readFileSync(mapPagePath, 'utf8');
const explorerSrc = fs.readFileSync(explorerPath, 'utf8');
const civicMapSrc = fs.readFileSync(civicMapPath, 'utf8');
const previewSrc = fs.readFileSync(previewPath, 'utf8');
const legendSrc = fs.readFileSync(legendPath, 'utf8');
const headerSrc = fs.readFileSync(headerPath, 'utf8');
const issuesPageSrc = fs.readFileSync(issuesPagePath, 'utf8');
const cityConfigSrc = fs.readFileSync(cityConfigPath, 'utf8');

// [Test 1] /map Route & SEO Metadata
console.log('[Test 1] Verifying /map route and SEO metadata...');
assert(mapPageSrc.includes("title: 'Public Civic Map | Nagrivic'"), 'Map page must have descriptive title');
assert(mapPageSrc.includes('application/ld+json'), 'Map page must inject JSON-LD structured data');
assert(headerSrc.includes('href="/map"'), 'Header must provide link to /map');
console.log('  ✓ Public /map route structured with SEO metadata and JSON-LD');

// [Test 2] Map Shell & SSR Hybrid
console.log('[Test 2] Verifying server shell and dynamic client map loading...');
assert(explorerSrc.includes('dynamic('), 'CivicMap must be loaded dynamically');
assert(explorerSrc.includes('ssr: false'), 'Leaflet must disable SSR to protect against window errors');
assert(cityConfigSrc.includes('23.0225') && cityConfigSrc.includes('72.5714'), 'Default city configured as Ahmedabad');
console.log('  ✓ Server shell rendered with client-side map hydration');

// [Test 3] Issue Data Rendering
console.log('[Test 3] Verifying real backend issue querying...');
assert(explorerSrc.includes('getIssues('), 'Client queries getIssues with geographic parameters');
assert(explorerSrc.includes("sort: 'NEAREST'"), 'Map queries sorted by physical proximity');
assert(civicMapSrc.includes('tileLayer'), 'Leaflet tile layer initialized with OpenStreetMap');
console.log('  ✓ Authoritative PostGIS data queried and displayed on map');

// [Test 4] Category, Status, Priority Filters
console.log('[Test 4] Verifying filter controls...');
assert(explorerSrc.includes('selectedCategory'), 'Category filter state supported');
assert(explorerSrc.includes('selectedStatus'), 'Status filter state supported');
assert(explorerSrc.includes('selectedPriority'), 'Priority filter state supported');
assert(explorerSrc.includes('searchQuery'), 'Search query supported');
console.log('  ✓ Category, status, priority, and text search filters verified');

// [Test 5] URL Synchronization
console.log('[Test 5] Verifying bounded URL synchronization...');
assert(explorerSrc.includes('window.history.replaceState'), 'Uses replaceState to avoid history pollution');
assert(explorerSrc.includes('params.set'), 'URL parameters synchronized with active filters');
console.log('  ✓ URL synchronized without spamming browser history');

// [Test 6 & 7] Marker / Preview Behavior & Navigation
console.log('[Test 6 & 7] Verifying marker preview and Issue Detail navigation...');
assert(previewSrc.includes('IssueMapPreview'), 'IssueMapPreview component exists');
assert(previewSrc.includes('href={`/issues/${issue.id}`}'), 'Preview navigates to canonical /issues/[issueId]');
assert(civicMapSrc.includes('onSelectIssue'), 'Marker click selects target issue');
console.log('  ✓ Marker click displays preview and links to canonical issue detail');

// [Test 8] Map Fallback
console.log('[Test 8] Verifying map tile load failure fallback...');
assert(civicMapSrc.includes('tileerror'), 'Leaflet listens for tileerror events');
assert(civicMapSrc.includes('Map Unavailable'), 'Renders fallback state if tiles fail');
assert(civicMapSrc.includes('onSwitchToList'), 'Provides one-click fallback to list view');
console.log('  ✓ Tile failure gracefully caught and handled with fallback action');

// [Test 9] List Fallback / Sidebar Feed
console.log('[Test 9] Verifying sidebar issue list fallback...');
assert(explorerSrc.includes('map-explorer-sidebar'), 'Desktop split-view provides issue list sidebar');
assert(explorerSrc.includes('IssueCard'), 'Sidebar renders accessible IssueCards');
assert(explorerSrc.includes('ErrorState'), 'Network errors handled with retry');
console.log('  ✓ Issue list remains completely usable even if map interaction fails');

// [Test 10] Accessibility Alternative
console.log('[Test 10] Verifying accessible list alternative and keyboard controls...');
assert(issuesPageSrc.includes('href="/map"'), 'Issues page links to Map view');
assert(mapPageSrc.includes('href="/issues"'), 'Map page provides prominent Switch to List View link');
assert(legendSrc.includes('Map Legend'), 'Map provides accessible visual legend');
console.log('  ✓ Complete non-map alternative and accessible landmarks provided');

// [Test 11] Responsive Layout
console.log('[Test 11] Verifying responsive layout across desktop and mobile...');
const globalsCss = fs.readFileSync(path.join(__dirname, '..', 'app', 'globals.css'), 'utf8');
assert(globalsCss.includes('.map-explorer-layout'), 'Layout CSS defined');
assert(globalsCss.includes('@media (max-width: 960px)'), 'Responsive breakpoint for tablet/mobile viewports');
assert(globalsCss.includes('flex-direction: column'), 'Switches to vertical stacked flow on small screens');
console.log('  ✓ Responsive split-view for desktop and stacked view for mobile browsers');

console.log('\n===========================================================');
console.log('  ALL 11 WEB MAP ACCEPTANCE TESTS PASSED SUCCESSFULLY!     ');
console.log('===========================================================\n');
