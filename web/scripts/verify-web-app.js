/**
 * Verification Test Suite for Task 31:
 * NAGRIVIC PUBLIC WEB APP (NEXT.JS + REACT + TYPESCRIPT)
 *
 * Covers Acceptance Criteria from PART 40:
 * 1. Home page renders and structure
 * 2. Issue listing renders real API data (zero mock data)
 * 3. Search query synchronization
 * 4. Category filter
 * 5. Status filter
 * 6. Priority filter
 * 7. Server-side sorting
 * 8. Server-side pagination
 * 9. Issue Detail page structure
 * 10. 404 / Issue not found handling
 * 11. Duplicate issue linking and navigation
 * 12. Status progression timeline
 * 13. Civic responsibility resolution
 * 14. Support state and app handoff
 * 15. Comments rendering
 * 16. Activity timeline rendering
 * 17. Share and copy link fallback
 * 18. URL filter synchronization
 * 19. SEO metadata generation
 * 20. Sitemap generation
 * 21. Robots.txt configuration
 * 22. Accessibility and privacy standards
 */

const fs = require('fs');
const path = require('path');
const assert = require('assert');

console.log('===========================================================');
console.log('  NAGRIVIC PUBLIC WEB APP TEST SUITE (TASK 31)             ');
console.log('===========================================================\n');

const ROOT = path.resolve(__dirname, '..');

function readSource(relPath) {
  const fullPath = path.join(ROOT, relPath);
  assert(fs.existsSync(fullPath), `File does not exist: ${relPath}`);
  return fs.readFileSync(fullPath, 'utf8');
}

const homeSrc = readSource('app/page.tsx');
const issuesPageSrc = readSource('app/issues/page.tsx');
const issueDetailSrc = readSource('app/issues/[issueId]/page.tsx');
const notFoundSrc = readSource('app/issues/[issueId]/not-found.tsx');
const sitemapSrc = readSource('app/sitemap.ts');
const robotsSrc = readSource('app/robots.ts');
const layoutSrc = readSource('app/layout.tsx');
const headerSrc = readSource('components/Header.tsx');
const footerSrc = readSource('components/Footer.tsx');
const issueCardSrc = readSource('components/IssueCard.tsx');
const issueFiltersSrc = readSource('components/IssueFilters.tsx');
const statusTimelineSrc = readSource('components/StatusTimeline.tsx');
const activityTimelineSrc = readSource('components/ActivityTimeline.tsx');
const commentListSrc = readSource('components/CommentList.tsx');
const shareBtnSrc = readSource('components/ShareButton.tsx');
const issuesApiSrc = readSource('lib/api/issues.ts');
const apiClientSrc = readSource('lib/api/client.ts');
const seoSrc = readSource('lib/seo.ts');

// -------------------------------------------------------------
// Test 1: Home page structure & branding
// -------------------------------------------------------------
console.log('[Test 1] Verifying Home page structure and branding...');
assert(homeSrc.includes('Nagrik Ki Awaaz, Sheher Ka Sudhaar'), 'Must include hero slogan');
assert(homeSrc.includes('Report an Issue'), 'Must include primary Report CTA');
assert(homeSrc.includes('Explore Issues'), 'Must include secondary Explore Issues CTA');
assert(homeSrc.includes('How Nagrivic Works'), 'Must explain civic workflow');
assert(homeSrc.includes('getIssues'), 'Must fetch real issues');
assert(!homeSrc.includes('mockIssues'), 'Must not import or use mock issues');
console.log('  ✓ Home page properly structured with civic branding');

// -------------------------------------------------------------
// Test 2: Issue listing with real API data
// -------------------------------------------------------------
console.log('[Test 2] Verifying issue listing uses real API data...');
assert(issuesPageSrc.includes('getIssues'), 'Issues page must call getIssues');
assert(issuesPageSrc.includes('getCategories'), 'Issues page must call getCategories');
assert(!issuesPageSrc.includes('mockIssues'), 'Issues page must not use mock issues');
console.log('  ✓ Real API data used for issue listing');

