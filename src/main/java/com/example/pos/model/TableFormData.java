package com.example.pos.model;

/**
 * Captures user input from the table form dialog.
 */
public record TableFormData(
        String name,
        int capacity,
        String status,
        String reservedFor,
        String reservationTime) {

    public boolean isReserved() {
        return "Reserved".equalsIgnoreCase(status);
    }
}


