package com.weddingapp.controller;

import com.weddingapp.dao.UserDAO;
import com.weddingapp.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    private final UserDAO userDAO = new UserDAO();
    private static User currentUser;

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        // Focus vào username field khi mở
        usernameField.requestFocus();
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Clear previous error
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        if (username.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập");
            usernameField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("Vui lòng nhập mật khẩu");
            passwordField.requestFocus();
            return;
        }

        try {
            User user = userDAO.authenticate(username, password);
            if (user != null) {
                currentUser = user;
                // Chuyển sang Dashboard
                loadDashboard();
            } else {
                showError("Tên đăng nhập hoặc mật khẩu không đúng");
                passwordField.clear();
                usernameField.requestFocus();
            }
        } catch (Exception e) {
            showError("Lỗi đăng nhập: " + e.getMessage());
        }
    }

    private void loadDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi khi tải Dashboard: " + e.getMessage(), ButtonType.OK);
            alert.setTitle("Lỗi");
            alert.showAndWait();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void logout() {
        currentUser = null;
    }
}
