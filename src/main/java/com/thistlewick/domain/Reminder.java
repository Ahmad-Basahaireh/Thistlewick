package com.thistlewick.domain;

import com.thistlewick.exception.InvalidTaskStateException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * {@code Reminder} — a scheduled notification about a {@link Task}.
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li><b>Entity:</b> has identity ({@code id}) and a lifecycle
 *       (PENDING → FIRED, or PENDING → CANCELLED).</li>
 *   <li><b>Dependent on Task:</b> a Reminder has no meaning without
 *       its parent task. When the task is completed or rescheduled,
 *       the reminder is cancelled or regenerated (see Judgment Call).</li>
 *   <li><b>Idempotent fire:</b> {@link #markFired()} is a no-op if
 *       already fired or cancelled — safe under concurrency.</li>
 * </ul>
 */
public class Reminder {

    private final Long id;
    private final Long taskId;          // reference by id, not object
    private final LocalDateTime triggerTime;
    private final ReminderChannel channel;

    private ReminderStatus status;
    private LocalDateTime firedAt;

    /**
     * Creates a new (not-yet-persisted) reminder in {@code PENDING} state.
     */
    public Reminder(Long taskId,
                    LocalDateTime triggerTime,
                    ReminderChannel channel) {
        this(null, taskId, triggerTime, channel, ReminderStatus.PENDING, null);
    }

    /**
     * Rehydrates a reminder from persistence.
     */
    public Reminder(Long id,
                    Long taskId,
                    LocalDateTime triggerTime,
                    ReminderChannel channel,
                    ReminderStatus status,
                    LocalDateTime firedAt) {
        this.id = id;
        this.taskId = Objects.requireNonNull(taskId, "taskId required");
        this.triggerTime = Objects.requireNonNull(triggerTime, "triggerTime required");
        this.channel = Objects.requireNonNull(channel, "channel required");
        this.status = Objects.requireNonNull(status, "status required");
        this.firedAt = firedAt;
    }

    // ------------------------------------------------------------------
    // Behavior
    // ------------------------------------------------------------------

    /**
     * Marks the reminder as fired. Idempotent:
     * <ul>
     *   <li>If already FIRED → no-op (safe under concurrent dispatch).</li>
     *   <li>If CANCELLED → throws, because firing a cancelled reminder
     *       indicates a logic bug.</li>
     * </ul>
     */
    public void markFired() {
        if (status == ReminderStatus.FIRED) {
            return; // idempotent
        }
        if (status == ReminderStatus.CANCELLED) {
            throw new InvalidTaskStateException(
                    "Cannot fire a cancelled reminder (id=" + id + ")");
        }
        this.status = ReminderStatus.FIRED;
        this.firedAt = LocalDateTime.now();
    }

    /**
     * Cancels the reminder. Idempotent: cancelling a fired reminder is
     * allowed (it simply becomes CANCELLED), cancelling an already
     * cancelled reminder is a no-op.
     */
    public void cancel() {
        if (status == ReminderStatus.CANCELLED) {
            return;
        }
        this.status = ReminderStatus.CANCELLED;
    }

    /** @return true if the reminder is pending and its trigger time has passed. */
    public boolean isDue() {
        return status == ReminderStatus.PENDING
                && !triggerTime.isAfter(LocalDateTime.now());
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public Long getId()                    { return id; }
    public Long getTaskId()                { return taskId; }
    public LocalDateTime getTriggerTime()  { return triggerTime; }
    public ReminderChannel getChannel()    { return channel; }
    public ReminderStatus getStatus()      { return status; }
    public LocalDateTime getFiredAt()      { return firedAt; }

    // ------------------------------------------------------------------
    // Equality based on identity
    // ------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reminder other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Reminder{id=" + id + ", taskId=" + taskId
                + ", at=" + triggerTime + ", via=" + channel
                + ", status=" + status + '}';
    }
}