package com.example.pos.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

public class BillingController {

    // Left Panel - Menu Items
    @FXML
    private ToggleButton dineInBtn;
    @FXML
    private ToggleButton takeawayBtn;
    @FXML
    private ToggleButton onlineBtn;
    @FXML
    private ToggleButton deliveryBtn;
    @FXML
    private TextField tableNumberField;
    @FXML
    private TextField searchField;
    @FXML
    private ToggleButton allBtn;
    @FXML
    private ToggleButton beveragesBtn;
    @FXML
    private ToggleButton mainCourseBtn;
    @FXML
    private ToggleButton snacksBtn;
    @FXML
    private ToggleButton dessertsBtn;
    @FXML
    private ToggleButton startersBtn;
    @FXML
    private TilePane menuGrid;

    // Center Panel - Order Details
    @FXML
    private Label billNumberLabel;
    @FXML
    private Label orderDateLabel;
    @FXML
    private TableView<OrderItem> orderTableView;
    @FXML
    private TableColumn<OrderItem, String> itemColumn;
    @FXML
    private TableColumn<OrderItem, Integer> qtyColumn;
    @FXML
    private TableColumn<OrderItem, Double> priceColumn;
    @FXML
    private TableColumn<OrderItem, Double> totalColumn;
    @FXML
    private TableColumn<OrderItem, Void> actionsColumn;
    @FXML
    private TextField customerNameField;
    @FXML
    private Label subtotalLabel;
    @FXML
    private Label taxLabel;
    @FXML
    private Label totalLabel;

    // Action Buttons
    @FXML
    private Button discountBtn;
    @FXML
    private Button couponBtn;
    @FXML
    private Button newBillBtn;
    @FXML
    private Button kotBtn;
    @FXML
    private Button saveBtn;
    @FXML
    private Button cashBtn;
    @FXML
    private Button cardBtn;
    @FXML
    private Button upiBtn;
    @FXML
    private Button settleBtn;

    // Data
    private ObservableList<OrderItem> orderItems = FXCollections.observableArrayList();
    private ObservableList<MenuItem> menuItems = FXCollections.observableArrayList();
    private double subtotal = 0.0;
    private double taxRate = 0.05; // 5% tax
    private String selectedPaymentMethod = "Cash";
    private int currentBillNumber = 12345;

    @FXML
    public void initialize() {
        System.out.println("BillingController initialized");

        // Initialize order table
        setupOrderTable();

        // Load sample menu items
        loadSampleMenuItems();

        // Populate menu grid
        populateMenuGrid();

        // Setup event handlers
        setupEventHandlers();

        // Initialize billing
        updateBilling();
    }

    private void setupOrderTable() {
        // Set up table columns
        itemColumn.setCellValueFactory(cellData
                -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));

