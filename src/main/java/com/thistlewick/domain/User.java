package com.thistlewick.domain;

import java.util.Objects;

/**
 *  User — an Aggregate Root in (DDD),
 *  It acts as the primary object that controls a group of related objects.
 *  representing a Thistlewick user.
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li><b>Identity:</b> identified by {@code id}, not by attributes.
 *       Two users with the same id are the same user even if the name
 *       differs.</li>
 *   <li><b>Email:</b> modeled as a {@link Email} Value Object, not a
 *       raw String. This pushes validation to the boundary and makes
 *       every User instance guaranteed-valid.</li>
 *   <li><b>Behavior:</b> mutation happens through methods like
 *       {@link #renameTo(String)} and {@link #changeEmail(Email)},
 *       never through setters.</li>
 * </ul>
 */
public class User {
    ////Long not long: null حتى يتم الحفظ في قاعدة البيانات
    private final Long id;       // null until persisted
    private String name;
    private Email email;

    /**
     * Creates a new (not-yet-persisted) user. The id will be assigned
     * by the repository on insert.
     */
    public User(String name, Email email) {
        this(null, name, email);
    }

    /**
     * تعيد بناء مستخدم موجود. Used by repositories only.
     */
    public User(Long id, String name, Email email) {
        this.id = id;
        this.name = validateName(name);
        this.email = Objects.requireNonNull(email, "email required");
    }

    // ------------------------------------------------------------------
    // Behavior
    // ------------------------------------------------------------------

    /** Renames the user. Name must be non-blank. */
    public void renameTo(String newName) {
        this.name = validateName(newName);
    }

    /** Changes the user's email. Guaranteed valid because Email is a Value Object. */
    public void changeEmail(Email newEmail) {
        this.email = Objects.requireNonNull(newEmail, "newEmail required");
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        //the name without spaces
        String trimmed = name.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("name must be ≤ 100 characters");
        }
        return trimmed;
    }

    // ------------------------------------------------------------------
    // Accessors (only where truly needed)
    // ------------------------------------------------------------------

    public Long getId()      { return id; }
    //we can delete this part
    public String getName()  { return name; }
    public Email getEmail()  { return email; }

    /** @return true if this user has been persisted (has an id). */
    public boolean isPersisted() {
        return id != null;
    }

    // ------------------------------------------------------------------
    // Equality: based on identity (id) when persisted, else on email
    // ------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        //Same reference → immediately true.
        if (this == o) return true;
        //Not User → false
        if (!(o instanceof User other)) return false;
        //Both have an ID → compare using the ID.
        if (id != null && other.id != null) return id.equals(other.id);
        //Otherwise (one or both lacking an ID) → compare using the email.
        return email.equals(other.email);
    }

    @Override
    public int hashCode() {
//        return id != null ? Objects.hash(id) : Objects.hash(email);
          return id != null ? id.hashCode() : email.hashCode();
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email=" + email + '}';
    }
}