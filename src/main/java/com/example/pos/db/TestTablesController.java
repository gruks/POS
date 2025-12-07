package com.example.pos.db;

import com.example.pos.model.TableModel;
import com.example.pos.service.TableService;
import java.util.List;

public class TestTablesController {
    
    public static void main(String[] args) {
        System.out.println("=== Testing TablesController Data Loading ===\n");
        
        try {
            // Initialize database
            System.out.println("1. Initializing database...");
            DatabaseInitializer.initialize();
            System.out.println("   ✓ Database initialized\n");
            
            // Test TableService.loadTables() (used by TablesController)
            System.out.println("2. Testing TableService.loadTables()...");
            TableService tableService = new TableService();
            List<TableModel> tables = tableService.loadTables();
            
            if (tables.isEmpty()) {
                System.out.println("   ✗ ERROR: No tables found!");
                System.out.println("   This is why TablesController shows 'Unable to load tables'");
            } else {
                System.out.println("   ✓ Found " + tables.size() + " tables:");
                for (TableModel table : tables) {
                    System.out.println("     - " + table.getTableName() + 
                                     " (Capacity: " + table.getCapacity() + 
                                     ", Status: " + table.getStatus() + ")");
                }
            }
            
            System.out.println("\n=== Test Complete ===");
            
        } catch (Exception e) {
            System.err.println("\n✗ ERROR: " + e.getMessage());
            System.err.println("\nFull stack trace:");
            e.printStackTrace();
            System.err.println("\nThis is the error TablesController is catching!");
        }
    }
}
