package com.example.pos.controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class StaffPayrollController {

    @FXML private Button btnExport;
    @FXML private Button btnAddStaff;
    @FXML private TabPane tabPane;
    @FXML private TableView<Staff> attendanceTable;
    @FXML private TableColumn<Staff, String> colName;
    @FXML private TableColumn<Staff, String> colRole;
    @FXML private TableColumn<Staff, String> colShift;
    @FXML private TableColumn<Staff, String> colStatus;
    @FXML private TableColumn<Staff, String> colCheckInTime;
    @FXML private TableColumn<Staff, Void> colMarkAttendance;

    private final ObservableList<Staff> staffData = FXCollections.observableArrayList();
    private final ObservableList<String> statusOptions = FXCollections.observableArrayList(
        "Checked-in", "On Break", "On Leave"
    );

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private void initialize() {
        setupTable();
        loadDummyData();
        setupEventHandlers();
    }

    /* ---------- Table Setup ---------- */

    private void setupTable() {
        colName.setCellValueFactory(data -> data.getValue().nameProperty());
        colRole.setCellValueFactory(data -> data.getValue().roleProperty());
        colShift.setCellValueFactory(data -> data.getValue().shiftProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());
        colCheckInTime.setCellValueFactory(data -> data.getValue().checkInTimeProperty());

        // Status column with colored labels
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label statusLabel = new Label(status);
                    statusLabel.getStyleClass().add("status-" + getStatusClass(status));
                    setGraphic(statusLabel);
                    setText(null);
                }
            }

            private String getStatusClass(String status) {
                switch (status.toLowerCase()) {
                    case "checked-in": return "green";
                    case "on break": return "yellow";
                    case "on leave": return "red";
                    default: return "green";
                }
            }
        });

        // Mark Attendance column with ComboBox
        colMarkAttendance.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<String> statusCombo = new ComboBox<>(statusOptions);

            {
                statusCombo.setPrefWidth(140);
                statusCombo.setOnAction(this::onStatusChange);
            }

            private void onStatusChange(ActionEvent event) {
                Staff staff = getTableView().getItems().get(getIndex());
                String newStatus = statusCombo.getValue();
                if (staff != null && newStatus != null) {
                    updateStatus(staff, newStatus);
                }
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Staff staff = getTableView().getItems().get(getIndex());
                    if (staff != null) {
                        statusCombo.setValue(staff.getStatus());
                        setGraphic(statusCombo);
                    }
                }
            }
        });

        attendanceTable.setItems(staffData);
    }

    /* ---------- Data Loading ---------- */

    private void loadDummyData() {
        staffData.setAll(
            new Staff("Rajesh Kumar", "Chef", "Morning", "Checked-in", "09:00"),
            new Staff("Priya Sharma", "Waiter", "Morning", "Checked-in", "09:15"),
            new Staff("Amit Singh", "Manager", "Full Day", "Checked-in", "08:30"),
            new Staff("Sneha Patel", "Cashier", "Morning", "On Break", "09:00"),
            new Staff("Vikram Mehta", "Chef", "Evening", "On Leave", "-"),
            new Staff("Anjali Reddy", "Waiter", "Evening", "Checked-in", "14:00")
        );
    }

    /* ---------- Event Handlers ---------- */

    private void setupEventHandlers() {
        btnExport.setOnAction(this::onExportAttendance);
        btnAddStaff.setOnAction(this::onAddStaff);
    }

    private void onExportAttendance(ActionEvent event) {
        System.out.println("Exporting attendance data...");
        // TODO: Implement export functionality
    }

    private void onAddStaff(ActionEvent event) {
        System.out.println("Adding new staff member...");
        // TODO: Implement add staff dialog
    }

    /* ---------- Status Management ---------- */

    public void updateStatus(Staff staff, String newStatus) {
        if (staff != null) {
            staff.setStatus(newStatus);
            
            // Update check-in time based on status
            if ("Checked-in".equals(newStatus)) {
                staff.setCheckInTime(LocalTime.now().format(TIME_FMT));
            } else if ("On Leave".equals(newStatus)) {
                staff.setCheckInTime("-");
            }
            
            // Refresh the table to update the display
            attendanceTable.refresh();
            
            System.out.println("Updated " + staff.getName() + " status to: " + newStatus);
        }
    }

    /* ---------- Staff Model ---------- */

    public static class Staff {
        private final StringProperty name = new SimpleStringProperty();
        private final StringProperty role = new SimpleStringProperty();
        private final StringProperty shift = new SimpleStringProperty();
        private final StringProperty status = new SimpleStringProperty();
        private final StringProperty checkInTime = new SimpleStringProperty();

        public Staff(String name, String role, String shift, String status, String checkInTime) {
            setName(name);
            setRole(role);
            setShift(shift);
            setStatus(status);
            setCheckInTime(checkInTime);
        }

        // Name
        public String getName() { return name.get(); }
        public void setName(String value) { name.set(value); }
        public StringProperty nameProperty() { return name; }

        // Role
        public String getRole() { return role.get(); }
        public void setRole(String value) { role.set(value); }
        public StringProperty roleProperty() { return role; }

        // Shift
        public String getShift() { return shift.get(); }
        public void setShift(String value) { shift.set(value); }
        public StringProperty shiftProperty() { return shift; }

        // Status
        public String getStatus() { return status.get(); }
        public void setStatus(String value) { status.set(value); }
        public StringProperty statusProperty() { return status; }

        // Check-in Time
        public String getCheckInTime() { return checkInTime.get(); }
        public void setCheckInTime(String value) { checkInTime.set(value); }
        public StringProperty checkInTimeProperty() { return checkInTime; }
    }
}
