package com.example.pos.controller;

import com.example.pos.service.DashboardService;
import com.example.pos.service.DashboardService.*;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardController {
    
    private final DashboardService dashboardService = new DashboardService();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
    
    // Summary cards
    @FXML private VBox cardTotalSales;
    @FXML private VBox cardTotalOrders;
    @FXML private VBox cardPendingOrders;
    @FXML private VBox cardActiveTables;
    @FXML private VBox cardLowStock;
    
    // Summary card labels
    @FXML private Label lblTotalSales;
    @FXML private Label lblTotalOrders;
    @FXML private Label lblPendingOrders;
    @FXML private Label lblActiveTables;
    @FXML private Label lblLowStockItems;
    @FXML private Label lblSalesGrowth;
    @FXML private Label lblOrdersGrowth;
    @FXML private Label lblPendingGrowth;

    @FXML
    private void initialize() {
        setupTable();
        setupEventHandlers();
        setupCardClickHandlers();
        
        if (scrollPane != null) {
            double scrollSpeed = 0.025;
            scrollPane.getContent().setOnScroll(event -> {
                double deltaY = event.getDeltaY() * scrollSpeed;
                scrollPane.setVvalue(scrollPane.getVvalue() - deltaY);
            });
        }
        
        // Load real data asynchronously
        loadDashboardData();
    }

    /* ---------- Data Loading ---------- */
    
    private void loadDashboardData() {
        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                DashboardSummary summary = dashboardService.getDashboardSummary();
                List<DailySales> dailySales = dashboardService.getDailySalesTrend();
                List<CategorySales> categorySales = dashboardService.getCategorySales();
                List<RecentOrder> recentOrders = dashboardService.getRecentOrders(10);
                return new DashboardData(summary, dailySales, categorySales, recentOrders);
            }
        };
        
        task.setOnSucceeded(e -> {
            DashboardData data = task.getValue();
            updateSummaryCards(data.summary);
            updateSalesChart(data.dailySales);
            updateCategoryChart(data.categorySales);
            updateRecentOrders(data.recentOrders);
        });
        
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("Failed to load dashboard data: " + (ex != null ? ex.getMessage() : "Unknown error"));
            if (ex != null) ex.printStackTrace();
            // Load dummy data as fallback
            loadDummyData();
        });
        
        executor.submit(task);
    }
    
    private void updateSummaryCards(DashboardSummary summary) {
        Platform.runLater(() -> {
            if (lblTotalSales != null) {
                lblTotalSales.setText("₹" + String.format("%,.0f", summary.totalSales));
            }
            if (lblTotalOrders != null) {
                lblTotalOrders.setText(String.valueOf(summary.totalOrders));
            }
            if (lblPendingOrders != null) {
                lblPendingOrders.setText(String.valueOf(summary.pendingOrders));
            }
            if (lblActiveTables != null) {
                lblActiveTables.setText(summary.activeTables);
            }
            if (lblLowStockItems != null) {
                lblLowStockItems.setText(String.valueOf(summary.lowStockItems));
            }
            if (lblSalesGrowth != null) {
                lblSalesGrowth.setText(formatGrowth(summary.salesGrowth));
            }
            if (lblOrdersGrowth != null) {
                lblOrdersGrowth.setText(formatGrowth(summary.ordersGrowth));
            }
            if (lblPendingGrowth != null) {
                lblPendingGrowth.setText(formatGrowth(summary.pendingGrowth));
            }
        });
    }
    
    private void updateSalesChart(List<DailySales> dailySales) {
        Platform.runLater(() -> {
            if (salesChart == null || salesXAxis == null) return;
            
            ObservableList<String> categories = FXCollections.observableArrayList();
            XYChart.Series<String, Number> salesSeries = new XYChart.Series<>();
            salesSeries.setName("Daily Sales");
            
            for (DailySales sales : dailySales) {
                String dayName = sales.date().getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                categories.add(dayName);
                salesSeries.getData().add(new XYChart.Data<>(dayName, sales.totalSales()));
            }
            
            salesXAxis.setCategories(categories);
            salesChart.getData().clear();
            salesChart.getData().add(salesSeries);
            salesChart.setLegendVisible(false);
        });
    }
    
    private void updateCategoryChart(List<CategorySales> categorySales) {
        Platform.runLater(() -> {
            if (categoryChart == null) return;
            
            ObservableList<PieChart.Data> categoryData = FXCollections.observableArrayList();
            for (CategorySales sales : categorySales) {
                categoryData.add(new PieChart.Data(sales.category(), sales.totalSales()));
            }
            
            categoryChart.setData(categoryData);
            categoryChart.setLegendVisible(true);
        });
    }
    
    private void updateRecentOrders(List<RecentOrder> orders) {
        Platform.runLater(() -> {
            recentOrders.clear();
            for (RecentOrder order : orders) {
                recentOrders.add(new OrderRecord(
                    order.billNumber(),
                    order.tableInfo(),
                    "₹" + String.format("%,.0f", order.total()),
                    order.status(),
                    order.timeAgo()
                ));
            }
        });
    }
    
    private String formatGrowth(double growth) {
        String sign = growth >= 0 ? "+" : "";
        return sign + String.format("%.1f", growth) + "%";
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

    private void loadDummyData() {
        // Fallback dummy data if database fails
        recentOrders.setAll(
            new OrderRecord("#12345", "Table 5", "₹1,250", "Completed", "2 min ago"),
            new OrderRecord("#12344", "Table 12", "₹3,400", "Preparing", "5 min ago"),
            new OrderRecord("#12343", "Takeaway", "₹850", "Ready", "8 min ago"),
            new OrderRecord("#12342", "Table 3", "₹2,100", "Completed", "12 min ago"),
            new OrderRecord("#12341", "Online", "₹1,900", "Delivered", "15 min ago")
        );
        
        // Dummy sales chart
        if (salesChart != null && salesXAxis != null) {
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
        }
        
        // Dummy category chart
        if (categoryChart != null) {
            ObservableList<PieChart.Data> categoryData = FXCollections.observableArrayList(
                new PieChart.Data("Food", 45),
                new PieChart.Data("Beverages", 25),
                new PieChart.Data("Desserts", 20),
                new PieChart.Data("Other", 10)
            );
            categoryChart.setData(categoryData);
            categoryChart.setLegendVisible(true);
        }
    }

    /* ---------- Event Handlers ---------- */

    private void setupEventHandlers() {
        if (btnNewBill != null) btnNewBill.setOnAction(this::onNewBill);
        if (btnKOT != null) btnKOT.setOnAction(this::onKOT);
        if (btnInventory != null) btnInventory.setOnAction(this::onInventory);
        if (btnOnlineOrders != null) btnOnlineOrders.setOnAction(this::onOnlineOrders);
        if (btnViewAllOrders != null) btnViewAllOrders.setOnAction(this::onViewAllOrders);
    }
    
    private void setupCardClickHandlers() {
        // Make summary cards clickable
        if (cardTotalSales != null) {
            cardTotalSales.setOnMouseClicked(e -> navigateToView("TransactionsView.fxml"));
            cardTotalSales.setStyle(cardTotalSales.getStyle() + "; -fx-cursor: hand;");
        }
        if (cardTotalOrders != null) {
            cardTotalOrders.setOnMouseClicked(e -> navigateToView("TransactionsView.fxml"));
            cardTotalOrders.setStyle(cardTotalOrders.getStyle() + "; -fx-cursor: hand;");
        }
        if (cardPendingOrders != null) {
            cardPendingOrders.setOnMouseClicked(e -> navigateToView("TransactionsView.fxml"));
            cardPendingOrders.setStyle(cardPendingOrders.getStyle() + "; -fx-cursor: hand;");
        }
        if (cardActiveTables != null) {
            cardActiveTables.setOnMouseClicked(e -> navigateToView("TablesView.fxml"));
            cardActiveTables.setStyle(cardActiveTables.getStyle() + "; -fx-cursor: hand;");
        }
        if (cardLowStock != null) {
            cardLowStock.setOnMouseClicked(e -> navigateToView("InventoryView.fxml"));
            cardLowStock.setStyle(cardLowStock.getStyle() + "; -fx-cursor: hand;");
        }
    }

    private void onNewBill(ActionEvent event) {
        navigateToView("BillingView.fxml");
    }

    private void onKOT(ActionEvent event) {
        navigateToView("KOTView.fxml");
    }

    private void onInventory(ActionEvent event) {
        navigateToView("InventoryView.fxml");
    }

    private void onOnlineOrders(ActionEvent event) {
        // For now, navigate to transactions
        navigateToView("TransactionsView.fxml");
    }

    private void onViewAllOrders(ActionEvent event) {
        navigateToView("TransactionsView.fxml");
    }
    
    private void navigateToView(String fxmlName) {
        try {
            // Find the main content area (StackPane in MainLayout)
            Node currentNode = scrollPane;
            while (currentNode.getParent() != null) {
                currentNode = currentNode.getParent();
                if (currentNode.getId() != null && currentNode.getId().equals("contentArea")) {
                    // Found the content area
                    javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) currentNode;
                    Node view = FXMLLoader.load(getClass().getResource("/com/example/pos/view/" + fxmlName));
                    contentArea.getChildren().setAll(view);
                    return;
                }
            }
            System.err.println("Could not find content area for navigation");
        } catch (IOException e) {
            System.err.println("Failed to load view: " + fxmlName);
            e.printStackTrace();
        }
    }

    /* ---------- Public Methods ---------- */

    public void refreshData() {
        dashboardService.clearCache();
        loadDashboardData();
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
    
    /* ---------- Data Container ---------- */
    
    private record DashboardData(DashboardSummary summary, List<DailySales> dailySales,
                                List<CategorySales> categorySales, List<RecentOrder> recentOrders) {}
}