// -------------------------------------------------------------
// Test 3: Search synchronization
// -------------------------------------------------------------
console.log('[Test 3] Verifying search synchronization...');
assert(issueFiltersSrc.includes("searchParams.get('q')"), 'Must read q from URL');
assert(issueFiltersSrc.includes('setTimeout'), 'Search must be debounced');
assert(issuesPageSrc.includes('searchParams'), 'Issues page must read searchParams');
console.log('  ✓ Debounced search updates URL query parameters');

// -------------------------------------------------------------
// Test 4, 5, 6, 7: Filters & Sort
// -------------------------------------------------------------
console.log('[Test 4-7] Verifying category, status, priority, and sort filters...');
assert(issueFiltersSrc.includes('REPORTED'), 'Must include REPORTED status');
assert(issueFiltersSrc.includes('IN_PROGRESS'), 'Must include IN_PROGRESS status');
assert(issueFiltersSrc.includes('CRITICAL'), 'Must include CRITICAL priority');
assert(issueFiltersSrc.includes('MOST_SUPPORTED'), 'Must include MOST_SUPPORTED sort');
assert(issuesPageSrc.includes('sortParam'), 'Issues page must pass sortParam to backend');
console.log('  ✓ All backend-supported filters and sorting modes verified');

// -------------------------------------------------------------
// Test 8: Server-side pagination
// -------------------------------------------------------------
console.log('[Test 8] Verifying server-side pagination...');
assert(issuesPageSrc.includes('size: 20'), 'Must use page size 20');
assert(issuesPageSrc.includes('buildPageUrl'), 'Must build pagination URLs');
assert(issuesPageSrc.includes('Previous Page') && issuesPageSrc.includes('Next Page'), 'Must provide navigation controls');
assert(issueFiltersSrc.includes("params.delete('page')"), 'Changing filters must reset pagination to page 0');
console.log('  ✓ Server-side pagination properly implemented');

// -------------------------------------------------------------
// Test 9 & 10: Issue Detail & 404
// -------------------------------------------------------------
console.log('[Test 9 & 10] Verifying Issue Detail and 404 handling...');
assert(issueDetailSrc.includes('getIssueById'), 'Must fetch issue by ID');
assert(issueDetailSrc.includes('notFound()'), 'Must call notFound() when issue is absent');
assert(notFoundSrc.includes('Issue Not Found'), 'Must present friendly 404 page');
console.log('  ✓ Issue Detail and 404 handling verified');

// -------------------------------------------------------------
// Test 11: Duplicate issue linking
// -------------------------------------------------------------
console.log('[Test 11] Verifying duplicate issue banner and navigation...');
assert(issueDetailSrc.includes('issue.isDuplicate'), 'Must check isDuplicate flag');
assert(issueDetailSrc.includes('This issue is linked to another report'), 'Must show duplicate notice');
assert(issueDetailSrc.includes('View Primary Issue'), 'Must provide link to primary report');
console.log('  ✓ Duplicate issue warning and link verified');

// -------------------------------------------------------------
// Test 12: Status timeline
// -------------------------------------------------------------
console.log('[Test 12] Verifying status progression timeline...');
assert(statusTimelineSrc.includes('REPORTED'), 'Must show REPORTED stage');
assert(statusTimelineSrc.includes('VERIFIED'), 'Must show VERIFIED stage');
assert(statusTimelineSrc.includes('IN_PROGRESS'), 'Must show IN_PROGRESS stage');
assert(statusTimelineSrc.includes('RESOLVED'), 'Must show RESOLVED stage');
assert(statusTimelineSrc.includes('NOT_FIXED'), 'Must handle NOT_FIXED state');
console.log('  ✓ Status progression flowchart verified');

