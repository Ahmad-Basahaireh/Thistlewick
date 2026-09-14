package com.thistlewick.domain;

/**
 * Task priority level.
 *
 * <p>Ordered from lowest to highest. The declaration order matters:
 * {@link Enum#compareTo} uses ordinal order, which is why HIGH is
 * declared last — so that {@code HIGH.compareTo(LOW) > 0}, matching
 * our "higher = more important" semantics.</p>
 *
 * <p>Each constant carries a {@code weight} for use in sorting and
 * display, plus a human-readable {@code label}.</p>
 */
public enum Priority {

    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High");

    private final int weight;
    private final String label;

    Priority(int weight, String label) {
        this.weight = weight;
        this.label = label;
    }

    /**
     * Numeric weight — useful when you need an integer for sorting
     * or comparison. Higher = more important.
     */
    public int weight() {
        return weight;
    }

    /** Human-readable label for CLI output. */
    public String label() {
        return label;
    }

    /**
     * Returns true if this priority is at least as urgent as {@code other}.
     * Examples:
     * <pre>
     *   HIGH.isAtLeast(MEDIUM) → true
     *   LOW.isAtLeast(HIGH)    → false
     *   MEDIUM.isAtLeast(MEDIUM) → true
     * </pre>
     */
    //ترجع true إذا كانت هذه الأولوية مساوية أو أعلى من other.
    public boolean isAtLeast(Priority other) {
        return this.weight >= other.weight;
    }
}