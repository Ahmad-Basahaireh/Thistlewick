package com.thistlewick.exception;

/**
 * Thrown when a reminder cannot be dispatched (e.g. channel failure,
 * invalid trigger time, or a reminder that has already fired).
 *
 * <p>Used later in {@code ReminderDispatcher} during concurrent
 * dispatch. Callers can inspect {@link #getChannel()} to know which
 * channel failed.</p>
 */
public class ReminderDispatchException extends ThistlewickException {

    private final String channel;

    public ReminderDispatchException(String message, String channel) {
        super(message);
        this.channel = channel;
    }

    public ReminderDispatchException(String message, String channel, Throwable cause) {
        super(message, cause);
        this.channel = channel;
    }

    public String getChannel() {
        return channel;
    }
}