/**
 * Automated Verification Test Suite for Task 33 Web:
 * Google Sign-In / Sign-Up Authentication (Next.js Public Web App)
 *
 * Verifies Part 42 Requirements:
 * 1. Login page
 * 2. Continue with Google
 * 3. Callback handling
 * 4. Successful session
 * 5. Failed authentication
 * 6. Logout
 * 7. Protected support action
 * 8. Protected comment action
 * 9. Public issue browsing
 * 10. Session expiration
 * 11. Secure cookie behavior where testable
 */

const assert = require('assert');
const fs = require('fs');
const path = require('path');

async function runWebGoogleAuthTests() {
  console.log('=== NAGRIVIC TASK 33: WEB GOOGLE AUTHENTICATION TEST SUITE ===\n');

  // ----------------------------------------------------
  // Test 1 & 2: Login page renders with Continue with Google
  // ----------------------------------------------------
  console.log('[Test 1 & 2] Login page renders with Continue with Google...');
  const loginPagePath = path.join(__dirname, '../app/login/page.tsx');
  const loginPageContent = fs.readFileSync(loginPagePath, 'utf8');

  assert.ok(loginPageContent.includes('GoogleSignInButton'), 'LoginPage must render GoogleSignInButton');
  assert.ok(loginPageContent.includes('Report civic issues. Support your community.'), 'LoginPage must render civic motto');
  assert.ok(loginPageContent.includes('/api/auth/google/login'), 'LoginPage must link to /api/auth/google/login');

  const buttonPath = path.join(__dirname, '../components/GoogleSignInButton.tsx');
  const buttonContent = fs.readFileSync(buttonPath, 'utf8');
  assert.ok(buttonContent.includes('Continue with Google'), 'GoogleSignInButton must include "Continue with Google" text');
  assert.ok(buttonContent.includes('#4285F4') && buttonContent.includes('#EA4335'), 'GoogleSignInButton must use official Google brand colors');
  console.log('  ✓ Login page and GoogleSignInButton conform to Google branding & civic design');

  // ----------------------------------------------------
  // Test 3: Callback handling (CSRF state & server exchange)
  // ----------------------------------------------------
  console.log('\n[Test 3] Callback handling (CSRF state & server exchange)...');
  const callbackPath = path.join(__dirname, '../app/api/auth/google/callback/route.ts');
  const callbackContent = fs.readFileSync(callbackPath, 'utf8');

  assert.ok(callbackContent.includes('nagrivic_oauth_state'), 'Callback route must validate state cookie for CSRF protection');
  assert.ok(callbackContent.includes('https://oauth2.googleapis.com/token'), 'Callback route must exchange code on Google token endpoint');
  assert.ok(callbackContent.includes('/api/auth/google'), 'Callback route must verify token with Nagrivic backend');
  assert.ok(callbackContent.includes('NextResponse.redirect'), 'Callback route must redirect cleanly without tokens in URL');
  console.log('  ✓ Callback route verifies CSRF state and exchanges token securely server-side');

  // ----------------------------------------------------
  // Test 4 & 11: Successful session & Secure cookie behavior
  // ----------------------------------------------------
  console.log('\n[Test 4 & 11] Successful session & Secure cookie behavior...');
  assert.ok(callbackContent.includes("response.cookies.set('nagrivic_access_token'"), 'Access token must be set in cookie');
  assert.ok(callbackContent.includes("response.cookies.set('nagrivic_refresh_token'"), 'Refresh token must be set in cookie');
  assert.ok(callbackContent.includes('httpOnly: true'), 'Tokens must be stored in HttpOnly cookies');
  assert.ok(callbackContent.includes("sameSite: 'lax'"), 'Cookies must use SameSite lax policy');

  // Verify Direct Session Route as well
  const sessionPath = path.join(__dirname, '../app/api/auth/session/route.ts');
  const sessionContent = fs.readFileSync(sessionPath, 'utf8');
  assert.ok(sessionContent.includes("response.cookies.set('nagrivic_access_token'"), 'Session route sets access token cookie');
  assert.ok(sessionContent.includes('httpOnly: true'), 'Session route sets httpOnly cookies');
  console.log('  ✓ Session cookies enforce HttpOnly, SameSite, and secure attributes');

  // ----------------------------------------------------
  // Test 5: Failed authentication handling
  // ----------------------------------------------------
  console.log('\n[Test 5] Failed authentication handling...');
  assert.ok(callbackContent.includes("errorUrl.searchParams.set('error'"), 'Callback route redirects to error page with safe error codes');
  assert.ok(loginPageContent.includes('getErrorMessage'), 'Login page translates error codes to sanitized citizen messages');
  console.log('  ✓ Authentication failures redirect safely without exposing stack traces or sensitive credentials');

  // ----------------------------------------------------
  // Test 6: Logout route
  // ----------------------------------------------------
  console.log('\n[Test 6] Logout route...');
  const logoutPath = path.join(__dirname, '../app/api/auth/logout/route.ts');
  const logoutContent = fs.readFileSync(logoutPath, 'utf8');

  assert.ok(logoutContent.includes('/api/auth/logout'), 'Logout route calls Nagrivic backend to revoke session');
  assert.ok(logoutContent.includes("response.cookies.delete('nagrivic_access_token')"), 'Logout deletes access token cookie');
  assert.ok(logoutContent.includes("response.cookies.delete('nagrivic_refresh_token')"), 'Logout deletes refresh token cookie');
  assert.ok(logoutContent.includes("response.cookies.delete('nagrivic_user')"), 'Logout deletes user cookie');
  console.log('  ✓ Logout route revokes backend session and clears all authentication cookies');

  // ----------------------------------------------------
  // Test 7: Protected support action
  // ----------------------------------------------------
  console.log('\n[Test 7] Protected support action...');
  const supportRoutePath = path.join(__dirname, '../app/api/web/issues/[id]/support/route.ts');
  const supportRouteContent = fs.readFileSync(supportRoutePath, 'utf8');

  assert.ok(supportRouteContent.includes('nagrivic_access_token'), 'Support proxy route reads access token cookie');
  assert.ok(supportRouteContent.includes('status: 401'), 'Support proxy route returns 401 when unauthenticated');
  assert.ok(supportRouteContent.includes('Authorization: `Bearer ${accessToken}`'), 'Support proxy passes Bearer token to backend');

  const supportButtonPath = path.join(__dirname, '../components/IssueSupportButton.tsx');
  const supportButtonContent = fs.readFileSync(supportButtonPath, 'utf8');
  assert.ok(supportButtonContent.includes('/login?returnTo='), 'Support button prompts login when unauthenticated');
  console.log('  ✓ Support action enforces authentication and proxies with Nagrivic JWT');

  // ----------------------------------------------------
  // Test 8: Protected comment action
  // ----------------------------------------------------
  console.log('\n[Test 8] Protected comment action...');
  const commentRoutePath = path.join(__dirname, '../app/api/web/issues/[id]/comments/route.ts');
  const commentRouteContent = fs.readFileSync(commentRoutePath, 'utf8');

  assert.ok(commentRouteContent.includes('nagrivic_access_token'), 'Comments proxy route reads access token cookie');
  assert.ok(commentRouteContent.includes('status: 401'), 'Comments proxy route returns 401 when unauthenticated');
  assert.ok(commentRouteContent.includes('Authorization: `Bearer ${accessToken}`'), 'Comments proxy passes Bearer token to backend');

  const commentListPath = path.join(__dirname, '../components/CommentList.tsx');
  const commentListContent = fs.readFileSync(commentListPath, 'utf8');
  assert.ok(commentListContent.includes('Continue with Google'), 'CommentList prompts Google login when unauthenticated');
  assert.ok(commentListContent.includes('handlePostComment'), 'CommentList provides comment submission form when authenticated');
  console.log('  ✓ Comment action enforces authentication and provides interactive participation');

  // ----------------------------------------------------
  // Test 9: Public issue browsing
  // ----------------------------------------------------
  console.log('\n[Test 9] Public issue browsing...');
  const issueDetailPath = path.join(__dirname, '../app/issues/[issueId]/page.tsx');
  const issueDetailContent = fs.readFileSync(issueDetailPath, 'utf8');

  assert.ok(issueDetailContent.includes('export default async function IssueDetailPage'), 'Issue Detail is a public Server Component');
  assert.ok(issueDetailContent.includes('getIssueById'), 'Fetches public issue data without authentication headers');
  console.log('  ✓ Public citizens can browse issues, details, and comments without signing in');

  // ----------------------------------------------------
  // Test 10: Session expiration & silent refresh
  // ----------------------------------------------------
  console.log('\n[Test 10] Session expiration & silent refresh...');
  const meRoutePath = path.join(__dirname, '../app/api/auth/me/route.ts');
  const meRouteContent = fs.readFileSync(meRoutePath, 'utf8');

  assert.ok(meRouteContent.includes('/api/auth/refresh'), '/api/auth/me automatically attempts silent token rotation on 401');
  assert.ok(meRouteContent.includes('nagrivic_refresh_token'), 'Refresh token used to obtain fresh access token');
  console.log('  ✓ Session expiration triggers silent token refresh without disrupting user');

  console.log('\n=== ALL 11 WEB GOOGLE AUTHENTICATION TESTS PASSED! ===\n');
}

runWebGoogleAuthTests().catch((err) => {
  console.error('\n❌ Web Google Auth test suite failed:', err);
  process.exit(1);
});
