package com.example.pos.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * High-performance connection pool using HikariCP
 * Provides fast, reusable database connections
 */
public class ConnectionPool {
    
    private static HikariDataSource dataSource;
    private static volatile boolean initialized = false;
    
    private ConnectionPool() {}
    
    /**
     * Initialize the connection pool
     */
    public static synchronized void initialize(String url, String user, String password) {
        if (initialized) {
            return;
        }
        
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(password);
            
            // Performance optimizations
            config.setMaximumPoolSize(10); // Max 10 connections
            config.setMinimumIdle(2); // Keep 2 connections ready
            config.setConnectionTimeout(3000); // 3 second timeout
            config.setIdleTimeout(600000); // 10 minutes idle timeout
            config.setMaxLifetime(1800000); // 30 minutes max lifetime
            config.setLeakDetectionThreshold(60000); // 1 minute leak detection
            
            // Connection test query
            config.setConnectionTestQuery("SELECT 1");
            
            // Cache prepared statements
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            
            // Additional performance settings
            config.addDataSourceProperty("reWriteBatchedInserts", "true");
            config.addDataSourceProperty("tcpKeepAlive", "true");
            
            dataSource = new HikariDataSource(config);
            initialized = true;
            
            System.out.println("✓ Connection pool initialized (max: 10, min: 2)");
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize connection pool: " + e.getMessage());
            throw new RuntimeException("Connection pool initialization failed", e);
        }
    }
    
    /**
     * Get a connection from the pool
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            throw new SQLException("Connection pool not initialized");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Check if pool is initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get pool statistics
     */
    public static String getPoolStats() {
        if (!initialized || dataSource == null) {
            return "Pool not initialized";
        }
        return String.format("Active: %d, Idle: %d, Total: %d, Waiting: %d",
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }
    
    /**
     * Shutdown the connection pool
     */
    public static synchronized void shutdown() {
        if (initialized && dataSource != null) {
            dataSource.close();
            initialized = false;
            System.out.println("✓ Connection pool shutdown");
        }
    }
}
