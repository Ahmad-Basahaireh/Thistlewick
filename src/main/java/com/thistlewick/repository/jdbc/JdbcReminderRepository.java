package com.thistlewick.repository.jdbc;

import com.thistlewick.config.DatabaseConfig;
import com.thistlewick.domain.Reminder;
import com.thistlewick.domain.ReminderChannel;
import com.thistlewick.domain.ReminderStatus;
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

/**
 * JDBC-backed {@link Repository} for {@link Reminder}.
 */
public class JdbcReminderRepository implements Repository<Reminder, Long> {

    private static final String INSERT =
            "INSERT INTO reminders (task_id, trigger_time, channel, status, fired_at) " +
                    "VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE =
            "UPDATE reminders SET trigger_time = ?, channel = ?, status = ?, fired_at = ? " +
                    "WHERE id = ?";

    private static final String DELETE =
            "DELETE FROM reminders WHERE id = ?";

    private static final String COUNT =
            "SELECT COUNT(*) FROM reminders";

    private static final String SELECT_BASE =
            "SELECT id, task_id, trigger_time, channel, status, fired_at FROM reminders ";

    private static final String SELECT_BY_ID = SELECT_BASE + "WHERE id = ?";

    private static final String SELECT_ALL = SELECT_BASE + "ORDER BY trigger_time";

    private static final String SELECT_PENDING_DUE =
            SELECT_BASE +
                    "WHERE status = 'PENDING' AND trigger_time <= CURRENT_TIMESTAMP " +
                    "ORDER BY trigger_time";

    private static final String SELECT_BY_TASK =
            SELECT_BASE + "WHERE task_id = ? ORDER BY trigger_time";

    @Override
    public Reminder save(Reminder reminder) {
        if (reminder.getId() == null) return insert(reminder);
        return update(reminder);
    }

    private Reminder insert(Reminder r) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, r.getTaskId());
            ps.setTimestamp(2, Timestamp.valueOf(r.getTriggerTime()));
            ps.setString(3, r.getChannel().name());
            ps.setString(4, r.getStatus().name());
            if (r.getFiredAt() != null) {
                ps.setTimestamp(5, Timestamp.valueOf(r.getFiredAt()));
            } else {
                ps.setNull(5, java.sql.Types.TIMESTAMP);
            }
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Reminder(
                            keys.getLong(1), r.getTaskId(), r.getTriggerTime(),
                            r.getChannel(), r.getStatus(), r.getFiredAt());
                }
            }
            throw new SQLException("No id returned");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert reminder", e);
        }
    }

    private Reminder update(Reminder r) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {

            ps.setTimestamp(1, Timestamp.valueOf(r.getTriggerTime()));
            ps.setString(2, r.getChannel().name());
            ps.setString(3, r.getStatus().name());
            if (r.getFiredAt() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(r.getFiredAt()));
            } else {
                ps.setNull(4, java.sql.Types.TIMESTAMP);
            }
            ps.setLong(5, r.getId());

            if (ps.executeUpdate() == 0) {
                throw new IllegalStateException(
                        "Update failed: no reminder with id " + r.getId());
            }
            return r;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update reminder", e);
        }
    }

    @Override
    public Optional<Reminder> findById(Long id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find reminder " + id, e);
        }
    }

    @Override
    public List<Reminder> findAll() {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            List<Reminder> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find all reminders", e);
        }
    }

    /** All pending reminders whose trigger time is now or in the past. */
    public List<Reminder> findPendingDue() {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_PENDING_DUE);
             ResultSet rs = ps.executeQuery()) {

            List<Reminder> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find pending reminders", e);
        }
    }

    /** All reminders attached to a task. */
    public List<Reminder> findByTaskId(Long taskId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_TASK)) {

            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reminder> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find reminders by task", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete reminder " + id, e);
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
            throw new IllegalStateException("Failed to count reminders", e);
        }
    }

    private Reminder map(ResultSet rs) throws SQLException {
        Timestamp fired = rs.getTimestamp("fired_at");
        return new Reminder(
                rs.getLong("id"),
                rs.getLong("task_id"),
                rs.getTimestamp("trigger_time").toLocalDateTime(),
                ReminderChannel.valueOf(rs.getString("channel")),
                ReminderStatus.valueOf(rs.getString("status")),
                fired == null ? null : fired.toLocalDateTime()
        );
    }
}