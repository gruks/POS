package com.example.pos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Kitchen Order Ticket model
 */
public class KOT {
    private Long id;
    private long kotNumber;
    private Long tableId;
    private String tableName;
    private String orderType;
    private String customerName;
    private String status; // Pending, Preparing, Ready, Completed, Cancelled
    private String priority; // Normal, High, Urgent
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<KOTItem> items;

    public KOT() {
        this.items = new ArrayList<>();
        this.status = "Pending";
        this.priority = "Normal";
        this.orderType = "Dine-In";
        this.createdAt = LocalDateTime.now();
    }

    public KOT(long kotNumber, String tableName, String orderType, String customerName) {
        this();
        this.kotNumber = kotNumber;
        this.tableName = tableName;
        this.orderType = orderType;
        this.customerName = customerName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getKotNumber() {
        return kotNumber;
    }

    public void setKotNumber(long kotNumber) {
        this.kotNumber = kotNumber;
    }

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<KOTItem> getItems() {
        return items;
    }

    public void setItems(List<KOTItem> items) {
        this.items = items;
    }

    public void addItem(KOTItem item) {
        this.items.add(item);
    }

    public String getDisplayTime() {
        if (createdAt == null) return "";
        java.time.format.DateTimeFormatter formatter = 
            java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        return createdAt.format(formatter);
    }

    public String getElapsedTime() {
        if (createdAt == null) return "0 min";
        long minutes = java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        if (minutes < 60) {
            return minutes + " min";
        } else {
            long hours = minutes / 60;
            long mins = minutes % 60;
            return hours + "h " + mins + "m";
        }
    }
}
