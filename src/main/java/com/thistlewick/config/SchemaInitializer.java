package com.thistlewick.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Executes {@code schema.sql} at application startup.
 *
 * <p>Reads the file from the classpath (it lives in
 * {@code src/main/resources}), splits it into individual statements by
 * semicolon, and executes each. The schema itself uses
 * {@code IF NOT EXISTS}, so this class is safe to run repeatedly.</p>
 *
 * <p><b>Why not use H2's {@code RUNSCRIPT}?</b> Because we want the same
 * code to work if the schema migrates to a different database later.
 * Splitting-and-executing is database-agnostic.</p>
 */
public final class SchemaInitializer {

    private static final String SCHEMA_FILE = "schema.sql";

    private SchemaInitializer() {
        // utility class
    }

    /**
     * Runs the schema. Idempotent.
     */
    public static void initialize() {
        String sql = readSchema();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String statement : splitStatements(sql)) {
                stmt.execute(statement);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to initialize database schema", e);
        }
    }

    private static String readSchema() {
        try (InputStream in = SchemaInitializer.class
                .getClassLoader()
                .getResourceAsStream(SCHEMA_FILE)) {

            if (in == null) {
                throw new IllegalStateException(
                        SCHEMA_FILE + " not found on classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read " + SCHEMA_FILE, e);
        }
    }

    /**
     * Splits the schema into executable statements.
     *
     * <p>Strips SQL line comments ({@code --}) before splitting, so
     * comment text does not confuse the parser. This is a simple
     * implementation adequate for our hand-written schema — a full SQL
     * parser would be overkill.</p>
     */
    private static String[] splitStatements(String sql) {
        StringBuilder cleaned = new StringBuilder();
        for (String line : sql.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            cleaned.append(line).append('\n');
        }
        return cleaned.toString().split(";\\s*");
    }
}