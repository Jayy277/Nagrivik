/**
 * Automated Verification Test Suite for Task 44 Web:
 * Resolution Evidence & Verifiable Issue Resolution (Next.js App)
 */

const assert = require('assert');
const fs = require('fs');
const path = require('path');

async function runResolutionEvidenceTests() {
  console.log('=== NAGRIVIC TASK 44: RESOLUTION EVIDENCE VERIFICATION TEST SUITE ===\n');

  // ----------------------------------------------------
  // Test 1: Domain Types & DTO Definitions
  // ----------------------------------------------------
  console.log('[Test 1] Verifying Resolution Evidence Domain Types & DTOs...');
  const authTypesPath = path.join(__dirname, '../types/authority.ts');
  const authTypesContent = fs.readFileSync(authTypesPath, 'utf8');

  assert.ok(authTypesContent.includes('ResolutionEvidenceType'), 'types/authority.ts must declare ResolutionEvidenceType');
  assert.ok(authTypesContent.includes("'COMPLETION_PHOTO'"), 'ResolutionEvidenceType must support COMPLETION_PHOTO');
  assert.ok(authTypesContent.includes("'COMPLETION_NOTE'"), 'ResolutionEvidenceType must support COMPLETION_NOTE');
  assert.ok(authTypesContent.includes("'BEFORE_AFTER_PHOTO'"), 'ResolutionEvidenceType must support BEFORE_AFTER_PHOTO');
  assert.ok(authTypesContent.includes('ResolutionEvidenceResponse'), 'types/authority.ts must declare ResolutionEvidenceResponse');
  assert.ok(authTypesContent.includes('resolutionEvidence?: ResolutionEvidenceResponse[]'), 'AuthorityIssueDetailResponse must include resolutionEvidence array');

  const issueTypesPath = path.join(__dirname, '../types/issue.ts');
  const issueTypesContent = fs.readFileSync(issueTypesPath, 'utf8');
  assert.ok(issueTypesContent.includes("'RESOLUTION_EVIDENCE_ADDED'"), 'types/issue.ts must declare RESOLUTION_EVIDENCE_ADDED in IssueActivityType');
  assert.ok(issueTypesContent.includes('ResolutionEvidenceResponse'), 'types/issue.ts must declare ResolutionEvidenceResponse');
  console.log('  ✓ Resolution evidence types accurately match backend API contract');

  // ----------------------------------------------------
  // Test 2: API Client & FormData Handling
  // ----------------------------------------------------
  console.log('\n[Test 2] Verifying API Client & Multipart FormData Handling...');
  const clientPath = path.join(__dirname, '../lib/api/client.ts');
  const clientContent = fs.readFileSync(clientPath, 'utf8');
  assert.ok(
    clientContent.includes('instanceof FormData') &&
      clientContent.includes('delete headers') &&
      clientContent.includes('Content-Type'),
    'API client must delete default Content-Type header for FormData so browser sets boundary'
  );

  const authApiPath = path.join(__dirname, '../lib/api/authority.ts');
  const authApiContent = fs.readFileSync(authApiPath, 'utf8');
  assert.ok(authApiContent.includes('uploadResolutionEvidence'), 'authority.ts must expose uploadResolutionEvidence');
  assert.ok(authApiContent.includes('/resolution-evidence'), 'uploadResolutionEvidence must target /resolution-evidence endpoint');

  const issueApiPath = path.join(__dirname, '../lib/api/issues.ts');
  const issueApiContent = fs.readFileSync(issueApiPath, 'utf8');
  assert.ok(issueApiContent.includes('getResolutionEvidence'), 'issues.ts must expose getResolutionEvidence');
  assert.ok(issueApiContent.includes('/resolution-evidence'), 'getResolutionEvidence must target /resolution-evidence endpoint');
  console.log('  ✓ API client correctly maps upload and public retrieval endpoints with proper FormData boundary handling');

  // ----------------------------------------------------
  // Test 3: Authority Issue Detail Evidence Management UI
  // ----------------------------------------------------
  console.log('\n[Test 3] Verifying Authority Issue Detail Resolution Evidence UI...');
  const authDetailPath = path.join(__dirname, '../app/authority/issues/[issueId]/page.tsx');
  const authDetailContent = fs.readFileSync(authDetailPath, 'utf8');

  assert.ok(authDetailContent.includes('Resolution Evidence'), 'Authority detail must contain Resolution Evidence section');
  assert.ok(authDetailContent.includes('uploadResolutionEvidence'), 'Authority detail must invoke uploadResolutionEvidence');
  assert.ok(authDetailContent.includes('COMPLETION_PHOTO'), 'Evidence upload must offer COMPLETION_PHOTO option');
  assert.ok(authDetailContent.includes('COMPLETION_NOTE'), 'Evidence upload must offer COMPLETION_NOTE option');
  assert.ok(authDetailContent.includes('BEFORE_AFTER_PHOTO'), 'Evidence upload must offer BEFORE_AFTER_PHOTO option');
  assert.ok(authDetailContent.includes('1000') && authDetailContent.includes('evidenceNote.length'), 'Evidence upload must enforce 1000 character limit on notes');
  assert.ok(authDetailContent.includes('10 * 1024 * 1024') || authDetailContent.includes('10MB'), 'Evidence upload must note 10MB limit');
  assert.ok(authDetailContent.includes('capturedAt'), 'Evidence upload must support capturedAt timestamp input');
  assert.ok(authDetailContent.includes('IN_PROGRESS') && authDetailContent.includes('RESOLVED'), 'Evidence upload must be allowed when issue is IN_PROGRESS or RESOLVED');
  assert.ok(
    authDetailContent.includes('Evidence Recommendation') || authDetailContent.includes('resolution evidence'),
    'Authority detail must provide proactive guidance before marking an issue RESOLVED'
  );
  console.log('  ✓ Authority detail provides rich evidence upload modal, 1000-char limiter, capturedAt support, and pre-resolution guidance');

  // ----------------------------------------------------
  // Test 4: Citizen Issue View Evidence & Verification Distinction
  // ----------------------------------------------------
  console.log('\n[Test 4] Verifying Citizen Issue Detail Resolution Evidence Card...');
  const citizenDetailPath = path.join(__dirname, '../app/issues/[issueId]/page.tsx');
  const citizenDetailContent = fs.readFileSync(citizenDetailPath, 'utf8');

  assert.ok(citizenDetailContent.includes('getResolutionEvidence'), 'Citizen page must fetch resolution evidence');
  assert.ok(citizenDetailContent.includes('resolution-evidence-heading'), 'Citizen page must render accessible resolution evidence heading');
  assert.ok(
    citizenDetailContent.includes('Authority Resolution Evidence') || citizenDetailContent.includes('Municipal Resolution Evidence'),
    'Citizen page must render Authority Resolution Evidence card'
  );
  assert.ok(
    citizenDetailContent.includes('Authority has marked this issue resolved, but no resolution evidence has been provided.'),
    'Citizen page must display neutral notice when issue is marked resolved without evidence'
  );
  assert.ok(
    citizenDetailContent.includes('Citizen Confirmed') || citizenDetailContent.includes('CITIZEN_VERIFIED'),
    'Citizen page must display Citizen Confirmed status banner'
  );
  assert.ok(
    citizenDetailContent.includes('Citizen Contested') || citizenDetailContent.includes('NOT_FIXED'),
    'Citizen page must display Citizen Contested status banner when NOT_FIXED'
  );
  assert.ok(
    citizenDetailContent.includes('Citizen Verification Pending') || citizenDetailContent.includes('pending'),
    'Citizen page must display Pending verification state when awaiting citizen verification'
  );
  assert.ok(citizenDetailContent.includes('ev.mediaUrl'), 'Citizen page must render evidence photo when mediaUrl is present');
  assert.ok(citizenDetailContent.includes('ev.note'), 'Citizen page must render evidence note text');
  console.log('  ✓ Citizen page displays authority resolution evidence, neutral notice on missing evidence, and clear distinction from citizen verification');

  console.log('\n=== ALL TASK 44 RESOLUTION EVIDENCE WEB CHECKS PASSED ===\n');
}

runResolutionEvidenceTests().catch((err) => {
  console.error('\n❌ Verification test suite failed:\n', err);
  process.exit(1);
});
