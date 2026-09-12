package com.thistlewick.repository.jdbc;

import com.thistlewick.config.DatabaseConfig;
import com.thistlewick.domain.Email;
import com.thistlewick.domain.Priority;
import com.thistlewick.domain.Task;
import com.thistlewick.domain.TaskStatus;
import com.thistlewick.domain.User;
import com.thistlewick.exception.TaskNotFoundException;
import com.thistlewick.patterns.observer.EventBus;
import com.thistlewick.repository.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JDBC-backed {@link Repository} for {@link Task}.
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li><b>Owner is loaded eagerly</b> via a JOIN. Tasks are small
 *       and always displayed with owner context.</li>
 *   <li><b>Tags are loaded separately</b> — one extra query per task.
 *       Acceptable for our scale; a batch loader is the next step if
 *       needed.</li>
 *   <li><b>Reads are side-effect-free:</b> mapping uses
 *       {@link Task#rehydrate} so loading tasks never fires lifecycle
 *       events.</li>
 *   <li><b>EventBus is injected</b> so tasks handed out by this repo
 *       can publish events when their state later changes.</li>
 * </ul>
 */
public class JdbcTaskRepository implements Repository<Task, Long> {

    private static final String INSERT =
            "INSERT INTO tasks (owner_id, title, description, due_date, priority, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE =
            "UPDATE tasks SET title = ?, description = ?, due_date = ?, " +
                    "priority = ?, status = ? WHERE id = ?";

    private static final String DELETE =
            "DELETE FROM tasks WHERE id = ?";

    private static final String COUNT =
            "SELECT COUNT(*) FROM tasks";

    /** JOIN with users so we can construct a fully-hydrated Task. */
    private static final String SELECT_BASE =
            "SELECT t.id, t.title, t.description, t.due_date, t.priority, t.status, " +
                    "       u.id AS owner_id, u.name AS owner_name, u.email AS owner_email " +
                    "FROM tasks t JOIN users u ON u.id = t.owner_id ";

    private static final String SELECT_BY_ID = SELECT_BASE + "WHERE t.id = ?";

    private static final String SELECT_ALL = SELECT_BASE + "ORDER BY t.id";

    private static final String SELECT_BY_OWNER =
            SELECT_BASE + "WHERE t.owner_id = ? ORDER BY t.due_date";

    private static final String INSERT_TAG =
            "INSERT INTO task_tags (task_id, tag) VALUES (?, ?)";

    private static final String SELECT_TAGS =
            "SELECT tag FROM task_tags WHERE task_id = ?";

    private static final String DELETE_TAGS =
            "DELETE FROM task_tags WHERE task_id = ?";

    private final EventBus eventBus;

    public JdbcTaskRepository(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    // ------------------------------------------------------------------
    // save (upsert)
    // ------------------------------------------------------------------

    @Override
    public Task save(Task task) {
        if (task.getId() == null) {
            return insert(task);
        }
        return update(task);
    }

    private Task insert(Task task) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement ps = conn.prepareStatement(
                        INSERT, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setLong(1, task.getOwner().getId());
                    ps.setString(2, task.getTitle());
                    ps.setString(3, task.getDescription());
                    ps.setTimestamp(4, Timestamp.valueOf(task.getDueDate()));
                    ps.setString(5, task.getPriority().name());
                    ps.setString(6, task.getStatus().name());
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No id returned");
                        id = keys.getLong(1);
                    }
                }
                insertTags(conn, id, task.getTags());
                conn.commit();

                // Return a fresh Task rehydrated with the generated id.
                return findById(id).orElseThrow(() ->
                        new IllegalStateException("Inserted task " + id + " disappeared"));

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert task", e);
        }
    }

    private Task update(Task task) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
                    ps.setString(1, task.getTitle());
                    ps.setString(2, task.getDescription());
                    ps.setTimestamp(3, Timestamp.valueOf(task.getDueDate()));
                    ps.setString(4, task.getPriority().name());
                    ps.setString(5, task.getStatus().name());
                    ps.setLong(6, task.getId());

                    if (ps.executeUpdate() == 0) {
                        throw new TaskNotFoundException(task.getId());
                    }
                }
                // Replace tags atomically (delete + insert).
                try (PreparedStatement ps = conn.prepareStatement(DELETE_TAGS)) {
                    ps.setLong(1, task.getId());
                    ps.executeUpdate();
                }
                insertTags(conn, task.getId(), task.getTags());
                conn.commit();
                return task;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update task", e);
        }
    }

    private void insertTags(Connection conn, long taskId, Set<String> tags)
            throws SQLException {
        if (tags.isEmpty()) return;
        try (PreparedStatement ps = conn.prepareStatement(INSERT_TAG)) {
            for (String tag : tags) {
                ps.setLong(1, taskId);
                ps.setString(2, tag);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    @Override
    public Optional<Task> findById(Long id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Task task = map(rs);
                loadTags(conn, task);
                return Optional.of(task);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find task " + id, e);
        }
    }

    @Override
    public List<Task> findAll() {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            List<Task> tasks = new ArrayList<>();
            while (rs.next()) {
                Task task = map(rs);
                loadTags(conn, task);
                tasks.add(task);
            }
            return tasks;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find all tasks", e);
        }
    }

    /** Finds all tasks owned by a given user, ordered by due date. */
    public List<Task> findByOwnerId(Long ownerId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_OWNER)) {

            ps.setLong(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Task> tasks = new ArrayList<>();
                while (rs.next()) {
                    Task task = map(rs);
                    loadTags(conn, task);
                    tasks.add(task);
                }
                return tasks;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find tasks by owner", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete task " + id, e);
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
            throw new IllegalStateException("Failed to count tasks", e);
        }
    }

    // ------------------------------------------------------------------
    // Mapping helpers
    // ------------------------------------------------------------------

    /**
     * Maps a row to a Task using {@link Task#rehydrate} — no events are
     * fired during read operations. This is critical: firing
     * {@code TaskCompletedEvent} for every loaded DONE task would flood
     * the EventBus and corrupt cache/reminder state.
     */
    private Task map(ResultSet rs) throws SQLException {
        User owner = new User(
                rs.getLong("owner_id"),
                rs.getString("owner_name"),
                new Email(rs.getString("owner_email"))
        );

        return Task.rehydrate(
                rs.getLong("id"),
                owner,
                rs.getString("title"),
                rs.getString("description"),
                rs.getTimestamp("due_date").toLocalDateTime(),
                Priority.valueOf(rs.getString("priority")),
                TaskStatus.valueOf(rs.getString("status")),
                eventBus
        );
    }

    private void loadTags(Connection conn, Task task) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_TAGS)) {
            ps.setLong(1, task.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    task.addTag(rs.getString("tag"));
                }
            }
        }
    }
}