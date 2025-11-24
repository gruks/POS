module com.example.pos {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    // MongoDB Java Driver modules
    requires org.mongodb.bson;
    requires org.mongodb.driver.core;
    requires org.mongodb.driver.sync.client;

    opens com.example.pos to javafx.fxml;
    opens com.example.pos.controller to javafx.fxml;
    opens com.example.pos.model to javafx.base; // for JavaFX properties/table reflection

    exports com.example.pos;
    exports com.example.pos.controller;
}
