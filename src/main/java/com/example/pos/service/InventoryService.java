package com.example.pos.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.example.pos.db.DatabaseConnection;
import com.example.pos.model.InventoryItem;

/**
 * Handles CRUD operations for inventory items stored in PostgreSQL via JDBC.
 */
public class InventoryService {

    public static final String RETAIL_CATEGORY = "Retail";

    public List<InventoryItem> getAllItems() {
        List<InventoryItem> items = new ArrayList<>();
        String sql = """
                SELECT id, name, rate, quantity, category
                FROM inventory_items
                ORDER BY name
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(toItem(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load inventory", ex);
        }
        return items;
    }

    public InventoryItem createItem(String name, double rate, int quantity, String category) {
        Objects.requireNonNull(name, "Item name required");
        String normalizedCategory = normalizeCategory(category);
        String insert = """
                INSERT INTO inventory_items (name, rate, quantity, category, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection()) {
            ensureUniqueName(connection, name);
            Instant now = Instant.now();
            try (PreparedStatement ps = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name.trim());
                ps.setDouble(2, rate);
                ps.setInt(3, quantity);
                ps.setString(4, normalizedCategory);
                ps.setObject(5, now);
                ps.setObject(6, now);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return findById(connection, keys.getLong(1));
                    }
                }
            }
            return null;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create inventory item", ex);
        }
    }

    public InventoryItem updateItem(String id, String name, double rate, int quantity, String category) {
        Objects.requireNonNull(id, "Item id required");
        String update = """
                UPDATE inventory_items
                SET name = ?, rate = ?, quantity = ?, category = ?, updated_at = ?
                WHERE id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(update)) {
            ps.setString(1, name.trim());
            ps.setDouble(2, rate);
            ps.setInt(3, quantity);
            ps.setString(4, normalizeCategory(category));
            ps.setObject(5, Instant.now());
            ps.setLong(6, Long.parseLong(id));
            ps.executeUpdate();
            return findById(connection, Long.parseLong(id));
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update inventory item", ex);
        }
    }

    public void deleteItem(String id) {
        Objects.requireNonNull(id, "Item id required");
        String sql = "DELETE FROM inventory_items WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, Long.parseLong(id));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete inventory item", ex);
        }
    }

    public ImportResult importCsv(File file) throws IOException {
        Objects.requireNonNull(file, "File is required");
        int added = 0;
        int updated = 0;
        int skipped = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length < 3) {
                    skipped++;
                    continue;
                }
                String name = parts[0].trim();
                double rate;
                int qty;
                try {
                    rate = Double.parseDouble(parts[1].trim());
                    qty = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException ex) {
                    skipped++;
                    continue;
                }
                String category = parts.length > 3 ? parts[3].trim() : RETAIL_CATEGORY;
                InventoryItem existing = findByName(name);
                if (existing == null) {
                    createItem(name, rate, qty, category);
                    added++;
                } else {
                    updateItem(existing.getId(), name, rate, qty, category);
                    updated++;
                }
            }
        }
        return new ImportResult(added, updated, skipped);
    }

    public Map<String, InventoryItem> retailItemsByName() {
        Map<String, InventoryItem> map = new HashMap<>();
        for (InventoryItem item : getAllItems()) {
            map.put(item.getName(), item);
        }
        return map;
    }

    public void decrementStock(Map<String, Integer> soldItems) {
        if (soldItems == null || soldItems.isEmpty()) {
            return;
        }
        try {
            DatabaseConnection.executeInTransaction(connection -> {
                decrementStock(connection, soldItems);
                return null;
            });
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to decrement stock", ex);
        }
    }

    public void decrementStock(Connection connection, Map<String, Integer> soldItems) throws SQLException {
        if (soldItems == null || soldItems.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : soldItems.entrySet()) {
            String name = entry.getKey();
            int qtySold = entry.getValue();
            InventoryItem existing = findByName(connection, name);
            if (existing == null) {
                continue;
            }
            int currentQty = existing.getQuantity();
            if (qtySold > currentQty) {
                throw new IllegalStateException(
                        "Insufficient stock for " + name + ". Available: " + currentQty);
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE inventory_items SET quantity = ?, updated_at = ? WHERE id = ?")) {
                ps.setInt(1, currentQty - qtySold);
                ps.setObject(2, Instant.now());
                ps.setLong(3, Long.parseLong(existing.getId()));
                ps.executeUpdate();
            }
        }
    }

    private InventoryItem findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, name, rate, quantity, category FROM inventory_items WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toItem(rs);
                }
            }
        }
        return null;
    }

    private InventoryItem findByName(String name) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return findByName(connection, name);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to lookup inventory item", ex);
        }
    }

    private InventoryItem findByName(Connection connection, String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, name, rate, quantity, category FROM inventory_items WHERE LOWER(name) = LOWER(?)")) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toItem(rs);
                }
            }
        }
        return null;
    }

    private void ensureUniqueName(Connection connection, String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM inventory_items WHERE LOWER(name) = LOWER(?)")) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new IllegalStateException("Item with name '" + name + "' already exists.");
                }
            }
        }
    }

    private static String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return RETAIL_CATEGORY;
        }
        String normalized = category.trim();
        if (normalized.equalsIgnoreCase(RETAIL_CATEGORY)) {
            return RETAIL_CATEGORY;
        }
        return normalized.substring(0, 1).toUpperCase(Locale.ENGLISH) + normalized.substring(1);
    }

    private static InventoryItem toItem(ResultSet rs) throws SQLException {
        String id = String.valueOf(rs.getLong("id"));
        String name = rs.getString("name");
        double rate = rs.getDouble("rate");
        int quantity = rs.getInt("quantity");
        String category = rs.getString("category");
        return new InventoryItem(id, name, rate, quantity, category);
    }

    public record ImportResult(int added, int updated, int skipped) {
    }
}