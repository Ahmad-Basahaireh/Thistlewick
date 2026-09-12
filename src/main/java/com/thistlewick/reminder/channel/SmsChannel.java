package com.thistlewick.reminder.channel;

import com.thistlewick.domain.Reminder;
import com.thistlewick.domain.ReminderChannel;

import java.util.logging.Logger;

/**
 * Simulated SMS delivery. Logs the delivery attempt.
 */
public class SmsChannel implements DeliveryChannel {

    private static final Logger LOG = Logger.getLogger(SmsChannel.class.getName());

    @Override
    public void deliver(Reminder reminder) {
        LOG.info(() -> String.format(
                "[SMS]   task=%d reminder=%d at=%s",
                reminder.getTaskId(), reminder.getId(), reminder.getTriggerTime()));
    }

    @Override
    public ReminderChannel channel() {
        return ReminderChannel.SMS;
    }
}