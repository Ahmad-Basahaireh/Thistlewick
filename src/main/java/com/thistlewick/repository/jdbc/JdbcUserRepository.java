package com.thistlewick.repository.jdbc;

import com.thistlewick.config.DatabaseConfig;
import com.thistlewick.domain.Email;
import com.thistlewick.domain.User;
import com.thistlewick.exception.DuplicateUserException;
import com.thistlewick.repository.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed {@link Repository} for {@link User}.
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li><b>PreparedStatement everywhere:</b> prevents SQL injection
 *       and lets H2 cache query plans.</li>
 *   <li><b>try-with-resources:</b> closes Connection/Statement/ResultSet
 *       deterministically.</li>
 *   <li><b>Duplicate email:</b> caught at the SQL layer and translated
 *       into {@link DuplicateUserException}. This keeps JDBC details
 *       out of the service layer.</li>
 * </ul>
 */
public class JdbcUserRepository implements Repository<User, Long> {

    private static final String INSERT =
            "INSERT INTO users (name, email) VALUES (?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT id, name, email FROM users WHERE id = ?";

    private static final String SELECT_ALL =
            "SELECT id, name, email FROM users ORDER BY id";

    private static final String UPDATE =
            "UPDATE users SET name = ?, email = ? WHERE id = ?";

    private static final String DELETE =
            "DELETE FROM users WHERE id = ?";

    private static final String COUNT =
            "SELECT COUNT(*) FROM users";

    private static final String SELECT_BY_EMAIL =
            "SELECT id, name, email FROM users WHERE email = ?";

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            return insert(user);
        }
        return update(user);
    }

    private User insert(User user) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail().value());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new User(keys.getLong(1), user.getName(), user.getEmail());
                }
            }
            throw new SQLException("Insert succeeded but no id was returned");

        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                throw new DuplicateUserException(user.getEmail().value());
            }
            throw new IllegalStateException("Failed to insert user", e);
        }
    }

    private User update(User user) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail().value());
            ps.setLong(3, user.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new IllegalStateException(
                        "Update failed: no user with id " + user.getId());
            }
            return user;

        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                throw new DuplicateUserException(user.getEmail().value());
            }
            throw new IllegalStateException("Failed to update user", e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find user by id " + id, e);
        }
    }

    /** Finds a user by email — useful for login and duplicate checks. */
    public Optional<User> findByEmail(Email email) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_EMAIL)) {

            ps.setString(1, email.value());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find user by email", e);
        }
    }

    @Override
    public List<User> findAll() {
        List<User> result = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find all users", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete user " + id, e);
        }
    }

    @Override
    public long count() {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(COUNT);
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to count users", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("name"),
                new Email(rs.getString("email"))
        );
    }

    /**
     * H2 unique-violation SQL state is "23505". We detect it by
     * state rather than by parsing the message (which is locale-dependent).
     */
    private static boolean isUniqueViolation(SQLException e) {
        return "23505".equals(e.getSQLState());
    }
}