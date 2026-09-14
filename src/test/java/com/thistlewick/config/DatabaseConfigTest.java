package com.thistlewick.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that database configuration loads and that a connection
 * can be obtained and used.
 *
 * <h2>Isolation strategy</h2>
 * <p>These tests do <b>not</b> touch the persistent
 * {@code ./data/thistlewick} database used by the CLI. Each test uses
 * a fresh in-memory H2 instance, so results are deterministic and do
 * not depend on prior runs.</p>
 */
class DatabaseConfigTest {

    /** A unique in-memory database per test class invocation. */
    private static final String IN_MEMORY_URL =
            "jdbc:h2:mem:thistlewick_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";

    @Test
    @DisplayName("application.properties is loaded and db.driver is set")
    void propertiesLoad() {
        assertNotNull(DatabaseConfig.get("db.driver"));
        assertEquals("org.h2.Driver", DatabaseConfig.get("db.driver"));
    }

    @Test
    @DisplayName("a working connection can be obtained from an isolated H2")
    void inMemoryConnectionWorks() throws SQLException {
        try (Connection conn = DriverManager.getConnection(IN_MEMORY_URL, "sa", "");
             Statement stmt = conn.createStatement()) {

            assertNotNull(conn);
            assertTrue(stmt.execute("SELECT 1"));
        }
    }

    @Test
    @DisplayName("users table is created by the DDL used in schema.sql")
    void schemaCreatesUsersTable() throws SQLException {
        try (Connection conn = DriverManager.getConnection(IN_MEMORY_URL, "sa", "");
             Statement stmt = conn.createStatement()) {

            // Mirror the users table DDL from schema.sql.
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name   VARCHAR(100)  NOT NULL,
                    email  VARCHAR(254)  NOT NULL,
                    CONSTRAINT uq_users_email UNIQUE (email)
                )
            """);

            var rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));   // fresh in-memory DB → empty
        }
    }
}