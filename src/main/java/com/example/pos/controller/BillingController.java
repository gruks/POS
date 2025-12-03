package com.example.pos.controller;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.example.pos.model.InventoryItem;
import com.example.pos.model.MenuItem;
import com.example.pos.model.TableModel;
import com.example.pos.model.TableSession;
import com.example.pos.model.TableSessionItem;
import com.example.pos.service.InventoryService;
import com.example.pos.service.MenuService;
import com.example.pos.service.SalesService;
import com.example.pos.service.SalesService.SaleItem;
import com.example.pos.service.SalesService.SaleRequest;
import com.example.pos.service.TableService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
    private boolean restoringTableSession = false;

    // Tables & inventory
    private TableService tableService = new TableService();
    private final InventoryService inventoryService = new InventoryService();
    private final MenuService menuService = new MenuService();
    private final SalesService salesService = new SalesService();
    private final ExecutorService dataExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
    private final Map<String, InventoryItem> retailInventoryByName = new HashMap<>();
    private TableModel activeTable;
    private boolean tableSessionMode = false;

    @FXML
    public void initialize() {
        System.out.println("BillingController initialized");
        restoringState = true;

        loadCategoriesFromDb();
        createCategoryFilterButtons();
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

        populateTableChoices();

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

    private void loadTableSession(TableSession session) {
        if (session == null) {
            return;
        }
        restoringTableSession = true;
        orderItems.setAll(fromTableSession(session));
        if (customerNameField != null) {
            customerNameField.setText(session.getCustomerName() != null ? session.getCustomerName() : "");
        }
        if (billNumberLabel != null && session.getBillLabel() != null) {
            billNumberLabel.setText(session.getBillLabel());
        }
        if (session.getPaymentMethod() != null) {
            selectedPaymentMethod = session.getPaymentMethod();
            setPaymentMethod(selectedPaymentMethod);
        }
        if (session.getOrderType() != null) {
            selectedOrderType = session.getOrderType();
        }
        if (orderTypeGroup != null && session.getOrderType() != null) {
            orderTypeGroup.getToggles().stream()
                    .filter(t -> t instanceof ToggleButton)
                    .map(t -> (ToggleButton) t)
                    .filter(btn -> btn.getText().equalsIgnoreCase(session.getOrderType()))
                    .findFirst()
                    .ifPresent(toggle -> toggle.setSelected(true));
        }
        restoringTableSession = false;
        updateBilling();
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

    public void openForTable(TableService service, TableModel table, TableSession session) {
        if (service != null) {
            this.tableService = service;
        }
        this.activeTable = table;
        this.tableSessionMode = table != null;
        populateTableChoices();

        if (tableChoice != null && table != null) {
            tableChoice.setValue(table.getTableName());
            tableChoice.setDisable(true);
        }

        if (session != null) {
            loadTableSession(session);
        } else if (table != null) {
            orderItems.clear();
            updateBilling();
        }
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

        persistActiveTableSession();
    }

    private void persistActiveTableSession() {
        if (!tableSessionMode || restoringTableSession || activeTable == null || activeTable.getId() == null) {
            return;
        }

        if (orderItems.isEmpty()) {
            tableService.clearSession(activeTable.getId(), "Available");
            return;
        }

        List<TableSessionItem> items = new ArrayList<>();
        for (OrderItem item : orderItems) {
            items.add(new TableSessionItem(item.getName(), item.getQuantity(), item.getPrice()));
        }

        TableSession session = new TableSession(
                activeTable.getId(),
                activeTable.getTableName(),
                billNumberLabel != null ? billNumberLabel.getText() : "",
                customerNameField != null ? customerNameField.getText() : "",
                selectedPaymentMethod,
                selectedOrderType,
                items,
                "Occupied",
                LocalDateTime.now());
        tableService.saveSession(session);
    }

    private void loadCategoriesFromDb() {
        categories.clear();
        categories.add("All");
        try {
            categories.addAll(menuService.loadCategories().values());
        } catch (Exception e) {
            System.err.println("Error loading categories: " + e.getMessage());
            if (categories.size() == 1) {
                categories.addAll(List.of("Beverages", "Main Course", "Snacks", "Desserts", "Starters"));
            }
        }
        ensureRetailCategoryPresence();
    }

    private void ensureRetailCategoryPresence() {
        boolean hasRetail = categories.stream()
                .anyMatch(cat -> cat.equalsIgnoreCase(InventoryService.RETAIL_CATEGORY));
        if (!hasRetail) {
            categories.add(InventoryService.RETAIL_CATEGORY);
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
                    if (!hasRetailStock(item.getName(), 1)) {
                        showStockWarning(item.getName());
                        return;
                    }
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
        Task<MenuData> task = new Task<>() {
            @Override
            protected MenuData call() {
                List<MenuItem> items = new ArrayList<>(menuService.loadMenuItemsForBilling());
                Map<String, InventoryItem> retail = new HashMap<>();
                List<InventoryItem> retailItems = inventoryService.getAllItems();
                for (InventoryItem item : retailItems) {
                    retail.put(item.getName(), item);
                    items.add(new MenuItem(
                            item.getName(),
                            item.getRate(),
                            InventoryService.RETAIL_CATEGORY,
                            "",
                            item.getQuantity()));
                }
                return new MenuData(items, retail);
            }
        };
        task.setOnSucceeded(e -> {
            MenuData data = task.getValue();
            retailInventoryByName.clear();
            retailInventoryByName.putAll(data.retailInventory());
            menuItems.setAll(data.items());
            ensureRetailCategoryPresence();
            populateMenuGrid();
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("Unable to load menu: " + (ex != null ? ex.getMessage() : "unknown error"));
        });
        dataExecutor.submit(task);
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

        if (item.hasLimitedInventory()) {
            int qty = item.getAvailableQuantity() != null ? item.getAvailableQuantity() : 0;
            Label stockLabel = new Label(qty > 0 ? "Stock: " + qty : "Out of stock");
            stockLabel.setStyle(qty > 0 ? "-fx-text-fill: #2563eb;" : "-fx-text-fill: #dc2626;");
            tile.getChildren().add(stockLabel);
            if (qty <= 0) {
                tile.setOpacity(0.6);
            }
        }

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
                if (!hasRetailStock(menuItem.getName(), 1)) {
                    showStockWarning(menuItem.getName());
                    return;
                }
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
        if (!hasRetailStock(menuItem.getName(), 1)) {
            showStockWarning(menuItem.getName());
            return;
        }
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

    private void populateTableChoices() {
        if (tableChoice == null) {
            return;
        }
        try {
            tableChoice.setItems(FXCollections.observableArrayList(tableService.loadTableNames()));
            tableChoice.setDisable(false);
        } catch (Exception ex) {
            System.err.println("Unable to load table list: " + ex.getMessage());
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
            if (!tableSessionMode) {
                tableChoice.setValue(null);
            }
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
        persistBill(orderItems, customerName, selectedPaymentMethod, this::clearOrder);
    }

    private void settleBillForTab(BillTab tab) {
        if (tab == null) {
            return;
        }
        String customerName = tab.customerField != null ? tab.customerField.getText() : "";
        persistBill(tab.orderItems, customerName, tab.paymentMethod, () -> {
            billTabs.getTabs().remove(tab.tab);
            billTabsList.remove(tab);
            persistState();
        });
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

    private void persistBill(ObservableList<OrderItem> currentOrderItems,
                             String customerName,
                             String paymentMethod,
                             Runnable onSuccess) {
        if (currentOrderItems == null || currentOrderItems.isEmpty()) {
            showAlert("Empty Order", "Please add items to the order before settling.");
            return;
        }

        double currentSubtotal = 0.0;
        for (OrderItem item : currentOrderItems) {
            currentSubtotal += item.getTotal();
        }
        double currentTax = currentSubtotal * taxRate;
        double currentTotal = currentSubtotal + currentTax;

        long billNumber = generateBillNumber();
        Long tableId = parseActiveTableId();
        String tableName = activeTable != null ? activeTable.getTableName() : null;

        List<SaleItem> saleItems = currentOrderItems.stream()
                .map(i -> new SaleItem(i.getName(), i.getQuantity(), i.getPrice(), i.getTotal()))
                .collect(Collectors.toList());

        SaleRequest request = new SaleRequest(
                billNumber,
                customerName,
                paymentMethod,
                selectedOrderType,
                currentSubtotal,
                currentTax,
                currentTotal,
                "Completed",
                saleItems,
                buildRetailAdjustments(currentOrderItems),
                tableId,
                tableName
        );

        String activeTableIdSnapshot = activeTable != null ? activeTable.getId() : null;
        Task<Long> task = new Task<>() {
            @Override
            protected Long call() {
                long saleId = salesService.recordSale(request);
                if (tableSessionMode && activeTableIdSnapshot != null) {
                    tableService.clearSession(activeTableIdSnapshot, "Available");
                }
                return saleId;
            }
        };
        task.setOnSucceeded(e -> {
            showAlert("Bill Settled", "Bill #" + billNumber + " settled successfully!\nPayment: " + paymentMethod + "\nTotal: ₹" + String.format("%.2f", currentTotal));
            refreshMenuItemsWithInventory();
            if (onSuccess != null) {
                onSuccess.run();
            }
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            showAlert("Error", "Failed to save bill: " + (ex != null ? ex.getMessage() : "Unknown error"));
            if (ex != null) {
                ex.printStackTrace();
            }
        });
        dataExecutor.submit(task);
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

    private boolean hasRetailStock(String itemName, int delta) {
        if (delta <= 0 || !retailInventoryByName.containsKey(itemName)) {
            return true;
        }
        InventoryItem stock = retailInventoryByName.get(itemName);
        int available = stock.getQuantity();
        int currentPending = getPendingRetailQuantity(itemName);
        return available >= currentPending + delta;
    }

    private int getPendingRetailQuantity(String itemName) {
        int qty = 0;
        for (OrderItem item : orderItems) {
            if (item.getName().equals(itemName)) {
                qty += item.getQuantity();
            }
        }
        for (BillTab tab : billTabsList) {
            for (OrderItem item : tab.orderItems) {
                if (item.getName().equals(itemName)) {
                    qty += item.getQuantity();
                }
            }
        }
        return qty;
    }

    private void showStockWarning(String itemName) {
        showAlert("Stock unavailable", itemName + " is out of stock for the Retail inventory.");
    }

    private Map<String, Integer> buildRetailAdjustments(List<OrderItem> soldItems) {
        Map<String, Integer> sold = new HashMap<>();
        for (OrderItem item : soldItems) {
            if (retailInventoryByName.containsKey(item.getName())) {
                sold.merge(item.getName(), item.getQuantity(), Integer::sum);
            }
        }
        return sold;
    }

    private Long parseActiveTableId() {
        if (activeTable == null || activeTable.getId() == null) {
            return null;
        }
        try {
            return Long.parseLong(activeTable.getId());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void refreshMenuItemsWithInventory() {
        String previousCategory = selectedCategory;
        loadMenuItemsFromDb();
        selectedCategory = previousCategory != null ? previousCategory : "All";
        populateMenuGrid();
    }

    private record MenuData(List<MenuItem> items, Map<String, InventoryItem> retailInventory) {
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
                        if (!hasRetailStock(it.getName(), 1)) {
                            showStockWarning(it.getName());
                            return;
                        }
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

    private static List<OrderItem> fromTableSession(TableSession session) {
        List<OrderItem> items = new ArrayList<>();
        if (session == null || session.getItems() == null) {
            return items;
        }
        for (TableSessionItem item : session.getItems()) {
            items.add(new OrderItem(item.name(), item.quantity(), item.price()));
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