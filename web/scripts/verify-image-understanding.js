const fs = require('fs');
const path = require('path');

function assert(condition, message) {
  if (!condition) {
    console.error(`❌ Assertion Failed: ${message}`);
    process.exit(1);
  }
  console.log(`  ✓ ${message}`);
}

console.log('\n=== NAGRIVIC TASK 47: AI IMAGE UNDERSTANDING TEST SUITE ===\n');

// Test 1: Verify Domain Types & DTOs
console.log('[Test 1] Verifying Image Understanding Types & Contracts...');
const typesPath = path.join(__dirname, '../types/image-understanding.ts');
assert(fs.existsSync(typesPath), 'types/image-understanding.ts must exist');
const typesContent = fs.readFileSync(typesPath, 'utf8');
assert(typesContent.includes('ImageAnalysisStatus'), 'types must define ImageAnalysisStatus');
assert(typesContent.includes('CivicVisualCategory'), 'types must define CivicVisualCategory');
assert(typesContent.includes('ROADS_POTHOLES'), 'CivicVisualCategory must include ROADS_POTHOLES');
assert(typesContent.includes('GARBAGE'), 'CivicVisualCategory must include GARBAGE');
assert(typesContent.includes('STREETLIGHTS'), 'CivicVisualCategory must include STREETLIGHTS');
assert(typesContent.includes('WATER'), 'CivicVisualCategory must include WATER');
assert(typesContent.includes('DRAINAGE'), 'CivicVisualCategory must include DRAINAGE');
assert(typesContent.includes('ImageQuality'), 'types must define ImageQuality');
assert(typesContent.includes('QualityIssue'), 'types must define QualityIssue');
assert(typesContent.includes('CivicRelevance'), 'types must define CivicRelevance');
assert(typesContent.includes('VisualSafetyConcern'), 'types must define VisualSafetyConcern');
assert(typesContent.includes('VisualProblemType'), 'types must define VisualProblemType');
assert(typesContent.includes('VisualSeveritySignal'), 'types must define VisualSeveritySignal');
assert(typesContent.includes('ImageAiAnalysis'), 'types must define ImageAiAnalysis');
assert(typesContent.includes('categoryConfidence'), 'ImageAiAnalysis must contain optional categoryConfidence');
assert(typesContent.includes('sensitiveVisualContentDetected'), 'ImageAiAnalysis must contain sensitiveVisualContentDetected');

// Test 2: Verify API Client Library
console.log('\n[Test 2] Verifying Image Understanding API Client Library...');
const clientPath = path.join(__dirname, '../lib/api/image-understanding.ts');
assert(fs.existsSync(clientPath), 'lib/api/image-understanding.ts must exist');
const clientContent = fs.readFileSync(clientPath, 'utf8');
assert(clientContent.includes('triggerImageAnalysis'), 'API client must export triggerImageAnalysis');
assert(clientContent.includes('getImageAnalysis'), 'API client must export getImageAnalysis');
assert(clientContent.includes('/issues/'), 'API client must call correct REST path');

// Test 3: Verify ImageAiAnalysisCard Component
console.log('\n[Test 3] Verifying ImageAiAnalysisCard Component...');
const cardPath = path.join(__dirname, '../components/ImageAiAnalysisCard.tsx');
assert(fs.existsSync(cardPath), 'components/ImageAiAnalysisCard.tsx must exist');
const cardContent = fs.readFileSync(cardPath, 'utf8');
assert(cardContent.includes('Advisory Only'), 'Card must prominently display Advisory Only disclaimer');
assert(cardContent.includes('AI-Assisted Image Observations'), 'Card must display title');
assert(cardContent.includes('Likely Category:'), 'Card must show likely category');
assert(cardContent.includes('categoryConfidence'), 'Card must show confidence percentage');
assert(cardContent.includes('Detected Visual Problem Types:'), 'Card must show visual problem chips');
assert(cardContent.includes('Quality:'), 'Card must display image quality');
assert(cardContent.includes('Civic Relevance:'), 'Card must display relevance');
assert(cardContent.includes('Severity Signals:'), 'Card must display severity signals');
assert(cardContent.includes('Safety Indicator:'), 'Card must display safety indicator');
assert(cardContent.includes('Purely advisory; human verification is authoritative'), 'Card must reinforce authority verification');

// Test 4: Verify Authority Issue Details Page Integration
console.log('\n[Test 4] Verifying Authority Issue Details Page Integration...');
const authorityPagePath = path.join(__dirname, '../app/authority/issues/[issueId]/page.tsx');
assert(fs.existsSync(authorityPagePath), 'Authority issue detail page must exist');
const pageContent = fs.readFileSync(authorityPagePath, 'utf8');
assert(pageContent.includes('ImageAiAnalysisCard'), 'Authority page must import and render ImageAiAnalysisCard');

// Test 5: Verify Privacy & Anti-Surveillance Safeguards
console.log('\n[Test 5] Verifying Privacy & Anti-Surveillance Safeguards...');
assert(!cardContent.includes('faceRecognition'), 'Must never include facial recognition');
assert(!cardContent.includes('licensePlate'), 'Must never include license plate recognition');
assert(!typesContent.includes('faceId'), 'No facial identification fields in types');
assert(!typesContent.includes('personName'), 'No person identification fields in types');

console.log('\n🎉 ALL TASK 47 FRONTEND VERIFICATION TESTS PASSED SUCCESSFULLY!\n');
