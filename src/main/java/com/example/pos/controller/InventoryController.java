package com.example.pos.controller;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.example.pos.model.InventoryItem;
import com.example.pos.service.InventoryService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

public class InventoryController {

    @FXML private StackPane uploadArea;
    @FXML private TableView<InventoryItem> itemsTable;
    @FXML private TableColumn<InventoryItem, String> colName;
    @FXML private TableColumn<InventoryItem, String> colRate;
    @FXML private TableColumn<InventoryItem, Integer> colQuantity;
    @FXML private TableColumn<InventoryItem, String> colCategory;
    @FXML private TableColumn<InventoryItem, Void> colActions;

    private final ObservableList<InventoryItem> items = FXCollections.observableArrayList();
    private final InventoryService inventoryService = new InventoryService();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    @FXML
    private void initialize() {
        setupTable();
        setupUploadHandlers();
        refreshTable();
    }

    @FXML
    private void onAddItem() {
        openItemDialog(null);
    }

    @FXML
    private void onUploadCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Inventory CSV");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        File file = chooser.showOpenDialog(uploadArea.getScene().getWindow());
        if (file != null) {
            importCsv(file);
        }
    }

    private void setupTable() {
        itemsTable.setItems(items);

        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        colRate.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                currency.format(data.getValue().getRate())));
        colQuantity.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getQuantity()));
        colCategory.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCategory()));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(8, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("secondary-button");
                btnDelete.getStyleClass().add("secondary-button");
                btnDelete.setStyle("-fx-text-fill: #dc2626;");
                btnEdit.setOnAction(e -> {
                    InventoryItem item = getTableView().getItems().get(getIndex());
                    openItemDialog(item);
                });
                btnDelete.setOnAction(e -> {
                    InventoryItem item = getTableView().getItems().get(getIndex());
                    confirmDelete(item);
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
    }

    private void refreshTable() {
        items.setAll(inventoryService.getAllItems());
    }

    private void setupUploadHandlers() {
        uploadArea.setOnMouseClicked(e -> onUploadCsv());
        uploadArea.setOnDragOver(this::onDragOver);
        uploadArea.setOnDragDropped(this::onDragDropped);
        uploadArea.setOnDragExited(e -> uploadArea.setStyle(""));
    }

    private void onDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
            uploadArea.setStyle("-fx-border-color:#2563eb; -fx-background-color: rgba(37,99,235,0.08);");
        }
        event.consume();
    }

    private void onDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            for (File file : db.getFiles()) {
                importCsv(file);
            }
            success = true;
        }
        event.setDropCompleted(success);
        uploadArea.setStyle("");
        event.consume();
    }

    private void importCsv(File file) {
        try {
            InventoryService.ImportResult result = inventoryService.importCsv(file);
            refreshTable();
            showInfo("Import complete",
                    "Added: " + result.added() + ", Updated: " + result.updated() + ", Skipped: " + result.skipped());
        } catch (IOException ex) {
            showError("Failed to import CSV", ex.getMessage());
        } catch (Exception ex) {
            showError("Import error", ex.getMessage());
        }
    }

    private void openItemDialog(InventoryItem existing) {
        Dialog<ItemFormResult> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Retail Item" : "Edit Retail Item");
        ButtonType saveButtonType = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveButtonType);

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        nameField.setPromptText("Item name");
        
        TextField barcodeField = new TextField();
        barcodeField.setPromptText("Scan or enter barcode");
        Label barcodeHint = new Label("Tip: Use barcode scanner to auto-fill");
        barcodeHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280;");
        
        TextField rateField = new TextField(existing != null ? String.valueOf(existing.getRate()) : "");
        rateField.setPromptText("Price");
        
        Spinner<Integer> qtySpinner = new Spinner<>();
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100000,
                existing != null ? existing.getQuantity() : 0));
        
        ChoiceBox<String> categoryChoice = new ChoiceBox<>(FXCollections.observableArrayList(
                InventoryService.RETAIL_CATEGORY, "Beverages", "Snacks", "Desserts"));
        categoryChoice.setValue(existing != null ? existing.getCategory() : InventoryService.RETAIL_CATEGORY);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.addRow(0, new Label("Name"), nameField);
        grid.addRow(1, new Label("Barcode"), barcodeField);
        grid.add(barcodeHint, 1, 2);
        grid.addRow(3, new Label("Rate (₹)"), rateField);
        grid.addRow(4, new Label("Quantity"), qtySpinner);
        grid.addRow(5, new Label("Category"), categoryChoice);

        // Auto-focus barcode field for quick scanning
        javafx.application.Platform.runLater(() -> barcodeField.requestFocus());

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(nameField.textProperty().isEmpty()
                .or(rateField.textProperty().isEmpty()));

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                try {
                    double rate = Double.parseDouble(rateField.getText());
                    if (rate < 0) {
                        throw new NumberFormatException("Rate cannot be negative");
                    }
                    int qty = qtySpinner.getValue();
                    String barcode = barcodeField.getText().trim();
                    return new ItemFormResult(nameField.getText().trim(), rate, qty, categoryChoice.getValue(), barcode);
                } catch (NumberFormatException ex) {
                    showError("Invalid rate", "Please enter a valid number for rate.");
                    return null;
                }
            }
            return null;
        });

        Optional<ItemFormResult> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        ItemFormResult data = result.get();
        try {
            if (existing == null) {
                inventoryService.createItem(data.name(), data.rate(), data.quantity(), data.category());
                System.out.println("Item created with barcode: " + data.barcode());
            } else {
                inventoryService.updateItem(existing.getId(), data.name(), data.rate(), data.quantity(), data.category());
                System.out.println("Item updated with barcode: " + data.barcode());
            }
            refreshTable();
            showInfo("Success", "Item saved successfully!" + 
                (data.barcode() != null && !data.barcode().isEmpty() ? "\nBarcode: " + data.barcode() : ""));
        } catch (Exception ex) {
            showError("Unable to save item", ex.getMessage());
        }
    }

    private void confirmDelete(InventoryItem item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Item");
        alert.setHeaderText("Delete " + item.getName() + "?");
        alert.setContentText("This action cannot be undone.");
        alert.showAndWait()
                .filter(res -> res == ButtonType.OK)
                .ifPresent(res -> {
                    try {
                        inventoryService.deleteItem(item.getId());
                        refreshTable();
                    } catch (Exception ex) {
                        showError("Unable to delete item", ex.getMessage());
                    }
                });
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record ItemFormResult(String name, double rate, int quantity, String category, String barcode) {
    }
}