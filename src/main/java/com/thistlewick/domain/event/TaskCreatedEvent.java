package com.thistlewick.domain.event;

import com.thistlewick.domain.Task;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Published when a new {@link Task} is created and persisted.
 */
public record TaskCreatedEvent(Task task, LocalDateTime occurredAt)
        implements DomainEvent {

    public TaskCreatedEvent {
        Objects.requireNonNull(task, "task required");
        Objects.requireNonNull(occurredAt, "occurredAt required");
    }
}