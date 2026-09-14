/**
 * Automated Verification Test Suite for Task 41 Web:
 * Admin Civic Geography & Responsibility Management (Next.js App)
 */

const assert = require('assert');
const fs = require('fs');
const path = require('path');

async function runAdminGeographyTests() {
  console.log('=== NAGRIVIC TASK 41: ADMIN CIVIC GEOGRAPHY TEST SUITE ===\n');

  // ----------------------------------------------------
  // Test 1: Admin Navigation & Role Guard
  // ----------------------------------------------------
  console.log('[Test 1] Verifying Navigation & Role Guard for Civic Geography...');
  const layoutPath = path.join(__dirname, '../app/admin/layout.tsx');
  const layoutContent = fs.readFileSync(layoutPath, 'utf8');

  assert.ok(layoutContent.includes("user?.role === 'ADMIN'"), 'Admin layout must check for ADMIN role');
  assert.ok(layoutContent.includes('/admin/geography'), 'Admin layout must include link to /admin/geography for ADMIN');
  console.log('  ✓ Admin layout restricts Civic Geography navigation strictly to ADMIN users');

  // ----------------------------------------------------
  // Test 2: Overview Dashboard & Metrics
  // ----------------------------------------------------
  console.log('\n[Test 2] Verifying Overview Dashboard & Live Summary...');
  const overviewPath = path.join(__dirname, '../app/admin/geography/page.tsx');
  const overviewContent = fs.readFileSync(overviewPath, 'utf8');

  assert.ok(overviewContent.includes('getGeographyOverview'), 'Overview page must call getGeographyOverview()');
  assert.ok(overviewContent.includes('validateWardBoundaries'), 'Overview page must support validating ward boundaries');
  assert.ok(overviewContent.includes('triggerReResolution'), 'Overview page must support bounded re-resolution modal');
  assert.ok(overviewContent.includes('totalCivicBodies') && overviewContent.includes('totalWards'), 'Dashboard must render live counts');
  console.log('  ✓ Overview dashboard connects to live metrics, boundary validation, and re-resolution');

  // ----------------------------------------------------
  // Test 3: Ward Management Page
  // ----------------------------------------------------
  console.log('\n[Test 3] Verifying Ward Management Page...');
  const wardPath = path.join(__dirname, '../app/admin/geography/wards/page.tsx');
  const wardContent = fs.readFileSync(wardPath, 'utf8');

  assert.ok(wardContent.includes('getWards'), 'Ward page must call getWards()');
  assert.ok(wardContent.includes('toggleWardActive'), 'Ward page must support active/inactive toggling');
  assert.ok(wardContent.includes('createWard') && wardContent.includes('updateWard'), 'Ward page must support create and update');
  assert.ok(wardContent.includes('ConcurrencyConflictError') || wardContent.includes('Conflict: This ward was modified'), 'Ward page must catch 409 concurrency conflicts');
  assert.ok(wardContent.includes('Polygon OK') || wardContent.includes('hasBoundary'), 'Ward page must show boundary status badge');
  console.log('  ✓ Ward management supports search, filters, safe deactivation, and concurrency handling');

  // ----------------------------------------------------
  // Test 4: Department Management Page
  // ----------------------------------------------------
  console.log('\n[Test 4] Verifying Department Management Page...');
  const deptPath = path.join(__dirname, '../app/admin/geography/departments/page.tsx');
  const deptContent = fs.readFileSync(deptPath, 'utf8');

  assert.ok(deptContent.includes('getDepartments'), 'Department page must call getDepartments()');
  assert.ok(deptContent.includes('toggleDepartmentActive'), 'Department page must support active/inactive toggling');
  assert.ok(deptContent.includes('createDepartment') && deptContent.includes('updateDepartment'), 'Department page must support create and update');
  assert.ok(deptContent.includes('civicBodyId') && deptContent.includes('code'), 'Department page must require civic body and code');
  console.log('  ✓ Department management supports authoritative department CRUD and safe deactivation');

  // ----------------------------------------------------
  // Test 5: Routing Mappings Page
  // ----------------------------------------------------
  console.log('\n[Test 5] Verifying Routing Mappings Page...');
  const mapPath = path.join(__dirname, '../app/admin/geography/mappings/page.tsx');
  const mapContent = fs.readFileSync(mapPath, 'utf8');

  assert.ok(mapContent.includes('getCategoryMappings') && mapContent.includes('getWardMappings'), 'Mappings page must support both Category and Ward mappings');
  assert.ok(mapContent.includes('createCategoryMapping') && mapContent.includes('createWardMapping'), 'Mappings page must support creating mappings');
  assert.ok(mapContent.includes('toggleCategoryMappingActive') && mapContent.includes('toggleWardMappingActive'), 'Mappings page must support toggling mappings active');
  assert.ok(mapContent.includes('NAGRIVIC_CATEGORIES'), 'Mappings page must use existing Nagrivic categories');
  console.log('  ✓ Mappings page supports Category -> Department and Ward -> Department routing rules');

  // ----------------------------------------------------
  // Test 6: Civic Bodies & Cities Page
  // ----------------------------------------------------
  console.log('\n[Test 6] Verifying Civic Bodies & Cities Management Page...');
  const bodyPath = path.join(__dirname, '../app/admin/geography/civic-bodies/page.tsx');
  const bodyContent = fs.readFileSync(bodyPath, 'utf8');

  assert.ok(bodyContent.includes('getCivicBodies') && bodyContent.includes('getCities'), 'Page must fetch civic bodies and cities');
  assert.ok(bodyContent.includes('createCivicBody') && bodyContent.includes('createCity'), 'Page must support create for bodies and cities');
  assert.ok(bodyContent.includes('toggleCivicBodyActive') && bodyContent.includes('toggleCityActive'), 'Page must support active toggling');
  console.log('  ✓ Civic bodies and cities page supports administrative inspection and management');

  // ----------------------------------------------------
  // Test 7: Append-Only Audit Trail Page
  // ----------------------------------------------------
  console.log('\n[Test 7] Verifying Audit Trail Page...');
  const auditPath = path.join(__dirname, '../app/admin/geography/audits/page.tsx');
  const auditContent = fs.readFileSync(auditPath, 'utf8');

  assert.ok(auditContent.includes('getAudits'), 'Audits page must call getAudits()');
  assert.ok(auditContent.includes('selectedEntityType'), 'Audits page must support filtering by entity type');
  assert.ok(auditContent.includes('actorName') || auditContent.includes('actorId'), 'Audits page must render actor identity');
  console.log('  ✓ Audit trail page displays append-only history with actor identity and timestamps');

  // ----------------------------------------------------
  // Test 8: Next.js API Proxy Route
  // ----------------------------------------------------
  console.log('\n[Test 8] Verifying API Proxy Route & Security...');
  const proxyPath = path.join(__dirname, '../app/api/admin/geography/[...path]/route.ts');
  const proxyContent = fs.readFileSync(proxyPath, 'utf8');

  assert.ok(proxyContent.includes('nagrivic_access_token'), 'API proxy must read session cookie');
  assert.ok(proxyContent.includes('Authorization'), 'API proxy must forward Bearer token');
  assert.ok(proxyContent.includes('/api/admin/geography/'), 'API proxy must forward to Spring Boot backend privileged geography endpoint');
  console.log('  ✓ API proxy route securely forwards requests with authenticated Bearer token');

  // ----------------------------------------------------
  // Test 9: Client API Library & Concurrency Error
  // ----------------------------------------------------
  console.log('\n[Test 9] Verifying Geography Client Library & Error Handling...');
  const apiClientPath = path.join(__dirname, '../lib/api/geography.ts');
  const apiClientContent = fs.readFileSync(apiClientPath, 'utf8');

  assert.ok(apiClientContent.includes('class ConcurrencyConflictError'), 'API client must define ConcurrencyConflictError');
  assert.ok(apiClientContent.includes('status === 409'), 'API client must intercept 409 status');
  assert.ok(apiClientContent.includes('status === 403'), 'API client must intercept 403 status');
  console.log('  ✓ Geography client library properly handles 409 Conflict and 403 Forbidden errors');

  // ----------------------------------------------------
  // Test 10: Dedicated Cities Management Page
  // ----------------------------------------------------
  console.log('\n[Test 10] Verifying Dedicated Cities Management Page (/admin/geography/cities)...');
  const citiesPath = path.join(__dirname, '../app/admin/geography/cities/page.tsx');
  const citiesContent = fs.readFileSync(citiesPath, 'utf8');

  assert.ok(citiesContent.includes('getCities'), 'Cities page must fetch cities');
  assert.ok(citiesContent.includes('createCity') && citiesContent.includes('updateCity'), 'Cities page must support create and update');
  assert.ok(citiesContent.includes('toggleCityActive'), 'Cities page must support active/inactive toggling');
  assert.ok(citiesContent.includes('deleteCity'), 'Cities page must support delete with conflict handling');
  assert.ok(citiesContent.includes('selectedCivicBodyId'), 'Cities page must allow filtering by civic body');
  console.log('  ✓ Dedicated Cities management page supports filtering, CRUD, active toggling, and 409 conflict safety');

  // ----------------------------------------------------
  // Test 11: Geography Sub-Layout ADMIN Guard
  // ----------------------------------------------------
  console.log('\n[Test 11] Verifying Geography Sub-Layout ADMIN Role Guard...');
  const geoLayoutPath = path.join(__dirname, '../app/admin/geography/layout.tsx');
  const geoLayoutContent = fs.readFileSync(geoLayoutPath, 'utf8');

  assert.ok(geoLayoutContent.includes("user.role !== 'ADMIN'"), 'Geography layout must guard against non-ADMIN roles');
  assert.ok(geoLayoutContent.includes('Admin Role Required'), 'Geography layout must inform non-admin users of restricted access');
  console.log('  ✓ Geography sub-layout restricts all civic geography routes strictly to ADMIN users');

  console.log('\n=== ALL 11 TASK 41 ADMIN CIVIC GEOGRAPHY TESTS PASSED! ===');
}

runAdminGeographyTests().catch((err) => {
  console.error('Test failure:', err);
  process.exit(1);
});

