package com.thistlewick.service;

import com.thistlewick.domain.Priority;
import com.thistlewick.domain.Task;
import com.thistlewick.domain.TaskStatus;
import com.thistlewick.repository.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Aggregation / reporting service.
 *
 * <p>Implements at least 3 real Streams aggregations required by the
 * spec, plus a couple of extras that make the CLI reports useful:</p>
 * <ol>
 *   <li>Completed tasks per user this week.</li>
 *   <li>Overdue-task count by priority.</li>
 *   <li>Average time-to-completion (in hours).</li>
 *   <li>Task count per status (extra).</li>
 *   <li>Most-used tags (extra).</li>
 * </ol>
 *
 * <p>All aggregations are pure functions of the input list — they do
 * not mutate state and can be unit-tested in isolation.</p>
 */
public class ReportService {

    private final Repository<Task, Long> taskRepository;

    public ReportService(Repository<Task, Long> taskRepository) {
        this.taskRepository = Objects.requireNonNull(taskRepository);
    }

    // ------------------------------------------------------------------
    // 1) Completed tasks per user — this week
    // ------------------------------------------------------------------

    /**
     * Groups completed tasks (created this week — we approximate
     * "completed this week" by the task's due date because we do not
     * yet persist a completedAt timestamp) by owner id and counts them.
     *
     * @return map: ownerId → number of completed tasks
     */
    public Map<Long, Long> completedTasksPerUserThisWeek() {
        LocalDate weekAgo = LocalDate.now().minusDays(7);

        return taskRepository.findAll().stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .filter(t -> t.getDueDate().toLocalDate().isAfter(weekAgo))
                .collect(Collectors.groupingBy(
                        t -> t.getOwner().getId(),
                        Collectors.counting()
                ));
    }

    // ------------------------------------------------------------------
    // 2) Overdue tasks by priority
    // ------------------------------------------------------------------

    /**
     * Counts currently-overdue tasks (not done, past due) grouped by
     * priority.
     *
     * @return map: priority → count
     */
    public Map<Priority, Long> overdueByPriority() {
        return taskRepository.findAll().stream()
                .filter(Task::isOverdue)
                .collect(Collectors.groupingBy(
                        Task::getPriority,
                        Collectors.counting()
                ));
    }

    // ------------------------------------------------------------------
    // 3) Average time-to-completion
    // ------------------------------------------------------------------

    /**
     * Average number of hours between a task's due date (as a proxy for
     * its completion target) and now, for completed tasks. Because we
     * do not persist a completedAt timestamp yet, we use "now" for
     * completed tasks — which makes this metric reflect "how many hours
     * after their due date they were completed". A future migration
     * would add a real completedAt column.
     *
     * @return average in hours, or empty if no completed tasks
     */
    public Optional<Double> averageTimeToCompletionHours() {
        LocalDateTime now = LocalDateTime.now();

        return taskRepository.findAll().stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .map(t -> Duration.between(t.getDueDate(), now).toMinutes() / 60.0)
                .collect(Collectors.collectingAndThen(
                        Collectors.averagingDouble(Double::doubleValue),
                        avg -> avg > 0 ? Optional.of(avg) : Optional.empty()
                ));
    }

    // ------------------------------------------------------------------
    // 4) Task count by status (extra)
    // ------------------------------------------------------------------

    public Map<TaskStatus, Long> taskCountByStatus() {
        return taskRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Task::getStatus,
                        Collectors.counting()
                ));
    }

    // ------------------------------------------------------------------
    // 5) Most-used tags (extra)
    // ------------------------------------------------------------------

    /**
     * @return the top {@code n} tags by frequency, with their counts.
     */
    public List<Map.Entry<String, Long>> topTags(int n) {
        return taskRepository.findAll().stream()
                .flatMap(t -> t.getTags().stream())
                .collect(Collectors.groupingBy(
                        tag -> tag,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n)
                .toList();
    }
}