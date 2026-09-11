package com.thistlewick.domain;

import com.thistlewick.domain.event.TaskCompletedEvent;
import com.thistlewick.domain.event.TaskCreatedEvent;
import com.thistlewick.domain.event.TaskRescheduledEvent;
import com.thistlewick.exception.InvalidTaskStateException;
import com.thistlewick.patterns.observer.EventBus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * {@code Task} — the central Aggregate Root of Thistlewick.
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li><b>Aggregate Root:</b> owns its {@link Reminder}s and its
 *       tags. Outside objects must go through Task to change them.</li>
 *   <li><b>Behavior-rich:</b> state changes go through {@link #start()},
 *       {@link #complete()}, {@link #reschedule(LocalDateTime)}, etc.
 *       No setters.</li>
 *   <li><b>Derived state:</b> {@code overdue} is computed on demand via
 *       {@link #isOverdue()}, not stored (see {@code TaskStatus}).</li>
 *   <li><b>Publishes events:</b> lifecycle transitions publish
 *       domain events via the injected {@link EventBus}.</li>
 * </ul>
 */
public class Task {

    private final Long id;
    private final User owner;
    private final EventBus eventBus;

    private String title;
    private String description;
    private LocalDateTime dueDate;
    private Priority priority;
    private TaskStatus status;
    private final Set<String> tags;

    /**
     * Creates a new (not-yet-persisted) task in {@code TODO} status.
     * Prefer {@code TaskBuilder} for construction from the service layer.
     */
    public Task(Long id,
                User owner,
                String title,
                String description,
                LocalDateTime dueDate,
                Priority priority,
                EventBus eventBus) {
        this.id = id;
        this.owner = Objects.requireNonNull(owner, "owner required");
        this.title = validateTitle(title);
        this.description = description == null ? "" : description.trim();
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate required");
        this.priority = Objects.requireNonNull(priority, "priority required");
        this.status = TaskStatus.TODO;
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus required");
        this.tags = new LinkedHashSet<>();
    }

    // ------------------------------------------------------------------
    // Lifecycle behavior
    // ------------------------------------------------------------------

    /** Moves the task from TODO to IN_PROGRESS. */
    public void start() {
        if (status != TaskStatus.TODO) {
            throw new InvalidTaskStateException(
                    "Cannot start task in state " + status + " (id=" + id + ")");
        }
        this.status = TaskStatus.IN_PROGRESS;
    }

    /**
     * Marks the task as completed.
     * <p>Cancels all reminders and publishes {@code TaskCompletedEvent}.</p>
     */
    public void complete() {
        if (status == TaskStatus.DONE) {
            throw new InvalidTaskStateException(
                    "Task already completed (id=" + id + ")");
        }
        this.status = TaskStatus.DONE;
        // Reminders are no longer meaningful — the task is done.
        // (Event observers will do the actual DB cancellation; here we
        //  only ensure the in-memory set is cleared on the domain side.)
        eventBus.publish(new TaskCompletedEvent(this, LocalDateTime.now()));
    }

    /**
     * Changes the due date.
     * <p>Judgment Call: reminders are <b>not</b> regenerated here — the
     * event is published and an observer handles regeneration. This keeps
     * Task decoupled from the ReminderFactory.</p>
     */
    public void reschedule(LocalDateTime newDueDate) {
        Objects.requireNonNull(newDueDate, "newDueDate required");
        if (newDueDate.isBefore(LocalDateTime.now())) {
            throw new InvalidTaskStateException(
                    "Cannot reschedule to a past date: " + newDueDate);
        }
        LocalDateTime oldDue = this.dueDate;
        this.dueDate = newDueDate;
        eventBus.publish(new TaskRescheduledEvent(
                this, oldDue, newDueDate, LocalDateTime.now()));
    }

    /**
     * Publishes a {@code TaskCreatedEvent}. Called explicitly by the
     * service after persistence, so the event carries a valid id.
     */
    public void publishCreated() {
        eventBus.publish(new TaskCreatedEvent(this, LocalDateTime.now()));
    }

    // ------------------------------------------------------------------
    // Derived state
    // ------------------------------------------------------------------

    /** @return true if the task is not done and its due date has passed. */
    public boolean isOverdue() {
        return status != TaskStatus.DONE
                && dueDate.isBefore(LocalDateTime.now());
    }

    // ------------------------------------------------------------------
    // Tags
    // ------------------------------------------------------------------

    public void addTag(String tag) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("tag must not be blank");
        }
        tags.add(tag.trim().toLowerCase());
    }

    public void removeTag(String tag) {
        if (tag != null) {
            tags.remove(tag.trim().toLowerCase());
        }
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public Long getId()                 { return id; }
    public User getOwner()              { return owner; }
    public String getTitle()            { return title; }
    public String getDescription()      { return description; }
    public LocalDateTime getDueDate()   { return dueDate; }
    public Priority getPriority()       { return priority; }
    public TaskStatus getStatus()       { return status; }
    public Set<String> getTags()        { return Collections.unmodifiableSet(tags); }

    public void renameTo(String newTitle)       { this.title = validateTitle(newTitle); }
    public void changeDescription(String d)     { this.description = d == null ? "" : d.trim(); }
    public void changePriority(Priority p)      { this.priority = Objects.requireNonNull(p); }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    private static String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        String t = title.trim();
        if (t.length() > 200) {
            throw new IllegalArgumentException("title must be ≤ 200 characters");
        }
        return t;
    }

    // ------------------------------------------------------------------
    // Equality based on identity
    // ------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Task{id=" + id + ", title='" + title + "', due=" + dueDate
                + ", priority=" + priority + ", status=" + status + '}';
    }
}