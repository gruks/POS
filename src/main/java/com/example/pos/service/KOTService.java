package com.example.pos.service;

import com.example.pos.db.DatabaseConnection;
import com.example.pos.model.KOT;
import com.example.pos.model.KOTItem;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing Kitchen Order Tickets
 */
public class KOTService {

    /**
     * Create a new KOT
     */
    public long createKOT(KOT kot) throws SQLException {
        String kotSql = """
            INSERT INTO kitchen_order_tickets 
            (kot_number, table_id, table_name, order_type, customer_name, status, priority, notes, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        String itemSql = """
            INSERT INTO kot_items (kot_id, item_name, quantity, special_notes, status)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Insert KOT
                long kotId;
                try (PreparedStatement ps = conn.prepareStatement(kotSql)) {
                    ps.setLong(1, kot.getKotNumber());
                    ps.setObject(2, kot.getTableId());
                    ps.setString(3, kot.getTableName());
                    ps.setString(4, kot.getOrderType());
                    ps.setString(5, kot.getCustomerName());
                    ps.setString(6, kot.getStatus());
                    ps.setString(7, kot.getPriority());
                    ps.setString(8, kot.getNotes());
                    ps.setTimestamp(9, Timestamp.valueOf(kot.getCreatedAt()));
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            kotId = rs.getLong(1);
                        } else {
                            throw new SQLException("Failed to create KOT");
                        }
                    }
                }

                // Insert KOT items
                try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                    for (KOTItem item : kot.getItems()) {
                        ps.setLong(1, kotId);
                        ps.setString(2, item.getItemName());
                        ps.setInt(3, item.getQuantity());
                        ps.setString(4, item.getSpecialNotes());
                        ps.setString(5, item.getStatus());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
                return kotId;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Get all active KOTs (not completed or cancelled)
     */
    public List<KOT> getActiveKOTs() throws SQLException {
        String sql = """
            SELECT id, kot_number, table_id, table_name, order_type, customer_name,
                   status, priority, notes, created_at, started_at, completed_at
            FROM kitchen_order_tickets
            WHERE status IN ('Pending', 'Preparing', 'Ready')
            ORDER BY 
                CASE priority 
                    WHEN 'Urgent' THEN 1
                    WHEN 'High' THEN 2
                    ELSE 3
                END,
                created_at ASC
            """;

        List<KOT> kots = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                KOT kot = mapKOT(rs);
                kot.setItems(getKOTItems(kot.getId()));
                kots.add(kot);
            }
        }
        return kots;
    }

    /**
     * Get all KOTs (including completed)
     */
    public List<KOT> getAllKOTs() throws SQLException {
        String sql = """
            SELECT id, kot_number, table_id, table_name, order_type, customer_name,
                   status, priority, notes, created_at, started_at, completed_at
            FROM kitchen_order_tickets
            ORDER BY created_at DESC
            LIMIT 100
            """;

        List<KOT> kots = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                KOT kot = mapKOT(rs);
                kot.setItems(getKOTItems(kot.getId()));
                kots.add(kot);
            }
        }
        return kots;
    }

    /**
     * Get KOT by ID
     */
    public KOT getKOTById(long id) throws SQLException {
        String sql = """
            SELECT id, kot_number, table_id, table_name, order_type, customer_name,
                   status, priority, notes, created_at, started_at, completed_at
            FROM kitchen_order_tickets
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    KOT kot = mapKOT(rs);
                    kot.setItems(getKOTItems(kot.getId()));
                    return kot;
                }
            }
        }
        return null;
    }

    /**
     * Get items for a KOT
     */
    private List<KOTItem> getKOTItems(long kotId) throws SQLException {
        String sql = """
            SELECT id, kot_id, item_name, quantity, special_notes, status
            FROM kot_items
            WHERE kot_id = ?
            ORDER BY id
            """;

        List<KOTItem> items = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, kotId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapKOTItem(rs));
                }
            }
        }
        return items;
    }

    /**
     * Update KOT status
     */
    public void updateKOTStatus(long kotId, String status) throws SQLException {
        String sql = """
            UPDATE kitchen_order_tickets
            SET status = ?,
                started_at = CASE WHEN ? = 'Preparing' AND started_at IS NULL THEN NOW() ELSE started_at END,
                completed_at = CASE WHEN ? IN ('Completed', 'Cancelled') THEN NOW() ELSE completed_at END
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setString(3, status);
            ps.setLong(4, kotId);
            ps.executeUpdate();
        }
    }

    /**
     * Update KOT priority
     */
    public void updateKOTPriority(long kotId, String priority) throws SQLException {
        String sql = "UPDATE kitchen_order_tickets SET priority = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, priority);
            ps.setLong(2, kotId);
            ps.executeUpdate();
        }
    }

    /**
     * Update item status
     */
    public void updateItemStatus(long itemId, String status) throws SQLException {
        String sql = "UPDATE kot_items SET status = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, itemId);
            ps.executeUpdate();
        }
    }

    /**
     * Delete KOT (for clearing old completed orders)
     */
    public void deleteKOT(long kotId) throws SQLException {
        String sql = "DELETE FROM kitchen_order_tickets WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, kotId);
            ps.executeUpdate();
        }
    }

    /**
     * Clear completed KOTs older than specified hours
     */
    public int clearCompletedKOTs(int hoursOld) throws SQLException {
        String sql = """
            DELETE FROM kitchen_order_tickets
            WHERE status IN ('Completed', 'Cancelled')
            AND completed_at < NOW() - INTERVAL '? hours'
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, hoursOld);
            return ps.executeUpdate();
        }
    }

    /**
     * Generate unique KOT number
     */
    public long generateKOTNumber() {
        LocalDateTime now = LocalDateTime.now();
        // Format: yyMMddHHmmss
        return Long.parseLong(now.format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).substring(2));
    }

    /**
     * Get count of active KOTs by status
     */
    public int getKOTCountByStatus(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM kitchen_order_tickets WHERE status = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    // Helper methods
    private KOT mapKOT(ResultSet rs) throws SQLException {
        KOT kot = new KOT();
        kot.setId(rs.getLong("id"));
        kot.setKotNumber(rs.getLong("kot_number"));
        
        long tableId = rs.getLong("table_id");
        kot.setTableId(rs.wasNull() ? null : tableId);
        
        kot.setTableName(rs.getString("table_name"));
        kot.setOrderType(rs.getString("order_type"));
        kot.setCustomerName(rs.getString("customer_name"));
        kot.setStatus(rs.getString("status"));
        kot.setPriority(rs.getString("priority"));
        kot.setNotes(rs.getString("notes"));
        
        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            kot.setCreatedAt(createdTs.toLocalDateTime());
        }
        
        Timestamp startedTs = rs.getTimestamp("started_at");
        if (startedTs != null) {
            kot.setStartedAt(startedTs.toLocalDateTime());
        }
        
        Timestamp completedTs = rs.getTimestamp("completed_at");
        if (completedTs != null) {
            kot.setCompletedAt(completedTs.toLocalDateTime());
        }
        
        return kot;
    }

    private KOTItem mapKOTItem(ResultSet rs) throws SQLException {
        KOTItem item = new KOTItem();
        item.setId(rs.getLong("id"));
        item.setKotId(rs.getLong("kot_id"));
        item.setItemName(rs.getString("item_name"));
        item.setQuantity(rs.getInt("quantity"));
        item.setSpecialNotes(rs.getString("special_notes"));
        item.setStatus(rs.getString("status"));
        return item;
    }
}
