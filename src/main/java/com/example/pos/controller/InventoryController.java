package com.example.pos.controller;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

public class InventoryController {

	@FXML private StackPane uploadArea;
	@FXML private Button btnUpload;

	@FXML private TableView<PurchaseRecord> purchaseTable;
	@FXML private TableColumn<PurchaseRecord, String> colSupplier;
	@FXML private TableColumn<PurchaseRecord, String> colDate;
	@FXML private TableColumn<PurchaseRecord, String> colItems;
	@FXML private TableColumn<PurchaseRecord, String> colAmount;
	@FXML private TableColumn<PurchaseRecord, String> colStatus;
	@FXML private TableColumn<PurchaseRecord, Void>   colActions;

	private final ObservableList<PurchaseRecord> tableData = FXCollections.observableArrayList();

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@FXML
	private void initialize() {
		setupTable();
		loadDummyData();
		setupUploadHandlers();
	}

	/* ---------- Table ---------- */

	private void setupTable() {
		colSupplier.setCellValueFactory(data -> data.getValue().supplierNameProperty());
		colDate.setCellValueFactory(data -> data.getValue().dateProperty());
		colItems.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().itemsProperty().get())));
		colAmount.setCellValueFactory(data -> data.getValue().amountProperty());
		colStatus.setCellValueFactory(data -> data.getValue().paymentStatusProperty());

		colStatus.setCellFactory(col -> new TableCell<>() {
			@Override
			protected void updateItem(String status, boolean empty) {
				super.updateItem(status, empty);
				if (empty || status == null) {
					setText(null);
					setGraphic(null);
				} else {
					Label chip = new Label(status);
					chip.getStyleClass().add("action-button");
					if ("Paid".equalsIgnoreCase(status)) {
						chip.getStyleClass().add("success");
					} else {
						chip.getStyleClass().add("warning");
					}
					setGraphic(chip);
					setText(null);
				}
			}
		});

		colActions.setCellFactory(col -> new TableCell<>() {
			private final Button btnView = styledButton("View");
			private final Button btnEdit = styledButton("Edit");
			private final Button btnPaid = styledButton("Mark Paid");

			{
				btnPaid.getStyleClass().add("success");
				btnView.setOnAction(this::onView);
				btnEdit.setOnAction(this::onEdit);
				btnPaid.setOnAction(this::onPaid);
			}

			private Button styledButton(String text) {
				Button b = new Button(text);
				b.getStyleClass().add("action-button");
				return b;
			}

			private void onView(ActionEvent e) {
				PurchaseRecord rec = getTableView().getItems().get(getIndex());
				System.out.println("View clicked: " + rec.getSupplierName());
			}

			private void onEdit(ActionEvent e) {
				PurchaseRecord rec = getTableView().getItems().get(getIndex());
				System.out.println("Edit clicked: " + rec.getSupplierName());
			}

			private void onPaid(ActionEvent e) {
				PurchaseRecord rec = getTableView().getItems().get(getIndex());
				rec.setPaymentStatus("Paid");
				purchaseTable.refresh();
			}

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
				} else {
					javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(8, btnView, btnEdit, btnPaid);
					setGraphic(box);
				}
			}
		});

		purchaseTable.setItems(tableData);
	}

	private void loadDummyData() {
		tableData.setAll(
			new PurchaseRecord("Spice Traders",       LocalDate.of(2025,10,8),  12, "₹16,000", "Paid"),
			new PurchaseRecord("Fresh Vegetables Co.", LocalDate.of(2025,10,7),   8, "₹8,500",  "Pending"),
			new PurchaseRecord("Dairy Suppliers Ltd.", LocalDate.of(2025,10,6),   5, "₹12,000", "Paid"),
			new PurchaseRecord("Meat Market",           LocalDate.of(2025,10,5),  15, "₹22,000", "Paid"),
			new PurchaseRecord("Beverages Wholesale",   LocalDate.of(2025,10,4),  20, "₹18,500", "Pending")
		);
	}

	/* ---------- Upload ---------- */

	private void setupUploadHandlers() {
		btnUpload.setOnAction(e -> openFileChooser());

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
			handleUploadedFiles(db.getFiles());
			success = true;
		}
		event.setDropCompleted(success);
		uploadArea.setStyle("");
		event.consume();
	}

	private void openFileChooser() {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Select Invoices");
		chooser.getExtensionFilters().addAll(
			new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.xls", "*.xlsx"),
			new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
		);
		List<File> files = chooser.showOpenMultipleDialog(uploadArea.getScene().getWindow());
		if (files != null && !files.isEmpty()) {
			handleUploadedFiles(files);
		}
	}

	private void handleUploadedFiles(List<File> files) {
		for (File f : files) {
			System.out.println("Uploaded: " + f.getAbsolutePath());
			// TODO: parse and add to table if needed
		}
	}

	/* ---------- Model ---------- */

	public static class PurchaseRecord {
		private final StringProperty supplierName = new SimpleStringProperty();
		private final StringProperty date         = new SimpleStringProperty();
		private final IntegerProperty items       = new SimpleIntegerProperty();
		private final StringProperty amount       = new SimpleStringProperty();
		private final StringProperty paymentStatus= new SimpleStringProperty();

		public PurchaseRecord(String supplierName, LocalDate date, int items, String amount, String paymentStatus) {
			setSupplierName(supplierName);
			setDate(date.format(DATE_FMT));
			setItems(items);
			setAmount(amount);
			setPaymentStatus(paymentStatus);
		}

		public String getSupplierName() { return supplierName.get(); }
		public void setSupplierName(String value) { supplierName.set(value); }
		public StringProperty supplierNameProperty() { return supplierName; }

		public String getDate() { return date.get(); }
		public void setDate(String value) { date.set(value); }
		public StringProperty dateProperty() { return date; }

		public int getItems() { return items.get(); }
		public void setItems(int value) { items.set(value); }
		public IntegerProperty itemsProperty() { return items; }

		public String getAmount() { return amount.get(); }
		public void setAmount(String value) { amount.set(value); }
		public StringProperty amountProperty() { return amount; }

		public String getPaymentStatus() { return paymentStatus.get(); }
		public void setPaymentStatus(String value) { paymentStatus.set(value); }
		public StringProperty paymentStatusProperty() { return paymentStatus; }
	}
}