package com.thistlewick.algorithm;

import com.thistlewick.domain.Task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Hand-rolled binary search helpers over a list that has already been
 * sorted by {@link DueSoonSorter#reorderByDueDate(List)}.
 *
 * <h2>Why?</h2>
 * <p>Once tasks are ordered by due date, "find the first task due
 * before X" is a classic lower-bound problem — {@code O(log n)}
 * instead of a linear scan.</p>
 *
 * <h2>Complexity</h2>
 * <p>Time: {@code O(log n)}. Space: {@code O(1)}.</p>
 */
public final class BinarySearchUtil {

    private BinarySearchUtil() {}

    /**
     * Returns the index of the first task whose due date is {@code >= threshold},
     * or {@code list.size()} if none. The list MUST be sorted by due date
     * ascending (which {@code reorderByDueDate} guarantees for the primary key).
     *
     * @param list      sorted by dueDate ascending
     * @param threshold the cut-off date
     * @return lower-bound index
     */
    public static int lowerBoundByDueDate(List<Task> list, LocalDateTime threshold) {
        Objects.requireNonNull(list, "list required");
        Objects.requireNonNull(threshold, "threshold required");

        int lo = 0;
        int hi = list.size();   // half-open interval [lo, hi)

        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (list.get(mid).getDueDate().isBefore(threshold)) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    /**
     * Returns the sub-list of tasks due within the given window starting
     * at {@code from} (inclusive) and ending at {@code to} (exclusive).
     * Relies on {@code list} being sorted by due date ascending.
     */
    public static List<Task> dueWithin(List<Task> list,
                                       LocalDateTime from,
                                       LocalDateTime to) {
        int start = lowerBoundByDueDate(list, from);
        int end   = lowerBoundByDueDate(list, to);
        return list.subList(start, end);
    }
}