        qtyColumn.setCellValueFactory(cellData
                -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getQuantity()));

        priceColumn.setCellValueFactory(cellData
                -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getPrice()));

        totalColumn.setCellValueFactory(cellData
                -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getTotal()));

        // Add action buttons column (delete, +, -)
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("×");
            private final Button plusBtn = new Button("+");
            private final Button minusBtn = new Button("-");

            {
                deleteBtn.setOnAction(e -> {
                    OrderItem item = getTableView().getItems().get(getIndex());
                    removeOrderItem(item);
                });

                plusBtn.setOnAction(e -> {
                    OrderItem item = getTableView().getItems().get(getIndex());
                    item.setQuantity(item.getQuantity() + 1);
                    getTableView().refresh();
                    updateBilling();
                });

                minusBtn.setOnAction(e -> {
                    OrderItem item = getTableView().getItems().get(getIndex());
                    if (item.getQuantity() > 1) {
                        item.setQuantity(item.getQuantity() - 1);
                        getTableView().refresh();
                        updateBilling();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(4);
                    buttons.getChildren().addAll(minusBtn, plusBtn, deleteBtn);
                    setGraphic(buttons);
                }
            }
        });

        orderTableView.setItems(orderItems);
    }

    private void loadSampleMenuItems() {
        menuItems.addAll(
                new MenuItem("Paneer Tikka", 250.0, "Starters", "🍢"),
                new MenuItem("Butter Chicken", 350.0, "Main Course", "🍗"),
                new MenuItem("Dal Makhani", 220.0, "Main Course", "🍲"),
                new MenuItem("Tandoori Roti", 25.0, "Starters", "🫓"),
                new MenuItem("Masala Dosa", 180.0, "Main Course", "🫔"),
                new MenuItem("Cold Coffee", 120.0, "Beverages", "☕"),
                new MenuItem("Mango Lassi", 100.0, "Beverages", "🥤"),
                new MenuItem("Gulab Jamun", 80.0, "Desserts", "🍡"),
                new MenuItem("Samosa", 40.0, "Snacks", "🥟"),
                new MenuItem("Spring Roll", 90.0, "Snacks", "🌯")
        );
    }

    private void populateMenuGrid() {
        menuGrid.getChildren().clear();

        String selectedCategory = getSelectedCategory();
        String searchText = searchField != null ? searchField.getText().toLowerCase() : "";

        for (MenuItem item : menuItems) {
            // Filter by category
            if (!selectedCategory.equals("All") && !item.getCategory().equals(selectedCategory)) {
                continue;
            }

            // Filter by search
            if (!searchText.isEmpty() && !item.getName().toLowerCase().contains(searchText)) {
                continue;
            }

            VBox tile = createMenuTile(item);
            menuGrid.getChildren().add(tile);
        }
    }

    private VBox createMenuTile(MenuItem item) {
        VBox tile = new VBox(4);
        tile.getStyleClass().add("tile");
        tile.setAlignment(javafx.geometry.Pos.CENTER);
        tile.setPadding(new Insets(8));

        Label icon = new Label(item.getIcon());
        icon.setStyle("-fx-font-size: 32px;");

        Label name = new Label(item.getName());
        name.getStyleClass().add("item-name");

        Label price = new Label("₹" + String.format("%.0f", item.getPrice()));
        price.getStyleClass().add("item-price");

        tile.getChildren().addAll(icon, name, price);

        // Click to add item to order
        tile.setOnMouseClicked(e -> addItemToOrder(item));

        return tile;
    }

    private void addItemToOrder(MenuItem menuItem) {
        // Check if item already exists in order
        for (OrderItem orderItem : orderItems) {
            if (orderItem.getName().equals(menuItem.getName())) {
                orderItem.setQuantity(orderItem.getQuantity() + 1);
                orderTableView.refresh();
                updateBilling();
                return;
            }
        }

        // Add new item
        OrderItem newItem = new OrderItem(menuItem.getName(), 1, menuItem.getPrice());
        orderItems.add(newItem);
        updateBilling();
    }

    private void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        updateBilling();
    }

    private void updateBilling() {
        subtotal = 0.0;

        for (OrderItem item : orderItems) {
            subtotal += item.getTotal();
        }

        double tax = subtotal * taxRate;
        double total = subtotal + tax;

        subtotalLabel.setText("₹" + String.format("%.2f", subtotal));
        taxLabel.setText("₹" + String.format("%.2f", tax));
        totalLabel.setText("₹" + String.format("%.2f", total));
    }

    private String getSelectedCategory() {
        if (allBtn != null && allBtn.isSelected()) {
            return "All";
        }
        if (beveragesBtn != null && beveragesBtn.isSelected()) {
            return "Beverages";
        }
        if (mainCourseBtn != null && mainCourseBtn.isSelected()) {
            return "Main Course";
        }
        if (snacksBtn != null && snacksBtn.isSelected()) {
            return "Snacks";
        }
        if (dessertsBtn != null && dessertsBtn.isSelected()) {
            return "Desserts";
        }
        if (startersBtn != null && startersBtn.isSelected()) {
            return "Starters";
        }
        return "All";
    }

    private void setupEventHandlers() {
        // Category filter buttons
        if (allBtn != null) {
            allBtn.setOnAction(e -> populateMenuGrid());
        }
        if (beveragesBtn != null) {
            beveragesBtn.setOnAction(e -> populateMenuGrid());
        }
        if (mainCourseBtn != null) {
            mainCourseBtn.setOnAction(e -> populateMenuGrid());
        }
        if (snacksBtn != null) {
            snacksBtn.setOnAction(e -> populateMenuGrid());
        }
        if (dessertsBtn != null) {
            dessertsBtn.setOnAction(e -> populateMenuGrid());
        }
        if (startersBtn != null) {
            startersBtn.setOnAction(e -> populateMenuGrid());
        }

        // Search field
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, newVal) -> populateMenuGrid());
        }

        // Payment method buttons
        if (cashBtn != null) {
            cashBtn.setOnAction(e -> setPaymentMethod("Cash"));
        }
        if (cardBtn != null) {
            cardBtn.setOnAction(e -> setPaymentMethod("Card"));
        }
        if (upiBtn != null) {
            upiBtn.setOnAction(e -> setPaymentMethod("UPI"));
        }

        // Action buttons
        if (newBillBtn != null) {
            newBillBtn.setOnAction(e -> clearOrder());
        }
        if (settleBtn != null) {
            settleBtn.setOnAction(e -> settleBill());
        }
        if (kotBtn != null) {
            kotBtn.setOnAction(e -> printKOT());
        }
        if (discountBtn != null) {
            discountBtn.setOnAction(e -> applyDiscount());
        }
        if (couponBtn != null) {
            couponBtn.setOnAction(e -> applyCoupon());
        }
    }

    private void setPaymentMethod(String method) {
        selectedPaymentMethod = method;
        System.out.println("Payment method: " + method);
    }

    private void clearOrder() {
        orderItems.clear();
        customerNameField.clear();
        if (tableNumberField != null) {
            tableNumberField.clear();
        }
        currentBillNumber++;
        billNumberLabel.setText("Bill #" + currentBillNumber);
        updateBilling();
    }

    private void settleBill() {
        if (orderItems.isEmpty()) {
            showAlert("Empty Order", "Please add items to the order before settling.");
            return;
        }

        System.out.println("Settling bill #" + currentBillNumber);
        System.out.println("Payment method: " + selectedPaymentMethod);
        System.out.println("Total: ₹" + totalLabel.getText());

        // TODO: Process payment and print receipt
        showAlert("Bill Settled", "Bill #" + currentBillNumber + " settled successfully!\nPayment: " + selectedPaymentMethod);
        clearOrder();
    }

    private void printKOT() {
        if (orderItems.isEmpty()) {
            showAlert("Empty Order", "No items to print KOT.");
            return;
        }
        System.out.println("Printing KOT...");
        // TODO: Implement KOT printing
    }

    private void applyDiscount() {
        System.out.println("Apply discount dialog");
        // TODO: Show discount dialog
    }

    private void applyCoupon() {
        System.out.println("Apply coupon dialog");
        // TODO: Show coupon dialog
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner class for menu items
    public static class MenuItem {

        private String name;
        private double price;
        private String category;
        private String icon;

        public MenuItem(String name, double price, String category, String icon) {
            this.name = name;
            this.price = price;
            this.category = category;
            this.icon = icon;
        }

        public String getName() {
            return name;
        }

        public double getPrice() {
            return price;
        }

        public String getCategory() {
            return category;
        }

        public String getIcon() {
            return icon;
        }
    }

    // Inner class for order items
    public static class OrderItem {

        private String name;
        private int quantity;
        private double price;

        public OrderItem(String name, int quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        public String getName() {
            return name;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public double getPrice() {
            return price;
        }

        public double getTotal() {
            return quantity * price;
        }
    }
}
