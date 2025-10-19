package com.example.pos.controller;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import com.example.pos.model.Transaction;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

public class TransactionHistoryController implements Initializable {

    // Header Controls
    @FXML
    private Button btnExportExcel;
    @FXML
    private Button btnExportPDF;

    // Filter Controls
    @FXML
    private TextField txtSearch;
    @FXML
    private DatePicker dateFrom;
    @FXML
    private DatePicker dateTo;
    @FXML
    private ComboBox<String> cmbPaymentMode;
    @FXML
    private ComboBox<String> cmbStatus;
    @FXML
    private Button btnClearFilters;

    // Table
    @FXML
    private TableView<Transaction> tableTransactions;
    @FXML
    private TableColumn<Transaction, String> colBillNumber;
    @FXML
    private TableColumn<Transaction, String> colDateTime;
    @FXML
    private TableColumn<Transaction, String> colCustomer;
    @FXML
    private TableColumn<Transaction, String> colType;
    @FXML
    private TableColumn<Transaction, String> colAmount;
    @FXML
    private TableColumn<Transaction, String> colPaymentMode;
    @FXML
    private TableColumn<Transaction, String> colStatus;
    @FXML
    private TableColumn<Transaction, Void> colActions;

    // Pagination
    @FXML
    private Label lblPaginationInfo;
    @FXML
    private Button btnPrevious;
    @FXML
    private Button btnPage1;
    @FXML
    private Button btnPage2;
    @FXML
    private Button btnPage3;
    @FXML
    private Button btnNext;

