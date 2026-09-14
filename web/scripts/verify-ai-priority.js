const fs = require('fs');
const path = require('path');

function assert(condition, message) {
  if (!condition) {
    console.error(`❌ Assertion Failed: ${message}`);
    process.exit(1);
  }
  console.log(`  ✓ ${message}`);
}

console.log('\n=== NAGRIVIC TASK 48: AI PRIORITY ASSISTANCE TEST SUITE ===\n');

// Test 1: Verify Domain Types & Contracts
console.log('[Test 1] Verifying AI Priority Types & Contracts...');
const typesPath = path.join(__dirname, '../types/priority.ts');
assert(fs.existsSync(typesPath), 'types/priority.ts must exist');
const typesContent = fs.readFileSync(typesPath, 'utf8');
assert(typesContent.includes('PriorityAiStatus'), 'types must define PriorityAiStatus');
assert(typesContent.includes("'PENDING'"), 'PriorityAiStatus must include PENDING');
assert(typesContent.includes("'COMPLETED'"), 'PriorityAiStatus must include COMPLETED');
assert(typesContent.includes("'FAILED'"), 'PriorityAiStatus must include FAILED');
assert(typesContent.includes("'UNAVAILABLE'"), 'PriorityAiStatus must include UNAVAILABLE');
assert(typesContent.includes('PriorityInfluenceMode'), 'types must define PriorityInfluenceMode');
assert(typesContent.includes("'ADVISORY'"), 'PriorityInfluenceMode must include ADVISORY');
assert(typesContent.includes("'BLENDED'"), 'PriorityInfluenceMode must include BLENDED');
assert(typesContent.includes('AiPriorityRecommendation'), 'types must define AiPriorityRecommendation');
assert(typesContent.includes('suggestedSeverity'), 'AiPriorityRecommendation must include suggestedSeverity (0-30)');
assert(typesContent.includes('suggestedImpact'), 'AiPriorityRecommendation must include suggestedImpact (0-25)');
assert(typesContent.includes('suggestedSafety'), 'AiPriorityRecommendation must include suggestedSafety (0-25)');
assert(typesContent.includes('confidence'), 'AiPriorityRecommendation must include confidence (0-100)');
assert(typesContent.includes('signals'), 'AiPriorityRecommendation must include signals');
assert(typesContent.includes('appliedToCalculation'), 'AiPriorityRecommendation must include appliedToCalculation');

// Test 2: Verify API Client Library
console.log('\n[Test 2] Verifying AI Priority API Client Library...');
const clientPath = path.join(__dirname, '../lib/api/priority.ts');
assert(fs.existsSync(clientPath), 'lib/api/priority.ts must exist');
const clientContent = fs.readFileSync(clientPath, 'utf8');
assert(clientContent.includes('getAiPriorityRecommendation'), 'API client must export getAiPriorityRecommendation');
assert(clientContent.includes('triggerAiPriorityAssessment'), 'API client must export triggerAiPriorityAssessment');
assert(clientContent.includes('/ai-recommendation'), 'API client must call /ai-recommendation endpoint');
assert(clientContent.includes('/ai-assess'), 'API client must call /ai-assess endpoint');

// Test 3: Verify AiPriorityCard Component
console.log('\n[Test 3] Verifying AiPriorityCard Component...');
const cardPath = path.join(__dirname, '../components/AiPriorityCard.tsx');
assert(fs.existsSync(cardPath), 'components/AiPriorityCard.tsx must exist');
const cardContent = fs.readFileSync(cardPath, 'utf8');
assert(cardContent.includes('Advisory Signal Only'), 'Card must prominently display Advisory Signal Only badge');
assert(cardContent.includes('Suggested Severity'), 'Card must display Suggested Severity breakdown');
assert(cardContent.includes('Suggested Impact'), 'Card must display Suggested Impact breakdown');
assert(cardContent.includes('Suggested Safety'), 'Card must display Suggested Safety breakdown');
assert(cardContent.includes('AI Confidence Rating'), 'Card must display confidence rating meter');
assert(cardContent.includes('Explainable Assessment Factors'), 'Card must display explainable signals');
assert(cardContent.includes('Re-assess') || cardContent.includes('reassess'), 'Card must support on-demand reassessment for authorities');

