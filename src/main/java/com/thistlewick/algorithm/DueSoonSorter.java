package com.thistlewick.algorithm;

import com.thistlewick.domain.Priority;
import com.thistlewick.domain.Task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Hand-rolled sorting for the "Due Soon" report.
 *
 * <h2>Requirements</h2>
 * <ul>
 *   <li>Order by {@code dueDate} ascending (soonest first).</li>
 *   <li>Ties broken by {@code priority} descending (HIGH before LOW).</li>
 *   <li>Remaining ties broken by {@code id} ascending — a stable,
 *       deterministic final key so the report never reorders between
 *       runs.</li>
 * </ul>
 *
 * <h2>Why Merge Sort?</h2>
 * <p>We chose <b>merge sort</b> for three reasons:</p>
 * <ol>
 *   <li><b>Stable:</b> equal keys preserve insertion order — important
 *       when two tasks have identical {@code dueDate} and
 *       {@code priority}. A stable sort guarantees deterministic
 *       output without a third tie-breaker key.</li>
 *   <li><b>Predictable:</b> worst, average, and best case are all
 *       {@code O(n log n)} — no quadratic cliff on adversarial input
 *       (unlike naive quicksort).</li>
 *   <li><b>Pedagogically clean:</b> it demonstrates divide-and-conquer
 *       and recursion without the partitioning subtleties of quicksort.</li>
 * </ol>
 *
 * <h2>Complexity</h2>
 * <ul>
 *   <li>Time:  {@code O(n log n)} — worst, average, best.</li>
 *   <li>Space: {@code O(n)} — auxiliary array for merging. Not in-place,
 *       but the cost is bounded and acceptable for our scale.</li>
 * </ul>
 *
 * <h2>Constraints honored</h2>
 * <ul>
 *   <li>No {@code Collections.sort}, no {@code Arrays.sort}, no
 *       {@code Comparator}-based sort — implemented from scratch.</li>
 *   <li>The comparator logic is hand-written as a static method
 *       {@link #compare(Task, Task)}.</li>
 * </ul>
 */
public final class DueSoonSorter {

    private DueSoonSorter() {
        // utility class
    }

    /**
     * Entry point required by the spec.
     *
     * <p>Returns a <b>new</b> list; the input is not mutated.</p>
     *
     * @param tasks the tasks to order; must not be null
     * @return a new list ordered by due date, then priority, then id
     */
    public static List<Task> reorderByDueDate(List<Task> tasks) {
        Objects.requireNonNull(tasks, "tasks required");

        // Copy — the caller's list must not be touched.
        Task[] buffer = tasks.toArray(new Task[0]);

        if (buffer.length > 1) {
            mergeSort(buffer, 0, buffer.length - 1, new Task[buffer.length]);
        }

        List<Task> result = new ArrayList<>(buffer.length);
        for (Task t : buffer) {
            result.add(t);
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Merge sort
    // ------------------------------------------------------------------

    /**
     * Recursive divide-and-conquer merge sort.
     *
     * @param arr    the array to sort (mutated in place)
     * @param left   inclusive left index
     * @param right  inclusive right index
     * @param temp   reusable scratch array of the same length
     */
    private static void mergeSort(Task[] arr, int left, int right, Task[] temp) {
        if (left >= right) {
            return;   // single element is trivially sorted
        }
        int mid = left + (right - left) / 2;   // avoids overflow for huge n
        mergeSort(arr, left, mid, temp);
        mergeSort(arr, mid + 1, right, temp);
        merge(arr, left, mid, right, temp);
    }

    /**
     * Merges two adjacent sorted runs: {@code arr[left..mid]} and
     * {@code arr[mid+1..right]}.
     */
    private static void merge(Task[] arr, int left, int mid, int right, Task[] temp) {
        int i = left;       // cursor into left run
        int j = mid + 1;    // cursor into right run
        int k = left;       // cursor into temp

        while (i <= mid && j <= right) {
            // <= keeps the sort STABLE: on tie, take from the left run first.
            if (compare(arr[i], arr[j]) <= 0) {
                temp[k++] = arr[i++];
            } else {
                temp[k++] = arr[j++];
            }
        }
        // Drain remaining elements (only one of these loops actually runs).
        while (i <= mid)   temp[k++] = arr[i++];
        while (j <= right) temp[k++] = arr[j++];

        // Copy merged run back into the original array.
        for (int x = left; x <= right; x++) {
            arr[x] = temp[x];
        }
    }

    // ------------------------------------------------------------------
    // Comparator (hand-written)
    // ------------------------------------------------------------------

    /**
     * Orders tasks by:
     * <ol>
     *   <li>{@code dueDate} ascending (earlier = "sooner" = first),</li>
     *   <li>then {@code priority} descending (HIGH > MEDIUM > LOW),</li>
     *   <li>then {@code id} ascending (deterministic tie-break).</li>
     * </ol>
     *
     * @return negative if {@code a} should come before {@code b},
     *         positive if after, zero if identical on all keys
     */
    static int compare(Task a, Task b) {
        LocalDateTime da = a.getDueDate();
        LocalDateTime db = b.getDueDate();
        int c = da.compareTo(db);
        if (c != 0) return c;

        // Priority: HIGH first → reverse natural order (weight descending).
        int pa = a.getPriority().weight();
        int pb = b.getPriority().weight();
        c = Integer.compare(pb, pa);   // note: reversed
        if (c != 0) return c;

        // Final tie-break by id to guarantee determinism.
        Long ia = a.getId();
        Long ib = b.getId();
        if (ia == null && ib == null) return 0;
        if (ia == null) return  1;   // nulls last
        if (ib == null) return -1;
        return ia.compareTo(ib);
    }
}