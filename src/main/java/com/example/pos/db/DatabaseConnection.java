
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
 * Centralized JDBC helper for PostgreSQL access with automatic fallback.
 * 
 * Features:
 * - Dual database support (Online/Offline)
 * - Automatic fallback to local database if online is slow/unavailable
 * - Connection timeout detection
 * - Performance monitoring
 *
 * Resolution order for configuration (highest priority first):
 * 1. JVM system properties: pos.db.url, pos.db.user, pos.db.password
 * 2. Environment variables or .env entries: POS_DB_URL, POS_DB_USER, POS_DB_PASSWORD
 * 3. Hardcoded defaults
 */
public final class DatabaseConnection {

    // Online Database (Neon PostgreSQL)
    private static final String ONLINE_URL_KEY = "POS_DB_URL";
    private static final String ONLINE_USER_KEY = "POS_DB_USER";
    private static final String ONLINE_PASSWORD_KEY = "POS_DB_PASSWORD";

    // Local Database (Fallback)
    private static final String LOCAL_URL = "jdbc:postgresql://localhost:5433/posdb";
    private static final String LOCAL_USER = "postgres";
    private static final String LOCAL_PASSWORD = "@estheticSQL1";

    // Connection timeout in milliseconds (3 seconds)
    private static final int CONNECTION_TIMEOUT_MS = 3000;

    private static final Map<String, String> DOT_ENV = loadDotEnv();
    
    // Track which database is currently active
    private static volatile boolean useLocalDatabase = false;
    private static volatile boolean hasTestedOnline = false;
    private static volatile long lastOnlineCheckTime = 0;
    private static final long ONLINE_RECHECK_INTERVAL_MS = 60000; // 1 minute

    private DatabaseConnection() {
    }

    /**
     * Get a database connection with automatic fallback and connection pooling.
     * Tries online database first, falls back to local if unavailable or slow.
     */
    public static Connection getConnection() throws SQLException {
        // Initialize pool if not done yet
        if (!ConnectionPool.isInitialized()) {
            initializeConnectionPool();
        }
        
        // Get connection from pool
        return ConnectionPool.getConnection();
    }
    
    /**
     * Initialize connection pool with appropriate database
     */
    private static synchronized void initializeConnectionPool() {
        if (ConnectionPool.isInitialized()) {
            return;
        }
        
        // Try online first
        String onlineUrl = getOnlineUrl();
        String onlineUser = getOnlineUser();
        String onlinePassword = getOnlinePassword();
        
        if (onlineUrl != null && !onlineUrl.isEmpty()) {
            try {
                // Test online connection
                long startTime = System.currentTimeMillis();
                DriverManager.setLoginTimeout(CONNECTION_TIMEOUT_MS / 1000);
                Connection testConn = DriverManager.getConnection(onlineUrl, onlineUser, onlinePassword);
                long duration = System.currentTimeMillis() - startTime;
                testConn.close();
                
                if (duration < CONNECTION_TIMEOUT_MS) {
                    // Online is fast, use it
                    ConnectionPool.initialize(onlineUrl, onlineUser, onlinePassword);
                    useLocalDatabase = false;
                    System.out.println("✓ Using online database with connection pool (" + duration + "ms)");
                    return;
                }
            } catch (SQLException e) {
                System.err.println("⚠ Online database unavailable: " + e.getMessage());
            }
        }
        
        // Fall back to local
        try {
            ConnectionPool.initialize(LOCAL_URL, LOCAL_USER, LOCAL_PASSWORD);
            useLocalDatabase = true;
            System.out.println("✓ Using local database with connection pool");
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize local database: " + e.getMessage());
            throw new RuntimeException("Cannot initialize database connection", e);
        }
    }

    /**
     * Get connection to online database (Neon PostgreSQL)
     */
    private static Connection getOnlineConnection() throws SQLException {
        String url = getOnlineUrl();
        String user = getOnlineUser();
        String password = getOnlinePassword();

        if (url == null || url.isEmpty()) {
            throw new SQLException("Online database URL not configured");
        }

        long startTime = System.currentTimeMillis();
        
        try {
            // Set connection timeout
            DriverManager.setLoginTimeout(CONNECTION_TIMEOUT_MS / 1000);
            Connection conn = DriverManager.getConnection(url, user, password);
            
            long duration = System.currentTimeMillis() - startTime;
            
            // If connection took too long, switch to local
            if (duration > CONNECTION_TIMEOUT_MS) {
                conn.close();
                throw new SQLException("Connection too slow: " + duration + "ms");
            }
            
            if (!hasTestedOnline) {
                System.out.println("✓ Connected to online database (" + duration + "ms)");
                hasTestedOnline = true;
            }
            
            return conn;
        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("✗ Online database connection failed (" + duration + "ms): " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get connection to local database
     */
    private static Connection getLocalConnection() throws SQLException {
        long startTime = System.currentTimeMillis();
        
        try {
            Connection conn = DriverManager.getConnection(LOCAL_URL, LOCAL_USER, LOCAL_PASSWORD);
            long duration = System.currentTimeMillis() - startTime;
            
            if (!hasTestedOnline || useLocalDatabase) {
                System.out.println("✓ Connected to local database (" + duration + "ms)");
            }
            
            return conn;
        } catch (SQLException e) {
            System.err.println("✗ Local database connection failed: " + e.getMessage());
            System.err.println("→ Please ensure PostgreSQL is running on localhost:5433");
            throw e;
        }
    }

    /**
     * Test if online database is available without throwing exception
     */
    private static boolean testOnlineConnection() {
        try {
            Connection conn = getOnlineConnection();
            conn.close();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Check if we should retry connecting to online database
     */
    private static boolean shouldRetryOnline() {
        return System.currentTimeMillis() - lastOnlineCheckTime > ONLINE_RECHECK_INTERVAL_MS;
    }

    /**
     * Get current database mode
     */
    public static String getCurrentDatabaseMode() {
        return useLocalDatabase ? "Local (Offline)" : "Online (Neon)";
    }

    /**
     * Force switch to local database
     */
    public static void forceLocalMode() {
        useLocalDatabase = true;
        System.out.println("→ Forced switch to local database");
    }

    /**
     * Force switch to online database
     */
    public static void forceOnlineMode() {
        useLocalDatabase = false;
        hasTestedOnline = false;
        System.out.println("→ Forced switch to online database");
    }

    private static String getOnlineUrl() {
        return System.getProperty("pos.db.url", envOrDefault(ONLINE_URL_KEY, null));
    }

    private static String getOnlineUser() {
        return System.getProperty("pos.db.user", envOrDefault(ONLINE_USER_KEY, null));
    }

    private static String getOnlinePassword() {
        return System.getProperty("pos.db.password", envOrDefault(ONLINE_PASSWORD_KEY, null));
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
