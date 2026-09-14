package com.thistlewick.web.cli;

import com.thistlewick.domain.Email;
import com.thistlewick.domain.Priority;
import com.thistlewick.domain.Reminder;
import com.thistlewick.domain.Task;
import com.thistlewick.domain.User;
import com.thistlewick.patterns.builder.TaskBuilder;
import com.thistlewick.patterns.observer.EventBus;
import com.thistlewick.patterns.observer.LoggingObserver;
import com.thistlewick.reminder.ReminderDispatcher;
import com.thistlewick.reminder.ReminderFactory;
import com.thistlewick.repository.Repository;
import com.thistlewick.repository.jdbc.JdbcReminderRepository;
import com.thistlewick.repository.jdbc.JdbcTaskRepository;
import com.thistlewick.repository.jdbc.JdbcUserRepository;
import com.thistlewick.service.ReportService;
import com.thistlewick.service.TaskService;
import com.thistlewick.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Terminal CLI — presentation layer. Owns wiring (composition root).
 */
public class CliApplication {

    private final Scanner scanner = new Scanner(System.in);
    private final UserService userService;
    private final TaskService taskService;
    private final ReportService reportService;
    private final ReminderDispatcher dispatcher;

    public CliApplication() {
        // --- Composition root ---
        EventBus eventBus = new EventBus();
        eventBus.register(com.thistlewick.domain.event.TaskCreatedEvent.class,
                new LoggingObserver()::onEvent);
        eventBus.register(com.thistlewick.domain.event.TaskCompletedEvent.class,
                new LoggingObserver()::onEvent);
        eventBus.register(com.thistlewick.domain.event.TaskOverdueEvent.class,
                new LoggingObserver()::onEvent);
        eventBus.register(com.thistlewick.domain.event.TaskRescheduledEvent.class,
                new LoggingObserver()::onEvent);

        Repository<User, Long> userRepo = new JdbcUserRepository();
        Repository<Task, Long> taskRepo = new JdbcTaskRepository(eventBus);
        Repository<Reminder, Long> reminderRepo = new JdbcReminderRepository();

        ReminderFactory reminderFactory = new ReminderFactory();
        this.dispatcher = new ReminderDispatcher(reminderFactory, reminderRepo);

        // Route lifecycle signals to the dispatcher.
        eventBus.register(com.thistlewick.domain.event.TaskOverdueEvent.class,
                dispatcher::onLifecycleSignal);
        eventBus.register(com.thistlewick.domain.event.TaskCompletedEvent.class,
                dispatcher::onLifecycleSignal);

        this.userService   = new UserService(userRepo);
        this.taskService   = new TaskService(taskRepo, eventBus);
        this.reportService = new ReportService(taskRepo);
    }

    public void run() {
        MenuRenderer.printBanner();
        boolean running = true;
        while (running) {
            MenuRenderer.printMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> createUser();
                    case "2" -> createTask();
                    case "3" -> listTasks();
                    case "4" -> completeTask();
                    case "5" -> showReports();
                    case "6" -> detectOverdue();
                    case "0" -> running = false;
                    default  -> MenuRenderer.printError("Unknown option: " + choice);
                }
            } catch (RuntimeException e) {
                MenuRenderer.printError(e.getMessage());
            }
        }
        System.out.println("Bye.");
    }

    public void shutdown() {
        dispatcher.shutdown();
    }

    private void createUser() {
        System.out.print("Name:  ");
        String name = scanner.nextLine();
        System.out.print("Email: ");
        String emailStr = scanner.nextLine();
        User u = userService.register(name, new Email(emailStr));
        MenuRenderer.printOk("User created: id=" + u.getId());
    }

    private void createTask() {
        List<User> users = userService.listAll();
        if (users.isEmpty()) {
            MenuRenderer.printError("No users yet — create one first.");
            return;
        }
        users.forEach(u -> System.out.printf("  [%d] %s <%s>%n",
                u.getId(), u.getName(), u.getEmail().value()));   // ← value() بدل toString

        System.out.print("Owner id: ");
        long ownerId = Long.parseLong(scanner.nextLine().trim());
        User owner = userService.getById(ownerId);

        System.out.print("Title: ");
        String title = scanner.nextLine();

        System.out.print("Description (optional): ");
        String description = scanner.nextLine();

        System.out.print("Due (YYYY-MM-DDTHH:MM): ");
        LocalDateTime due = LocalDateTime.parse(scanner.nextLine().trim());

        System.out.print("Priority (LOW/MEDIUM/HIGH): ");
        Priority p = Priority.valueOf(scanner.nextLine().trim().toUpperCase());

        System.out.print("Tags (comma-separated, optional): ");
        String tagsLine = scanner.nextLine();

        Task task = new TaskBuilder()
                .owner(owner)
                .title(title)
                .description(description)
                .dueDate(due)
                .priority(p)
                .eventBus(new EventBus())
                .build();

        // أضف الـ tags
        if (!tagsLine.isBlank()) {
            for (String tag : tagsLine.split(",")) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    task.addTag(trimmed);
                }
            }
        }

        Task saved = taskService.createTask(task);
        MenuRenderer.printOk("Task created: id=" + saved.getId()
                + " (description and tags saved)");
    }

    private void listTasks() {
        List<Task> tasks = taskService.listAll();
        if (tasks.isEmpty()) {
            System.out.println("  (no tasks)");
            return;
        }
        tasks.forEach(t -> System.out.printf(
                "  [%d] %-20s due=%s prio=%s status=%s%n",
                t.getId(), t.getTitle(), t.getDueDate(), t.getPriority(), t.getStatus()));
    }

    private void completeTask() {
        System.out.print("Task id: ");
        long id = Long.parseLong(scanner.nextLine().trim());
        taskService.completeTask(id);
        MenuRenderer.printOk("Task " + id + " completed.");
    }

    private void showReports() {
        System.out.println("\n--- Overdue by priority ---");
        Map<Priority, Long> overdue = reportService.overdueByPriority();
        overdue.forEach((p, c) -> System.out.printf("  %-8s %d%n", p, c));

        System.out.println("\n--- Task count by status ---");
        reportService.taskCountByStatus()
                .forEach((s, c) -> System.out.printf("  %-12s %d%n", s, c));

        System.out.println("\n--- Completed per user (this week) ---");
        reportService.completedTasksPerUserThisWeek()
                .forEach((uid, c) -> System.out.printf("  user %d → %d%n", uid, c));

        System.out.println("\n--- Average time-to-completion (hours) ---");
        reportService.averageTimeToCompletionHours()
                .ifPresentOrElse(
                        avg -> System.out.printf("  %.2f h%n", avg),
                        () -> System.out.println("  (no completed tasks)"));
    }

    private void detectOverdue() {
        int n = taskService.detectOverdue();
        MenuRenderer.printOk("Detected " + n + " overdue task(s).");
    }
}