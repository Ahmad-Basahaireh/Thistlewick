package com.thistlewick.algorithm;

import com.thistlewick.domain.Email;
import com.thistlewick.domain.Priority;
import com.thistlewick.domain.Task;
import com.thistlewick.domain.User;
import com.thistlewick.patterns.observer.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DueSoonSorterTest {

    private User owner;
    private EventBus bus;

    @BeforeEach
    void setUp() {
        owner = new User(1L, "Alice", new Email("alice@example.com"));
        bus = new EventBus();
    }

    private Task task(long id, LocalDateTime due, Priority p) {
        return new Task(id, owner, "T" + id, "", due, p, bus);
    }

    @Test
    @DisplayName("orders by due date ascending")
    void ordersByDueDate() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0);
        Task later  = task(1, base.plusDays(2), Priority.LOW);
        Task sooner = task(2, base.plusDays(1), Priority.LOW);
        Task mid    = task(3, base.plusDays(1).plusHours(6), Priority.LOW);

        List<Task> sorted = DueSoonSorter.reorderByDueDate(
                new ArrayList<>(List.of(later, sooner, mid)));

        assertEquals(2, sorted.get(0).getId());
        assertEquals(3, sorted.get(1).getId());
        assertEquals(1, sorted.get(2).getId());
    }

    @Test
    @DisplayName("breaks due-date ties by priority descending")
    void breaksTiesByPriority() {
        LocalDateTime same = LocalDateTime.of(2026, 6, 1, 9, 0);
        Task low    = task(1, same, Priority.LOW);
        Task high   = task(2, same, Priority.HIGH);
        Task medium = task(3, same, Priority.MEDIUM);

        List<Task> sorted = DueSoonSorter.reorderByDueDate(
                new ArrayList<>(List.of(low, high, medium)));

        assertEquals(2, sorted.get(0).getId());   // HIGH
        assertEquals(3, sorted.get(1).getId());   // MEDIUM
        assertEquals(1, sorted.get(2).getId());   // LOW
    }

    @Test
    @DisplayName("preserves insertion order on full ties (stability)")
    void stableOnFullTies() {
        LocalDateTime same = LocalDateTime.of(2026, 6, 1, 9, 0);
        Task first  = task(10, same, Priority.HIGH);
        Task second = task(11, same, Priority.HIGH);
        Task third  = task(12, same, Priority.HIGH);

        List<Task> sorted = DueSoonSorter.reorderByDueDate(
                new ArrayList<>(List.of(first, second, third)));

        assertEquals(10, sorted.get(0).getId());
        assertEquals(11, sorted.get(1).getId());
        assertEquals(12, sorted.get(2).getId());
    }

    @Test
    @DisplayName("does not mutate the input list")
    void doesNotMutateInput() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0);
        List<Task> input = new ArrayList<>(List.of(
                task(1, base.plusDays(2), Priority.LOW),
                task(2, base, Priority.LOW)
        ));
        List<Task> before = new ArrayList<>(input);

        DueSoonSorter.reorderByDueDate(input);

        assertEquals(before, input);
    }

    @Test
    @DisplayName("handles empty and single-element lists")
    void handlesEdges() {
        assertTrue(DueSoonSorter.reorderByDueDate(new ArrayList<>()).isEmpty());
        Task only = task(1, LocalDateTime.now(), Priority.LOW);
        assertEquals(1, DueSoonSorter.reorderByDueDate(
                new ArrayList<>(List.of(only))).size());
    }
}