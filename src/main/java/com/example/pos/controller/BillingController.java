package com.example.pos.controller;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.example.pos.model.MenuItem;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

public class BillingController {

    private static final String PAYMENT_SELECTED_STYLE = "-fx-background-color: #3b82f6; -fx-text-fill: white;";
    private static final BillingViewState VIEW_STATE = new BillingViewState();

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
    private ToggleGroup orderTypeGroup;
    @FXML
    private ComboBox<String> tableChoice;
    @FXML
    private TextField searchField;
    @FXML
    private FlowPane categoryFilterPane;
    @FXML
    private TilePane menuGrid;

    // Center Panel - Order Details (Main Tab)
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
    @FXML
    private ToggleGroup categoryGroup;

    // Tabs
    @FXML 
    private TabPane billTabs;

    // Data
    private ObservableList<OrderItem> orderItems = FXCollections.observableArrayList();
    private ObservableList<MenuItem> menuItems = FXCollections.observableArrayList();
    private List<String> categories = new ArrayList<>();
    private String selectedCategory = "All";
    private String selectedOrderType = "Dine-In";
    private double subtotal = 0.0;
    private double taxRate = 0.05; // 5% tax
    private String selectedPaymentMethod = "Cash";
    private List<BillTab> billTabsList = new ArrayList<>();
    private boolean restoringState = false;

    // Mongo
    private static MongoClient mongoClient;
    private MongoCollection<Document> itemsCollection;
    private MongoCollection<Document> billsCollection;
    private MongoCollection<Document> categoriesCollection;

