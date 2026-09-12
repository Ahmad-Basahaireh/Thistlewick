package com.thistlewick.reminder.channel;

import com.thistlewick.domain.Reminder;
import com.thistlewick.domain.ReminderChannel;

/**
 * Strategy for delivering a reminder.
 *
 * <p>Implementations simulate delivery (no real Email/SMS/Push) but log
 * enough to prove the pattern is wired correctly.</p>
 */
public interface DeliveryChannel {

    /**
     * Delivers the given reminder.
     *
     * @throws com.thistlewick.exception.ReminderDispatchException on failure
     */
    void deliver(Reminder reminder);

    /** @return which {@link ReminderChannel} this implementation handles. */
    ReminderChannel channel();
}