package com.example.pos.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Utility to test and switch between online and local databases
 */
public class DatabaseModeSwitcher {

    public static void main(String[] args) {
        System.out.println("=== Database Mode Switcher ===\n");
        
        // Test current connection
        System.out.println("1. Testing current database connection...");
        testCurrentConnection();
        
        // Show current mode
        System.out.println("\n2. Current database mode: " + DatabaseConnection.getCurrentDatabaseMode());
        
        // Test both databases
        System.out.println("\n3. Testing both databases...");
        testBothDatabases();
        
        // Show recommendations
        System.out.println("\n4. Recommendations:");
        showRecommendations();
    }

    private static void testCurrentConnection() {
        long startTime = System.currentTimeMillis();
        try (Connection conn = DatabaseConnection.getConnection()) {
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("   ✓ Connection successful");
            System.out.println("   Time: " + duration + "ms");
            System.out.println("   Mode: " + DatabaseConnection.getCurrentDatabaseMode());
            
            if (duration > 1000) {
                System.out.println("   ⚠ Warning: Connection is slow (>" + duration + "ms)");
            }
        } catch (SQLException e) {
            System.err.println("   ✗ Connection failed: " + e.getMessage());
        }
    }

    private static void testBothDatabases() {
        // Test online
        System.out.println("\n   Testing Online Database:");
        DatabaseConnection.forceOnlineMode();
        long onlineTime = testConnection();
        
        // Test local
        System.out.println("\n   Testing Local Database:");
        DatabaseConnection.forceLocalMode();
        long localTime = testConnection();
        
        // Compare
        System.out.println("\n   Performance Comparison:");
        System.out.println("   Online: " + (onlineTime > 0 ? onlineTime + "ms" : "Failed"));
        System.out.println("   Local:  " + (localTime > 0 ? localTime + "ms" : "Failed"));
        
        if (onlineTime > 0 && localTime > 0) {
            double speedup = (double) onlineTime / localTime;
            System.out.println("   Local is " + String.format("%.1f", speedup) + "x faster");
        }
    }

    private static long testConnection() {
        long startTime = System.currentTimeMillis();
        try (Connection conn = DatabaseConnection.getConnection()) {
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("      ✓ Connected in " + duration + "ms");
            return duration;
        } catch (SQLException e) {
            System.err.println("      ✗ Failed: " + e.getMessage());
            return -1;
        }
    }

    private static void showRecommendations() {
        System.out.println("   • For best performance, use local database");
        System.out.println("   • Online database is good for remote access");
        System.out.println("   • App automatically switches to local if online is slow");
        System.out.println("   • To force local mode, comment out online settings in .env");
        System.out.println("\n   Setup Local Database:");
        System.out.println("   1. Install PostgreSQL on localhost:5433");
        System.out.println("   2. Create database 'posdb'");
        System.out.println("   3. Set password '@estheticSQL1' for user 'postgres'");
        System.out.println("   4. Run DatabaseInitializer to create tables");
    }

    /**
     * Force local database mode
     */
    public static void switchToLocal() {
        DatabaseConnection.forceLocalMode();
        System.out.println("Switched to local database mode");
    }

    /**
     * Force online database mode
     */
    public static void switchToOnline() {
        DatabaseConnection.forceOnlineMode();
        System.out.println("Switched to online database mode");
    }
}
