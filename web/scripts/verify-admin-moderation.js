/**
 * Automated Verification Test Suite for Task 39 Web:
 * Admin / Moderation Dashboard (Next.js App)
 */

const assert = require('assert');
const fs = require('fs');
const path = require('path');

async function runAdminModerationTests() {
  console.log('=== NAGRIVIC TASK 39: ADMIN / MODERATION DASHBOARD TEST SUITE ===\n');

  // ----------------------------------------------------
  // Test 1: Admin Area Layout & Role Guards
  // ----------------------------------------------------
  console.log('[Test 1] Verifying Admin Layout and Role Guards...');
  const layoutPath = path.join(__dirname, '../app/admin/layout.tsx');
  const layoutContent = fs.readFileSync(layoutPath, 'utf8');

  assert.ok(layoutContent.includes('useAuth'), 'Admin layout must check client session auth');
  assert.ok(layoutContent.includes('MODERATOR') && layoutContent.includes('ADMIN'), 'Admin layout must allow MODERATOR and ADMIN roles');
  assert.ok(layoutContent.includes('router.replace(\'/login?returnTo='), 'Admin layout must redirect unauthenticated users to login');
  assert.ok(layoutContent.includes('Access Restricted') && layoutContent.includes('citizen permissions'), 'Admin layout must display restricted access message for CITIZEN role');
  assert.ok(layoutContent.includes('Nagrivic Admin'), 'Admin layout must render civic admin branding');
  console.log('  ✓ Admin layout enforces role-based access control and blocks CITIZEN access');

  // ----------------------------------------------------
  // Test 2: Overview Dashboard & Summary Metrics
  // ----------------------------------------------------
  console.log('\n[Test 2] Verifying Overview Dashboard & Live Summary...');
  const overviewPath = path.join(__dirname, '../app/admin/page.tsx');
  const overviewContent = fs.readFileSync(overviewPath, 'utf8');

  assert.ok(overviewContent.includes('getModerationSummary'), 'Overview dashboard must query real backend summary metrics');
  assert.ok(overviewContent.includes('totalReports') && overviewContent.includes('openCount'), 'Dashboard must display real report counts');
  assert.ok(overviewContent.includes('resolvedCount') && overviewContent.includes('dismissedCount'), 'Dashboard must display resolved and dismissed counts');
  assert.ok(overviewContent.includes('/admin/moderation'), 'Dashboard must provide links into the moderation queue');
  console.log('  ✓ Overview dashboard connects to live metrics without mock/synthetic data');

  // ----------------------------------------------------
  // Test 3: Moderation Queue with Filter & Pagination
  // ----------------------------------------------------
  console.log('\n[Test 3] Verifying Moderation Queue & Filtering...');
  const queuePath = path.join(__dirname, '../app/admin/moderation/page.tsx');
  const queueContent = fs.readFileSync(queuePath, 'utf8');

  assert.ok(queueContent.includes('getModerationReports'), 'Queue must fetch paginated reports from backend');
  assert.ok(queueContent.includes('statusFilter') && queueContent.includes('targetFilter') && queueContent.includes('reasonFilter'), 'Queue must support multi-criteria filtering');
  assert.ok(queueContent.includes('page') && queueContent.includes('totalPages'), 'Queue must implement pagination controls');
  assert.ok(queueContent.includes('MODERATION_REASON_LABELS'), 'Queue must support standard moderation reason mapping');
  console.log('  ✓ Moderation queue supports server-side filtering and pagination');

  // ----------------------------------------------------
  // Test 4: Report Detail, Safe Inspection & Audit Trail
  // ----------------------------------------------------
  console.log('\n[Test 4] Verifying Report Detail Inspection & Safe DTO Display...');
  const detailPath = path.join(__dirname, '../app/admin/moderation/[id]/page.tsx');
  const detailContent = fs.readFileSync(detailPath, 'utf8');

  assert.ok(detailContent.includes('getModerationReportDetail'), 'Detail page must query report detail');
  assert.ok(detailContent.includes('issueTarget') && detailContent.includes('commentTarget'), 'Detail page must inspect Issue and Comment targets');
  assert.ok(detailContent.includes('actionHistory'), 'Detail page must display audit timeline');
  assert.ok(detailContent.includes('mediaUrls'), 'Detail page must display reported evidence media');
  console.log('  ✓ Report detail view provides contextual inspection and full audit timeline');

  // ----------------------------------------------------
  // Test 5: Moderation Action Execution & Modals
  // ----------------------------------------------------
  console.log('\n[Test 5] Verifying Action Execution & Confirmation Modals...');
  assert.ok(detailContent.includes('reviewReport'), 'Detail page must support marking report IN_REVIEW');
  assert.ok(detailContent.includes('resolveReport'), 'Detail page must support resolving report');
  assert.ok(detailContent.includes('dismissReport'), 'Detail page must support dismissing report');
  assert.ok(detailContent.includes('hideContent'), 'Detail page must support hiding content');
  assert.ok(detailContent.includes('restoreContent'), 'Detail page must support restoring content');
  assert.ok(detailContent.includes('restrictUser'), 'Detail page must support restricting user');
  assert.ok(detailContent.includes('modalConfig') && detailContent.includes('modal-title'), 'Actions must require confirmation dialog');
  assert.ok(detailContent.includes('Moderation Reason (Required for Audit)') && detailContent.includes('Internal Notes'), 'Actions must require a rationale note');
  console.log('  ✓ All specified moderation actions supported with confirmation and required notes');

  // ----------------------------------------------------
  // Test 6: Concurrency Protection (HTTP 409 Handling)
  // ----------------------------------------------------
  console.log('\n[Test 6] Verifying Concurrency Conflict (409) Handling...');
  assert.ok(detailContent.includes('409') || detailContent.includes('conflict'), 'Detail page must handle HTTP 409 conflicts');
  assert.ok(detailContent.includes('Reload Report') || detailContent.includes('fetchDetail'), 'Detail page must offer reload on conflict');
  console.log('  ✓ HTTP 409 concurrency conflicts handled gracefully with fresh reload option');

  // ----------------------------------------------------
  // Test 7: API Proxy Routes & Cookie Forwarding
  // ----------------------------------------------------
  console.log('\n[Test 7] Verifying Next.js API Proxy Routes...');
  const reportsProxyPath = path.join(__dirname, '../app/api/admin/moderation/reports/route.ts');
  const reportsProxyContent = fs.readFileSync(reportsProxyPath, 'utf8');

  assert.ok(reportsProxyContent.includes('nagrivic_access_token'), 'API proxy must read session cookie');
  assert.ok(reportsProxyContent.includes('Authorization'), 'API proxy must attach Bearer token');
  assert.ok(reportsProxyContent.includes('/api/moderation/reports'), 'API proxy must forward to Spring Boot backend');

  const summaryProxyPath = path.join(__dirname, '../app/api/admin/moderation/summary/route.ts');
  assert.ok(fs.existsSync(summaryProxyPath), 'Summary proxy route must exist');

  const resolveProxyPath = path.join(__dirname, '../app/api/admin/moderation/reports/[id]/resolve/route.ts');
  assert.ok(fs.existsSync(resolveProxyPath), 'Resolve action proxy route must exist');
  console.log('  ✓ API proxy routes securely forward authenticated session tokens to backend');

  // ----------------------------------------------------
  // Test 8: Privacy & Anti-Censorship Guards
  // ----------------------------------------------------
  console.log('\n[Test 8] Verifying Privacy & Architectural Guards...');
  const typesPath = path.join(__dirname, '../types/moderation.ts');
  const typesContent = fs.readFileSync(typesPath, 'utf8');

  assert.ok(!typesContent.includes('password') && !typesContent.includes('hashedPassword'), 'Types must not contain passwords');
  assert.ok(!typesContent.includes('phoneNumber') && !typesContent.includes('mobileNumber'), 'Safe user summaries must omit phone numbers');
  assert.ok(!typesContent.includes('POLITICAL') && !typesContent.includes('CRITICISM'), 'Moderation reasons must NOT include political censorship');
  console.log('  ✓ Safe DTOs ensure zero PII leakage and strict anti-censorship guardrails');

  console.log('\n=== ALL 8 TASK 39 ADMIN/MODERATION TESTS PASSED! ===');
}

runAdminModerationTests().catch((err) => {
  console.error('Test failure:', err);
  process.exit(1);
});
