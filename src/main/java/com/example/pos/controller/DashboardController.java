package com.example.pos.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class DashboardController {

    @FXML private Button btnNewBill;
    @FXML private Button btnKOT;
    @FXML private Button btnInventory;
    @FXML private Button btnOnlineOrders;
    @FXML private Button btnViewAllOrders;

    @FXML private LineChart<String, Number> salesChart;
    @FXML private CategoryAxis salesXAxis;
    @FXML private PieChart categoryChart;

    @FXML private TableView<OrderRecord> recentOrdersTable;
    @FXML private TableColumn<OrderRecord, String> colOrderId;
    @FXML private TableColumn<OrderRecord, String> colTableType;
    @FXML private TableColumn<OrderRecord, String> colAmount;
    @FXML private TableColumn<OrderRecord, String> colStatus;
    @FXML private TableColumn<OrderRecord, String> colTime;

    private final ObservableList<OrderRecord> recentOrders = FXCollections.observableArrayList();

    @FXML private ScrollPane scrollPane;

    @FXML
    private void initialize() {
        setupCharts();
        setupTable();
        loadDummyData();
        setupEventHandlers();

        if (scrollPane == null) {
            System.err.println("ScrollPane not injected! Check fx:id in FXML.");
            return;
        }
    
        double scrollSpeed = 0.025;
        scrollPane.getContent().setOnScroll(event -> {
            double deltaY = event.getDeltaY() * scrollSpeed;
            scrollPane.setVvalue(scrollPane.getVvalue() - deltaY);
        });
    }

    /* ---------- Chart Setup ---------- */

    private void setupCharts() {
        // Setup Sales Chart
        salesXAxis.setCategories(FXCollections.observableArrayList(
            "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
        ));
        
        XYChart.Series<String, Number> salesSeries = new XYChart.Series<>();
        salesSeries.setName("Daily Sales");
        salesSeries.getData().addAll(
            new XYChart.Data<>("Mon", 4000),
            new XYChart.Data<>("Tue", 3500),
            new XYChart.Data<>("Wed", 5500),
            new XYChart.Data<>("Thu", 6500),
            new XYChart.Data<>("Fri", 7500),
            new XYChart.Data<>("Sat", 8500),
            new XYChart.Data<>("Sun", 7200)
        );
        
        salesChart.getData().add(salesSeries);
        salesChart.setLegendVisible(false);

        // Setup Category Chart
        ObservableList<PieChart.Data> categoryData = FXCollections.observableArrayList(
            new PieChart.Data("Food", 45),
            new PieChart.Data("Beverages", 25),
            new PieChart.Data("Desserts", 20),
            new PieChart.Data("Other", 10)
        );
        
        categoryChart.setData(categoryData);
        categoryChart.setLegendVisible(true);
    }

    /* ---------- Table Setup ---------- */

    private void setupTable() {
        colOrderId.setCellValueFactory(data -> data.getValue().orderIdProperty());
        colTableType.setCellValueFactory(data -> data.getValue().tableTypeProperty());
        colAmount.setCellValueFactory(data -> data.getValue().amountProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());
        colTime.setCellValueFactory(data -> data.getValue().timeProperty());

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
                return switch (status.toLowerCase()) {
                    case "completed" -> "completed";
                    case "preparing" -> "preparing";
                    case "ready" -> "ready";
                    case "delivered" -> "delivered";
                    default -> "completed";
                };
            }
        });

        recentOrdersTable.setItems(recentOrders);
    }

    /* ---------- Data Loading ---------- */

    private void loadDummyData() {
        recentOrders.setAll(
            new OrderRecord("#12345", "Table 5", "₹1,250", "Completed", "2 min ago"),
            new OrderRecord("#12344", "Table 12", "₹3,400", "Preparing", "5 min ago"),
            new OrderRecord("#12343", "Takeaway", "₹850", "Ready", "8 min ago"),
            new OrderRecord("#12342", "Table 3", "₹2,100", "Completed", "12 min ago"),
            new OrderRecord("#12341", "Online", "₹1,900", "Delivered", "15 min ago")
        );
    }

    /* ---------- Event Handlers ---------- */

    private void setupEventHandlers() {
        btnNewBill.setOnAction(this::onNewBill);
        btnKOT.setOnAction(this::onKOT);
        btnInventory.setOnAction(this::onInventory);
        btnOnlineOrders.setOnAction(this::onOnlineOrders);
        btnViewAllOrders.setOnAction(this::onViewAllOrders);
    }

    private void onNewBill(ActionEvent event) {
        System.out.println("Opening New Bill...");
        // TODO: Navigate to BillingView
    }

    private void onKOT(ActionEvent event) {
        System.out.println("Opening KOT...");
        // TODO: Navigate to KOTView
    }

    private void onInventory(ActionEvent event) {
        System.out.println("Opening Inventory...");
        // TODO: Navigate to InventoryView
    }

    private void onOnlineOrders(ActionEvent event) {
        System.out.println("Opening Online Orders...");
        // TODO: Navigate to Online Orders view
    }

    private void onViewAllOrders(ActionEvent event) {
        System.out.println("Viewing all orders...");
        // TODO: Navigate to Orders/Transactions view
    }

    /* ---------- Public Methods ---------- */

    public void refreshData() {
        // TODO: Implement data refresh logic
        System.out.println("Refreshing dashboard data...");
    }

    public void updateQuickActionCounts(int kotCount, int inventoryCount, int onlineOrdersCount) {
        // Update button graphics with counts
        updateButtonCount(btnKOT, kotCount);
        updateButtonCount(btnInventory, inventoryCount);
        updateButtonCount(btnOnlineOrders, onlineOrdersCount);
    }

    private void updateButtonCount(Button button, int count) {
        // Find the action-count label in the button's graphic
        if (button.getGraphic() instanceof Label label) {
            label.setText(String.valueOf(count));
        }
    }

    /* ---------- Order Record Model ---------- */

    public static class OrderRecord {
        private final StringProperty orderId = new SimpleStringProperty();
        private final StringProperty tableType = new SimpleStringProperty();
        private final StringProperty amount = new SimpleStringProperty();
        private final StringProperty status = new SimpleStringProperty();
        private final StringProperty time = new SimpleStringProperty();

        public OrderRecord(String orderId, String tableType, String amount, String status, String time) {
            setOrderId(orderId);
            setTableType(tableType);
            setAmount(amount);
            setStatus(status);
            setTime(time);
        }

        // Order ID
        public String getOrderId() { return orderId.get(); }
        public void setOrderId(String value) { orderId.set(value); }
        public StringProperty orderIdProperty() { return orderId; }

        // Table Type
        public String getTableType() { return tableType.get(); }
        public void setTableType(String value) { tableType.set(value); }
        public StringProperty tableTypeProperty() { return tableType; }

        // Amount
        public String getAmount() { return amount.get(); }
        public void setAmount(String value) { amount.set(value); }
        public StringProperty amountProperty() { return amount; }

        // Status
        public String getStatus() { return status.get(); }
        public void setStatus(String value) { status.set(value); }
        public StringProperty statusProperty() { return status; }

        // Time
        public String getTime() { return time.get(); }
        public void setTime(String value) { time.set(value); }
        public StringProperty timeProperty() { return time; }
    }
}
