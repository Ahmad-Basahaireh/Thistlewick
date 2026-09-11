package com.thistlewick.domain;

/**
 * Lifecycle state of a {@link Task}.
 *
 * <h2>Design note: why no OVERDUE here?</h2>
 * <p>An earlier design considered adding {@code OVERDUE} as a fourth
 * constant. We deliberately did <b>not</b>:</p>
 * <ul>
 *   <li>{@code OVERDUE} is a <b>derived</b> state — it depends on
 *       the current time ({@code now > dueDate}) and on the task not
 *       being done.</li>
 *   <li>Storing it as an enum would require a background job to
 *       transition tasks, and would make the persisted state stale
 *       the moment {@code now} ticks past {@code dueDate}.</li>
 *   <li>It is cleaner to ask {@code task.isOverdue()} — the domain
 *       object owns the rule, not the persistence layer.</li>
 * </ul>
 *
 * <p>This is the "state vs. derived state" distinction from DDD.</p>
 */
public enum TaskStatus {

    /** Task created but not yet started. */
    TODO,

    /** Work in progress. */
    IN_PROGRESS,

    /** Task completed successfully. Terminal state. */
    DONE;

    /**
     * @return true if this is a terminal state (no further transitions allowed).
     */
    public boolean isTerminal() {
        return this == DONE;
    }

    /**
     * @return true if work is actively happening.
     */
    public boolean isActive() {
        return this == IN_PROGRESS;
    }
}