package com.thistlewick.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that database configuration loads and a connection can be
 * obtained and used against the embedded H2 instance.
 *
 * <p>Uses an in-memory H2 override so tests are fast and isolated.</p>
 */
class DatabaseConfigTest {

    @Test
    @DisplayName("obtains a working connection from DatabaseConfig")
    void connectionWorks() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            assertNotNull(conn);
            assertTrue(stmt.execute("SELECT 1"));
        }
    }

    @Test
    @DisplayName("schema initialization is idempotent")
    void schemaInitRunsTwice() {
        assertDoesNotThrow(() -> {
            SchemaInitializer.initialize();
            SchemaInitializer.initialize();   // must not fail
        });
    }

    @Test
    @DisplayName("users table exists after initialization")
    void usersTableExists() throws SQLException {
        SchemaInitializer.initialize();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            var rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }
}