package com.example.pos.controller;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import com.example.pos.model.StaffMember;
import com.example.pos.model.StaffPayrollInfo;
import com.example.pos.service.PayrollService;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class StaffPayrollController {

    private final PayrollService payrollService = new PayrollService();

    // Summary Cards
    @FXML private Label lblTotalEmployees;
    @FXML private Label lblCheckedInToday;
    @FXML private Label lblOnLeave;
    @FXML private Label lblPendingSalary;

    // Buttons
    @FXML private Button btnExport;
    @FXML private Button btnAddStaff;
    @FXML private Button btnCalculatePayroll;
    @FXML private Button btnPaySalary;

    // Tabs
    @FXML private TabPane tabPane;

    // Attendance Table
    @FXML private TableView<Staff> attendanceTable;
    @FXML private TableColumn<Staff, String> colName;
    @FXML private TableColumn<Staff, String> colRole;
    @FXML private TableColumn<Staff, String> colShift;
    @FXML private TableColumn<Staff, String> colStatus;
    @FXML private TableColumn<Staff, String> colCheckInTime;
    @FXML private TableColumn<Staff, Void> colMarkAttendance;

    // Payroll Table
    @FXML private ComboBox<String> monthSelector;
    @FXML private TableView<PayrollRow> payrollTable;
    @FXML private TableColumn<PayrollRow, String> colPayrollName;
    @FXML private TableColumn<PayrollRow, String> colPayrollRole;
    @FXML private TableColumn<PayrollRow, Number> colMonthlySalary;
    @FXML private TableColumn<PayrollRow, Number> colDaysWorked;
    @FXML private TableColumn<PayrollRow, Number> colEarnedSalary;
    @FXML private TableColumn<PayrollRow, Number> colAdvancePaid;
    @FXML private TableColumn<PayrollRow, Number> colPendingSalary;
    @FXML private TableColumn<PayrollRow, String> colPayrollStatus;

    private final ObservableList<Staff> staffData = FXCollections.observableArrayList();
    private final ObservableList<PayrollRow> payrollData = FXCollections.observableArrayList();
    private final ObservableList<String> statusOptions = FXCollections.observableArrayList(
        "Checked-in", "On Break", "On Leave"
    );

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private void initialize() {
        setupAttendanceTable();
        setupPayrollTable();
        setupMonthSelector();
        loadStaffData();
        updateSummaryCards();
        setupEventHandlers();
    }

    /* ---------- Table Setup ---------- */

    private void setupAttendanceTable() {
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

    private void setupPayrollTable() {
        colPayrollName.setCellValueFactory(data -> data.getValue().nameProperty());
        colPayrollRole.setCellValueFactory(data -> data.getValue().roleProperty());
        colMonthlySalary.setCellValueFactory(data -> data.getValue().monthlySalaryProperty());
        colDaysWorked.setCellValueFactory(data -> data.getValue().daysWorkedProperty());
        colEarnedSalary.setCellValueFactory(data -> data.getValue().earnedSalaryProperty());
        colAdvancePaid.setCellValueFactory(data -> data.getValue().advancePaidProperty());
        colPendingSalary.setCellValueFactory(data -> data.getValue().pendingSalaryProperty());
        colPayrollStatus.setCellValueFactory(data -> data.getValue().statusProperty());

        // Format currency columns
        colMonthlySalary.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText("₹" + String.format("%,.2f", value.doubleValue()));
                }
            }
        });

        colEarnedSalary.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText("₹" + String.format("%,.2f", value.doubleValue()));
                }
            }
        });

        colAdvancePaid.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText("₹" + String.format("%,.2f", value.doubleValue()));
                }
            }
        });

        colPendingSalary.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText("₹" + String.format("%,.2f", value.doubleValue()));
                }
            }
        });

        payrollTable.setItems(payrollData);
    }

    private void setupMonthSelector() {
        if (monthSelector != null) {
            // Add last 6 months
            YearMonth current = YearMonth.now();
            for (int i = 0; i < 6; i++) {
                YearMonth month = current.minusMonths(i);
                monthSelector.getItems().add(month.toString());
            }
            monthSelector.setValue(current.toString());
        }
    }

    /* ---------- Data Loading ---------- */

    private void loadStaffData() {
        try {
            staffData.clear();
            for (StaffMember member : payrollService.getAllStaff()) {
                if (member.isActive()) {
                    String status = getTodayStatus(member.getId());
                    String checkInTime = getCheckInTime(member.getId());
                    staffData.add(new Staff(
                        member.getId(),
                        member.getName(),
                        member.getRole(),
                        member.getShift(),
                        status,
                        checkInTime
                    ));
                }
            }
            updateSummaryCards();
        } catch (SQLException e) {
            showError("Failed to load staff data", e.getMessage());
        }
    }

    private void loadPayrollData() {
        try {
            payrollData.clear();
            String selectedMonth = monthSelector != null ? monthSelector.getValue() : YearMonth.now().toString();
            YearMonth month = YearMonth.parse(selectedMonth);
            
            java.util.List<StaffPayrollInfo> payrollList = payrollService.getAllPayrollInfo(month);
            for (StaffPayrollInfo info : payrollList) {
                payrollData.add(new PayrollRow(
                    info.getStaffId(),
                    info.getName(),
                    info.getRole(),
                    info.getMonthlySalary(),
                    info.getDaysWorked(),
                    info.getEarnedSalary(),
                    info.getAdvancePaid(),
                    info.getPendingSalary(),
                    info.getStatus()
                ));
            }
            updateSummaryCards();
        } catch (SQLException e) {
            showError("Failed to load payroll data", e.getMessage());
        }
    }

    private void updateSummaryCards() {
        try {
            // Total Employees
            int totalEmployees = staffData.size();
            if (lblTotalEmployees != null) {
                lblTotalEmployees.setText(String.valueOf(totalEmployees));
            }

            // Checked-in Today
            long checkedIn = staffData.stream()
                .filter(s -> "Checked-in".equalsIgnoreCase(s.getStatus()) || 
                            "Present".equalsIgnoreCase(s.getStatus()))
                .count();
            if (lblCheckedInToday != null) {
                lblCheckedInToday.setText(String.valueOf(checkedIn));
            }

            // On Leave
            long onLeave = staffData.stream()
                .filter(s -> "On Leave".equalsIgnoreCase(s.getStatus()))
                .count();
            if (lblOnLeave != null) {
                lblOnLeave.setText(String.valueOf(onLeave));
            }

            // Pending Salary
            double totalPending = 0.0;
            YearMonth currentMonth = YearMonth.now();
            for (Staff staff : staffData) {
                try {
                    StaffPayrollInfo payroll = payrollService.calculatePayroll(staff.getId(), currentMonth);
                    totalPending += payroll.getPendingSalary();
                } catch (SQLException e) {
                    // Skip this staff member
                }
            }
            if (lblPendingSalary != null) {
                lblPendingSalary.setText("₹" + String.format("%,.0f", totalPending));
            }
        } catch (Exception e) {
            System.err.println("Error updating summary cards: " + e.getMessage());
        }
    }

    private String getTodayStatus(Long staffId) {
        try {
            StaffPayrollInfo info = payrollService.calculatePayroll(staffId, YearMonth.now());
            return info.getStatus();
        } catch (SQLException e) {
            return "Not Checked-in";
        }
    }

    private String getCheckInTime(Long staffId) {
        try {
            com.example.pos.model.StaffAttendance attendance = payrollService.getTodayAttendance(staffId);
            if (attendance != null && attendance.getCheckInTime() != null) {
                return attendance.getCheckInTime().format(TIME_FMT);
            }
        } catch (SQLException e) {
            // Ignore and return default
        }
        return "-";
    }

    /* ---------- Event Handlers ---------- */

    private void setupEventHandlers() {
        btnExport.setOnAction(this::onExportAttendance);
        btnAddStaff.setOnAction(this::onAddStaff);
        
        if (btnCalculatePayroll != null) {
            btnCalculatePayroll.setOnAction(this::onCalculatePayroll);
        }
        
        if (btnPaySalary != null) {
            btnPaySalary.setOnAction(this::onPaySalary);
        }
        
        if (monthSelector != null) {
            monthSelector.setOnAction(e -> loadPayrollData());
        }
    }

    private void onCalculatePayroll(ActionEvent event) {
        loadPayrollData();
        showInfo("Payroll Calculated", "Payroll has been calculated for the selected month.");
    }

    private void onPaySalary(ActionEvent event) {
        PayrollRow selected = payrollTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a staff member to pay salary.");
            return;
        }

        Dialog<Double> dialog = createPaySalaryDialog(selected);
        dialog.showAndWait().ifPresent(amount -> {
            try {
                String selectedMonth = monthSelector.getValue();
                YearMonth month = YearMonth.parse(selectedMonth);
                
                payrollService.recordSalaryPayment(
                    selected.getStaffId(),
                    amount,
                    "Payment",
                    month,
                    "Salary payment via UI"
                );
                
                showInfo("Payment Recorded", 
                    String.format("Payment of ₹%.2f recorded for %s", amount, selected.getName()));
                loadPayrollData(); // Refresh
            } catch (SQLException e) {
                showError("Payment Failed", "Failed to record payment: " + e.getMessage());
            }
        });
    }

    private Dialog<Double> createPaySalaryDialog(PayrollRow staff) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Pay Salary");
        dialog.setHeaderText("Pay salary to " + staff.getName());

        ButtonType payButtonType = new ButtonType("Pay", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(payButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField amountField = new TextField(String.format("%.2f", staff.getPendingSalary()));
        ComboBox<String> paymentTypeCombo = new ComboBox<>();
        paymentTypeCombo.getItems().addAll("Full Payment", "Advance", "Partial Payment");
        paymentTypeCombo.setValue("Full Payment");

        grid.add(new Label("Amount:"), 0, 0);
        grid.add(amountField, 1, 0);
        grid.add(new Label("Payment Type:"), 0, 1);
        grid.add(paymentTypeCombo, 1, 1);
        grid.add(new Label("Pending: ₹" + String.format("%.2f", staff.getPendingSalary())), 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == payButtonType) {
                try {
                    return Double.parseDouble(amountField.getText().trim());
                } catch (NumberFormatException e) {
                    showError("Invalid Amount", "Please enter a valid amount.");
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }

    private void onExportAttendance(ActionEvent event) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Attendance");
            fileChooser.setInitialFileName("attendance_" + LocalDate.now() + ".csv");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            
            File file = fileChooser.showSaveDialog(btnExport.getScene().getWindow());
            if (file != null) {
                exportAttendanceToCSV(file);
                showInfo("Export Successful", "Attendance data exported to:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showError("Export Failed", "Failed to export attendance: " + e.getMessage());
        }
    }

    private void onAddStaff(ActionEvent event) {
        Dialog<StaffMember> dialog = createAddStaffDialog();
        dialog.showAndWait().ifPresent(staff -> {
            try {
                long staffId = payrollService.addStaff(staff);
                showInfo("Success", "Staff member added successfully!\nID: " + staffId);
                loadStaffData(); // Refresh the table
            } catch (SQLException e) {
                showError("Failed to add staff", e.getMessage());
            }
        });
    }

    /* ---------- Add Staff Dialog ---------- */

    private Dialog<StaffMember> createAddStaffDialog() {
        Dialog<StaffMember> dialog = new Dialog<>();
        dialog.setTitle("Add New Staff Member");
        dialog.setHeaderText("Enter staff member details");

        // Set button types
        ButtonType addButtonType = new ButtonType("Add", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");
        
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Chef", "Waiter", "Manager", "Cashier", "Kitchen Helper", "Cleaner");
        roleCombo.setPromptText("Select Role");
        
        ComboBox<String> shiftCombo = new ComboBox<>();
        shiftCombo.getItems().addAll("Morning", "Evening", "Full Day", "Night");
        shiftCombo.setPromptText("Select Shift");
        
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number");
        
        TextField emailField = new TextField();
        emailField.setPromptText("Email (optional)");
        
        TextField salaryField = new TextField();
        salaryField.setPromptText("Monthly Salary");
        
        DatePicker joinDatePicker = new DatePicker(LocalDate.now());
        
        TextField leavesField = new TextField("2");
        leavesField.setPromptText("Allowed Leaves per Month");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Role:"), 0, 1);
        grid.add(roleCombo, 1, 1);
        grid.add(new Label("Shift:"), 0, 2);
        grid.add(shiftCombo, 1, 2);
        grid.add(new Label("Phone:"), 0, 3);
        grid.add(phoneField, 1, 3);
        grid.add(new Label("Email:"), 0, 4);
        grid.add(emailField, 1, 4);
        grid.add(new Label("Monthly Salary:"), 0, 5);
        grid.add(salaryField, 1, 5);
        grid.add(new Label("Join Date:"), 0, 6);
        grid.add(joinDatePicker, 1, 6);
        grid.add(new Label("Allowed Leaves:"), 0, 7);
        grid.add(leavesField, 1, 7);

        dialog.getDialogPane().setContent(grid);

        // Enable/disable add button based on validation
        Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);

        // Validation
        nameField.textProperty().addListener((obs, old, newVal) -> 
            addButton.setDisable(newVal.trim().isEmpty() || roleCombo.getValue() == null || 
                                 shiftCombo.getValue() == null || salaryField.getText().trim().isEmpty())
        );
        roleCombo.valueProperty().addListener((obs, old, newVal) -> 
            addButton.setDisable(nameField.getText().trim().isEmpty() || newVal == null || 
                                 shiftCombo.getValue() == null || salaryField.getText().trim().isEmpty())
        );
        shiftCombo.valueProperty().addListener((obs, old, newVal) -> 
            addButton.setDisable(nameField.getText().trim().isEmpty() || roleCombo.getValue() == null || 
                                 newVal == null || salaryField.getText().trim().isEmpty())
        );
        salaryField.textProperty().addListener((obs, old, newVal) -> 
            addButton.setDisable(nameField.getText().trim().isEmpty() || roleCombo.getValue() == null || 
                                 shiftCombo.getValue() == null || newVal.trim().isEmpty())
        );

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    StaffMember staff = new StaffMember();
                    staff.setName(nameField.getText().trim());
                    staff.setRole(roleCombo.getValue());
                    staff.setShift(shiftCombo.getValue());
                    staff.setPhone(phoneField.getText().trim());
                    staff.setEmail(emailField.getText().trim());
                    staff.setMonthlySalary(Double.parseDouble(salaryField.getText().trim()));
                    staff.setJoinDate(joinDatePicker.getValue());
                    staff.setAllowedLeaves(Integer.parseInt(leavesField.getText().trim()));
                    staff.setActive(true);
                    return staff;
                } catch (NumberFormatException e) {
                    showError("Invalid Input", "Please enter valid numbers for salary and leaves.");
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }

    /* ---------- Export Functionality ---------- */

    private void exportAttendanceToCSV(File file) throws IOException, SQLException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Write header
            writer.println("Staff ID,Name,Role,Shift,Status,Check-in Time,Date");
            
            // Write current attendance data
            LocalDate today = LocalDate.now();
            for (Staff staff : staffData) {
                writer.printf("%d,%s,%s,%s,%s,%s,%s%n",
                    staff.getId(),
                    escapeCsv(staff.getName()),
                    escapeCsv(staff.getRole()),
                    escapeCsv(staff.getShift()),
                    escapeCsv(staff.getStatus()),
                    escapeCsv(staff.getCheckInTime()),
                    today
                );
            }
            
            // Add monthly summary
            writer.println();
            writer.println("Monthly Payroll Summary - " + YearMonth.now());
            writer.println("Staff ID,Name,Role,Monthly Salary,Days Worked,Earned Salary,Advance Paid,Pending Salary");
            
            for (Staff staff : staffData) {
                StaffPayrollInfo payroll = payrollService.calculatePayroll(staff.getId(), YearMonth.now());
                writer.printf("%d,%s,%s,%.2f,%d,%.2f,%.2f,%.2f%n",
                    staff.getId(),
                    escapeCsv(payroll.getName()),
                    escapeCsv(payroll.getRole()),
                    payroll.getMonthlySalary(),
                    payroll.getDaysWorked(),
                    payroll.getEarnedSalary(),
                    payroll.getAdvancePaid(),
                    payroll.getPendingSalary()
                );
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /* ---------- Status Management ---------- */

    public void updateStatus(Staff staff, String newStatus) {
        if (staff != null) {
            try {
                LocalDate today = LocalDate.now();
                LocalTime now = LocalTime.now();
                
                if ("Checked-in".equals(newStatus)) {
                    payrollService.markAttendance(staff.getId(), today, now, "Present");
                    staff.setCheckInTime(now.format(TIME_FMT));
                } else if ("On Leave".equals(newStatus)) {
                    payrollService.recordLeave(staff.getId(), today, "Casual Leave", "Marked from UI");
                    staff.setCheckInTime("-");
                } else if ("On Break".equals(newStatus)) {
                    payrollService.markAttendance(staff.getId(), today, null, "On Break");
                }
                
                staff.setStatus(newStatus);
                attendanceTable.refresh();
                
                System.out.println("Updated " + staff.getName() + " status to: " + newStatus);
            } catch (SQLException e) {
                showError("Failed to update status", e.getMessage());
            }
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /* ---------- Staff Model ---------- */

    public static class Staff {
        private Long id;
        private final StringProperty name = new SimpleStringProperty();
        private final StringProperty role = new SimpleStringProperty();
        private final StringProperty shift = new SimpleStringProperty();
        private final StringProperty status = new SimpleStringProperty();
        private final StringProperty checkInTime = new SimpleStringProperty();

        public Staff(Long id, String name, String role, String shift, String status, String checkInTime) {
            this.id = id;
            setName(name);
            setRole(role);
            setShift(shift);
            setStatus(status);
            setCheckInTime(checkInTime);
        }

        public Long getId() { return id; }

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

    /* ---------- Payroll Row Model ---------- */

    public static class PayrollRow {
        private final Long staffId;
        private final StringProperty name = new SimpleStringProperty();
        private final StringProperty role = new SimpleStringProperty();
        private final SimpleDoubleProperty monthlySalary = new SimpleDoubleProperty();
        private final SimpleIntegerProperty daysWorked = new SimpleIntegerProperty();
        private final SimpleDoubleProperty earnedSalary = new SimpleDoubleProperty();
        private final SimpleDoubleProperty advancePaid = new SimpleDoubleProperty();
        private final SimpleDoubleProperty pendingSalary = new SimpleDoubleProperty();
        private final StringProperty status = new SimpleStringProperty();

        public PayrollRow(Long staffId, String name, String role, double monthlySalary,
                         int daysWorked, double earnedSalary, double advancePaid,
                         double pendingSalary, String status) {
            this.staffId = staffId;
            setName(name);
            setRole(role);
            setMonthlySalary(monthlySalary);
            setDaysWorked(daysWorked);
            setEarnedSalary(earnedSalary);
            setAdvancePaid(advancePaid);
            setPendingSalary(pendingSalary);
            setStatus(status);
        }

        public Long getStaffId() { return staffId; }

        public String getName() { return name.get(); }
        public void setName(String value) { name.set(value); }
        public StringProperty nameProperty() { return name; }

        public String getRole() { return role.get(); }
        public void setRole(String value) { role.set(value); }
        public StringProperty roleProperty() { return role; }

        public double getMonthlySalary() { return monthlySalary.get(); }
        public void setMonthlySalary(double value) { monthlySalary.set(value); }
        public SimpleDoubleProperty monthlySalaryProperty() { return monthlySalary; }

        public int getDaysWorked() { return daysWorked.get(); }
        public void setDaysWorked(int value) { daysWorked.set(value); }
        public SimpleIntegerProperty daysWorkedProperty() { return daysWorked; }

        public double getEarnedSalary() { return earnedSalary.get(); }
        public void setEarnedSalary(double value) { earnedSalary.set(value); }
        public SimpleDoubleProperty earnedSalaryProperty() { return earnedSalary; }

        public double getAdvancePaid() { return advancePaid.get(); }
        public void setAdvancePaid(double value) { advancePaid.set(value); }
        public SimpleDoubleProperty advancePaidProperty() { return advancePaid; }

        public double getPendingSalary() { return pendingSalary.get(); }
        public void setPendingSalary(double value) { pendingSalary.set(value); }
        public SimpleDoubleProperty pendingSalaryProperty() { return pendingSalary; }

        public String getStatus() { return status.get(); }
        public void setStatus(String value) { status.set(value); }
        public StringProperty statusProperty() { return status; }
    }
}
