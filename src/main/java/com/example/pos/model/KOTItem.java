package com.example.pos.model;

/**
 * Individual item in a Kitchen Order Ticket
 */
public class KOTItem {
    private Long id;
    private Long kotId;
    private String itemName;
    private int quantity;
    private String specialNotes;
    private String status; // Pending, Preparing, Ready

    public KOTItem() {
        this.status = "Pending";
    }

    public KOTItem(String itemName, int quantity) {
        this();
        this.itemName = itemName;
        this.quantity = quantity;
    }

    public KOTItem(String itemName, int quantity, String specialNotes) {
        this(itemName, quantity);
        this.specialNotes = specialNotes;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKotId() {
        return kotId;
    }

    public void setKotId(Long kotId) {
        this.kotId = kotId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getSpecialNotes() {
        return specialNotes;
    }

    public void setSpecialNotes(String specialNotes) {
        this.specialNotes = specialNotes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
