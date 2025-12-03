package com.example.pos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.example.pos.db.DatabaseInitializer;

public class MainApp extends Application {

    @Override
    public void start(@SuppressWarnings("exports") Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pos/view/MainLayout.fxml"));
        DatabaseInitializer.initialize();
        Parent root = loader.load();
        Scene scene = new Scene(root, 1440, 900);
        scene.getStylesheets().addAll(
                getClass().getResource("/com/example/pos/styles/app.css").toExternalForm(),
                getClass().getResource("/com/example/pos/styles/transaction.css").toExternalForm()
        );
        primaryStage.setTitle("RestaurantPOS");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true); // Open maximized, but with native window controls
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
