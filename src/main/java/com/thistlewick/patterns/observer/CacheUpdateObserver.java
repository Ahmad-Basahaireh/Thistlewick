package com.thistlewick.patterns.observer;

import com.thistlewick.domain.Task;
import com.thistlewick.domain.event.TaskCompletedEvent;
import com.thistlewick.domain.event.TaskCreatedEvent;
import com.thistlewick.domain.event.TaskRescheduledEvent;
import com.thistlewick.repository.Repository;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Observer that keeps the "today's cache" in sync with task lifecycle.
 *
 * <p>When a task is created or rescheduled to today, it is added to
 * the cache. When it is completed, it is removed.</p>
 */
public class CacheUpdateObserver {

    private final Repository<Task, Long> cache;

    public CacheUpdateObserver(Repository<Task, Long> cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    /** Registered for TaskCreatedEvent. */
    public void onCreated(TaskCreatedEvent event) {
        Task t = event.task();
        if (isDueToday(t)) {
            cache.save(t);
        }
    }

    /** Registered for TaskRescheduledEvent. */
    public void onRescheduled(TaskRescheduledEvent event) {
        Task t = event.task();
        if (isDueToday(t)) {
            cache.save(t);
        } else {
            cache.deleteById(t.getId());
        }
    }

    /** Registered for TaskCompletedEvent. */
    public void onCompleted(TaskCompletedEvent event) {
        cache.deleteById(event.task().getId());
    }

    private boolean isDueToday(Task t) {
        return t.getDueDate().toLocalDate().equals(LocalDate.now());
    }
}