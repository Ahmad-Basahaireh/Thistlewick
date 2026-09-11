package com.thistlewick.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Loads database configuration from {@code application.properties}
 * and provides a single entry point for obtaining JDBC connections.
 *
 * <p>At this stage we use {@link DriverManager} directly. A connection
 * pool can be introduced later if needed — but for an embedded H2 with
 * a single CLI user, DriverManager is sufficient and easier to reason
 * about.</p>
 */
public final class DatabaseConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DatabaseConfig.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (in == null) {
                throw new IllegalStateException(
                        "application.properties not found on classpath");
            }
            PROPS.load(in);

            Class.forName(PROPS.getProperty("db.driver"));

        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "Failed to load database configuration: " + e.getMessage());
        }
    }

    private DatabaseConfig() {
        // utility class
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPS.getProperty("db.url"),
                PROPS.getProperty("db.username"),
                PROPS.getProperty("db.password")
        );
    }

    public static String get(String key) {
        return PROPS.getProperty(key);
    }
}