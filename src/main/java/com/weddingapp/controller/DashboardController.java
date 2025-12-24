package com.weddingapp.controller;

import com.weddingapp.dao.BookingDAO;
import com.weddingapp.dao.CustomerDAO;
import com.weddingapp.model.Booking;
import com.weddingapp.model.MenuItem;
import com.weddingapp.util.CurrencyFormatter;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {
    private final BookingDAO bookingDAO = new BookingDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ObservableList<Booking> bookings = FXCollections.observableArrayList();

    @FXML private StackPane contentPane;
    @FXML private Label userInfoLabel;
    @FXML private Button btnDashboard;
    @FXML private Button btnHall;
    @FXML private Button btnMenu;
    @FXML private Button btnBooking;
    @FXML private Button btnCustomer;
    @FXML private Button btnInvoice;

    // Dashboard content fields
    @FXML private Label customerCountLabel;
    @FXML private Label bookingCountLabel;
    @FXML private Label revenueLabel;
    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, String> colBookingCustomer;
    @FXML private TableColumn<Booking, String> colBookingHall;
    @FXML private TableColumn<Booking, String> colBookingDate;
    @FXML private TableColumn<Booking, Number> colBookingTables;
    @FXML private TableColumn<Booking, String> colBookingTotal;
    @FXML private TableColumn<Booking, String> colBookingMenu;
    @FXML private PieChart menuRevenueChart;
    @FXML private PieChart serviceTypeChart;
    @FXML private StackPane donutChartPane;

    /**
     * JavaFX sẽ gọi initialize() mỗi lần một FXML được load với cùng controller.
     * Trong lớp này, chúng ta lại load `dashboard-content.fxml` và set controller là `this`,
     * nên initialize() bị gọi đệ quy vô hạn → StackOverflowError.
     * Cờ này đảm bảo khối khởi tạo chỉ chạy đúng một lần.
     */
    private boolean initialized = false;

    @FXML
    public void initialize() {
        // Ngăn chặn gọi đệ quy khi load `dashboard-content.fxml`
        if (initialized) {
            return;
        }
        initialized = true;
        // Set user info
        if (LoginController.getCurrentUser() != null) {
            userInfoLabel.setText(LoginController.getCurrentUser().getFullName());
        }
        
        // Load dashboard content
        loadDashboardContent();
        updateActiveButton(btnDashboard);
    }

    private void loadDashboardContent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard-content.fxml"));
            loader.setController(this); // Use this controller
            Parent content = loader.load();
            contentPane.getChildren().clear();
            contentPane.getChildren().add(content);
            
            setupDashboardTables();
            reloadDashboardData();
        } catch (Exception e) {
            // In addition to showing a user-friendly alert, log full stack trace to console
            e.printStackTrace();
            showError("Lỗi khi tải Dashboard: " + e.getMessage());
        }
    }

    private void setupDashboardTables() {
        colBookingCustomer.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCustomer().getName()));
        colBookingHall.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getHall().getName()));
        colBookingDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEventDate().format(DateTimeFormatter.ISO_DATE)));
        colBookingTables.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getTables()));
        colBookingTotal.setCellValueFactory(cell -> 
            new SimpleStringProperty(CurrencyFormatter.formatVND(cell.getValue().getTotal())));
        colBookingMenu.setCellValueFactory(cell -> {
            String menuTitles = cell.getValue().getMenuItems().stream()
                    .map(MenuItem::getTitle)
                    .collect(Collectors.joining(", "));
            return new SimpleStringProperty(menuTitles);
        });
        bookingTable.setItems(bookings);
    }

    private void reloadDashboardData() {
        bookings.setAll(bookingDAO.findAll());
        updateTotals();
        updateCharts();
    }

    private void updateTotals() {
        if (customerCountLabel != null) {
            customerCountLabel.setText(String.valueOf(customerDAO.findAll().size()));
        }
        if (bookingCountLabel != null) {
            bookingCountLabel.setText(String.valueOf(bookings.size()));
        }
        if (revenueLabel != null) {
            double revenue = bookings.stream().mapToDouble(Booking::getTotal).sum();
            revenueLabel.setText(CurrencyFormatter.formatVND(revenue));
        }
    }

    private void updateCharts() {
        if (menuRevenueChart != null) {
            menuRevenueChart.setData(buildPieDataByMenu());
        }
        if (serviceTypeChart != null) {
            serviceTypeChart.setData(buildDonutDataByServiceType());
            // Ensure donut effect
            if (donutChartPane != null && donutChartPane.getChildren().size() > 1) {
                Circle centerCircle = (Circle) donutChartPane.getChildren().get(1);
                centerCircle.setRadius(80);
            }
        }
    }

    private ObservableList<PieChart.Data> buildPieDataByMenu() {
        Map<String, Double> totals = new HashMap<>();
        for (Booking b : bookings) {
            if (b.getMenuItems().isEmpty()) {
                totals.merge("Chưa chọn menu", b.getTotal(), Double::sum);
            } else {
                b.getMenuItems().forEach(mi ->
                        totals.merge(mi.getTitle(), mi.getPrice() * b.getTables(), Double::sum));
            }
        }
        if (totals.isEmpty()) {
            return FXCollections.observableArrayList(new PieChart.Data("Chưa có dữ liệu", 1));
        }
        return totals.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(8) // Top 8 items
                .map(e -> new PieChart.Data(e.getKey() + " (" + String.format("%.0f%%", (e.getValue() / totals.values().stream().mapToDouble(Double::doubleValue).sum()) * 100) + ")", e.getValue()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), FXCollections::observableArrayList));
    }

    private ObservableList<PieChart.Data> buildDonutDataByServiceType() {
        // Simulate service types - you can modify based on your data model
        Map<String, Double> serviceTypes = new HashMap<>();
        serviceTypes.put("Tại bàn", bookings.stream().mapToDouble(Booking::getTotal).sum() * 0.68);
        serviceTypes.put("Giao hàng", bookings.stream().mapToDouble(Booking::getTotal).sum() * 0.33);
        serviceTypes.put("Mang về", bookings.stream().mapToDouble(Booking::getTotal).sum() * 0.29);
        
        if (serviceTypes.isEmpty() || serviceTypes.values().stream().mapToDouble(Double::doubleValue).sum() == 0) {
            return FXCollections.observableArrayList(new PieChart.Data("Chưa có dữ liệu", 1));
        }
        
        double total = serviceTypes.values().stream().mapToDouble(Double::doubleValue).sum();
        return serviceTypes.entrySet().stream()
                .map(e -> new PieChart.Data(e.getKey() + " (" + String.format("%.0f%%", (e.getValue() / total) * 100) + ")", e.getValue()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), FXCollections::observableArrayList));
    }

    @FXML
    public void showDashboard() {
        loadDashboardContent();
        updateActiveButton(btnDashboard);
    }

    @FXML
    public void showHallManagement() {
        loadContent("/fxml/hall-management.fxml");
        updateActiveButton(btnHall);
    }

    @FXML
    public void showMenuManagement() {
        loadContent("/fxml/menu-management.fxml");
        updateActiveButton(btnMenu);
    }

    @FXML
    public void showBooking() {
        loadContent("/fxml/booking.fxml");
        updateActiveButton(btnBooking);
    }

    @FXML
    public void showCustomerManagement() {
        loadContent("/fxml/customer-management.fxml");
        updateActiveButton(btnCustomer);
    }

    @FXML
    public void showInvoice() {
        loadContent("/fxml/invoice.fxml");
        updateActiveButton(btnInvoice);
    }

    @FXML
    public void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc muốn đăng xuất?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Xác nhận đăng xuất");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                LoginController.logout();
                loadLogin();
            }
        });
    }

    private void loadLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentPane.getScene().getWindow();
            Scene scene = new Scene(root, 600, 700);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            showError("Lỗi khi tải Login: " + e.getMessage());
        }
    }

    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            contentPane.getChildren().clear();
            contentPane.getChildren().add(content);
        } catch (Exception e) {
            // Log stack trace để dễ debug khi load FXML thất bại
            e.printStackTrace();
            showError("Lỗi khi tải nội dung: " + e.getMessage());
        }
    }

    private void updateActiveButton(Button activeBtn) {
        // Reset all buttons
        btnDashboard.getStyleClass().remove("active");
        btnHall.getStyleClass().remove("active");
        btnMenu.getStyleClass().remove("active");
        btnBooking.getStyleClass().remove("active");
        btnCustomer.getStyleClass().remove("active");
        btnInvoice.getStyleClass().remove("active");
        
        // Set active button
        if (activeBtn != null) {
            activeBtn.getStyleClass().add("active");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
