/**
 * Automated Verification Test Suite for Task 45 Web:
 * Civic Accountability Dashboard (Next.js App)
 */

const assert = require('assert');
const fs = require('fs');
const path = require('path');

async function runAccountabilityTests() {
  console.log('=== NAGRIVIC TASK 45: PUBLIC CIVIC ACCOUNTABILITY TEST SUITE ===\n');

  // ----------------------------------------------------
  // Test 1: Accountability Domain Types & DTOs
  // ----------------------------------------------------
  console.log('[Test 1] Verifying Accountability Domain Types...');
  const typesPath = path.join(__dirname, '../types/accountability.ts');
  const typesContent = fs.readFileSync(typesPath, 'utf8');

  assert.ok(typesContent.includes('AccountabilityFilterParams'), 'Types must declare AccountabilityFilterParams');
  assert.ok(typesContent.includes('AccountabilitySummary'), 'Types must declare AccountabilitySummary');
  assert.ok(typesContent.includes('StatusBreakdown'), 'Types must declare StatusBreakdown');
  assert.ok(typesContent.includes('PriorityBreakdown'), 'Types must declare PriorityBreakdown');
  assert.ok(typesContent.includes('CategoryBreakdownItem'), 'Types must declare CategoryBreakdownItem');
  assert.ok(typesContent.includes('WardBreakdownItem'), 'Types must declare WardBreakdownItem');
  assert.ok(typesContent.includes('AgingBreakdown'), 'Types must declare AgingBreakdown');
  assert.ok(typesContent.includes('VerificationSummary'), 'Types must declare VerificationSummary');
  assert.ok(typesContent.includes('ResponsibilitySummary'), 'Types must declare ResponsibilitySummary');
  assert.ok(typesContent.includes('TrendPoint'), 'Types must declare TrendPoint');
  assert.ok(typesContent.includes('PublicAccountabilityResponse'), 'Types must declare PublicAccountabilityResponse');
  console.log('  ✓ Accountability domain types accurately reflect backend API contract');

  // ----------------------------------------------------
  // Test 2: API Client & Endpoint Mapping
  // ----------------------------------------------------
  console.log('\n[Test 2] Verifying API Client Library...');
  const apiPath = path.join(__dirname, '../lib/api/accountability.ts');
  const apiContent = fs.readFileSync(apiPath, 'utf8');

  assert.ok(apiContent.includes('getPublicAccountability'), 'Client must export getPublicAccountability');
  assert.ok(apiContent.includes('/public/accountability'), 'Client must call /public/accountability endpoint');
  assert.ok(apiContent.includes('cityId') && apiContent.includes('wardId') && apiContent.includes('range'), 'Client must pass query parameters');
  console.log('  ✓ API client correctly maps query parameters to /public/accountability');

  // ----------------------------------------------------
  // Test 3: Public Accountability Page Structure & Visualizations
  // ----------------------------------------------------
  console.log('\n[Test 3] Verifying Public Accountability Page (/accountability)...');
  const pagePath = path.join(__dirname, '../app/accountability/page.tsx');
  const pageContent = fs.readFileSync(pagePath, 'utf8');

  assert.ok(pageContent.includes('AccountabilityPage'), 'Page component must be exported');
  assert.ok(pageContent.includes('generateMetadata'), 'Page must provide dynamic SEO metadata');
  assert.ok(pageContent.includes('Accountability Dashboard'), 'Page title must reflect Civic Accountability Dashboard');
  assert.ok(pageContent.includes('AccountabilityFilters'), 'Page must render AccountabilityFilters component');
  assert.ok(pageContent.includes('summary-metrics-heading'), 'Page must contain executive summary cards');
  assert.ok(pageContent.includes('resolution-funnel-heading'), 'Page must contain resolution funnel');
  assert.ok(pageContent.includes('citizen-verification-heading'), 'Page must contain citizen verification section');
  assert.ok(pageContent.includes('category-breakdown-heading'), 'Page must contain category breakdown table');
  assert.ok(pageContent.includes('ward-breakdown-heading'), 'Page must contain authoritative ward table');
  assert.ok(pageContent.includes('aging-breakdown-heading'), 'Page must contain analytical age distribution');
  assert.ok(pageContent.includes('trend-heading'), 'Page must contain reporting and resolution timeline');
  assert.ok(pageContent.includes('responsibility-heading'), 'Page must contain civic responsibility coverage');
  assert.ok(pageContent.includes('methodology-heading'), 'Page must contain methodology and neutrality principles');
  console.log('  ✓ Public accountability page presents all required operational and civic transparency sections');

  // ----------------------------------------------------
  // Test 4: Neutrality, Anti-Politicization & Non-SLA Disclaimers
  // ----------------------------------------------------
  console.log('\n[Test 4] Verifying Neutrality & Non-SLA Disclaimers...');
  assert.ok(
    pageContent.includes('Non-Partisan & Objective') || pageContent.includes('does not rate elected representatives'),
    'Page must explicitly state non-partisan policy with zero political ranking'
  );
  assert.ok(
    pageContent.includes('Anti-Double Counting') || pageContent.includes('duplicates are excluded'),
    'Page must explain canonical deduplication methodology'
  );
  assert.ok(
    pageContent.includes('Non-SLA Disclaimer') || pageContent.includes('not statutory government service level agreements'),
    'Page must state that age brackets are analytical distributions, not government SLAs'
  );
  assert.ok(
    !pageContent.includes('worst department') && !pageContent.includes('best department'),
    'Page must strictly avoid subjective department ranking or blame allocation'
  );
  console.log('  ✓ Neutrality, deduplication, and non-SLA disclaimers verified');

  // ----------------------------------------------------
  // Test 5: Accessible Navigation & Landmarks
  // ----------------------------------------------------
  console.log('\n[Test 5] Verifying Header & Footer Navigation Links...');
  const headerPath = path.join(__dirname, '../components/Header.tsx');
  const headerContent = fs.readFileSync(headerPath, 'utf8');
  assert.ok(headerContent.includes('/accountability'), 'Header must contain link to /accountability');
  assert.ok(headerContent.includes('Accountability'), 'Header link label must say Accountability');

  const footerPath = path.join(__dirname, '../components/Footer.tsx');
  const footerContent = fs.readFileSync(footerPath, 'utf8');
  assert.ok(footerContent.includes('/accountability'), 'Footer must contain link to /accountability');
  console.log('  ✓ Navigation links to /accountability confirmed in Header and Footer');

  // ----------------------------------------------------
  // Test 6: Drill-Down Links to Public Issues
  // ----------------------------------------------------
  console.log('\n[Test 6] Verifying Drill-Down Exploration Links...');
  assert.ok(pageContent.includes('/issues?category='), 'Category rows must link to filtered issues');
  assert.ok(pageContent.includes('/issues') && pageContent.includes('Explore All Issues'), 'Page must provide general explore link');
  assert.ok(!pageContent.includes('/admin') && !pageContent.includes('/authority'), 'Public dashboard must not expose privileged admin or officer links');
  console.log('  ✓ Public drill-down links verified without leaking administrative routes');

  console.log('\n=== ALL TASK 45 PUBLIC ACCOUNTABILITY WEB CHECKS PASSED ===\n');
}

runAccountabilityTests().catch((err) => {
  console.error('\n❌ Verification test suite failed:\n', err);
  process.exit(1);
});
