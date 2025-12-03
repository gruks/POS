package com.example.pos.model;

/**
 * Represents an inventory item that can be sold through the Retail menu.
 */
public class InventoryItem {

    private final String id;
    private final String name;
    private final double rate;
    private final int quantity;
    private final String category;

    public InventoryItem(String id, String name, double rate, int quantity, String category) {
        this.id = id;
        this.name = name;
        this.rate = rate;
        this.quantity = quantity;
        this.category = category != null ? category : "Retail";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getRate() {
        return rate;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getCategory() {
        return category;
    }

    public InventoryItem withQuantity(int newQuantity) {
        return new InventoryItem(id, name, rate, newQuantity, category);
    }
}


