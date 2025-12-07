package com.example.pos.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML
    private BorderPane root;
    @FXML
    private StackPane contentArea;
    @FXML
    private ChoiceBox<String> outletChoice;

    @FXML
    private Button navDashboard;
    @FXML
    private Button navBilling;
    @FXML
    private Button navTransactions;
    @FXML
    private Button navInventory;
    @FXML
    private Button navMenu;
    @FXML
    private Button navTables;
    @FXML
    private Button navKOT;
    @FXML
    private Button navReports;
    @FXML
    private Button navStaffPayroll;
    @FXML
    private Button profileBtn;

    @FXML
    public void initialize() {
        if (outletChoice != null) {
            outletChoice.setItems(FXCollections.observableArrayList("Main Outlet", "Outlet 2"));
            outletChoice.setValue("Main Outlet");
        }
        loadView("DashboardView.fxml");
    }

    @FXML
    private void onOpenDashboard() {
        loadView("DashboardView.fxml");
    }

    @FXML
    private void onOpenBilling() {
        loadView("BillingView.fxml");
    }

    @FXML
    private void onOpenTransactions() {
        loadView("TransactionsView.fxml");
    }

    @FXML
    private void onOpenInventory() {
        loadView("InventoryView.fxml");
    }

    @FXML
    private void onOpenMenu() {
        loadView("MenuView.fxml");
    }

    @FXML
    private void onOpenTables() {
        loadView("TablesView.fxml");
    }

    @FXML
    private void onOpenKOT() {
        loadView("KOTView.fxml");
    }

    @FXML
    private void onOpenReports() {
        loadView("ReportsView.fxml");
    }

    @FXML
    private void onOpenStaffPayroll() {
        loadView("StaffPayrollView.fxml");
    }

    @FXML
    private void onOpenRestaurantInfo() {
        loadView("RestaurantInfoView.fxml");
    }

    private void loadView(String fxmlName) {
        try {
            Node view = FXMLLoader.load(getClass().getResource("/com/example/pos/view/" + fxmlName));
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace(); // 👈 print full cause
            Label errorLabel = new Label(" Failed to load: " + fxmlName + "\n" + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
            contentArea.getChildren().setAll(errorLabel);
        }
    }
}
