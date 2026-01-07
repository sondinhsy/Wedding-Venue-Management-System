package com.weddingapp;

import com.weddingapp.util.Database;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application class for Wedding Venue Management System
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize database in background thread for better UI responsiveness
        new Thread(() -> {
            try {
                Database.initialize();
                System.out.println("Database initialized successfully.");
            } catch (Exception e) {
                System.err.println("Database initialization failed: " + e.getMessage());
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Database Error");
                    alert.setHeaderText("Không thể khởi tạo database");
                    alert.setContentText("Vui lòng kiểm tra lại cấu hình database.\n" + e.getMessage());
                    alert.showAndWait();
                });
            }
        }, "db-init-thread").start();

        // Load Login FXML - start from login screen
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(root, 600, 700);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

        // Configure primary stage
        primaryStage.setTitle("💒 Hệ Thống Quản Lý Sảnh Cưới - Đăng nhập");
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(600);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