    private ObservableList<Transaction> transactionList;
    private FilteredList<Transaction> filteredData;
    private int currentPage = 1;
    private final int itemsPerPage = 12;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        loadSampleData();
        setupFilters();
        setupPagination();
        setupEventHandlers();
    }

    private void setupTableColumns() {
        // Bill Number Column
        colBillNumber.setCellValueFactory(new PropertyValueFactory<>("billNumber"));
        colBillNumber.setCellFactory(col -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-cursor: hand;");
                }
            }
        });

        // Date & Time Column
        colDateTime.setCellValueFactory(cellData -> {
            LocalDateTime dateTime = cellData.getValue().getDateTime();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            String formatted = dateTime.format(dateFormatter) + "\n" + dateTime.format(timeFormatter);
            return new SimpleStringProperty(formatted);
        });
        colDateTime.setCellFactory(col -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
                }
            }
        });

        // Customer Column
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        // Type Column
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setCellFactory(col -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");
                }
            }
        });

        // Amount Column
        colAmount.setCellValueFactory(cellData
                -> new SimpleStringProperty("₹" + String.format("%,d", cellData.getValue().getAmount()))
        );
        colAmount.setCellFactory(col -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER_RIGHT);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #1f2937;");
                }
            }
        });

        // Payment Mode Column
        colPaymentMode.setCellValueFactory(new PropertyValueFactory<>("paymentMode"));
        colPaymentMode.setCellFactory(col -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setGraphic(null);
                } else {
                    Label label = new Label(item);
                    label.setAlignment(Pos.CENTER);
                    label.setMaxWidth(Double.MAX_VALUE);

                    String bgColor = switch (item) {
                        case "Card" ->
                            "#dbeafe";
                        case "Cash" ->
                            "#d1fae5";
                        case "UPI" ->
                            "#e9d5ff";
                        case "Online" ->
                            "#fef3c7";
                        default ->
                            "#f3f4f6";
                    };
                    String textColor = switch (item) {
                        case "Card" ->
                            "#1e40af";
                        case "Cash" ->
                            "#065f46";
                        case "UPI" ->
                            "#6b21a8";
                        case "Online" ->
                            "#92400e";
                        default ->
                            "#374151";
                    };

                    label.setStyle("-fx-background-color: " + bgColor + "; "
                            + "-fx-text-fill: " + textColor + "; "
                            + "-fx-background-radius: 4; "
                            + "-fx-padding: 4 12 4 12; "
                            + "-fx-font-weight: bold; "
                            + "-fx-font-size: 11px;");

                    setGraphic(label);
                    setAlignment(Pos.CENTER);
                    setText(null);
                }
            }
        });

        // Status Column
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setGraphic(null);
                } else {
                    Label label = new Label(item);
                    label.setAlignment(Pos.CENTER);
                    label.setMaxWidth(Double.MAX_VALUE);

                    String bgColor = item.equals("Completed") ? "#d1fae5" : "#fee2e2";
                    String textColor = item.equals("Completed") ? "#065f46" : "#991b1b";

                    label.setStyle("-fx-background-color: " + bgColor + "; "
                            + "-fx-text-fill: " + textColor + "; "
                            + "-fx-background-radius: 4; "
                            + "-fx-padding: 4 12 4 12; "
                            + "-fx-font-weight: bold; "
                            + "-fx-font-size: 11px;");

                    setGraphic(label);
                    setAlignment(Pos.CENTER);
                    setText(null);
                }
            }
        });

        // Actions Column
        colActions.setCellFactory(col -> new TableCell<Transaction, Void>() {
            private final Button btnView = createActionButton("👁", "#3b82f6");
            private final Button btnPrint = createActionButton("🖨", "#10b981");
            private final Button btnRefund = createActionButton("Refund", "#dc2626");

            {
                btnView.setOnAction(e -> {
                    Transaction t = getTableView().getItems().get(getIndex());
                    viewTransaction(t);
                });

                btnPrint.setOnAction(e -> {
                    Transaction t = getTableView().getItems().get(getIndex());
                    printTransaction(t);
                });

                btnRefund.setOnAction(e -> {
                    Transaction t = getTableView().getItems().get(getIndex());
                    refundTransaction(t);
                });
            }

            private Button createActionButton(String text, String color) {
                Button btn = new Button(text);
                btn.setStyle("-fx-background-color: transparent; "
                        + "-fx-text-fill: " + color + "; "
                        + "-fx-cursor: hand; "
                        + "-fx-font-size: 12px; "
                        + "-fx-padding: 4 8 4 8;");
                btn.setOnMouseEntered(e
                        -> btn.setStyle("-fx-background-color: " + color + "20; "
                                + "-fx-text-fill: " + color + "; "
                                + "-fx-cursor: hand; "
                                + "-fx-font-size: 12px; "
                                + "-fx-padding: 4 8 4 8; "
                                + "-fx-background-radius: 4;"));
                btn.setOnMouseExited(e
                        -> btn.setStyle("-fx-background-color: transparent; "
                                + "-fx-text-fill: " + color + "; "
                                + "-fx-cursor: hand; "
                                + "-fx-font-size: 12px; "
                                + "-fx-padding: 4 8 4 8;"));
                return btn;
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(5, btnView, btnPrint, btnRefund);
                    hbox.setAlignment(Pos.CENTER);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void loadSampleData() {
        transactionList = FXCollections.observableArrayList(
                new Transaction("#12345", LocalDateTime.of(2025, 10, 9, 18, 16), "John Doe", "Dine-in", 1250, "Card", "Completed"),
                new Transaction("#12344", LocalDateTime.of(2025, 10, 9, 15, 30), "Walk-in", "Dine-in", 3400, "Cash", "Completed"),
                new Transaction("#12343", LocalDateTime.of(2025, 10, 9, 14, 10), "Sarah Smith", "Takeaway", 850, "UPI", "Completed"),
                new Transaction("#12342", LocalDateTime.of(2025, 10, 9, 13, 45), "Mike Johnson", "Dine-in", 2100, "Card", "Completed"),
                new Transaction("#12341", LocalDateTime.of(2025, 10, 9, 12, 30), "Zomato Order", "Delivery", 1900, "Online", "Completed"),
                new Transaction("#12340", LocalDateTime.of(2025, 10, 9, 11, 15), "Walk-in", "Takeaway", 450, "Cash", "Completed"),
                new Transaction("#12339", LocalDateTime.of(2025, 10, 9, 10, 0), "Emily Brown", "Dine-in", 2800, "Card", "Refunded"),
                new Transaction("#12338", LocalDateTime.of(2025, 10, 9, 9, 25), "Swiggy Order", "Delivery", 1650, "Online", "Completed"),
                new Transaction("#12337", LocalDateTime.of(2025, 10, 9, 8, 30), "Walk-in", "Takeaway", 890, "UPI", "Completed"),
                new Transaction("#12336", LocalDateTime.of(2025, 10, 9, 7, 15), "David Wilson", "Dine-in", 3200, "Cash", "Completed"),
                new Transaction("#12335", LocalDateTime.of(2025, 10, 8, 20, 45), "Anna Lee", "Dine-in", 1500, "UPI", "Completed"),
                new Transaction("#12334", LocalDateTime.of(2025, 10, 8, 19, 20), "Walk-in", "Takeaway", 750, "Cash", "Completed")
        );

        filteredData = new FilteredList<>(transactionList, p -> true);
        tableTransactions.setItems(filteredData);
    }

    private void setupFilters() {
        // Payment Mode ComboBox
        cmbPaymentMode.setItems(FXCollections.observableArrayList(
                "All", "Cash", "Card", "UPI", "Online"
        ));
        cmbPaymentMode.getSelectionModel().selectFirst();

        // Status ComboBox
        cmbStatus.setItems(FXCollections.observableArrayList(
                "All", "Completed", "Refunded"
        ));
        cmbStatus.getSelectionModel().selectFirst();

        // Add listeners
        txtSearch.textProperty().addListener((obs, old, newVal) -> applyFilters());
        dateFrom.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        dateTo.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        cmbPaymentMode.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        cmbStatus.valueProperty().addListener((obs, old, newVal) -> applyFilters());

        btnClearFilters.setOnAction(e -> clearFilters());
    }

    private void applyFilters() {
        filteredData.setPredicate(transaction -> {
            // Search filter
            String search = txtSearch.getText().toLowerCase();
            boolean matchesSearch = search.isEmpty()
                    || transaction.getBillNumber().toLowerCase().contains(search)
                    || transaction.getCustomerName().toLowerCase().contains(search);

            // Date filter
            LocalDate from = dateFrom.getValue();
            LocalDate to = dateTo.getValue();
            LocalDate txnDate = transaction.getDateTime().toLocalDate();
            boolean matchesDate = true;
            if (from != null && to != null) {
                matchesDate = !txnDate.isBefore(from) && !txnDate.isAfter(to);
            } else if (from != null) {
                matchesDate = !txnDate.isBefore(from);
            } else if (to != null) {
                matchesDate = !txnDate.isAfter(to);
            }

            // Payment mode filter
            String payment = cmbPaymentMode.getValue();
            boolean matchesPayment = payment.equals("All")
                    || transaction.getPaymentMode().equals(payment);

            // Status filter
            String status = cmbStatus.getValue();
            boolean matchesStatus = status.equals("All")
                    || transaction.getStatus().equals(status);

            return matchesSearch && matchesDate && matchesPayment && matchesStatus;
        });

        updatePaginationInfo();
    }

    private void clearFilters() {
        txtSearch.clear();
        dateFrom.setValue(null);
        dateTo.setValue(null);
        cmbPaymentMode.getSelectionModel().selectFirst();
        cmbStatus.getSelectionModel().selectFirst();
    }

    private void setupPagination() {
        btnPage1.setOnAction(e -> goToPage(1));
        btnPage2.setOnAction(e -> goToPage(2));
        btnPage3.setOnAction(e -> goToPage(3));
        btnPrevious.setOnAction(e -> previousPage());
        btnNext.setOnAction(e -> nextPage());

        updatePaginationInfo();
    }

    private void goToPage(int page) {
        currentPage = page;
        updatePaginationButtons();
    }

    private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            updatePaginationButtons();
        }
    }

    private void nextPage() {
        currentPage++;
        updatePaginationButtons();
    }

    private void updatePaginationButtons() {
        btnPage1.getStyleClass().removeAll("pagination-active");
        btnPage2.getStyleClass().removeAll("pagination-active");
        btnPage3.getStyleClass().removeAll("pagination-active");

        switch (currentPage) {
            case 1 ->
                btnPage1.getStyleClass().add("pagination-active");
            case 2 ->
                btnPage2.getStyleClass().add("pagination-active");
            case 3 ->
                btnPage3.getStyleClass().add("pagination-active");
        }

        updatePaginationInfo();
    }

    private void updatePaginationInfo() {
        int total = filteredData.size();
        int showing = Math.min(itemsPerPage, total);
        lblPaginationInfo.setText("Showing 1 to " + showing + " of " + total + " results");
    }

    private void setupEventHandlers() {
        btnExportExcel.setOnAction(e -> exportToExcel());
        btnExportPDF.setOnAction(e -> exportToPDF());
    }

    // Action Methods
    private void viewTransaction(Transaction transaction) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Transaction Details");
        alert.setHeaderText("Bill: " + transaction.getBillNumber());
        alert.setContentText(
                "Customer: " + transaction.getCustomerName() + "\n"
                + "Type: " + transaction.getType() + "\n"
                + "Amount: ₹" + transaction.getAmount() + "\n"
                + "Payment: " + transaction.getPaymentMode() + "\n"
                + "Status: " + transaction.getStatus() + "\n"
                + "Date: " + transaction.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
        alert.showAndWait();
    }

    private void printTransaction(Transaction transaction) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Print");
        alert.setHeaderText("Print Transaction");
        alert.setContentText("Printing bill " + transaction.getBillNumber() + "...");
        alert.showAndWait();
    }

    private void refundTransaction(Transaction transaction) {
        if (transaction.getStatus().equals("Refunded")) {
            showAlert("Already Refunded", "This transaction has already been refunded.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Refund Transaction");
        confirm.setHeaderText("Refund Bill: " + transaction.getBillNumber());
        confirm.setContentText("Are you sure you want to refund ₹" + transaction.getAmount() + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                transaction.setStatus("Refunded");
                tableTransactions.refresh();
                showAlert("Success", "Transaction refunded successfully!");
            }
        });
    }

    private void exportToExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export to Excel");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );
        fc.setInitialFileName("transactions.xlsx");

        File file = fc.showSaveDialog(tableTransactions.getScene().getWindow());
        if (file != null) {
            showAlert("Export Success", "Transactions exported to:\n" + file.getAbsolutePath());
        }
    }

    private void exportToPDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export to PDF");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        fc.setInitialFileName("transactions.pdf");

        File file = fc.showSaveDialog(tableTransactions.getScene().getWindow());
        if (file != null) {
            showAlert("Export Success", "Transactions exported to:\n" + file.getAbsolutePath());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
