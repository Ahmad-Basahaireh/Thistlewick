package com.thistlewick.service;

import com.thistlewick.domain.Email;
import com.thistlewick.domain.User;
import com.thistlewick.exception.DuplicateUserException;
import com.thistlewick.repository.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application service for {@link User} use cases.
 *
 * <p>Enforces the uniqueness invariant on email — the SQL layer
 * already does this via {@code UNIQUE}, but we pre-check for a cleaner
 * error at the service boundary too.</p>
 */
public class UserService {

    private final Repository<User, Long> userRepository;

    public UserService(Repository<User, Long> userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    public User register(String name, Email email) {
        Objects.requireNonNull(email, "email required");

        // Pre-check for a clean, domain-level error before hitting SQL.
        userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst()
                .ifPresent(u -> { throw new DuplicateUserException(email.value()); });

        return userRepository.save(new User(name, email));
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> listAll() {
        return userRepository.findAll();
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }
}