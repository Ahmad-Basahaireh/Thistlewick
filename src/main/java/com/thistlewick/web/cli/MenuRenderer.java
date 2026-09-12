package com.thistlewick.web.cli;

/**
 * Renders the terminal menu and prompt. Pure I/O helper — no logic.
 */
public final class MenuRenderer {

    private MenuRenderer() {}

    public static void printBanner() {
        System.out.println("""
            ============================================
               THISTLEWICK — Task & Reminder Engine
            ============================================
            """);
    }

    public static void printMenu() {
        System.out.println("""
            --------------------------------------------
            1) Create user
            2) Create task
            3) List all tasks
            4) Complete task
            5) Show reports
            6) Detect overdue tasks
            0) Exit
            --------------------------------------------""");
    }

    public static void printError(String msg) {
        System.out.println("✗ " + msg);
    }

    public static void printOk(String msg) {
        System.out.println("✓ " + msg);
    }
}