    @FXML
    public void initialize() {
        System.out.println("BillingController initialized");
        restoringState = true;

        // Mongo setup
        if (mongoClient == null) {
            mongoClient = MongoClients.create("mongodb://localhost:27017");
        }
        MongoDatabase db = mongoClient.getDatabase("posapp");
        itemsCollection = db.getCollection("menu_items");
        billsCollection = db.getCollection("bills");
        categoriesCollection = db.getCollection("menu_categories");

        // Load categories from DB
        loadCategoriesFromDb();

        // Create dynamic category filter buttons
        createCategoryFilterButtons();

        // Load menu items from DB
        loadMenuItemsFromDb();

        // Initialize order table
        setupOrderTable();

        // Populate menu grid
        populateMenuGrid();

        // Setup event handlers
        setupEventHandlers();

        // Initialize billing (will be overwritten if we restore state)
        updateBillInfo();
        updateBilling();

        // Populate table dropdown with sample table numbers 1..20
        if (tableChoice != null) {
            tableChoice.setItems(FXCollections.observableArrayList(
                java.util.stream.IntStream.rangeClosed(1, 20).mapToObj(i -> "T" + i).toList()
            ));
        }

        // Make the first tab non-closable or handle tab close events
        if (billTabs != null && !billTabs.getTabs().isEmpty()) {
            billTabs.getTabs().get(0).setClosable(false);
        }

        if (orderTypeGroup != null) {
            orderTypeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
                if (newToggle instanceof ToggleButton toggleButton) {
                    selectedOrderType = toggleButton.getText();
                    if (!restoringState) {
                        persistState();
                    }
                }
            });
        }

        if (customerNameField != null) {
            customerNameField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!restoringState) {
                    persistState();
                }
            });
        }

        if (tableChoice != null) {
            tableChoice.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!restoringState) {
                    persistState();
                }
            });
        }

        restoringState = false;
        restoreState();
        persistState();
    }

    private void restoreState() {
        restoringState = true;

        if (VIEW_STATE.mainOrderItems != null && !VIEW_STATE.mainOrderItems.isEmpty()) {
            orderItems.setAll(fromSnapshots(VIEW_STATE.mainOrderItems));
            updateBilling();
        }

        if (customerNameField != null) {
            customerNameField.setText(VIEW_STATE.customerName);
        }

        if (tableChoice != null && VIEW_STATE.tableSelection != null) {
            tableChoice.setValue(VIEW_STATE.tableSelection);
        }

        selectedPaymentMethod = VIEW_STATE.paymentMethod;
        setPaymentMethod(selectedPaymentMethod);

        selectedOrderType = VIEW_STATE.orderType;
        if (orderTypeGroup != null) {
            orderTypeGroup.getToggles().stream()
                    .filter(t -> t instanceof ToggleButton)
                    .map(t -> (ToggleButton) t)
                    .filter(btn -> btn.getText().equals(selectedOrderType))
                    .findFirst()
                    .ifPresent(toggle -> toggle.setSelected(true));
        }

        if (billNumberLabel != null && VIEW_STATE.billLabel != null && !VIEW_STATE.billLabel.isBlank()) {
            billNumberLabel.setText(VIEW_STATE.billLabel);
        }
        if (orderDateLabel != null && VIEW_STATE.orderDate != null && !VIEW_STATE.orderDate.isBlank()) {
            orderDateLabel.setText(VIEW_STATE.orderDate);
        }

        if (VIEW_STATE.extraTabs != null && !VIEW_STATE.extraTabs.isEmpty()) {
            for (TabSnapshot snapshot : VIEW_STATE.extraTabs) {
                BillTab tab = new BillTab(billTabsList.size() + 2, this);
                billTabsList.add(tab);
                billTabs.getTabs().add(tab.tab);
                tab.loadSnapshot(snapshot);
            }
        }

        restoringState = false;
    }

    private void persistState() {
        if (restoringState) {
            return;
        }

        VIEW_STATE.mainOrderItems = toSnapshots(orderItems);
        VIEW_STATE.customerName = customerNameField != null ? customerNameField.getText() : "";
        VIEW_STATE.tableSelection = tableChoice != null ? tableChoice.getValue() : null;
        VIEW_STATE.paymentMethod = selectedPaymentMethod;
        VIEW_STATE.orderType = selectedOrderType;
        VIEW_STATE.billLabel = billNumberLabel != null ? billNumberLabel.getText() : "";
        VIEW_STATE.orderDate = orderDateLabel != null ? orderDateLabel.getText() : "";

        VIEW_STATE.extraTabs = new ArrayList<>();
        for (BillTab bt : billTabsList) {
            TabSnapshot snapshot = new TabSnapshot();
            snapshot.items = toSnapshots(bt.orderItems);
            snapshot.customerName = bt.customerField != null ? bt.customerField.getText() : "";
            snapshot.paymentMethod = bt.paymentMethod;
            snapshot.title = bt.tab.getText();
            snapshot.billNumber = bt.billNumber;
            snapshot.dateLabel = bt.dateLabel != null ? bt.dateLabel.getText() : "";
            VIEW_STATE.extraTabs.add(snapshot);
        }
    }

    private void loadCategoriesFromDb() {
        categories.clear();
        categories.add("All"); // Always add "All" first
        
        try (MongoCursor<Document> cur = categoriesCollection.find().iterator()) {
            while (cur.hasNext()) {
                Document d = cur.next();
                String categoryName = d.getString("name");
                if (categoryName != null && !categoryName.isBlank()) {
                    categories.add(categoryName);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading categories: " + e.getMessage());
            // Fallback to default categories
            if (categories.size() == 1) {
                categories.addAll(List.of("Beverages", "Main Course", "Snacks", "Desserts", "Starters"));
            }
        }
    }

    private void createCategoryFilterButtons() {
        if (categoryFilterPane == null || categoryGroup == null) {
            System.err.println("Category UI references are null - check FXML");
            return;
        }

        categoryGroup.getToggles().clear();
        categoryFilterPane.getChildren().clear();

        for (String category : categories) {
            final String categoryName = category;
            ToggleButton btn = new ToggleButton(categoryName);
            btn.setToggleGroup(categoryGroup);
            btn.getStyleClass().add("category-btn");
            btn.setStyle("-fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
            
            // Select "All" by default
            if (categoryName.equals("All")) {
                btn.setSelected(true);
            }

            btn.setOnAction(e -> {
                selectedCategory = categoryName;
                populateMenuGrid();
            });

            categoryFilterPane.getChildren().add(btn);
        }
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
            private final Button deleteBtn = new Button("x");
            private final Button plusBtn = new Button("+");
            private final Button minusBtn = new Button("-");

            {
                deleteBtn.setStyle("-fx-font-size: 12px; -fx-text-fill: red;");
                plusBtn.setStyle("-fx-font-size: 12px;");
                minusBtn.setStyle("-fx-font-size: 12px;");

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
                    HBox buttons = new HBox(4);
                    buttons.setAlignment(Pos.CENTER);
                    buttons.getChildren().addAll(minusBtn, plusBtn, deleteBtn);
                    setGraphic(buttons);
                }
            }
        });

        orderTableView.setItems(orderItems);
    }

    private void loadMenuItemsFromDb() {
        menuItems.clear();
        try (MongoCursor<Document> cur = itemsCollection.find().iterator()) {
            while (cur.hasNext()) {
                Document d = cur.next();
                String name = d.getString("name");
                Double price = d.getDouble("price");
                String category = d.getString("category_name");
                if (category == null) category = d.getString("category");
                String imageUrl = d.getString("imageUrl");
                if (name != null && price != null) {
                    menuItems.add(new MenuItem(name, price, category == null ? "" : category, imageUrl == null ? "" : imageUrl));
                }
            }
        }
    }

    private void populateMenuGrid() {
        menuGrid.getChildren().clear();

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
        tile.setAlignment(Pos.CENTER);
        tile.setPadding(new Insets(8));
        tile.setStyle("-fx-cursor: hand; -fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; " +
                      "-fx-border-radius: 8; -fx-background-radius: 8;");
        
        // Image thumbnail (fallback to text if missing)
        javafx.scene.Node visual;
        String imgPath = item.getImageUrl();
        if (imgPath != null && !imgPath.isBlank()) {
            File f = new File(imgPath);
            if (!f.exists()) {
                Label fallback = new Label(item.getName().substring(0, Math.min(2, item.getName().length())).toUpperCase());
                fallback.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
                visual = fallback;
            } else {
                ImageView iv = new ImageView(new Image(f.toURI().toString(), 64, 64, true, true));
                iv.setFitWidth(64);
                iv.setFitHeight(64);
                visual = iv;
            }
        } else {
            Label fallback = new Label(item.getName().substring(0, Math.min(2, item.getName().length())).toUpperCase());
            fallback.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            visual = fallback;
        }

        Label name = new Label(item.getName());
        name.getStyleClass().add("item-name");
        name.setStyle("-fx-font-weight: 600;");

        Label price = new Label("₹" + String.format("%.0f", item.getPrice()));
        price.getStyleClass().add("item-price");
        price.setStyle("-fx-text-fill: #059669;");

        tile.getChildren().addAll(visual, name, price);

        // Click to add item to order
        tile.setOnMouseClicked(e -> addItemToOrder(item));

        // Hover effect
        tile.setOnMouseEntered(e -> tile.setStyle(tile.getStyle() + "-fx-background-color: #f3f4f6;"));
        tile.setOnMouseExited(e -> tile.setStyle(tile.getStyle() + "-fx-background-color: #f9fafb;"));

        return tile;
    }

    private void addItemToOrder(MenuItem menuItem) {
        // Get current active tab's order items
        ObservableList<OrderItem> currentOrderItems = getCurrentTabOrderItems();
        
        // Check if item already exists in order
        for (OrderItem orderItem : currentOrderItems) {
            if (orderItem.getName().equals(menuItem.getName())) {
                orderItem.setQuantity(orderItem.getQuantity() + 1);
                getCurrentTabTableView().refresh();
                updateCurrentTabBilling();
                if (!restoringState) {
                    persistState();
                }
                return;
            }
        }

        // Add new item
        OrderItem newItem = new OrderItem(menuItem.getName(), 1, menuItem.getPrice());
        currentOrderItems.add(newItem);
        updateCurrentTabBilling();
        if (!restoringState) {
            persistState();
        }
    }

    private ObservableList<OrderItem> getCurrentTabOrderItems() {
        if (billTabs.getSelectionModel().getSelectedIndex() == 0) {
            // Main tab
            return orderItems;
        } else {
            // Additional tabs
            javafx.scene.control.Tab selectedTab = billTabs.getSelectionModel().getSelectedItem();
            for (BillTab bt : billTabsList) {
                if (bt.tab == selectedTab) {
                    return bt.orderItems;
                }
            }
        }
        return orderItems;
    }

    private TableView<OrderItem> getCurrentTabTableView() {
        if (billTabs.getSelectionModel().getSelectedIndex() == 0) {
            return orderTableView;
        } else {
            javafx.scene.control.Tab selectedTab = billTabs.getSelectionModel().getSelectedItem();
            for (BillTab bt : billTabsList) {
                if (bt.tab == selectedTab) {
                    return bt.tableView;
                }
            }
        }
        return orderTableView;
    }

    private void updateCurrentTabBilling() {
        if (billTabs.getSelectionModel().getSelectedIndex() == 0) {
            updateBilling();
        } else {
            javafx.scene.control.Tab selectedTab = billTabs.getSelectionModel().getSelectedItem();
            for (BillTab bt : billTabsList) {
                if (bt.tab == selectedTab) {
                    bt.updateBilling();
                    return;
                }
            }
        }
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
        persistState();
    }

    private void updateBillInfo() {
        long billNumber = generateBillNumber();
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        
        billNumberLabel.setText("Bill #" + billNumber);
        orderDateLabel.setText(currentDateTime);
    }

    private long generateBillNumber() {
        // Generate unique bill number based on timestamp
        LocalDateTime now = LocalDateTime.now();
        // Format: yyMMddHHmmss (removes first 2 digits of year)
        return Long.parseLong(now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).substring(2));
    }

    private void setupEventHandlers() {
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
            newBillBtn.setOnAction(e -> openNewBillTab());
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
        // Visual feedback for selected payment method
        if (cashBtn != null && cardBtn != null && upiBtn != null) {
            cashBtn.setStyle(method.equals("Cash") ? PAYMENT_SELECTED_STYLE : "");
            cardBtn.setStyle(method.equals("Card") ? PAYMENT_SELECTED_STYLE : "");
            upiBtn.setStyle(method.equals("UPI") ? PAYMENT_SELECTED_STYLE : "");
        }
        if (!restoringState) {
            persistState();
        }
    }

    private void clearOrder() {
        orderItems.clear();
        if (customerNameField != null) {
            customerNameField.clear();
        }
        if (tableChoice != null) {
            tableChoice.setValue(null);
        }
        updateBillInfo();
        updateBilling();
        persistState();
    }

    private void openNewBillTab() {
        int tabNumber = billTabsList.size() + 2; // +2 because first tab is #1
        BillTab newBillTab = new BillTab(tabNumber, this);
        billTabsList.add(newBillTab);
        billTabs.getTabs().add(newBillTab.tab);
        billTabs.getSelectionModel().select(newBillTab.tab);
        persistState();
    }

    private void settleBill() {
        if (billTabs.getSelectionModel().getSelectedIndex() == 0) {
            settleMainTab();
        } else {
            BillTab selected = getSelectedBillTab();
            settleBillForTab(selected);
        }
    }

    private void settleMainTab() {
        String customerName = customerNameField != null ? customerNameField.getText() : "";
        if (persistBill(orderItems, customerName, selectedPaymentMethod)) {
            clearOrder();
        }
    }

    private void settleBillForTab(BillTab tab) {
        if (tab == null) {
            return;
        }
        String customerName = tab.customerField != null ? tab.customerField.getText() : "";
        if (persistBill(tab.orderItems, customerName, tab.paymentMethod)) {
            billTabs.getTabs().remove(tab.tab);
            billTabsList.remove(tab);
            persistState();
        }
    }

    private BillTab getSelectedBillTab() {
        javafx.scene.control.Tab selectedTab = billTabs.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            return null;
        }
        for (BillTab bt : billTabsList) {
            if (bt.tab == selectedTab) {
                return bt;
            }
        }
        return null;
    }

    private boolean persistBill(ObservableList<OrderItem> currentOrderItems, String customerName, String paymentMethod) {
        if (currentOrderItems == null || currentOrderItems.isEmpty()) {
            showAlert("Empty Order", "Please add items to the order before settling.");
            return false;
        }

        double currentSubtotal = 0.0;
        for (OrderItem item : currentOrderItems) {
            currentSubtotal += item.getTotal();
        }
        double currentTax = currentSubtotal * taxRate;
        double currentTotal = currentSubtotal + currentTax;

        long billNumber = generateBillNumber();
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        Document bill = new Document("billNumber", billNumber)
                .append("customerName", customerName)
                .append("paymentMethod", paymentMethod)
                .append("orderType", selectedOrderType)
                .append("subtotal", currentSubtotal)
                .append("tax", currentTax)
                .append("total", currentTotal)
                .append("status", "Completed")
                .append("items", currentOrderItems.stream().map(i -> new Document("name", i.getName())
                        .append("qty", i.getQuantity())
                        .append("price", i.getPrice())
                        .append("total", i.getTotal())).toList())
                .append("createdAt", currentDateTime);

        try {
            billsCollection.insertOne(bill);
            showAlert("Bill Settled", "Bill #" + billNumber + " settled successfully!\nPayment: " + paymentMethod + "\nTotal: ₹" + String.format("%.2f", currentTotal));
            return true;
        } catch (Exception e) {
            showAlert("Error", "Failed to save bill: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void printKOT() {
        printKOTForItems(getCurrentTabOrderItems());
    }

    private void printKOTForItems(ObservableList<OrderItem> currentOrderItems) {
        if (currentOrderItems == null || currentOrderItems.isEmpty()) {
            showAlert("Empty Order", "No items to print KOT.");
            return;
        }

        System.out.println("=== KOT (Kitchen Order Ticket) ===");
        System.out.println("Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        System.out.println("Items:");
        for (OrderItem item : currentOrderItems) {
            System.out.println("  " + item.getQuantity() + "x " + item.getName());
        }
        System.out.println("================================");

        showAlert("KOT Printed", "Kitchen Order Ticket sent to kitchen!");
    }

    private void applyDiscount() {
        System.out.println("Apply discount dialog");
        showAlert("Coming Soon", "Discount feature will be available soon!");
    }

    private void applyCoupon() {
        System.out.println("Apply coupon dialog");
        showAlert("Coming Soon", "Coupon feature will be available soon!");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner class for managing additional bill tabs
    private class BillTab {
        javafx.scene.control.Tab tab;
        TableView<OrderItem> tableView;
        ObservableList<OrderItem> orderItems;
        Label subtotalLabel;
        Label taxLabel;
        Label totalLabel;
        Label billNumberLabel;
        Label dateLabel;
        TextField customerField;
        int billNumber;
        String paymentMethod = "Cash";
        Button cashBtn;
        Button cardBtn;
        Button upiBtn;
        Button settleBtn;
        Button discountBtn;
        Button couponBtn;
        Button kotBtn;
        Button saveBtn;
        Button newBillBtn;

        BillTab(int tabNumber, BillingController controller) {
            this.billNumber = (int) generateBillNumber();
            this.orderItems = FXCollections.observableArrayList();

            // Create table
            tableView = new TableView<>();
            TableColumn<OrderItem, String> itemCol = new TableColumn<>("Item");
            TableColumn<OrderItem, Integer> qtyCol = new TableColumn<>("Qty");
            TableColumn<OrderItem, Double> priceCol = new TableColumn<>("Price");
            TableColumn<OrderItem, Double> totalCol = new TableColumn<>("Total");
            TableColumn<OrderItem, Void> actionsCol = new TableColumn<>("Actions");

            itemCol.setPrefWidth(240);
            qtyCol.setPrefWidth(80);
            priceCol.setPrefWidth(120);
            totalCol.setPrefWidth(120);
            actionsCol.setPrefWidth(120);

            itemCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getName()));
            qtyCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getQuantity()));
            priceCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getPrice()));
            totalCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getTotal()));

            tableView.setItems(orderItems);

            actionsCol.setCellFactory(param -> new TableCell<>() {
                private final Button deleteBtn = new Button("x");
                private final Button plusBtn = new Button("+");
                private final Button minusBtn = new Button("-");
                {
                    deleteBtn.setStyle("-fx-font-size: 12px; -fx-text-fill: red;");
                    plusBtn.setStyle("-fx-font-size: 12px;");
                    minusBtn.setStyle("-fx-font-size: 12px;");

                    deleteBtn.setOnAction(e -> {
                        orderItems.remove(getTableView().getItems().get(getIndex()));
                        updateBilling();
                    });
                    plusBtn.setOnAction(e -> {
                        OrderItem it = getTableView().getItems().get(getIndex());
                        it.setQuantity(it.getQuantity() + 1);
                        getTableView().refresh();
                        updateBilling();
                    });
                    minusBtn.setOnAction(e -> {
                        OrderItem it = getTableView().getItems().get(getIndex());
                        if (it.getQuantity() > 1) {
                            it.setQuantity(it.getQuantity() - 1);
                            getTableView().refresh();
                            updateBilling();
                        }
                    });
                }
                @Override protected void updateItem(Void v, boolean empty){
                    super.updateItem(v, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        HBox buttons = new HBox(4);
                        buttons.setAlignment(Pos.CENTER);
                        buttons.getChildren().addAll(minusBtn, plusBtn, deleteBtn);
                        setGraphic(buttons);
                    }
                }
            });

            tableView.getColumns().addAll(itemCol, qtyCol, priceCol, totalCol, actionsCol);

            // Create header
            billNumberLabel = new Label("Bill #" + billNumber);
            billNumberLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            dateLabel = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            dateLabel.setStyle("-fx-text-fill: #6b7280;");

            HBox header = new HBox(8, new Label("Current Order"), new Region(), billNumberLabel, dateLabel);
            header.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(header.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);

            // Customer field
            customerField = new TextField();
            customerField.setPromptText("Customer name (optional)");
            HBox customerBox = new HBox(8, new Label("Customer:"), customerField);
            customerBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(customerField, javafx.scene.layout.Priority.ALWAYS);
            customerField.textProperty().addListener((obs, oldVal, newVal) -> persistState());

            // Create labels
            subtotalLabel = new Label("₹0.00");
            taxLabel = new Label("₹0.00");
            totalLabel = new Label("₹0.00");
            totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            // Billing section
            HBox subtotalBox = new HBox(8, new Label("Subtotal:"), subtotalLabel, new Region(), new Label("Tax (5%):"), taxLabel);
            subtotalBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(subtotalBox.getChildren().get(2), javafx.scene.layout.Priority.ALWAYS);

            HBox totalBox = new HBox(8, new Label("Total:"), totalLabel);
            totalBox.setAlignment(Pos.CENTER_RIGHT);

            VBox billingBox = new VBox(8, subtotalBox, totalBox);

            // Action buttons
            discountBtn = new Button("Discount");
            couponBtn = new Button("Coupon");
            kotBtn = new Button("KOT");
            saveBtn = new Button("Save");
            newBillBtn = new Button("New Bill");

            discountBtn.setOnAction(e -> applyDiscount());
            couponBtn.setOnAction(e -> applyCoupon());
            kotBtn.setOnAction(e -> printKOTForItems(orderItems));
            saveBtn.setOnAction(e -> showAlert("Coming Soon", "Save feature will be available soon!"));
            newBillBtn.setOnAction(e -> openNewBillTab());

            discountBtn.getStyleClass().add("secondary-button");
            couponBtn.getStyleClass().add("secondary-button");
            kotBtn.getStyleClass().add("secondary-button");
            saveBtn.getStyleClass().add("secondary-button");
            newBillBtn.getStyleClass().add("secondary-button");

            Region actionSpacer = new Region();
            HBox actionBox = new HBox(8, discountBtn, couponBtn, actionSpacer, newBillBtn, kotBtn, saveBtn);
            actionBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(actionSpacer, javafx.scene.layout.Priority.ALWAYS);

            // Payment buttons
            cashBtn = new Button("Cash");
            cardBtn = new Button("Card");
            upiBtn = new Button("UPI");
            settleBtn = new Button("Settle Bill");

            cashBtn.getStyleClass().add("payment-button");
            cardBtn.getStyleClass().add("payment-button");
            upiBtn.getStyleClass().add("payment-button");
            settleBtn.getStyleClass().add("primary-button");

            cashBtn.setOnAction(e -> setPaymentMethod("Cash"));
            cardBtn.setOnAction(e -> setPaymentMethod("Card"));
            upiBtn.setOnAction(e -> setPaymentMethod("UPI"));
            settleBtn.setOnAction(e -> settleBillForTab(this));
            setPaymentMethod("Cash");

            Region paymentSpacer = new Region();
            HBox paymentBox = new HBox(8, new Label("Payment:"), cashBtn, cardBtn, upiBtn, paymentSpacer, settleBtn);
            paymentBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(paymentSpacer, javafx.scene.layout.Priority.ALWAYS);

            // Layout
            VBox content = new VBox(8);
            content.getChildren().addAll(header, tableView, customerBox, actionBox, new Separator(), billingBox, paymentBox);
            content.setPadding(new Insets(12));
            VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS);

            tab = new javafx.scene.control.Tab("Bill #" + tabNumber, content);
            tab.setClosable(true);
            tab.setOnClosed(e -> {
                billTabsList.remove(this);
                persistState();
            });
        }

        void updateBilling() {
            double sub = 0.0;
            for (OrderItem item : orderItems) {
                sub += item.getTotal();
            }
            double tax = sub * taxRate;
            double tot = sub + tax;

            subtotalLabel.setText("₹" + String.format("%.2f", sub));
            taxLabel.setText("₹" + String.format("%.2f", tax));
            totalLabel.setText("₹" + String.format("%.2f", tot));
            persistState();
        }

        private void setPaymentMethod(String method) {
            paymentMethod = method;
            if (cashBtn != null && cardBtn != null && upiBtn != null) {
                cashBtn.setStyle(method.equals("Cash") ? PAYMENT_SELECTED_STYLE : "");
                cardBtn.setStyle(method.equals("Card") ? PAYMENT_SELECTED_STYLE : "");
                upiBtn.setStyle(method.equals("UPI") ? PAYMENT_SELECTED_STYLE : "");
            }
            persistState();
        }

        void loadSnapshot(TabSnapshot snapshot) {
            if (snapshot.items != null) {
                orderItems.setAll(fromSnapshots(snapshot.items));
            }
            customerField.setText(snapshot.customerName != null ? snapshot.customerName : "");
            setPaymentMethod(snapshot.paymentMethod != null ? snapshot.paymentMethod : "Cash");
            billNumber = snapshot.billNumber != 0 ? snapshot.billNumber : billNumber;
            billNumberLabel.setText("Bill #" + billNumber);
            if (snapshot.dateLabel != null && !snapshot.dateLabel.isBlank()) {
                dateLabel.setText(snapshot.dateLabel);
            }
            if (snapshot.title != null && !snapshot.title.isBlank()) {
                tab.setText(snapshot.title);
            }
            updateBilling();
        }
    }

    private static List<OrderItemSnapshot> toSnapshots(List<OrderItem> source) {
        List<OrderItemSnapshot> snapshots = new ArrayList<>();
        if (source == null) {
            return snapshots;
        }
        for (OrderItem item : source) {
            snapshots.add(new OrderItemSnapshot(item.getName(), item.getQuantity(), item.getPrice()));
        }
        return snapshots;
    }

    private static List<OrderItem> fromSnapshots(List<OrderItemSnapshot> snapshots) {
        List<OrderItem> items = new ArrayList<>();
        if (snapshots == null) {
            return items;
        }
        for (OrderItemSnapshot snapshot : snapshots) {
            items.add(new OrderItem(snapshot.name, snapshot.quantity, snapshot.price));
        }
        return items;
    }

    private static class BillingViewState {
        private List<OrderItemSnapshot> mainOrderItems = new ArrayList<>();
        private String customerName = "";
        private String tableSelection;
        private String paymentMethod = "Cash";
        private String orderType = "Dine-In";
        private String billLabel = "";
        private String orderDate = "";
        private List<TabSnapshot> extraTabs = new ArrayList<>();
    }

    private static class TabSnapshot {
        private List<OrderItemSnapshot> items = new ArrayList<>();
        private String customerName = "";
        private String paymentMethod = "Cash";
        private String title;
        private int billNumber;
        private String dateLabel;
    }

    private static class OrderItemSnapshot {
        private final String name;
        private final int quantity;
        private final double price;

        private OrderItemSnapshot(String name, int quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
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