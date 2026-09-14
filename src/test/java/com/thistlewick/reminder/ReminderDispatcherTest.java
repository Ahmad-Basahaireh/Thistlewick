package com.thistlewick.reminder;

import com.thistlewick.domain.Reminder;
import com.thistlewick.domain.ReminderChannel;
import com.thistlewick.domain.ReminderStatus;
import com.thistlewick.repository.Repository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class ReminderDispatcherTest {

    private ReminderDispatcher dispatcher;
    private InMemoryReminderRepo repo;
    private ReminderFactory factory;

    @BeforeEach
    void setUp() {
        repo = new InMemoryReminderRepo();
        factory = new ReminderFactory();
        dispatcher = new ReminderDispatcher(factory, repo);
    }

    @AfterEach
    void tearDown() {
        dispatcher.shutdown();
    }

    @Test
    @DisplayName("dispatches due reminders concurrently")
    void dispatchesDue() {
        for (long i = 1; i <= 20; i++) {
            repo.save(new Reminder(i, 100L,
                    LocalDateTime.now().minusMinutes(1),
                    ReminderChannel.EMAIL,
                    ReminderStatus.PENDING, null));
        }

        List<Reminder> due = repo.findPendingDue();
        dispatcher.dispatchBatch(due);

        assertEquals(20, dispatcher.dispatchedCount());
        assertEquals(0, dispatcher.failedCount());
    }

    @Test
    @DisplayName("firing is idempotent even with duplicate submissions")
    void idempotentFiring() {
        Reminder r = new Reminder(1L, 100L,
                LocalDateTime.now().minusMinutes(1),
                ReminderChannel.EMAIL,
                ReminderStatus.PENDING, null);
        repo.save(r);

        dispatcher.dispatchBatch(List.of(r, r, r));

        // Only one actual dispatch — duplicates were blocked by state.
        assertEquals(1, dispatcher.dispatchedCount());
    }

    // ------------------------------------------------------------------

    static class InMemoryReminderRepo implements Repository<Reminder, Long> {
        private final ConcurrentHashMap<Long, Reminder> store = new ConcurrentHashMap<>();

        @Override
        public Reminder save(Reminder r) {
            store.put(r.getId(), r);
            return r;
        }

        @Override
        public Optional<Reminder> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Reminder> findAll() {
            return new ArrayList<>(store.values());
        }

        public List<Reminder> findPendingDue() {
            return store.values().stream().filter(Reminder::isDue).toList();
        }

        @Override
        public boolean deleteById(Long id) {
            return store.remove(id) != null;
        }

        @Override
        public long count() {
            return store.size();
        }
    }
}