package com.thistlewick.domain;

/**
 * Lifecycle state of a {@link Reminder}.
 *
 * <p>Unlike {@code TaskStatus}, this is a pure state — not derived.
 * A reminder is scheduled, then either fires or is cancelled.</p>
 */
public enum ReminderStatus {

    /** Scheduled and waiting to fire. */
    PENDING,

    /** Successfully delivered. Terminal. */
    FIRED,

    /** Cancelled before firing (e.g. task completed or rescheduled). Terminal. */
    CANCELLED;
    //بدنا نتاكد اذا هي canceled or fired ولا لا
    public boolean isTerminal() {
        return this != PENDING;
    }
}