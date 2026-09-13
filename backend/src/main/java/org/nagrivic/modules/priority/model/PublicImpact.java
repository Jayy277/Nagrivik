package org.nagrivic.modules.priority.model;

/**
 * Structured public impact signal representing breadth of citizens affected.
 */
public enum PublicImpact {
    LOW(5),
    MEDIUM(15),
    HIGH(25);

    private final int score;

    PublicImpact(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }
}
