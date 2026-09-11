package com.thistlewick.domain.event;

import com.thistlewick.domain.Task;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Published when a {@link Task} transitions to {@code DONE}.
 */
public record TaskCompletedEvent(Task task, LocalDateTime occurredAt)
        implements DomainEvent {

    public TaskCompletedEvent {
        Objects.requireNonNull(task, "task required");
        Objects.requireNonNull(occurredAt, "occurredAt required");
    }
}