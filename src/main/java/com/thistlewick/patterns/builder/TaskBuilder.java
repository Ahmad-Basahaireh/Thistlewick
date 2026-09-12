package com.thistlewick.patterns.builder;

import com.thistlewick.domain.Priority;
import com.thistlewick.domain.Task;
import com.thistlewick.domain.User;
import com.thistlewick.patterns.observer.EventBus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Fluent Builder for {@link Task}.
 *
 * <h2>Why a Builder?</h2>
 * <ul>
 *   <li><b>Readability:</b> {@code new TaskBuilder().owner(u).title(t)...}
 *       is self-documenting, especially with several optional fields.</li>
 *   <li><b>Validation placement:</b> the builder can enforce that
 *       required fields are set before {@link #build()}.</li>
 *   <li><b>Immutability-friendly:</b> the entity remains free of
 *       telescoping constructors.</li>
 * </ul>
 *
 * <p>Required fields: {@code owner}, {@code title}, {@code dueDate},
 * {@code priority}, {@code eventBus}. Optional: {@code id},
 * {@code description}, {@code tags}.</p>
 */
public class TaskBuilder {

    private Long id;
    private User owner;
    private String title;
    private String description = "";
    private LocalDateTime dueDate;
    private Priority priority;
    private EventBus eventBus;

    public TaskBuilder id(Long id)                 { this.id = id; return this; }
    public TaskBuilder owner(User owner)           { this.owner = owner; return this; }
    public TaskBuilder title(String title)         { this.title = title; return this; }
    public TaskBuilder description(String d)       { this.description = d; return this; }
    public TaskBuilder dueDate(LocalDateTime d)    { this.dueDate = d; return this; }
    public TaskBuilder priority(Priority p)        { this.priority = p; return this; }
    public TaskBuilder eventBus(EventBus e)        { this.eventBus = e; return this; }

    public Task build() {
        Objects.requireNonNull(owner,     "owner is required");
        Objects.requireNonNull(title,     "title is required");
        Objects.requireNonNull(dueDate,   "dueDate is required");
        Objects.requireNonNull(priority,  "priority is required");
        Objects.requireNonNull(eventBus,  "eventBus is required");

        return new Task(id, owner, title, description, dueDate, priority, eventBus);
    }
}