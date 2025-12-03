
package com.example.pos.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized JDBC helper for PostgreSQL access.
 *
 * Resolution order for configuration (highest priority first):
 * 1. JVM system properties: pos.db.url, pos.db.user, pos.db.password
 * 2. Environment variables or .env entries: POS_DB_URL, POS_DB_USER, POS_DB_PASSWORD
 * 3. Hardcoded defaults
 */
public final class DatabaseConnection {

    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5433/posdb";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "@estheticSQL1";

    private static final Map<String, String> DOT_ENV = loadDotEnv();

    private static final String URL = System.getProperty(
            "pos.db.url",
            envOrDefault("POS_DB_URL", DEFAULT_URL)
    );

    private static final String USER = System.getProperty(
            "pos.db.user",
            envOrDefault("POS_DB_USER", DEFAULT_USER)
    );

    private static final String PASSWORD = System.getProperty(
            "pos.db.password",
            envOrDefault("POS_DB_PASSWORD", DEFAULT_PASSWORD)
    );

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static <T> T executeInTransaction(SQLTransaction<T> action) throws SQLException {
        try (Connection connection = getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = action.apply(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    private static String envOrDefault(String key, String defaultValue) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromDotEnv = DOT_ENV.get(key);
        if (fromDotEnv != null && !fromDotEnv.isBlank()) {
            return fromDotEnv;
        }
        return defaultValue;
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> values = new HashMap<>();
        Path path = Paths.get(".env");
        if (!Files.exists(path)) {
            return values;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int idx = trimmed.indexOf('=');
                String key = trimmed.substring(0, idx).trim();
                String value = trimmed.substring(idx + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException ignored) {
            // Failing to read .env should not break the app; fall back to defaults.
        }
        return values;
    }

    @FunctionalInterface
    public interface SQLTransaction<T> {
        T apply(Connection connection) throws SQLException;
    }
}
