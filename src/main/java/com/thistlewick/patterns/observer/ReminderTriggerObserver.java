package com.thistlewick.patterns.observer;

import com.thistlewick.domain.Reminder;
import com.thistlewick.domain.event.TaskCompletedEvent;
import com.thistlewick.domain.event.TaskCreatedEvent;
import com.thistlewick.domain.event.TaskRescheduledEvent;
import com.thistlewick.reminder.ReminderFactory;
import com.thistlewick.repository.Repository;

import java.util.List;
import java.util.Objects;

/**
 * Observer that manages the reminder lifecycle for a task.
 *
 * <h2>This is the heart of the Judgment Call</h2>
 * <ul>
 *   <li><b>TaskCreated:</b> generate reminders via {@link ReminderFactory}.</li>
 *   <li><b>TaskRescheduled:</b> cancel all reminders for the task, then
 *       regenerate from the new due date. This keeps the reminders
 *       consistent with the task's current timing invariant.</li>
 *   <li><b>TaskCompleted:</b> cancel all reminders — they would fire
 *       after the task is done, which is meaningless.</li>
 * </ul>
 */
public class ReminderTriggerObserver {

    private final Repository<Reminder, Long> reminderRepository;
    private final ReminderFactory reminderFactory;

    public ReminderTriggerObserver(Repository<Reminder, Long> reminderRepository,
                                   ReminderFactory reminderFactory) {
        this.reminderRepository = Objects.requireNonNull(reminderRepository);
        this.reminderFactory = Objects.requireNonNull(reminderFactory);
    }

    public void onCreated(TaskCreatedEvent event) {
        List<Reminder> reminders = reminderFactory.generateFor(event.task());
        reminders.forEach(reminderRepository::save);
    }

    public void onRescheduled(TaskRescheduledEvent event) {
        cancelAllFor(event.task().getId());
        List<Reminder> regenerated = reminderFactory.generateFor(event.task());
        regenerated.forEach(reminderRepository::save);
    }

    public void onCompleted(TaskCompletedEvent event) {
        cancelAllFor(event.task().getId());
    }

    private void cancelAllFor(Long taskId) {
        // In a full implementation we would call a repo method
        // "findByTaskId" and cancel in a single query. For now we
        // iterate everything — acceptable for our scale.
        reminderRepository.findAll().stream()
                .filter(r -> r.getTaskId().equals(taskId))
                .forEach(r -> {
                    r.cancel();
                    reminderRepository.save(r);
                });
    }
}