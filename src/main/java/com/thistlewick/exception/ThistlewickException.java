package com.thistlewick.exception;

/**
 * Base class for all application-specific exceptions in Thistlewick.
 *
 * <h2>Why a hierarchy?</h2>
 * <ul>
 *   <li><b>Catch-all:</b> {@code catch (ThistlewickException e)} catches
 *       all app-specific errors without catching unrelated JDK exceptions.</li>
 *   <li><b>Semantic clarity:</b> each subclass names a real domain or
 *       infrastructure concern (e.g. invalid state, duplicate user).</li>
 *   <li><b>Centralized error codes (optional):</b> a base class is the
 *       natural home for future additions like {@code errorCode}.</li>
 * </ul>
 *
 * <p>Extends {@link RuntimeException} because application errors are
 * typically not recoverable mid-flow, and forcing callers to declare
 * {@code throws} on every method leads to noisy signatures.</p>
 */
public class ThistlewickException extends RuntimeException {

    public ThistlewickException(String message) {
        super(message);
    }

    public ThistlewickException(String message, Throwable cause) {
        super(message, cause);
    }
}