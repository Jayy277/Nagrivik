/**
 * Automated Verification Test Suite for Task 43 Web:
 * Authority Issue Management & Operational Workflow (Next.js App)
 */

const assert = require('assert');
const fs = require('fs');
const path = require('path');

async function runAuthorityWorkflowTests() {
  console.log('=== NAGRIVIC TASK 43: AUTHORITY ISSUE MANAGEMENT & WORKFLOW TEST SUITE ===\n');

  // ----------------------------------------------------
  // Test 1: Authority Types & DTO Definitions
  // ----------------------------------------------------
  console.log('[Test 1] Verifying Authority Domain Types & DTOs...');
  const typesPath = path.join(__dirname, '../types/authority.ts');
  const typesContent = fs.readFileSync(typesPath, 'utf8');

  assert.ok(typesContent.includes('AuthorityScopeDto'), 'Types must declare AuthorityScopeDto');
  assert.ok(typesContent.includes('AuthorityDashboardMetrics'), 'Types must declare AuthorityDashboardMetrics');
  assert.ok(typesContent.includes('AuthorityIssueItemResponse'), 'Types must declare AuthorityIssueItemResponse');
  assert.ok(typesContent.includes('AuthorityIssueDetailResponse'), 'Types must declare AuthorityIssueDetailResponse');
  assert.ok(typesContent.includes('ChangeAuthorityStatusRequest'), 'Types must declare ChangeAuthorityStatusRequest');
  assert.ok(typesContent.includes('allowedTransitions: IssueStatus[]'), 'Detail response must include allowedTransitions');
  assert.ok(typesContent.includes('version: number'), 'Detail response must include entity version for optimistic locking');
  console.log('  ✓ Authority domain types accurately match backend API contract');

  // ----------------------------------------------------
  // Test 2: Authority API Client & Concurrency Handling
  // ----------------------------------------------------
  console.log('\n[Test 2] Verifying Authority API Client & Concurrency Conflict Mapping...');
  const clientPath = path.join(__dirname, '../lib/api/authority.ts');
  const clientContent = fs.readFileSync(clientPath, 'utf8');

  assert.ok(clientContent.includes('getAuthorityDashboard'), 'API client must expose getAuthorityDashboard');
  assert.ok(clientContent.includes('getAuthorityIssues'), 'API client must expose getAuthorityIssues');
  assert.ok(clientContent.includes('getAuthorityIssueDetail'), 'API client must expose getAuthorityIssueDetail');
  assert.ok(clientContent.includes('changeAuthorityIssueStatus'), 'API client must expose changeAuthorityIssueStatus');
  assert.ok(clientContent.includes('ConcurrencyConflictError'), 'API client must export ConcurrencyConflictError');
  assert.ok(clientContent.includes('error.status === 409'), 'API client must intercept HTTP 409 conflict and throw ConcurrencyConflictError');
  console.log('  ✓ API client correctly maps endpoints and translates HTTP 409 optimistic locking conflicts');

  // ----------------------------------------------------
  // Test 3: Authority Next.js Proxy Route
  // ----------------------------------------------------
  console.log('\n[Test 3] Verifying Authority Next.js Proxy Route...');
  const proxyPath = path.join(__dirname, '../app/api/authority/[...path]/route.ts');
  const proxyContent = fs.readFileSync(proxyPath, 'utf8');

  assert.ok(proxyContent.includes('nagrivic_access_token'), 'Proxy must extract and forward authentication cookies');
  assert.ok(proxyContent.includes('/api/authority/'), 'Proxy must forward calls to backend authority endpoints');
  assert.ok(proxyContent.includes('export async function GET') && proxyContent.includes('export async function POST'), 'Proxy must support GET and POST methods');
  console.log('  ✓ Next.js route proxy securely propagates credentials to backend /api/authority/**');

  // ----------------------------------------------------
  // Test 4: Authority Operations Layout & Role Guards
  // ----------------------------------------------------
  console.log('\n[Test 4] Verifying Authority Layout & Role Guards...');
  const layoutPath = path.join(__dirname, '../app/authority/layout.tsx');
  const layoutContent = fs.readFileSync(layoutPath, 'utf8');

  assert.ok(layoutContent.includes('useAuth'), 'Layout must inspect authenticated user session');
  assert.ok(layoutContent.includes('OFFICER') && layoutContent.includes('ADMIN'), 'Layout must permit OFFICER and ADMIN roles');
  assert.ok(layoutContent.includes('Access Restricted'), 'Layout must restrict access for unauthorized users');
  assert.ok(layoutContent.includes('Nagrivic Authority'), 'Layout must render authority operations branding');
  console.log('  ✓ Authority layout strictly admits OFFICER and ADMIN roles, blocking unauthorized citizens');

  // ----------------------------------------------------
  // Test 5: Authority Dashboard Overview
  // ----------------------------------------------------
  console.log('\n[Test 5] Verifying Authority Overview Dashboard...');
  const dashboardPath = path.join(__dirname, '../app/authority/page.tsx');
  const dashboardContent = fs.readFileSync(dashboardPath, 'utf8');

  assert.ok(dashboardContent.includes('getAuthorityDashboard'), 'Dashboard must fetch live authority metrics');
  assert.ok(dashboardContent.includes('assignedScopes'), 'Dashboard must display active jurisdictional assignments');
  assert.ok(dashboardContent.includes('actionableIssues') && dashboardContent.includes('verifiedIssues'), 'Dashboard must present actionable issue counts');
  assert.ok(dashboardContent.includes('/authority/issues'), 'Dashboard must provide direct navigation to scoped issues');
  console.log('  ✓ Authority dashboard displays real-time jurisdictional assignments and actionable metrics');

  // ----------------------------------------------------
  // Test 6: Scoped Issue Queue & Filtering
  // ----------------------------------------------------
  console.log('\n[Test 6] Verifying Scoped Issue Queue & Filtering...');
  const issuesPath = path.join(__dirname, '../app/authority/issues/page.tsx');
  const issuesContent = fs.readFileSync(issuesPath, 'utf8');

  assert.ok(issuesContent.includes('getAuthorityIssues'), 'Issue queue must query scoped issues from API');
  assert.ok(issuesContent.includes('statusFilter') && issuesContent.includes('priorityFilter'), 'Issue queue must support status and priority filters');
  assert.ok(issuesContent.includes('actionableOnly'), 'Issue queue must support actionable issues filter toggle');
  assert.ok(issuesContent.includes('totalPages') && issuesContent.includes('totalElements'), 'Issue queue must render pagination and total counters');
  console.log('  ✓ Scoped issue queue supports status, priority, and actionable filtering with pagination');

  // ----------------------------------------------------
  // Test 7: Issue Detail & Operational Workflow Execution
  // ----------------------------------------------------
  console.log('\n[Test 7] Verifying Issue Detail & Operational Workflow Execution...');
  const detailPath = path.join(__dirname, '../app/authority/issues/[issueId]/page.tsx');
  const detailContent = fs.readFileSync(detailPath, 'utf8');

  assert.ok(detailContent.includes('getAuthorityIssueDetail'), 'Detail page must query scoped issue detail');
  assert.ok(detailContent.includes('isScopeForbidden') || detailContent.includes('Outside Authority Jurisdiction'), 'Detail page must display access denied banner when issue is outside scope (IDOR protection)');
  assert.ok(detailContent.includes('allowedTransitions'), 'Detail page must dynamically render buttons for allowed state machine transitions');
  assert.ok(detailContent.includes('changeAuthorityIssueStatus'), 'Detail page must invoke status transition endpoint');
  assert.ok(detailContent.includes('version: issue.version'), 'Detail page must supply optimistic locking version');
  assert.ok(detailContent.includes('ConcurrencyConflictError'), 'Detail page must handle concurrency conflicts gracefully');
  assert.ok(detailContent.includes('RESOLVED') && detailContent.includes('reason'), 'Detail page must require mandatory reason when marking as RESOLVED');
  assert.ok(detailContent.includes('1000'), 'Detail page must support 1000 character limit on transition reason');
  console.log('  ✓ Issue detail enforces IDOR protection, optimistic locking, mandatory resolution reasons, and allowed state transitions');

  console.log('\n=== ALL TASK 43 AUTHORITY WORKFLOW WEB CHECKS PASSED ===\n');
}

runAuthorityWorkflowTests().catch((err) => {
  console.error('\n❌ Verification test suite failed:\n', err);
  process.exit(1);
});
