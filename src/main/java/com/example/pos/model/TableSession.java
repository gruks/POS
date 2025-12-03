package com.example.pos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot of a running bill that is attached to a table.
 */
public class TableSession {

    private final String tableId;
    private final String tableName;
    private final String billLabel;
    private final String customerName;
    private final String paymentMethod;
    private final String orderType;
    private final List<TableSessionItem> items;
    private final String status;
    private final LocalDateTime updatedAt;

    public TableSession(
            String tableId,
            String tableName,
            String billLabel,
            String customerName,
            String paymentMethod,
            String orderType,
            List<TableSessionItem> items,
            String status,
            LocalDateTime updatedAt) {
        this.tableId = tableId;
        this.tableName = tableName;
        this.billLabel = billLabel;
        this.customerName = customerName;
        this.paymentMethod = paymentMethod;
        this.orderType = orderType;
        this.items = items != null ? items : new ArrayList<>();
        this.status = status != null ? status : "Occupied";
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    public double subtotal() {
        return items.stream().mapToDouble(TableSessionItem::total).sum();
    }

    public String getTableId() {
        return tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public String getBillLabel() {
        return billLabel;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getOrderType() {
        return orderType;
    }

    public List<TableSessionItem> getItems() {
        return items;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}