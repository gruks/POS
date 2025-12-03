package com.example.pos.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.example.pos.model.TableFormData;
import com.example.pos.model.TableModel;
import com.example.pos.model.TableSession;
import com.example.pos.service.TableService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class TablesController {

    @FXML private VBox cardTotal;
    @FXML private VBox cardAvailable;
    @FXML private VBox cardOccupied;
    @FXML private VBox cardReserved;
    @FXML private GridPane tableGrid;
    @FXML private Button btnRefresh;
    @FXML private Button btnAddTable;

    private final ObservableList<TableModel> tables = FXCollections.observableArrayList();
    private final TableService tableService = new TableService();

    @FXML
    private void initialize() {
        loadTables();
        createTableCards();
        setupEventHandlers();
        updateSummaryCards();
    }

    /* ---------- Data Loading ---------- */

    private void loadTables() {
        try {
            tables.setAll(tableService.loadTables());
        } catch (Exception ex) {
            logError("Unable to load tables", ex);
            showAlert("Unable to load tables", ex.getMessage());
        }
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
        card.setPadding(new Insets(12));
        card.setAlignment(javafx.geometry.Pos.CENTER);

        Label nameLabel = new Label(table.getTableName());
        nameLabel.getStyleClass().add("table-name");

        Label capacityLabel = new Label("Capacity: " + table.getCapacity());
        capacityLabel.getStyleClass().add("table-capacity");

        Label detailsLabel = new Label();
        detailsLabel.getStyleClass().add("table-details");
        if (table.isOccupied()) {
            detailsLabel.setText(table.getGuests().isBlank()
                    ? "In service"
                    : "Guest: " + table.getGuests());
        } else if (table.isReserved()) {
            detailsLabel.setText(table.getReservationTime().isBlank()
                    ? "Reserved"
                    : table.getReservationTime());
        } else {
            detailsLabel.setText("Available");
        }

        card.getChildren().addAll(nameLabel, capacityLabel, detailsLabel);

        card.setOnMouseClicked(event -> openBillingView(table));
        card.setOnContextMenuRequested(event -> showTableContextMenu(event, table));

        return card;
    }

    /* ---------- Event Handlers ---------- */

    private void setupEventHandlers() {
        btnRefresh.setOnAction(this::onRefresh);
        btnAddTable.setOnAction(this::onAddTable);
    }

    private void onRefresh(ActionEvent event) {
        loadTables();
        createTableCards();
        updateSummaryCards();
    }

    private void onAddTable(ActionEvent event) {
        openTableDialog(null);
    }

    private void openBillingView(TableModel table) {
        if (table.getId() == null) {
            showAlert("Missing Table Id", "Please save the table before opening billing.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pos/view/BillingView.fxml"));
            Parent root = loader.load();
            BillingController controller = loader.getController();
            TableSession session = tableService.loadSession(table.getId());
            controller.openForTable(tableService, table, session);

            Stage stage = new Stage();
            stage.setTitle("Billing - " + table.getTableName());
            stage.initModality(Modality.NONE);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException ex) {
            logError("Unable to open billing view", ex);
            showAlert("Unable to open billing", ex.getMessage());
        }
    }

    private void showTableContextMenu(ContextMenuEvent event, TableModel table) {
        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();

        javafx.scene.control.MenuItem editItem = new javafx.scene.control.MenuItem("Edit Table");
        editItem.setOnAction(e -> openTableDialog(table));

        javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete Table");
        deleteItem.setOnAction(e -> deleteTable(table));

        if (!table.canModify()) {
            editItem.setDisable(true);
            deleteItem.setDisable(true);
        }

        menu.getItems().addAll(editItem, deleteItem);
        menu.show(tableGrid.getScene().getWindow(), event.getScreenX(), event.getScreenY());
    }

    private void deleteTable(TableModel table) {
        if (!table.canModify()) {
            showAlert("Table in use", "You can delete a table only after its bill is settled or cancelled.");
            return;
        }
        if (table.getId() == null) {
            tables.remove(table);
            createTableCards();
            updateSummaryCards();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Table");
        confirm.setHeaderText("Delete " + table.getTableName() + "?");
        confirm.setContentText("This action cannot be undone.");
        confirm.showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> {
            try {
                tableService.deleteTable(table.getId());
                tables.remove(table);
                createTableCards();
                updateSummaryCards();
            } catch (Exception ex) {
                logError("Failed to delete table", ex);
                showAlert("Failed to delete table", ex.getMessage());
            }
        });
    }

    private void openTableDialog(TableModel existing) {
        if (existing != null && !existing.canModify()) {
            showAlert("Table in use", "Settle or cancel the bill before modifying this table.");
            return;
        }

        Dialog<TableFormData> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add table" : "Edit table");
        ButtonType saveButtonType = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveButtonType);

        TextField nameField = new TextField(existing != null ? existing.getTableName() : "");
        nameField.setPromptText("Table name");

        Spinner<Integer> capacitySpinner = new Spinner<>();
        capacitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20,
                existing != null ? existing.getCapacity() : 4));

        ChoiceBox<String> statusChoice = new ChoiceBox<>(FXCollections.observableArrayList("Available", "Reserved"));
        statusChoice.setValue(existing != null && existing.isReserved() ? "Reserved" : "Available");

        TextField reservedForField = new TextField(existing != null ? existing.getReservedFor() : "");
        reservedForField.setPromptText("Reserved for");
        TextField reservationTimeField = new TextField(existing != null ? existing.getReservationTime() : "");
        reservationTimeField.setPromptText("Reservation time");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.add(new Label("Name"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Capacity"), 0, 1);
        grid.add(capacitySpinner, 1, 1);
        grid.add(new Label("Status"), 0, 2);
        grid.add(statusChoice, 1, 2);
        grid.add(new Label("Reserved for"), 0, 3);
        grid.add(reservedForField, 1, 3);
        grid.add(new Label("Time"), 0, 4);
        grid.add(reservationTimeField, 1, 4);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(nameField.textProperty().isEmpty());

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                return new TableFormData(
                        nameField.getText().trim(),
                        capacitySpinner.getValue(),
                        statusChoice.getValue(),
                        reservedForField.getText().trim(),
                        reservationTimeField.getText().trim());
            }
            return null;
        });

        Optional<TableFormData> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        try {
            if (existing == null) {
                TableModel created = tableService.createTable(result.get());
                tables.add(created);
            } else {
                tableService.updateTable(existing.getId(), result.get());
                loadTables();
            }
            createTableCards();
            updateSummaryCards();
        } catch (Exception ex) {
            logError("Failed to save table", ex);
            showAlert("Failed to save table", ex.getMessage());
        }
    }

    /* ---------- Summary Updates ---------- */

    private void updateSummaryCards() {
        long totalTables = tables.size();
        long availableTables = tables.stream().filter(TableModel::isAvailable).count();
        long occupiedTables = tables.stream().filter(TableModel::isOccupied).count();
        long reservedTables = tables.stream().filter(TableModel::isReserved).count();

        updateCardValue(cardTotal, String.valueOf(totalTables));
        updateCardValue(cardAvailable, String.valueOf(availableTables));
        updateCardValue(cardOccupied, String.valueOf(occupiedTables));
        updateCardValue(cardReserved, String.valueOf(reservedTables));
    }

    private void updateCardValue(VBox card, String value) {
        for (Node child : card.getChildren()) {
            if (child instanceof Label label && label.getStyleClass().contains("summary-value")) {
                    label.setText(value);
                    break;
            }
        }
    }

    /* ---------- Public Methods ---------- */

    public List<TableModel> getTables() {
        return tables;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void logError(String message, Exception ex) {
        System.err.println(message + ": " + ex.getMessage());
    }
}