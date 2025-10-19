package com.example.pos.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TableModel {
    private final StringProperty tableName;
    private final IntegerProperty capacity;
    private final StringProperty status;  // "Available", "Occupied", "Reserved"
    private final StringProperty guests;
    private final StringProperty duration;
    private final StringProperty reservationTime;

    public TableModel(String tableName, int capacity, String status) {
        this.tableName = new SimpleStringProperty(tableName);
        this.capacity = new SimpleIntegerProperty(capacity);
        this.status = new SimpleStringProperty(status);
        this.guests = new SimpleStringProperty("");
        this.duration = new SimpleStringProperty("");
        this.reservationTime = new SimpleStringProperty("");
    }

    public TableModel(String tableName, int capacity, String status, String guests, String duration) {
        this.tableName = new SimpleStringProperty(tableName);
        this.capacity = new SimpleIntegerProperty(capacity);
        this.status = new SimpleStringProperty(status);
        this.guests = new SimpleStringProperty(guests);
        this.duration = new SimpleStringProperty(duration);
        this.reservationTime = new SimpleStringProperty("");
    }

    public TableModel(String tableName, int capacity, String status, String reservationTime) {
        this.tableName = new SimpleStringProperty(tableName);
        this.capacity = new SimpleIntegerProperty(capacity);
        this.status = new SimpleStringProperty(status);
        this.guests = new SimpleStringProperty("");
        this.duration = new SimpleStringProperty("");
        this.reservationTime = new SimpleStringProperty(reservationTime);
    }

    // Table Name
    public String getTableName() { return tableName.get(); }
    public void setTableName(String value) { tableName.set(value); }
    public StringProperty tableNameProperty() { return tableName; }

    // Capacity
    public int getCapacity() { return capacity.get(); }
    public void setCapacity(int value) { capacity.set(value); }
    public IntegerProperty capacityProperty() { return capacity; }

    // Status
    public String getStatus() { return status.get(); }
    public void setStatus(String value) { status.set(value); }
    public StringProperty statusProperty() { return status; }

    // Guests
    public String getGuests() { return guests.get(); }
    public void setGuests(String value) { guests.set(value); }
    public StringProperty guestsProperty() { return guests; }

    // Duration
    public String getDuration() { return duration.get(); }
    public void setDuration(String value) { duration.set(value); }
    public StringProperty durationProperty() { return duration; }

    // Reservation Time
    public String getReservationTime() { return reservationTime.get(); }
    public void setReservationTime(String value) { reservationTime.set(value); }
    public StringProperty reservationTimeProperty() { return reservationTime; }

    // Helper methods
    public boolean isAvailable() {
        return "Available".equals(getStatus());
    }

    public boolean isOccupied() {
        return "Occupied".equals(getStatus());
    }

    public boolean isReserved() {
        return "Reserved".equals(getStatus());
    }

    public String getStatusDisplayText() {
        switch (getStatus()) {
            case "Available":
                return "Available";
            case "Occupied":
                return "Guests: " + getGuests() + " | " + getDuration();
            case "Reserved":
                return getReservationTime();
            default:
                return getStatus();
        }
    }

    @Override
    public String toString() {
        return getTableName() + " (" + getCapacity() + " seats) - " + getStatus();
    }
}
