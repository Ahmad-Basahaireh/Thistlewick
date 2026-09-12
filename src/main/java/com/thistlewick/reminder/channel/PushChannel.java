package com.thistlewick.reminder.channel;

import com.thistlewick.domain.Reminder;
import com.thistlewick.domain.ReminderChannel;

import java.util.logging.Logger;

/**
 * Simulated push notification delivery. Logs the delivery attempt.
 */
public class PushChannel implements DeliveryChannel {

    private static final Logger LOG = Logger.getLogger(PushChannel.class.getName());

    @Override
    public void deliver(Reminder reminder) {
        LOG.info(() -> String.format(
                "[PUSH]  task=%d reminder=%d at=%s",
                reminder.getTaskId(), reminder.getId(), reminder.getTriggerTime()));
    }

    @Override
    public ReminderChannel channel() {
        return ReminderChannel.PUSH;
    }
}