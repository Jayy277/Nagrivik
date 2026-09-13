package org.nagrivic.modules.priority.model;

/**
 * Structured safety-impact signal for hazards.
 */
public enum SafetyImpact {
    NONE(0),
    LOW(5),
    MEDIUM(15),
    HIGH(20),
    CRITICAL(25);

    private final int score;

    SafetyImpact(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }
}
