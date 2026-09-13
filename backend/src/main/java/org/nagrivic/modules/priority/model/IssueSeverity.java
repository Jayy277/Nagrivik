package org.nagrivic.modules.priority.model;

/**
 * Controlled severity classification signal for an issue.
 */
public enum IssueSeverity {
    LOW(5),
    MEDIUM(15),
    HIGH(25),
    CRITICAL(30);

    private final int score;

    IssueSeverity(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }
}
