package org.nagrivic.modules.priority.ai.model;

/**
 * Server-controlled mode defining how AI priority suggestions relate to the deterministic score.
 */
public enum PriorityInfluenceMode {
    /**
     * AI recommendations are purely advisory and stored for inspection without modifying the deterministic score.
     */
    ADVISORY,

    /**
     * AI recommendations provide bounded adjustments (max ±10 points total) to severity, impact, and safety,
     * without bypassing deterministic priority bands or forcing CRITICAL escalation.
     */
    BLENDED
}