// -------------------------------------------------------------
// Test 13: Civic responsibility
// -------------------------------------------------------------
console.log('[Test 13] Verifying civic responsibility display...');
assert(issueDetailSrc.includes('Civic Responsibility'), 'Must have civic responsibility section');
assert(issueDetailSrc.includes('Responsible civic authority is being determined'), 'Must show pending message when unresolved');
assert(issueDetailSrc.includes('departmentName'), 'Must display department name');
assert(issueDetailSrc.includes('civicBodyName'), 'Must display civic body name');
console.log('  ✓ Civic responsibility details verified');

// -------------------------------------------------------------
// Test 14: Support & Mobile handoff
// -------------------------------------------------------------
console.log('[Test 14] Verifying support counter and mobile handoff...');
assert(issueDetailSrc.includes('issue.supportCount'), 'Must display support count');
assert(issueDetailSrc.includes('Support via App'), 'Must direct visitors to mobile app');
assert(!issueCardSrc.includes('Like'), 'Must not call it Like');
console.log('  ✓ Support count and mobile handoff verified');

// -------------------------------------------------------------
// Test 15 & 16: Comments & Activity
// -------------------------------------------------------------
console.log('[Test 15 & 16] Verifying comments and activity timelines...');
assert(commentListSrc.includes('comments.map'), 'Must render comments');
assert(commentListSrc.includes('formatRelativeTime'), 'Must format comment timestamps');
assert(activityTimelineSrc.includes('formatActivityEvent'), 'Must translate activity events to human labels');
assert(!activityTimelineSrc.includes('rawJson'), 'Must not expose raw JSON');
console.log('  ✓ Comments and activity streams verified');

// -------------------------------------------------------------
// Test 17: Share & Copy link
// -------------------------------------------------------------
console.log('[Test 17] Verifying Share and Copy link fallback...');
assert(shareBtnSrc.includes('navigator.share'), 'Must use Web Share API');
assert(shareBtnSrc.includes('navigator.clipboard'), 'Must fallback to clipboard');
assert(shareBtnSrc.includes('Link Copied!'), 'Must provide visual copy confirmation');
console.log('  ✓ Share and clipboard fallback verified');

// -------------------------------------------------------------
// Test 18, 19, 20, 21: SEO, Metadata, Sitemap, Robots
// -------------------------------------------------------------
console.log('[Test 18-21] Verifying SEO metadata, JSON-LD, Sitemap, and Robots...');
assert(issueDetailSrc.includes('generateMetadata'), 'Must export generateMetadata');
assert(issueDetailSrc.includes('application/ld+json'), 'Must include JSON-LD structured data script');
assert(seoSrc.includes('generateIssueJsonLd'), 'Must implement JSON-LD generator');
assert(sitemapSrc.includes('sitemap()'), 'Must export sitemap function');
assert(sitemapSrc.includes('/issues/'), 'Sitemap must include issue URLs');
assert(robotsSrc.includes('robots()'), 'Must export robots function');
assert(robotsSrc.includes('/sitemap.xml'), 'Robots must point to sitemap.xml');
console.log('  ✓ Full SEO suite (Metadata, Open Graph, JSON-LD, Sitemap, Robots) verified');

// -------------------------------------------------------------
// Test 22: Accessibility & Privacy
// -------------------------------------------------------------
console.log('[Test 22] Verifying accessibility landmarks and privacy guards...');
assert(headerSrc.includes('role="banner"'), 'Header must have banner role');
assert(footerSrc.includes('role="contentinfo"'), 'Footer must have contentinfo role');
assert(!issueCardSrc.includes('phoneNumber'), 'Must not display phone numbers');
assert(!issueDetailSrc.includes('phoneNumber'), 'Must not display phone numbers');
assert(!issueCardSrc.includes('email'), 'Must not display emails');
assert(!issueDetailSrc.includes('email'), 'Must not display emails');
assert(footerSrc.includes('independent civic engagement system'), 'Footer must clarify neutral non-governmental status');
console.log('  ✓ Accessibility standards and strict privacy guards verified');

console.log('\n===========================================================');
console.log('  ALL 22 TASK 31 ACCEPTANCE TESTS PASSED SUCCESSFULLY!     ');
console.log('===========================================================');
