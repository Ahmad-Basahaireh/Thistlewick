package com.thistlewick.exception;

/**
 * Thrown when an email string fails validation or normalization.
 *
 * <p>Extends {@link RuntimeException} because invalid email is a
 * programming/input error that callers cannot reasonably recover from
 * mid-flow. If callers need to recover, they can catch this explicitly.</p>
 */
public class InvalidEmailException extends RuntimeException {

    private final String rejectedValue;

    public InvalidEmailException(String message) {
        super(message);
        this.rejectedValue = null;
    }

    public InvalidEmailException(String message, String rejectedValue) {
        super(message);
        this.rejectedValue = rejectedValue;
    }

    /**
     * @return the raw (un-normalized) input that failed validation,
     *         or {@code null} if not provided.
     */
    public String getRejectedValue() {
        return rejectedValue;
    }
}