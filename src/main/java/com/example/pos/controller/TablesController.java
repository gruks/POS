package com.example.pos.controller;

import java.util.List;

import com.example.pos.model.TableModel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class TablesController {

    @FXML private VBox cardTotal;
    @FXML private VBox cardAvailable;
    @FXML private VBox cardOccupied;
    @FXML private VBox cardReserved;
    @FXML private GridPane tableGrid;
    @FXML private Button btnRefresh;
    @FXML private Button btnAddTable;

    private final ObservableList<TableModel> tables = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        loadDummyData();
        createTableCards();
        setupEventHandlers();
        updateSummaryCards();
    }

    /* ---------- Data Loading ---------- */

    private void loadDummyData() {
        tables.setAll(
            // Available tables
            new TableModel("Table 1", 4, "Available"),
            new TableModel("Table 4", 6, "Available"),
            new TableModel("Table 7", 2, "Available"),
            new TableModel("Table 8", 8, "Available"),
            new TableModel("Table 10", 4, "Available"),

            // Occupied tables
            new TableModel("Table 2", 4, "Occupied", "3", "25 min"),
            new TableModel("Table 3", 2, "Occupied", "2", "15 min"),
            new TableModel("Table 6", 4, "Occupied", "4", "40 min"),
            new TableModel("Table 9", 6, "Occupied", "5", "30 min"),
            new TableModel("Table 12", 2, "Occupied", "2", "10 min"),

            // Reserved tables
            new TableModel("Table 5", 4, "Reserved", "18:00"),
            new TableModel("Table 11", 6, "Reserved", "19:30")
        );
    }

    /* ---------- UI Creation ---------- */

    private void createTableCards() {
        tableGrid.getChildren().clear();
        
        for (int i = 0; i < tables.size(); i++) {
            TableModel table = tables.get(i);
            VBox tableCard = createTableCard(table);
            
            int row = i / 3;
            int col = i % 3;
            tableGrid.add(tableCard, col, row);
        }
    }

    private VBox createTableCard(TableModel table) {
        VBox card = new VBox();
        card.getStyleClass().addAll("table-card", table.getStatus().toLowerCase().replace(" ", ""));
        card.setSpacing(6);
        card.setAlignment(javafx.geometry.Pos.CENTER);

        // Table name
        Label nameLabel = new Label(table.getTableName());
        nameLabel.getStyleClass().add("table-name");

        // Capacity
        Label capacityLabel = new Label("Capacity: " + table.getCapacity());
        capacityLabel.getStyleClass().add("table-capacity");

        // Status-specific details
        Label detailsLabel = new Label();
        detailsLabel.getStyleClass().add("table-details");

        if (table.isOccupied()) {
            detailsLabel.setText("Guests: " + table.getGuests() + " | " + table.getDuration());
        } else if (table.isReserved()) {
            detailsLabel.setText(table.getReservationTime());
        } else {
            detailsLabel.setText("Available");
        }

        card.getChildren().addAll(nameLabel, capacityLabel, detailsLabel);

        // Click handler
        card.setOnMouseClicked(event -> openBillingView(table));

        return card;
    }

    /* ---------- Event Handlers ---------- */

    private void setupEventHandlers() {
        btnRefresh.setOnAction(this::onRefresh);
        btnAddTable.setOnAction(this::onAddTable);
    }

    private void onRefresh(ActionEvent event) {
        System.out.println("Refreshing table data...");
        // TODO: Implement refresh logic
        createTableCards();
        updateSummaryCards();
    }

    private void onAddTable(ActionEvent event) {
        System.out.println("Adding new table...");
        // TODO: Implement add table dialog
    }

    private void openBillingView(TableModel table) {
        System.out.println("Opening billing view for: " + table.getTableName());
        // TODO: Load BillingView.fxml and pass table data
        // This would typically involve:
        // 1. Loading the BillingView.fxml
        // 2. Getting the controller
        // 3. Passing the table data to the billing controller
        // 4. Switching the view in the main content area
    }

    /* ---------- Summary Updates ---------- */

    private void updateSummaryCards() {
        long totalTables = tables.size();
        long availableTables = tables.stream().filter(TableModel::isAvailable).count();
        long occupiedTables = tables.stream().filter(TableModel::isOccupied).count();
        long reservedTables = tables.stream().filter(TableModel::isReserved).count();

        // Update summary card values
        updateCardValue(cardTotal, String.valueOf(totalTables));
        updateCardValue(cardAvailable, String.valueOf(availableTables));
        updateCardValue(cardOccupied, String.valueOf(occupiedTables));
        updateCardValue(cardReserved, String.valueOf(reservedTables));
    }

    private void updateCardValue(VBox card, String value) {
        // Find the summary-value label in the card
        for (Node child : card.getChildren()) {
            if (child instanceof Label label) {
                if (label.getStyleClass().contains("summary-value")) {
                    label.setText(value);
                    break;
                }
            }
        }
    }

    /* ---------- Public Methods ---------- */

    public void addTable(TableModel table) {
        tables.add(table);
        createTableCards();
        updateSummaryCards();
    }

    public void removeTable(TableModel table) {
        tables.remove(table);
        createTableCards();
        updateSummaryCards();
    }

    public void updateTableStatus(TableModel table, String newStatus) {
        table.setStatus(newStatus);
        createTableCards();
        updateSummaryCards();
    }

    public List<TableModel> getTables() {
        return tables;
    }

    public TableModel getTableByName(String tableName) {
        return tables.stream()
                .filter(table -> table.getTableName().equals(tableName))
                .findFirst()
                .orElse(null);
    }
}
