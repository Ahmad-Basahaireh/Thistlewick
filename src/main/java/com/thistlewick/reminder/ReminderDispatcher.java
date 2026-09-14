package com.thistlewick.reminder;

import com.thistlewick.domain.Reminder;
import com.thistlewick.domain.event.DomainEvent;
import com.thistlewick.domain.event.TaskCompletedEvent;
import com.thistlewick.domain.event.TaskCreatedEvent;
import com.thistlewick.domain.event.TaskOverdueEvent;
import com.thistlewick.domain.event.TaskRescheduledEvent;
import com.thistlewick.exception.ReminderDispatchException;
import com.thistlewick.reminder.channel.DeliveryChannel;
import com.thistlewick.repository.Repository;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Dispatches due reminders concurrently.
 *
 * <h2>Concurrency design</h2>
 * <ul>
 *   <li><b>ExecutorService</b> with a fixed pool (4 threads) — CPU/IO
 *       mix is dominated by simulated IO, so 4 is a reasonable default.</li>
 *   <li><b>Per-reminder lock</b> ({@link ReentrantLock} keyed on the
 *       reminder id) prevents two workers from firing the same reminder
 *       if the batch contains duplicates.</li>
 *   <li><b>Idempotent firing</b>: {@code Reminder.markFired()} is a
 *       no-op if already fired — a second safety net.</li>
 *   <li><b>Atomic counters</b> for dispatched / failed, so partial
 *       failures are observable without races.</li>
 *   <li><b>Deterministic shutdown</b>: the dispatcher exposes
 *       {@link #shutdown()} that gracefully awaits running tasks.</li>
 * </ul>
 *
 * <h2>What we protect against</h2>
 * <ul>
 *   <li><b>Race #1 — double-dispatch:</b> the same reminder could be
 *       submitted twice if it appears twice in a batch. The per-id lock
 *       plus {@code markFired} idempotency eliminates this.</li>
 *   <li><b>Race #2 — concurrent read/write on the reminder:</b> two
 *       threads reading {@code isDue()} while another marks it fired.
 *       The lock serializes the critical section.</li>
 *   <li><b>Race #3 — counters:</b> {@link AtomicInteger} avoids lost
 *       increments.</li>
 * </ul>
 */
public class ReminderDispatcher {

    private static final int POOL_SIZE = 4;

    private final ReminderFactory reminderFactory;
    private final Repository<Reminder, Long> reminderRepository;
    private final ExecutorService executor;

    private final AtomicInteger dispatched = new AtomicInteger();
    private final AtomicInteger failed     = new AtomicInteger();

    /** Locks keyed by reminder id to serialize firing of the same reminder. */
    private final java.util.concurrent.ConcurrentHashMap<Long, ReentrantLock> locks
            = new java.util.concurrent.ConcurrentHashMap<>();

    public ReminderDispatcher(ReminderFactory reminderFactory,
                              Repository<Reminder, Long> reminderRepository) {
        this.reminderFactory = Objects.requireNonNull(reminderFactory);
        this.reminderRepository = Objects.requireNonNull(reminderRepository);
        this.executor = Executors.newFixedThreadPool(POOL_SIZE, r -> {
            Thread t = new Thread(r, "reminder-dispatcher");
            t.setDaemon(true);   // don't block JVM shutdown
            return t;
        });
    }

    // ------------------------------------------------------------------
    // Public API — the spec-required entry point
    // ------------------------------------------------------------------

    /**
     * Entry point for all lifecycle-derived reminder work.
     *
     * <p>Semantics by event type:</p>
     * <ul>
     *   <li>{@code TaskCreatedEvent}   → nothing (reminders scheduled later).</li>
     *   <li>{@code TaskRescheduledEvent} → regenerate (handled by
     *       {@code ReminderTriggerObserver} at the service layer).</li>
     *   <li>{@code TaskCompletedEvent} → cancel pending reminders.</li>
     *   <li>{@code TaskOverdueEvent}   → fire due reminders now.</li>
     * </ul>
     *
     * <p>This method is the dispatcher's single "signal sink" so observers
     * only need to know one method name.</p>
     */
    public void onLifecycleSignal(DomainEvent event) {
        Objects.requireNonNull(event, "event required");

        if (event instanceof TaskCompletedEvent completed) {
            cancelFor(completed.task().getId());
        } else if (event instanceof TaskOverdueEvent overdue) {
            dispatchBatch(reminderRepository.findAll().stream()
                    .filter(r -> r.getTaskId().equals(overdue.task().getId()))
                    .filter(Reminder::isDue)
                    .toList());
        }
        // TaskCreated / TaskRescheduled are handled elsewhere (observer chain).
    }

    /**
     * Fires a batch of due reminders concurrently.
     *
     * <p>Returns after all submitted tasks complete. Callers see the
     * results via {@link #dispatchedCount()} and {@link #failedCount()}.</p>
     */
    public void dispatchBatch(List<Reminder> dueReminders) {
        List<Future<Void>> futures = dueReminders.stream()
                .map(this::submitSafe)
                .toList();

        for (Future<Void> f : futures) {
            try {
                f.get();   // wait; exceptions are already handled inside
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ignored) {
                // submitSafe already logged and counted failures.
            }
        }
    }

    private Future<Void> submitSafe(Reminder reminder) {
        try {
            return executor.submit(() -> {
                fireOne(reminder);
                return null;   // Callable<Void> يجب أن يُرجع قيمة
            });
        } catch (RejectedExecutionException e) {
            failed.incrementAndGet();
            throw new ReminderDispatchException(
                    "Dispatcher rejected reminder " + reminder.getId(),
                    reminder.getChannel().name(), e);
        }
    }

    /**
     * Critical section: acquire the per-id lock, re-check the reminder's
     * state from the repository, mark fired, persist, and deliver.
     */
    private void fireOne(Reminder reminder) {
        ReentrantLock lock = locks.computeIfAbsent(
                reminder.getId(), k -> new ReentrantLock());

        lock.lock();
        try {
            // Re-read to avoid firing something cancelled in the meantime.
            Reminder current = reminderRepository.findById(reminder.getId())
                    .orElseThrow(() -> new ReminderDispatchException(
                            "Reminder disappeared: " + reminder.getId(),
                            reminder.getChannel().name()));

            if (!current.isDue()) {
                return;   // already fired or cancelled
            }

            // markFired is idempotent → safe even if another thread raced.
            current.markFired();
            reminderRepository.save(current);

            DeliveryChannel channel = reminderFactory.channelFor(current.getChannel());
            channel.deliver(current);

            dispatched.incrementAndGet();

        } catch (ReminderDispatchException e) {
            failed.incrementAndGet();
            throw e;
        } catch (RuntimeException e) {
            failed.incrementAndGet();
            throw new ReminderDispatchException(
                    "Failed to fire reminder " + reminder.getId(),
                    reminder.getChannel().name(), e);
        } finally {
            lock.unlock();
        }
    }

    private void cancelFor(Long taskId) {
        reminderRepository.findAll().stream()
                .filter(r -> r.getTaskId().equals(taskId))
                .forEach(r -> {
                    r.cancel();
                    reminderRepository.save(r);
                });
    }

    // ------------------------------------------------------------------
    // Observability + lifecycle
    // ------------------------------------------------------------------

    public int dispatchedCount() { return dispatched.get(); }
    public int failedCount()     { return failed.get(); }

    /**
     * Gracefully shuts down the executor. Call from {@code Thistlewick.main}
     * or a shutdown hook.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}