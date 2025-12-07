package com.example.pos.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.example.pos.db.DatabaseConnection;
import com.example.pos.model.TableFormData;
import com.example.pos.model.TableModel;
import com.example.pos.model.TableSession;
import com.example.pos.model.TableSessionItem;

/**
 * Encapsulates PostgreSQL access for table metadata and live sessions.
 */
public class TableService {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HH:mm");

    public List<TableModel> loadTables() {
        String sql = """
                SELECT t.id,
                       t.name,
                       t.capacity,
                       t.status,
                       t.reservation_name,
                       t.reservation_time,
                       ts.bill_label,
                       ts.customer_name,
                       ts.status AS session_status,
                       ts.updated_at
                FROM restaurant_tables t
                LEFT JOIN table_sessions ts ON ts.table_id = t.id
                ORDER BY t.name
                """;
        List<TableModel> models = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                models.add(toModel(connection, rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load tables", ex);
        }
        return models;
    }

    public List<String> loadTableNames() {
        String sql = "SELECT name FROM restaurant_tables ORDER BY name";
        List<String> names = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load table names", ex);
        }
        return names;
    }

    public TableModel createTable(TableFormData data) {
        Objects.requireNonNull(data, "Table data must not be null");
        String sql = """
                INSERT INTO restaurant_tables (name, capacity, status, reservation_name, reservation_time, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        Instant now = Instant.now();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, data.name());
            ps.setInt(2, data.capacity());
            ps.setString(3, normalizeStatus(data.status()));
            ps.setString(4, data.isReserved() ? data.reservedFor() : null);
            ps.setString(5, data.isReserved() ? data.reservationTime() : null);
            ps.setTimestamp(6, Timestamp.from(now));
            ps.setTimestamp(7, Timestamp.from(now));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return findTableById(connection, id);
                }
            }
            return null;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create table", ex);
        }
    }

    public void updateTable(String id, TableFormData data) {
        Objects.requireNonNull(id, "Table id required");
        Objects.requireNonNull(data, "Table data must not be null");
        String sql = """
                UPDATE restaurant_tables
                SET name = ?, capacity = ?, status = ?, reservation_name = ?, reservation_time = ?, updated_at = ?
                WHERE id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, data.name());
            ps.setInt(2, data.capacity());
            ps.setString(3, normalizeStatus(data.status()));
            ps.setString(4, data.isReserved() ? data.reservedFor() : null);
            ps.setString(5, data.isReserved() ? data.reservationTime() : null);
            ps.setTimestamp(6, Timestamp.from(Instant.now()));
            ps.setLong(7, Long.parseLong(id));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update table", ex);
        }
    }

    public void deleteTable(String id) {
        Objects.requireNonNull(id, "Table id required");
        String deleteSessions = "DELETE FROM table_sessions WHERE table_id = ?";
        String deleteItems = "DELETE FROM table_session_items WHERE table_id = ?";
        String deleteTable = "DELETE FROM restaurant_tables WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement psItems = connection.prepareStatement(deleteItems);
                 PreparedStatement psSessions = connection.prepareStatement(deleteSessions);
                 PreparedStatement psTable = connection.prepareStatement(deleteTable)) {
                long tableId = Long.parseLong(id);
                psItems.setLong(1, tableId);
                psItems.executeUpdate();
                psSessions.setLong(1, tableId);
                psSessions.executeUpdate();
                psTable.setLong(1, tableId);
                psTable.executeUpdate();
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete table", ex);
        }
    }

    public TableSession loadSession(String tableId) {
        String sql = """
                SELECT t.id,
                       t.name,
                       ts.bill_label,
                       ts.customer_name,
                       ts.payment_method,
                       ts.order_type,
                       ts.status,
                       ts.updated_at
                FROM restaurant_tables t
                LEFT JOIN table_sessions ts ON ts.table_id = t.id
                WHERE t.id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, Long.parseLong(tableId));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getString("bill_label") == null) {
                    return null;
                }
                List<TableSessionItem> items = loadSessionItems(connection, Long.parseLong(tableId));
                
                // Convert TIMESTAMPTZ to LocalDateTime
                LocalDateTime updatedAt = null;
                try {
                    java.sql.Timestamp timestamp = rs.getTimestamp("updated_at");
                    if (timestamp != null) {
                        updatedAt = timestamp.toLocalDateTime();
                    }
                } catch (SQLException e) {
                    System.err.println("Warning: Could not parse updated_at: " + e.getMessage());
                }
                
                return new TableSession(
                        tableId,
                        rs.getString("name"),
                        rs.getString("bill_label"),
                        rs.getString("customer_name"),
                        rs.getString("payment_method"),
                        rs.getString("order_type"),
                        items,
                        rs.getString("status"),
                        updatedAt);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load table session", ex);
        }
    }

    public void saveSession(TableSession session) {
        Objects.requireNonNull(session, "Table session required");
        String upsert = """
                INSERT INTO table_sessions (table_id, bill_label, customer_name, payment_method, order_type, status, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (table_id)
                DO UPDATE SET bill_label = EXCLUDED.bill_label,
                              customer_name = EXCLUDED.customer_name,
                              payment_method = EXCLUDED.payment_method,
                              order_type = EXCLUDED.order_type,
                              status = EXCLUDED.status,
                              updated_at = EXCLUDED.updated_at
                """;
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(upsert)) {
                long tableId = Long.parseLong(session.getTableId());
                ps.setLong(1, tableId);
                ps.setString(2, session.getBillLabel());
                ps.setString(3, session.getCustomerName());
                ps.setString(4, session.getPaymentMethod());
                ps.setString(5, session.getOrderType());
                ps.setString(6, session.getStatus());
                ps.setObject(7, session.getUpdatedAt());
                ps.executeUpdate();
                replaceSessionItems(connection, tableId, session.getItems());
                updateTableStatus(connection, tableId, "Occupied");
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save table session", ex);
        }
    }

    public void clearSession(String tableId, String newStatus) {
        Objects.requireNonNull(tableId, "Table id required");
        String deleteSession = "DELETE FROM table_sessions WHERE table_id = ?";
        String deleteItems = "DELETE FROM table_session_items WHERE table_id = ?";
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement psItems = connection.prepareStatement(deleteItems);
                 PreparedStatement psSession = connection.prepareStatement(deleteSession)) {
                long id = Long.parseLong(tableId);
                psItems.setLong(1, id);
                psItems.executeUpdate();
                psSession.setLong(1, id);
                psSession.executeUpdate();
                updateTableStatus(connection, id, normalizeStatus(newStatus));
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to clear table session", ex);
        }
    }

    private TableModel toModel(Connection connection, ResultSet rs) throws SQLException {
        String id = String.valueOf(rs.getLong("id"));
        String name = rs.getString("name");
        int capacity = rs.getInt("capacity");
        String status = rs.getString("status");
        TableModel model = new TableModel(id, name, capacity, status);

        String reservationName = rs.getString("reservation_name");
        String reservationTime = rs.getString("reservation_time");
        if (reservationName != null) {
            model.setReservationDetails(reservationName, reservationTime);
        }

        String billLabel = rs.getString("bill_label");
        if (billLabel != null) {
            model.setActiveBillLabel(billLabel);
            model.setStatus("Occupied");
            model.setGuests(rs.getString("customer_name") != null ? rs.getString("customer_name") : "");
            try {
                // Try to get as Timestamp first (PostgreSQL TIMESTAMPTZ)
                java.sql.Timestamp timestamp = rs.getTimestamp("updated_at");
                if (timestamp != null) {
                    LocalDateTime updated = timestamp.toLocalDateTime();
                    String duration = updated.atZone(ZoneId.systemDefault()).toLocalTime().format(DISPLAY_TIME);
                    model.setDuration(duration);
                }
            } catch (SQLException e) {
                // If timestamp conversion fails, just skip duration
                System.err.println("Warning: Could not parse updated_at timestamp: " + e.getMessage());
            }
        }
        return model;
    }

    private TableModel findTableById(Connection connection, long id) throws SQLException {
        String sql = """
                SELECT t.id,
                       t.name,
                       t.capacity,
                       t.status,
                       t.reservation_name,
                       t.reservation_time,
                       ts.bill_label,
                       ts.customer_name,
                       ts.status AS session_status,
                       ts.updated_at
                FROM restaurant_tables t
                LEFT JOIN table_sessions ts ON ts.table_id = t.id
                WHERE t.id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toModel(connection, rs);
                }
            }
        }
        return null;
    }

    private List<TableSessionItem> loadSessionItems(Connection connection, long tableId) throws SQLException {
        String sql = """
                SELECT item_name, quantity, price
                FROM table_session_items
                WHERE table_id = ?
                """;
        List<TableSessionItem> items = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new TableSessionItem(
                            rs.getString("item_name"),
                            rs.getInt("quantity"),
                            rs.getDouble("price")));
                }
            }
        }
        return items;
    }

    private void replaceSessionItems(Connection connection, long tableId, List<TableSessionItem> items) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM table_session_items WHERE table_id = ?");
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO table_session_items (table_id, item_name, quantity, price) VALUES (?, ?, ?, ?)")) {
            delete.setLong(1, tableId);
            delete.executeUpdate();
            if (items == null) {
                return;
            }
            for (TableSessionItem item : items) {
                insert.setLong(1, tableId);
                insert.setString(2, item.name());
                insert.setInt(3, item.quantity());
                insert.setDouble(4, item.price());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void updateTableStatus(Connection connection, long tableId, String newStatus) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE restaurant_tables SET status = ?, updated_at = ? WHERE id = ?")) {
            ps.setString(1, normalizeStatus(newStatus));
            ps.setTimestamp(2, Timestamp.from(Instant.now()));
            ps.setLong(3, tableId);
            ps.executeUpdate();
        }
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Available";
        }
        String normalized = status.trim();
        return switch (normalized.toLowerCase()) {
            case "reserved" -> "Reserved";
            case "occupied" -> "Occupied";
            default -> "Available";
        };
    }
}