package com.example.pos.controller;

import com.example.pos.model.RestaurantInfo;
import com.example.pos.service.RestaurantInfoService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class RestaurantInfoController {

    @FXML private TextField nameField;
    @FXML private TextField contactField;
    @FXML private TextField emailField;
    @FXML private TextField websiteField;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private TextField stateField;
    @FXML private TextField pinCodeField;
    @FXML private TextField gstinField;
    @FXML private TextField fssaiField;

    private final RestaurantInfoService service = new RestaurantInfoService();

    @FXML
    public void initialize() {
        loadRestaurantInfo();
    }

    private void loadRestaurantInfo() {
        RestaurantInfo info = service.getRestaurantInfo();
        if (info != null) {
            nameField.setText(info.getName());
            contactField.setText(info.getContactNumber());
            emailField.setText(info.getEmail());
            websiteField.setText(info.getWebsite());
            addressField.setText(info.getAddress());
            cityField.setText(info.getCity());
            stateField.setText(info.getState());
            pinCodeField.setText(info.getPinCode());
            gstinField.setText(info.getGstin());
            fssaiField.setText(info.getFssaiLicense());
        }
    }

    @FXML
    private void onSave() {
        // Validate required fields
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showError("Restaurant name is required");
            return;
        }
        if (contactField.getText() == null || contactField.getText().trim().isEmpty()) {
            showError("Contact number is required");
            return;
        }
        if (addressField.getText() == null || addressField.getText().trim().isEmpty()) {
            showError("Address is required");
            return;
        }
        if (cityField.getText() == null || cityField.getText().trim().isEmpty()) {
            showError("City is required");
            return;
        }
        if (stateField.getText() == null || stateField.getText().trim().isEmpty()) {
            showError("State is required");
            return;
        }
        if (pinCodeField.getText() == null || pinCodeField.getText().trim().isEmpty()) {
            showError("PIN code is required");
            return;
        }
        if (fssaiField.getText() == null || fssaiField.getText().trim().isEmpty()) {
            showError("FSSAI License is required");
            return;
        }

        RestaurantInfo info = new RestaurantInfo(
            nameField.getText().trim(),
            addressField.getText().trim(),
            cityField.getText().trim(),
            stateField.getText().trim(),
            pinCodeField.getText().trim(),
            contactField.getText().trim(),
            emailField.getText() != null ? emailField.getText().trim() : "",
            websiteField.getText() != null ? websiteField.getText().trim() : "",
            gstinField.getText() != null ? gstinField.getText().trim() : "",
            fssaiField.getText().trim(),
            null
        );

        service.saveRestaurantInfo(info);
        showSuccess("Restaurant information saved successfully!");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
