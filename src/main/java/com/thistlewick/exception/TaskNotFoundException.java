package com.thistlewick.exception;

/**
 * Thrown when a task lookup by id fails.
 *
 * <p>This is distinct from {@link InvalidTaskStateException}: here the
 * task does not exist at all, while there it exists but is in the
 * wrong state for the requested operation.</p>
 */
public class TaskNotFoundException extends ThistlewickException {

    private final Long taskId;

    public TaskNotFoundException(Long taskId) {
        super("Task not found with id: " + taskId);
        this.taskId = taskId;
    }

    public Long getTaskId() {
        return taskId;
    }
}