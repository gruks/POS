package com.example.pos.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Simple utility to test PostgreSQL database connection.
 * Run this to verify your Neon database is properly configured.
 */
public class DatabaseConnectionTest {

    public static void main(String[] args) {
        System.out.println("=== PostgreSQL Database Connection Test ===\n");
        
        try {
            testConnection();
            testDatabaseInfo();
            testQuery();
            
            System.out.println("\n✓ All tests passed! Database is working properly.");
            
        } catch (SQLException e) {
            System.err.println("\n✗ Database connection failed!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testConnection() throws SQLException {
        System.out.println("1. Testing connection...");
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("   ✓ Connection established successfully");
            }
        }
    }

    private static void testDatabaseInfo() throws SQLException {
        System.out.println("\n2. Retrieving database information...");
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            System.out.println("   Database: " + metaData.getDatabaseProductName());
            System.out.println("   Version: " + metaData.getDatabaseProductVersion());
            System.out.println("   Driver: " + metaData.getDriverName());
            System.out.println("   URL: " + metaData.getURL());
            System.out.println("   User: " + metaData.getUserName());
        }
    }

    private static void testQuery() throws SQLException {
        System.out.println("\n3. Testing simple query...");
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version(), current_database(), current_user")) {
            
            if (rs.next()) {
                System.out.println("   PostgreSQL Version: " + rs.getString(1));
                System.out.println("   Current Database: " + rs.getString(2));
                System.out.println("   Current User: " + rs.getString(3));
            }
        }
    }

    /**
     * Test if tables exist in the database
     */
    public static void checkTables() throws SQLException {
        System.out.println("\n4. Checking existing tables...");
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT table_name FROM information_schema.tables " +
                 "WHERE table_schema = 'public' ORDER BY table_name")) {
            
            boolean hasTables = false;
            while (rs.next()) {
                System.out.println("   - " + rs.getString(1));
                hasTables = true;
            }
            
            if (!hasTables) {
                System.out.println("   No tables found. Run DatabaseInitializer to create schema.");
            }
        }
    }
}
