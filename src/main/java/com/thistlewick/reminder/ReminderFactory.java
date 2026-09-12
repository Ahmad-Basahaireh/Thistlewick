package com.thistlewick.reminder;

import com.thistlewick.domain.Priority;
import com.thistlewick.domain.Reminder;
import com.thistlewick.domain.ReminderChannel;
import com.thistlewick.domain.Task;
import com.thistlewick.reminder.channel.DeliveryChannel;
import com.thistlewick.reminder.channel.EmailChannel;
import com.thistlewick.reminder.channel.PushChannel;
import com.thistlewick.reminder.channel.SmsChannel;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Factory for {@link Reminder} creation and {@link DeliveryChannel}
 * selection.
 *
 * <h2>Two responsibilities, both factory-shaped</h2>
 * <ol>
 *   <li><b>Channel selection:</b> chooses which {@link DeliveryChannel}
 *       to use for a given priority. This is the classic Factory
 *       pattern — callers ask for an abstraction, the factory picks
 *       the concrete implementation.</li>
 *   <li><b>Reminder generation:</b> given a task, produces the list of
 *       reminders that should exist for it. Encapsulates our
 *       "reminder policy" (how many, when, via what).</li>
 * </ol>
 *
 * <h2>Policy (the judgment call companion)</h2>
 * <ul>
 *   <li>HIGH   → 1 day + 1 hour before, via PUSH + EMAIL (two reminders)</li>
 *   <li>MEDIUM → 1 hour before, via EMAIL</li>
 *   <li>LOW    → 15 minutes before, via SMS</li>
 * </ul>
 *
 * <p>This policy is what makes the "reschedule → regenerate reminders"
 * judgment call possible: on reschedule, the same factory produces a
 * fresh set, no state carried over.</p>
 */
public class ReminderFactory {

    private final Map<ReminderChannel, DeliveryChannel> channels;

    public ReminderFactory() {
        this.channels = new EnumMap<>(ReminderChannel.class);
        register(new EmailChannel());
        register(new SmsChannel());
        register(new PushChannel());
    }

    private void register(DeliveryChannel channel) {
        channels.put(channel.channel(), channel);
    }

    /** @throws IllegalArgumentException if no channel is registered for the type */
    public DeliveryChannel channelFor(ReminderChannel type) {
        DeliveryChannel c = channels.get(type);
        if (c == null) {
            throw new IllegalArgumentException("No channel registered: " + type);
        }
        return c;
    }

    /**
     * Chooses the preferred channel for a task based on its priority.
     * (Primary channel only — see {@link #generateFor(Task)} for the
     * full multi-channel set.)
     */
    public ReminderChannel preferredChannelFor(Priority priority) {
        return switch (priority) {
            case HIGH   -> ReminderChannel.PUSH;
            case MEDIUM -> ReminderChannel.EMAIL;
            case LOW    -> ReminderChannel.SMS;
        };
    }

    /**
     * Produces the full list of reminders that should exist for a task,
     * derived from its due date and priority.
     *
     * <p>Each reminder's {@code taskId} is pre-populated so it can be
     * persisted immediately after the task has an id.</p>
     */
    public List<Reminder> generateFor(Task task) {
        Objects.requireNonNull(task, "task required");
        Objects.requireNonNull(task.getId(), "task must be persisted before generating reminders");

        LocalDateTime due = task.getDueDate();

        return switch (task.getPriority()) {
            case HIGH -> List.of(
                    new Reminder(task.getId(), due.minusDays(1).minusHours(1), ReminderChannel.PUSH),
                    new Reminder(task.getId(), due.minusHours(1),               ReminderChannel.EMAIL)
            );
            case MEDIUM -> List.of(
                    new Reminder(task.getId(), due.minusHours(1),               ReminderChannel.EMAIL)
            );
            case LOW -> List.of(
                    new Reminder(task.getId(), due.minusMinutes(15),            ReminderChannel.SMS)
            );
        };
    }
}