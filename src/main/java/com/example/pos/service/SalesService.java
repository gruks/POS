package com.example.pos.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.example.pos.db.DatabaseConnection;
import com.example.pos.model.Transaction;

/**
 * Handles persistence of sales and associated items using JDBC transactions.
 */
public class SalesService {

    private final InventoryService inventoryService = new InventoryService();

    public long recordSale(SaleRequest request) {
        Objects.requireNonNull(request, "Sale request required");
        try {
            return DatabaseConnection.executeInTransaction(connection -> {
                long saleId = insertSale(connection, request);
                insertSaleItems(connection, saleId, request.items());
                if (request.retailAdjustments() != null && !request.retailAdjustments().isEmpty()) {
                    inventoryService.decrementStock(connection, request.retailAdjustments());
                }
                return saleId;
            });
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to complete sale", ex);
        }
    }

    public List<Transaction> loadTransactions() {
        String sql = """
                SELECT bill_number,
                       customer_name,
                       order_type,
                       total,
                       payment_method,
                       status,
                       created_at
                FROM sales
                ORDER BY created_at DESC
                """;
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("created_at");
                LocalDateTime createdAt = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();
                transactions.add(new Transaction(
                        String.valueOf(rs.getLong("bill_number")),
                        createdAt,
                        rs.getString("customer_name"),
                        rs.getString("order_type"),
                        (int) Math.round(rs.getDouble("total")),
                        rs.getString("payment_method"),
                        rs.getString("status")));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load sales history", ex);
        }
        return transactions;
    }

    private long insertSale(Connection connection, SaleRequest request) throws SQLException {
        String sql = """
                INSERT INTO sales (bill_number, customer_name, payment_method, order_type,
                                   subtotal, tax, total, status, created_at, table_id, table_name)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, request.billNumber());
            ps.setString(2, request.customerName());
            ps.setString(3, request.paymentMethod());
            ps.setString(4, request.orderType());
            ps.setDouble(5, request.subtotal());
            ps.setDouble(6, request.tax());
            ps.setDouble(7, request.total());
            ps.setString(8, request.status());
            ps.setTimestamp(9, Timestamp.from(Instant.now()));
            if (request.tableId() == null) {
                ps.setNull(10, java.sql.Types.BIGINT);
            } else {
                ps.setLong(10, request.tableId());
            }
            ps.setString(11, request.tableName());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        throw new SQLException("Failed to insert sale record");
    }

    private void insertSaleItems(Connection connection, long saleId, List<SaleItem> items) throws SQLException {
        if (items == null || items.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO sale_items (sale_id, name, quantity, price, total)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (SaleItem item : items) {
                ps.setLong(1, saleId);
                ps.setString(2, item.name());
                ps.setInt(3, item.quantity());
                ps.setDouble(4, item.price());
                ps.setDouble(5, item.total());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public record SaleItem(String name, int quantity, double price, double total) {
    }

    public record SaleRequest(long billNumber,
                              String customerName,
                              String paymentMethod,
                              String orderType,
                              double subtotal,
                              double tax,
                              double total,
                              String status,
                              List<SaleItem> items,
                              Map<String, Integer> retailAdjustments,
                              Long tableId,
                              String tableName) {
    }
}

