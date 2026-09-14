package com.thistlewick;

import com.thistlewick.config.SchemaInitializer;
import com.thistlewick.web.cli.CliApplication;

/**
 * Thistlewick — main entry point.
 */
public final class Thistlewick {

    private Thistlewick() {
        // utility
    }

    public static void main(String[] args) {
        // 1. Ensure the schema exists (idempotent).
        SchemaInitializer.initialize();

        // 2. Launch the CLI. The composition root inside CliApplication
        //    wires repositories, observers, and the reminder dispatcher.
        CliApplication app = new CliApplication();
        try {
            app.run();
        } finally {
            app.shutdown();
        }
    }
}