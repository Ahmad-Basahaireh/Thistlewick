package com.thistlewick.domain.event;

import com.thistlewick.domain.Task;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Published when a {@link Task} is detected as overdue.
 *
 * <p>Overdue is a derived state; this event is published by an explicit
 * check or a periodic scanner, not by the task's own state transitions.</p>
 */
public record TaskOverdueEvent(Task task, LocalDateTime occurredAt)
        implements DomainEvent {

    public TaskOverdueEvent {
        Objects.requireNonNull(task, "task required");
        Objects.requireNonNull(occurredAt, "occurredAt required");
    }
}