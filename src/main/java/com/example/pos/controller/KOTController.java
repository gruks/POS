package com.example.pos.controller;

import com.example.pos.model.KOT;
import com.example.pos.model.KOTItem;
import com.example.pos.service.KOTService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KOTController {

    @FXML private FlowPane kotContainer;
    @FXML private Label pendingCountLabel;
    @FXML private Label preparingCountLabel;
    @FXML private Label readyCountLabel;
    @FXML private Button refreshBtn;
    @FXML private Button clearCompletedBtn;
    @FXML private ComboBox<String> filterComboBox;

    private final KOTService kotService = new KOTService();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ObservableList<KOT> activeKOTs = FXCollections.observableArrayList();
    private String currentFilter = "All";

    @FXML
    private void initialize() {
        setupEventHandlers();
        setupFilter();
        loadKOTs();
        
        // Auto-refresh every 30 seconds
        startAutoRefresh();
    }

    private void setupEventHandlers() {
        if (refreshBtn != null) {
            refreshBtn.setOnAction(e -> loadKOTs());
        }
        if (clearCompletedBtn != null) {
            clearCompletedBtn.setOnAction(e -> clearCompletedKOTs());
        }
    }

    private void setupFilter() {
        if (filterComboBox != null) {
            filterComboBox.setItems(FXCollections.observableArrayList(
                "All", "Pending", "Preparing", "Ready", "Completed"));
            filterComboBox.setValue("All");
            filterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                currentFilter = newVal;
                loadKOTs();
            });
        }
    }

    private void loadKOTs() {
        Task<List<KOT>> task = new Task<>() {
            @Override
            protected List<KOT> call() throws Exception {
                if (currentFilter.equals("All")) {
                    return kotService.getAllKOTs();
                } else {
                    return kotService.getActiveKOTs();
                }
            }
        };

        task.setOnSucceeded(e -> {
            List<KOT> kots = task.getValue();
            activeKOTs.setAll(kots);
            displayKOTs(kots);
            updateCounts();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("Failed to load KOTs: " + (ex != null ? ex.getMessage() : "Unknown error"));
            if (ex != null) ex.printStackTrace();
        });

        executor.submit(task);
    }

    private void displayKOTs(List<KOT> kots) {
        Platform.runLater(() -> {
            if (kotContainer == null) return;
            
            kotContainer.getChildren().clear();

            // Filter based on current filter
            List<KOT> filteredKOTs = kots;
            if (!currentFilter.equals("All")) {
                filteredKOTs = kots.stream()
                    .filter(kot -> kot.getStatus().equals(currentFilter))
                    .toList();
            }

            if (filteredKOTs.isEmpty()) {
                Label emptyLabel = new Label("No KOTs found");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280;");
                kotContainer.getChildren().add(emptyLabel);
                return;
            }

            for (KOT kot : filteredKOTs) {
                VBox kotCard = createKOTCard(kot);
                kotContainer.getChildren().add(kotCard);
            }
        });
    }

    private VBox createKOTCard(KOT kot) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle(getCardStyle(kot.getStatus()));
        card.setPrefWidth(280);
        card.setMinHeight(200);

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label kotNumber = new Label("KOT #" + kot.getKotNumber());
        kotNumber.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label priority = new Label(kot.getPriority());
        priority.setStyle(getPriorityStyle(kot.getPriority()));
        priority.setPadding(new Insets(2, 8, 2, 8));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label time = new Label(kot.getDisplayTime());
        time.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        header.getChildren().addAll(kotNumber, priority, spacer, time);

        // Table/Order info
        Label tableInfo = new Label(kot.getTableName() + " • " + kot.getOrderType());
        tableInfo.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");

        if (kot.getCustomerName() != null && !kot.getCustomerName().isEmpty()) {
            Label customer = new Label("Customer: " + kot.getCustomerName());
            customer.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
            card.getChildren().add(customer);
        }

        // Elapsed time
        Label elapsed = new Label("⏱ " + kot.getElapsedTime());
        elapsed.setStyle(getElapsedTimeStyle(kot));

        // Items list
        VBox itemsList = new VBox(4);
        itemsList.setStyle("-fx-background-color: #f9fafb; -fx-padding: 8; -fx-background-radius: 6;");
        
        for (KOTItem item : kot.getItems()) {
            HBox itemRow = new HBox(8);
            itemRow.setAlignment(Pos.CENTER_LEFT);
            
            Label qty = new Label(item.getQuantity() + "x");
            qty.setStyle("-fx-font-weight: bold; -fx-min-width: 30;");
            
            Label itemName = new Label(item.getItemName());
            itemName.setStyle("-fx-text-fill: #111827;");
            
            if (item.getSpecialNotes() != null && !item.getSpecialNotes().isEmpty()) {
                Label notes = new Label("(" + item.getSpecialNotes() + ")");
                notes.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 10px;");
                itemRow.getChildren().addAll(qty, itemName, notes);
            } else {
                itemRow.getChildren().addAll(qty, itemName);
            }
            
            itemsList.getChildren().add(itemRow);
        }

        // Status buttons
        HBox statusButtons = new HBox(4);
        statusButtons.setAlignment(Pos.CENTER);

        if (kot.getStatus().equals("Pending")) {
            Button startBtn = new Button("Start");
            startBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand;");
            startBtn.setOnAction(e -> updateKOTStatus(kot, "Preparing"));
            
            Button cancelBtn = new Button("Cancel");
            cancelBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");
            cancelBtn.setOnAction(e -> updateKOTStatus(kot, "Cancelled"));
            
            statusButtons.getChildren().addAll(startBtn, cancelBtn);
        } else if (kot.getStatus().equals("Preparing")) {
            Button readyBtn = new Button("Mark Ready");
            readyBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-cursor: hand;");
            readyBtn.setOnAction(e -> updateKOTStatus(kot, "Ready"));
            
            statusButtons.getChildren().add(readyBtn);
        } else if (kot.getStatus().equals("Ready")) {
            Button completeBtn = new Button("Complete");
            completeBtn.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-cursor: hand;");
            completeBtn.setOnAction(e -> updateKOTStatus(kot, "Completed"));
            
            statusButtons.getChildren().add(completeBtn);
        }

        // Priority buttons
        if (!kot.getStatus().equals("Completed") && !kot.getStatus().equals("Cancelled")) {
            Button priorityBtn = new Button("⚡");
            priorityBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-cursor: hand;");
            priorityBtn.setTooltip(new Tooltip("Set as Urgent"));
            priorityBtn.setOnAction(e -> updateKOTPriority(kot, "Urgent"));
            statusButtons.getChildren().add(priorityBtn);
        }

        card.getChildren().addAll(header, tableInfo, elapsed, new Separator(), itemsList, statusButtons);

        return card;
    }

    private String getCardStyle(String status) {
        String baseStyle = "-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0.2, 0, 2); ";
        
        return switch (status) {
            case "Pending" -> baseStyle + "-fx-border-color: #f59e0b; -fx-border-width: 2;";
            case "Preparing" -> baseStyle + "-fx-border-color: #3b82f6; -fx-border-width: 2;";
            case "Ready" -> baseStyle + "-fx-border-color: #10b981; -fx-border-width: 2;";
            case "Completed" -> baseStyle + "-fx-border-color: #6b7280; -fx-border-width: 1; -fx-opacity: 0.7;";
            case "Cancelled" -> baseStyle + "-fx-border-color: #ef4444; -fx-border-width: 1; -fx-opacity: 0.7;";
            default -> baseStyle + "-fx-border-color: #e5e7eb; -fx-border-width: 1;";
        };
    }

    private String getPriorityStyle(String priority) {
        return switch (priority) {
            case "Urgent" -> "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;";
            case "High" -> "-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;";
            default -> "-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280; -fx-background-radius: 4; -fx-font-size: 10px;";
        };
    }

    private String getElapsedTimeStyle(KOT kot) {
        if (kot.getCreatedAt() == null) {
            return "-fx-text-fill: #6b7280; -fx-font-size: 12px;";
        }
        
        long minutes = java.time.Duration.between(kot.getCreatedAt(), 
            java.time.LocalDateTime.now()).toMinutes();
        
        if (minutes > 30) {
            return "-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold;";
        } else if (minutes > 15) {
            return "-fx-text-fill: #f59e0b; -fx-font-size: 12px; -fx-font-weight: bold;";
        } else {
            return "-fx-text-fill: #10b981; -fx-font-size: 12px;";
        }
    }

    private void updateKOTStatus(KOT kot, String newStatus) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                kotService.updateKOTStatus(kot.getId(), newStatus);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            kot.setStatus(newStatus);
            loadKOTs();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            showAlert("Error", "Failed to update KOT status: " + 
                (ex != null ? ex.getMessage() : "Unknown error"));
        });

        executor.submit(task);
    }

    private void updateKOTPriority(KOT kot, String newPriority) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                kotService.updateKOTPriority(kot.getId(), newPriority);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            kot.setPriority(newPriority);
            loadKOTs();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            showAlert("Error", "Failed to update KOT priority: " + 
                (ex != null ? ex.getMessage() : "Unknown error"));
        });

        executor.submit(task);
    }

    private void clearCompletedKOTs() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Completed KOTs");
        confirm.setHeaderText("Clear completed KOTs older than 2 hours?");
        confirm.setContentText("This will permanently delete old completed and cancelled KOTs.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Integer> task = new Task<>() {
                    @Override
                    protected Integer call() throws Exception {
                        return kotService.clearCompletedKOTs(2);
                    }
                };

                task.setOnSucceeded(e -> {
                    int count = task.getValue();
                    showAlert("Success", count + " KOT(s) cleared successfully!");
                    loadKOTs();
                });

                task.setOnFailed(e -> {
                    Throwable ex = task.getException();
                    showAlert("Error", "Failed to clear KOTs: " + 
                        (ex != null ? ex.getMessage() : "Unknown error"));
                });

                executor.submit(task);
            }
        });
    }

    private void updateCounts() {
        Task<int[]> task = new Task<>() {
            @Override
            protected int[] call() throws Exception {
                int pending = kotService.getKOTCountByStatus("Pending");
                int preparing = kotService.getKOTCountByStatus("Preparing");
                int ready = kotService.getKOTCountByStatus("Ready");
                return new int[]{pending, preparing, ready};
            }
        };

        task.setOnSucceeded(e -> {
            int[] counts = task.getValue();
            Platform.runLater(() -> {
                if (pendingCountLabel != null) pendingCountLabel.setText(String.valueOf(counts[0]));
                if (preparingCountLabel != null) preparingCountLabel.setText(String.valueOf(counts[1]));
                if (readyCountLabel != null) readyCountLabel.setText(String.valueOf(counts[2]));
            });
        });

        executor.submit(task);
    }

    private void startAutoRefresh() {
        Thread refreshThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000); // 30 seconds
                    Platform.runLater(this::loadKOTs);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
