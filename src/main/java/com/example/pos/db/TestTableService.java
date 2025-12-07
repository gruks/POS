package com.example.pos.db;

import com.example.pos.service.TableService;
import java.util.List;

public class TestTableService {
    
    public static void main(String[] args) {
        System.out.println("=== Testing Table Service ===\n");
        
        try {
            // Initialize database first
            System.out.println("1. Initializing database...");
            DatabaseInitializer.initialize();
            System.out.println("   ✓ Database initialized\n");
            
            // Test loading table names
            System.out.println("2. Loading table names...");
            TableService tableService = new TableService();
            List<String> tableNames = tableService.loadTableNames();
            
            if (tableNames.isEmpty()) {
                System.out.println("   ✗ ERROR: No tables found!");
                System.out.println("   The restaurant_tables table might be empty.");
            } else {
                System.out.println("   ✓ Found " + tableNames.size() + " tables:");
                for (String name : tableNames) {
                    System.out.println("     - " + name);
                }
            }
            
            System.out.println("\n=== Test Complete ===");
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
