const fs = require('fs');
const path = require('path');

function assert(condition, message) {
  if (!condition) {
    console.error(`❌ Assertion Failed: ${message}`);
    process.exit(1);
  }
  console.log(`  ✓ ${message}`);
}

console.log('\n=== NAGRIVIC TASK 46: AI DUPLICATE DETECTION TEST SUITE ===\n');

// Test 1: Verify duplicate types
console.log('[Test 1] Verifying Duplicate Domain Types & DTOs...');
const typesPath = path.join(__dirname, '../types/duplicate.ts');
assert(fs.existsSync(typesPath), 'types/duplicate.ts must exist');
const typesContent = fs.readFileSync(typesPath, 'utf8');
assert(typesContent.includes('DuplicateConfidence'), 'types must define DuplicateConfidence');
assert(typesContent.includes('DuplicateMatchType'), 'types must define DuplicateMatchType');
assert(typesContent.includes('DuplicateSuggestionStatus'), 'types must define DuplicateSuggestionStatus');
assert(typesContent.includes('DuplicateCandidate'), 'types must define DuplicateCandidate');
assert(typesContent.includes('DuplicateCheckResponse'), 'types must define DuplicateCheckResponse');
assert(typesContent.includes('AiDuplicateSuggestion'), 'types must define AiDuplicateSuggestion');
assert(typesContent.includes('aiScore'), 'DuplicateCandidate must contain optional aiScore');
assert(typesContent.includes('confidence'), 'DuplicateCandidate must contain optional confidence');
assert(typesContent.includes('signals: string[]'), 'DuplicateCandidate must contain signals');

// Test 2: Verify API Client Library
console.log('\n[Test 2] Verifying Duplicates API Client Library...');
const clientPath = path.join(__dirname, '../lib/api/duplicates.ts');
assert(fs.existsSync(clientPath), 'lib/api/duplicates.ts must exist');
const clientContent = fs.readFileSync(clientPath, 'utf8');
assert(clientContent.includes('getDuplicateSuggestions'), 'API client must export getDuplicateSuggestions');
assert(clientContent.includes('linkDuplicateSuggestion'), 'API client must export linkDuplicateSuggestion');
assert(clientContent.includes('dismissDuplicateSuggestion'), 'API client must export dismissDuplicateSuggestion');
assert(clientContent.includes('scanIssueForDuplicates'), 'API client must export scanIssueForDuplicates');

// Test 3: Verify Proxy Routes
console.log('\n[Test 3] Verifying Next.js Proxy Routes for Admin Duplicates...');
assert(fs.existsSync(path.join(__dirname, '../app/api/admin/duplicates/suggestions/route.ts')), 'Proxy route for suggestions list must exist');
assert(fs.existsSync(path.join(__dirname, '../app/api/admin/duplicates/suggestions/[id]/link/route.ts')), 'Proxy route for link suggestion must exist');
assert(fs.existsSync(path.join(__dirname, '../app/api/admin/duplicates/suggestions/[id]/dismiss/route.ts')), 'Proxy route for dismiss suggestion must exist');

// Test 4: Verify Admin Duplicate Review Page
console.log('\n[Test 4] Verifying Admin Duplicate Review Page (/admin/duplicates)...');
const adminPagePath = path.join(__dirname, '../app/admin/duplicates/page.tsx');
assert(fs.existsSync(adminPagePath), 'app/admin/duplicates/page.tsx must exist');
const adminPageContent = fs.readFileSync(adminPagePath, 'utf8');
assert(adminPageContent.includes('Advisory Signal Only'), 'Admin page must have advisory signal disclaimer');
assert(adminPageContent.includes('AI will never automatically merge'), 'Admin page must state AI will never auto-merge');
assert(adminPageContent.includes('Pending Review'), 'Admin page must support Pending Review status filter');
assert(adminPageContent.includes('Linked as Duplicate'), 'Admin page must support Linked status filter');
assert(adminPageContent.includes('Dismissed'), 'Admin page must support Dismissed status filter');
assert(adminPageContent.includes('Link as Duplicate'), 'Admin page must provide Link as Duplicate action');
assert(adminPageContent.includes('Not a Duplicate'), 'Admin page must provide Not a Duplicate action');
assert(adminPageContent.includes('CONFIDENCE'), 'Admin page must display confidence badge');
assert(adminPageContent.includes('Detected Signals:'), 'Admin page must display explainable signals');

// Test 5: Verify Admin Layout Navigation Link
console.log('\n[Test 5] Verifying Admin Layout Navigation Link...');
const layoutPath = path.join(__dirname, '../app/admin/layout.tsx');
const layoutContent = fs.readFileSync(layoutPath, 'utf8');
assert(layoutContent.includes("href: '/admin/duplicates'"), 'Admin layout must include link to /admin/duplicates');

console.log('\n=== ALL TASK 46 AI DUPLICATE DETECTION WEB CHECKS PASSED ===\n');