// Test 4: Verify Authority Issue Details Page Integration
console.log('\n[Test 4] Verifying Authority Issue Details Page Integration...');
const authorityPagePath = path.join(__dirname, '../app/authority/issues/[issueId]/page.tsx');
assert(fs.existsSync(authorityPagePath), 'Authority issue detail page must exist');
const pageContent = fs.readFileSync(authorityPagePath, 'utf8');
assert(pageContent.includes('AiPriorityCard'), 'Authority page must import and render AiPriorityCard');
assert(pageContent.includes('canReassess={true}'), 'Authority page must allow privileged reassessment');

// Test 5: Verify Anti-Critical Safeguard Logic (Client Simulation)
console.log('\n[Test 5] Verifying Anti-Critical Safeguards & Bounded Blending Rules...');
function simulateBlendedScore(deterministicTotal, detSev, detImp, detSafe, aiRec, confidenceThreshold = 70) {
  if (aiRec.confidence < confidenceThreshold) return deterministicTotal;

  // Clamped component deltas
  let sevDelta = (aiRec.suggestedSeverity ?? detSev) - detSev;
  sevDelta = Math.max(-5, Math.min(5, Math.round(sevDelta * (aiRec.confidence / 100))));

  let impDelta = (aiRec.suggestedImpact ?? detImp) - detImp;
  impDelta = Math.max(-4, Math.min(4, Math.round(impDelta * (aiRec.confidence / 100))));

  let safeDelta = (aiRec.suggestedSafety ?? detSafe) - detSafe;
  safeDelta = Math.max(-4, Math.min(4, Math.round(safeDelta * (aiRec.confidence / 100))));

  let totalDelta = sevDelta + impDelta + safeDelta;
  totalDelta = Math.max(-10, Math.min(10, totalDelta));

  let candidate = deterministicTotal + totalDelta;

  // CRITICAL ANTI-ESCALATION SAFEGUARD:
  // AI alone cannot force an issue into CRITICAL (>= 75)
  if (deterministicTotal < 75 && candidate >= 75) {
    candidate = 74;
  }
  return Math.max(0, Math.min(100, candidate));
}

// Case A: Non-critical baseline (70) cannot reach CRITICAL (75+) via AI
const recHigh = { suggestedSeverity: 30, suggestedImpact: 25, suggestedSafety: 25, confidence: 95 };
const blendedNonCritical = simulateBlendedScore(70, 15, 10, 5, recHigh);
assert(blendedNonCritical === 74, `Non-critical baseline 70 must cap at 74 (got ${blendedNonCritical})`);

// Case B: Critical baseline (78) preserves critical
const blendedCritical = simulateBlendedScore(78, 25, 20, 20, recHigh);
assert(blendedCritical >= 75, `Critical baseline 78 preserves critical level (got ${blendedCritical})`);

// Case C: Max total delta strictly bounded to ±10
const recMax = { suggestedSeverity: 30, suggestedImpact: 25, suggestedSafety: 25, confidence: 100 };
const blendedCapped = simulateBlendedScore(10, 5, 5, 0, recMax);
assert(blendedCapped === 20, `Max positive delta bounded to +10 (10 + 10 = 20, got ${blendedCapped})`);

// Case D: Low confidence (< 70) produces 0 delta
const recLowConf = { suggestedSeverity: 30, suggestedImpact: 25, suggestedSafety: 25, confidence: 50 };
const blendedLowConf = simulateBlendedScore(40, 15, 10, 5, recLowConf);
assert(blendedLowConf === 40, `Low confidence produces zero delta (got ${blendedLowConf})`);

console.log('\n✅ All AI Priority Assistance verification tests passed successfully!\n');
