package com.thistlewick.service;

import com.thistlewick.domain.Task;
import com.thistlewick.domain.TaskStatus;
import com.thistlewick.domain.User;
import com.thistlewick.exception.TaskNotFoundException;
import com.thistlewick.patterns.observer.EventBus;
import com.thistlewick.repository.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Application service for {@link Task} use cases.
 *
 * <p>Sits between the CLI (or future web layer) and the repository.
 * Owns cross-cutting behavior: creation with event publication,
 * lookups that translate "not found" into a domain exception, etc.</p>
 *
 * <p><b>Why not put this on Task?</b> Domain objects should not know
 * about repositories, id generation, or event publication timing.
 * The service orchestrates; the entity models.</p>
 */
public class TaskService {

    private final Repository<Task, Long> taskRepository;
    private final EventBus eventBus;

    public TaskService(Repository<Task, Long> taskRepository, EventBus eventBus) {
        this.taskRepository = Objects.requireNonNull(taskRepository);
        this.eventBus = Objects.requireNonNull(eventBus);
    }

    /**
     * Creates and persists a new task, then publishes a
     * {@code TaskCreatedEvent}. The event is published <b>after</b>
     * persistence so observers see a task with an id.
     */
    public Task createTask(Task task) {
        Objects.requireNonNull(task, "task required");
        Task saved = taskRepository.save(task);
        saved.publishCreated();
        return saved;
    }

    public Task getById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    public List<Task> listAll() {
        return taskRepository.findAll();
    }

    public Task update(Task task) {
        Objects.requireNonNull(task, "task required");
        if (task.getId() == null) {
            throw new IllegalArgumentException("cannot update a task without id");
        }
        return taskRepository.save(task);
    }

    public void delete(Long id) {
        if (!taskRepository.deleteById(id)) {
            throw new TaskNotFoundException(id);
        }
    }

    /** Marks a task as completed and persists the change. */
    public void completeTask(Long id) {
        Task task = getById(id);
        task.complete();
        taskRepository.save(task);
    }

    /** Starts a task (TODO → IN_PROGRESS) and persists. */
    public void startTask(Long id) {
        Task task = getById(id);
        task.start();
        taskRepository.save(task);
    }

    /**
     * Reschedules a task. The judgment-call observer reacts to the
     * resulting {@code TaskRescheduledEvent} by regenerating reminders.
     */
    public void rescheduleTask(Long id, LocalDateTime newDueDate) {
        Task task = getById(id);
        task.reschedule(newDueDate);
        taskRepository.save(task);
    }

    /**
     * Scans all tasks and marks overdue ones (non-done, past due) by
     * publishing {@code TaskOverdueEvent}. Does not mutate status:
     * overdue is derived (see {@code TaskStatus}).
     */
    public int detectOverdue() {
        List<Task> overdue = taskRepository.findAll().stream()
                .filter(Task::isOverdue)
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .toList();

        overdue.forEach(t -> eventBus.publish(
                new com.thistlewick.domain.event.TaskOverdueEvent(t, LocalDateTime.now())));

        return overdue.size();
    }
}