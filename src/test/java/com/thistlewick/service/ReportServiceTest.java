package com.thistlewick.service;

import com.thistlewick.domain.Email;
import com.thistlewick.domain.Priority;
import com.thistlewick.domain.Task;
import com.thistlewick.domain.TaskStatus;
import com.thistlewick.domain.User;
import com.thistlewick.patterns.observer.EventBus;
import com.thistlewick.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {

    private InMemoryTaskRepo repo;
    private ReportService reports;
    private User alice;

    @BeforeEach
    void setUp() {
        repo = new InMemoryTaskRepo();
        reports = new ReportService(repo);
        alice = new User(1L, "Alice", new Email("alice@example.com"));
    }

    @Test
    @DisplayName("overdueByPriority groups correctly")
    void overdueByPriorityWorks() {
        Task t1 = overdueTask(1L, Priority.HIGH);
        Task t2 = overdueTask(2L, Priority.HIGH);
        Task t3 = overdueTask(3L, Priority.LOW);
        repo.save(t1); repo.save(t2); repo.save(t3);

        Map<Priority, Long> result = reports.overdueByPriority();

        assertEquals(2L, result.get(Priority.HIGH));
        assertEquals(1L, result.get(Priority.LOW));
    }

    @Test
    @DisplayName("taskCountByStatus groups correctly")
    void statusCountWorks() {
        Task todo = makeTask(1L, Priority.LOW, LocalDateTime.now().plusDays(5));
        repo.save(todo);
        Task done = makeTask(2L, Priority.LOW, LocalDateTime.now().plusDays(5));
        done.complete();
        repo.save(done);

        Map<TaskStatus, Long> result = reports.taskCountByStatus();

        assertEquals(1L, result.get(TaskStatus.TODO));
        assertEquals(1L, result.get(TaskStatus.DONE));
    }

    @Test
    @DisplayName("topTags returns ordered entries")
    void topTagsWorks() {
        Task t1 = makeTask(1L, Priority.LOW, LocalDateTime.now().plusDays(5));
        t1.addTag("urgent"); t1.addTag("work");
        Task t2 = makeTask(2L, Priority.LOW, LocalDateTime.now().plusDays(5));
        t2.addTag("urgent");
        repo.save(t1); repo.save(t2);

        var top = reports.topTags(2);

        assertEquals("urgent", top.get(0).getKey());
        assertEquals(2L, top.get(0).getValue());
    }

    @Test
    @DisplayName("averageTimeToCompletionHours returns empty when no done tasks")
    void avgEmptyWhenNoDone() {
        repo.save(makeTask(1L, Priority.LOW, LocalDateTime.now().plusDays(5)));
        Optional<Double> avg = reports.averageTimeToCompletionHours();
        assertTrue(avg.isEmpty());
    }

    // ------------------------------------------------------------------
    // Test helpers
    // ------------------------------------------------------------------

    private Task overdueTask(Long id, Priority p) {
        return makeTask(id, p, LocalDateTime.now().minusDays(2));
    }

    private Task makeTask(Long id, Priority p, LocalDateTime due) {
        return new Task(id, alice, "Task " + id, "", due, p, new EventBus());
    }

    /**
     * Minimal in-memory repository for tests — keeps ReportServiceTest
     * independent of JDBC and H2.
     */
    static class InMemoryTaskRepo implements Repository<Task, Long> {
        private final List<Task> data = new ArrayList<>();

        @Override
        public Task save(Task task) {
            data.removeIf(t -> t.getId().equals(task.getId()));
            data.add(task);
            return task;
        }

        @Override
        public Optional<Task> findById(Long id) {
            return data.stream().filter(t -> t.getId().equals(id)).findFirst();
        }

        @Override
        public List<Task> findAll() { return new ArrayList<>(data); }

        @Override
        public boolean deleteById(Long id) {
            return data.removeIf(t -> t.getId().equals(id));
        }

        @Override
        public long count() { return data.size(); }
    }
}