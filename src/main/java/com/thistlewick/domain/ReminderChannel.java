package com.thistlewick.domain;

/**
 * Delivery channel for a {@link Reminder}.
 *
 * <p>This enum is intentionally minimal: it names the channel.
 * The actual sending logic lives in {@code com.thistlewick.reminder.channel}
 * implementations, dispatched via {@code ReminderFactory}.</p>
 */
public enum ReminderChannel {

    /** Email delivery (simulated). */
    EMAIL,

    /** SMS delivery (simulated). */
    SMS,

    /** Push notification (simulated). */
    PUSH
}