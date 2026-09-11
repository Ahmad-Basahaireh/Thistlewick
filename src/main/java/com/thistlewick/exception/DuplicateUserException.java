package com.thistlewick.exception;

/**
 * Thrown when an attempt is made to create a {@code User} whose email
 * is already registered.
 *
 * <p>Email is the natural unique key for users: two users with the
 * same email are the same identity, so this is a genuine conflict.</p>
 */
public class DuplicateUserException extends ThistlewickException {

    private final String email;

    public DuplicateUserException(String email) {
        super("User already exists with email: " + email);
        this.email = email;
    }

    /** @return the email that caused the conflict. */
    public String getEmail() {
        return email;
    }
}