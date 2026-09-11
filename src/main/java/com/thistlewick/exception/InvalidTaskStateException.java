package com.thistlewick.exception;

/**
 * Thrown when a {@code Task} is asked to perform a transition that its
 * current {@link com.thistlewick.domain.TaskStatus} does not allow.
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>Calling {@code complete()} on a task already {@code DONE}.</li>
 *   <li>Calling {@code start()} on a task already {@code IN_PROGRESS}.</li>
 *   <li>Rescheduling a task to a past date.</li>
 * </ul>
 */
public class InvalidTaskStateException extends ThistlewickException {

    public InvalidTaskStateException(String message) {
        super(message);
    }

    public InvalidTaskStateException(String message, Throwable cause) {
        super(message, cause);
    }
}