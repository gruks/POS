package com.example.pos.model;

/**
 * Represents a single order item that is attached to a live table session.
 */
public record TableSessionItem(String name, int quantity, double price) {

    public double total() {
        return quantity * price;
    }
}


