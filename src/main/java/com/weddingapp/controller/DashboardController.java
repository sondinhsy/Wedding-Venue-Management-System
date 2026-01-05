package com.weddingapp.controller;

import com.weddingapp.dao.BookingDAO;
import com.weddingapp.dao.CustomerDAO;
import com.weddingapp.dao.ExtrasServiceDAO;
import com.weddingapp.model.Booking;
import com.weddingapp.model.ExtrasServiceItem;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {
    private final BookingDAO bookingDAO = new BookingDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ExtrasServiceDAO extrasServiceDAO = new ExtrasServiceDAO();
    private final ObservableList<Booking> bookings = FXCollections.observableArrayList();

    @FXML private StackPane contentPane;
    @FXML private Label userInfoLabel;
    @FXML private Button btnDashboard;
    @FXML private Button btnHall;
    @FXML private Button btnVoucher;
    @FXML private Button btnMenu;
    @FXML private Button btnBooking;
    @FXML private Button btnExtras;
    @FXML private Button btnCustomer;
    @FXML private Button btnInvoice;

    // Dashboard content fields
    @FXML private Label customerCountLabel;
    @FXML private Label bookingCountLabel;
    @FXML private Label revenueLabel;
    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, String> colBookingCode;
    @FXML private TableColumn<Booking, String> colBookingCustomer;
    @FXML private TableColumn<Booking, String> colBookingHall;
    @FXML private TableColumn<Booking, String> colBookingDate;
    @FXML private TableColumn<Booking, Number> colBookingTables;
    @FXML private TableColumn<Booking, String> colBookingTotal;
    @FXML private TableColumn<Booking, String> colBookingMenu;
    @FXML private TableColumn<Booking, String> colBookingPaymentStatus;
    @FXML private TableColumn<Booking, Void> colBookingActions;
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
            
            // Sau khi load(), các field @FXML đã được inject
            // Kiểm tra xem các field quan trọng đã được inject chưa
            if (bookingTable == null || colBookingCode == null) {
                System.err.println("Warning: Dashboard FXML fields not injected properly");
                // Thử load lại hoặc hiển thị lỗi
                showError("Lỗi khi tải Dashboard: Các thành phần không được khởi tạo đúng");
                return;
            }
            
            contentPane.getChildren().clear();
            contentPane.getChildren().add(content);
            
            setupDashboardTables();
            reloadDashboardData();
        } catch (Exception e) {
            // In addition to showing a user-friendly alert, log full stack trace to console
            System.err.println("Error loading dashboard content: " + e.getMessage());
            e.printStackTrace();
            showError("Lỗi khi tải Dashboard: " + e.getMessage());
        }
    }

    private void setupDashboardTables() {
        // Kiểm tra null để tránh NullPointerException
        if (colBookingCode == null || colBookingCustomer == null || colBookingHall == null || 
            colBookingDate == null || colBookingTables == null || colBookingTotal == null ||
            colBookingMenu == null || colBookingPaymentStatus == null || colBookingActions == null ||
            bookingTable == null) {
            System.err.println("Warning: Some dashboard table columns are null. FXML may not be loaded correctly.");
            return;
        }
        
        colBookingCode.setCellValueFactory(cell -> {
            String code = cell.getValue().getBookingCode();
            return new SimpleStringProperty(code != null ? code : "");
        });
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
        
        // Setup payment status column with ComboBox
        colBookingPaymentStatus.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getPaymentStatus().getDisplayName()));
        colBookingPaymentStatus.setCellFactory(column -> new TableCell<>() {
            private final ComboBox<Booking.PaymentStatus> comboBox = new ComboBox<>();
            private Booking currentBooking;
            private boolean updating = false;
            
            {
                comboBox.getItems().addAll(Booking.PaymentStatus.values());
                comboBox.setStyle("-fx-pref-width: 180px;");
                comboBox.setEditable(false);
                
                // Sử dụng listener để tránh event loop
                comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                    // Bỏ qua nếu đang update từ code
                    if (updating) {
                        return;
                    }
                    
                    if (newVal != null && currentBooking != null && oldVal != null && oldVal != newVal) {
                        // Tính toán lại paidAmount dựa trên trạng thái mới
                        double billTotal = currentBooking.getTotal();
                        double newPaidAmount = currentBooking.getPaidAmount();
                        
                        if (newVal == Booking.PaymentStatus.PENDING) {
                            // Chuyển về pending: reset về 0
                            newPaidAmount = 0.0;
                        } else if (newVal == Booking.PaymentStatus.IN_PROGRESS) {
                            // Chuyển sang in_: tính 20% billTotal
                            newPaidAmount = billTotal * 0.20;
                        } else if (newVal == Booking.PaymentStatus.COMPLETED) {
                            // Chuyển sang completed: giữ nguyên paidAmount hiện tại
                            // Nếu chưa có thì set = 20%
                            if (newPaidAmount == 0.0) {
                                newPaidAmount = billTotal * 0.20;
                            }
                        }
                        
                        currentBooking.setPaymentStatus(newVal);
                        currentBooking.setPaidAmount(newPaidAmount);
                        
                        try {
                            bookingDAO.updatePaymentStatusAndAmount(currentBooking.getId(), newVal, newPaidAmount);
                            // Reload booking từ DB để có dữ liệu mới nhất
                            Booking updatedBooking = bookingDAO.findById(currentBooking.getId());
                            if (updatedBooking != null) {
                                currentBooking.setPaidAmount(updatedBooking.getPaidAmount());
                            }
                            // Reload data để cập nhật UI
                            reloadDashboardData();
                        } catch (Exception e) {
                            // Nếu có lỗi, revert lại giá trị cũ
                            updating = true;
                            comboBox.setValue(oldVal);
                            currentBooking.setPaymentStatus(oldVal);
                            updating = false;
                        }
                    }
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    currentBooking = null;
                } else {
                    Booking booking = getTableRow().getItem();
                    currentBooking = booking;
                    
                    // Tạm thời disable listener khi setValue
                    updating = true;
                    comboBox.setValue(booking.getPaymentStatus());
                    // Áp dụng màu theo status
                    String colorStyle = booking.getPaymentStatus().getColorStyle();
                    if (!colorStyle.isEmpty()) {
                        comboBox.setStyle("-fx-pref-width: 140px; " + colorStyle);
                    } else {
                        comboBox.setStyle("-fx-pref-width: 140px;");
                    }
                    updating = false;
                    
                    setGraphic(comboBox);
                }
            }
        });
        
        // Setup action column with buttons
        colBookingActions.setCellFactory(param -> new TableCell<>() {
            private final Button viewDetailsBtn = new Button("👁️ Xem...");
            private final Button editBtn = new Button("✏️ Sửa");
            private final Button deleteBtn = new Button("🗑️ Xóa");
            private final HBox buttonBox = new HBox(6, viewDetailsBtn, editBtn, deleteBtn);

            {
                viewDetailsBtn.setStyle("-fx-font-size: 10px; -fx-pref-width: 70px;");
                editBtn.setStyle("-fx-font-size: 11px; -fx-pref-width: 70px; -fx-background-color: #fbbf24; -fx-text-fill: white;");
                deleteBtn.setStyle("-fx-font-size: 11px; -fx-pref-width: 70px; -fx-background-color: #ef4444; -fx-text-fill: white;");
                buttonBox.setStyle("-fx-alignment: CENTER;");
                
                viewDetailsBtn.setOnAction(e -> {
                    Booking booking = getTableView().getItems().get(getIndex());
                    if (booking != null) {
                        showBookingDetails(booking);
                    }
                });
                
                editBtn.setOnAction(e -> {
                    Booking booking = getTableView().getItems().get(getIndex());
                    if (booking != null) {
                        editBooking(booking);
                    }
                });
                
                deleteBtn.setOnAction(e -> {
                    Booking booking = getTableView().getItems().get(getIndex());
                    if (booking != null) {
                        deleteBooking(booking);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonBox);
                }
            }
        });
        
        bookingTable.setItems(bookings);
    }

    private void reloadDashboardData() {
        try {
            if (bookings == null) {
                System.err.println("Error: bookings ObservableList is null");
                return;
            }
            bookings.setAll(bookingDAO.findAll());
            updateTotals();
            updateCharts();
        } catch (Exception e) {
            System.err.println("Error reloading dashboard data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateTotals() {
        try {
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
        } catch (Exception e) {
            System.err.println("Error updating totals: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateCharts() {
        try {
            if (menuRevenueChart != null) {
                menuRevenueChart.setData(buildPieDataByMenu());
            }
            if (serviceTypeChart != null) {
                serviceTypeChart.setData(buildDonutDataByMonth());
                // Ensure donut effect
                if (donutChartPane != null && donutChartPane.getChildren().size() > 1) {
                    Circle centerCircle = (Circle) donutChartPane.getChildren().get(1);
                    centerCircle.setRadius(80);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating charts: " + e.getMessage());
            e.printStackTrace();
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

    /**
     * Doanh thu theo tháng (YearMonth) dựa trên ngày tổ chức của booking.
     * Hiển thị tối đa 6 tháng gần nhất.
     */
    private ObservableList<PieChart.Data> buildDonutDataByMonth() {
        if (bookings.isEmpty()) {
            return FXCollections.observableArrayList(new PieChart.Data("Chưa có dữ liệu", 1));
        }

        Map<YearMonth, Double> totalsByMonth = new HashMap<>();
        for (Booking b : bookings) {
            if (b.getEventDate() == null) continue;
            YearMonth ym = YearMonth.from(b.getEventDate());
            totalsByMonth.merge(ym, b.getTotal(), Double::sum);
        }

        if (totalsByMonth.isEmpty()) {
            return FXCollections.observableArrayList(new PieChart.Data("Chưa có dữ liệu", 1));
        }

        // Sắp xếp theo thời gian, lấy 6 tháng gần nhất
        Map<YearMonth, Double> sorted = totalsByMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // tăng dần theo thời gian
                .skip(Math.max(0, totalsByMonth.size() - 6)) // chỉ giữ lại 6 tháng gần nhất
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Double::sum,
                        LinkedHashMap::new
                ));

        double grandTotal = sorted.values().stream().mapToDouble(Double::doubleValue).sum();
        return sorted.entrySet().stream()
                .map(e -> {
                    YearMonth ym = e.getKey();
                    double value = e.getValue();
                    String label = String.format("Tháng %02d/%d (%.0f%%)",
                            ym.getMonthValue(),
                            ym.getYear(),
                            (value / grandTotal) * 100);
                    return new PieChart.Data(label, value);
                })
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
    public void showVoucherManagement() {
        loadContent("/fxml/voucher-management.fxml");
        updateActiveButton(btnVoucher);
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
    public void showExtrasServices() {
        loadContent("/fxml/extras-services.fxml");
        updateActiveButton(btnExtras);
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
            System.err.println("Error loading FXML: " + fxmlPath);
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (e.getCause() != null) {
                errorMsg += "\n" + e.getCause().getMessage();
            }
            showError("Lỗi khi tải nội dung:\n" + fxmlPath + "\n" + errorMsg);
        }
    }

    private void updateActiveButton(Button activeBtn) {
        // Reset all buttons - kiểm tra null để tránh lỗi
        if (btnDashboard != null) {
            btnDashboard.getStyleClass().remove("active");
        }
        if (btnHall != null) {
            btnHall.getStyleClass().remove("active");
        }
        if (btnVoucher != null) {
            btnVoucher.getStyleClass().remove("active");
        }
        if (btnMenu != null) {
            btnMenu.getStyleClass().remove("active");
        }
        if (btnBooking != null) {
            btnBooking.getStyleClass().remove("active");
        }
        if (btnExtras != null) {
            btnExtras.getStyleClass().remove("active");
        }
        if (btnCustomer != null) {
            btnCustomer.getStyleClass().remove("active");
        }
        if (btnInvoice != null) {
            btnInvoice.getStyleClass().remove("active");
        }
        
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

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle("Thành công");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showBookingDetails(Booking booking) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết Booking #" + booking.getId());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(800, 600);

        VBox content = new VBox(16);
        content.setStyle("-fx-padding: 20;");

        double billTotal = booking.getTotal();
        
        // Booking Info Section
        VBox bookingInfo = new VBox(8);
        Label bookingHeader = new Label("📋 Thông tin Booking");
        bookingHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
        
        bookingInfo.getChildren().add(bookingHeader);
        bookingInfo.getChildren().add(new Label("Mã booking: #" + booking.getId()));
        bookingInfo.getChildren().add(new Label("Khách hàng: " + booking.getCustomer().getName()));
        if (booking.getCustomer().getPhone() != null) {
            bookingInfo.getChildren().add(new Label("Điện thoại: " + booking.getCustomer().getPhone()));
        }
        bookingInfo.getChildren().add(new Label("Sảnh: " + booking.getHall().getName()));
        bookingInfo.getChildren().add(new Label("Ngày tổ chức: " + booking.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        bookingInfo.getChildren().add(new Label("Số bàn: " + booking.getTables()));
        bookingInfo.getChildren().add(new Label("Tổng tiền đơn gốc (trước phát sinh): " + CurrencyFormatter.formatVND(billTotal)));
        
        // Tính toán theo status
        double actualPaidAmount = 0.0;
        double depositRequired = billTotal * 0.20;
        
        if (booking.getPaymentStatus() == Booking.PaymentStatus.PENDING) {
            // pending: tiền ứng trước = 0
            actualPaidAmount = 0.0;
            bookingInfo.getChildren().add(new Label("Tiền cần ứng trước: " + CurrencyFormatter.formatVND(0.0)));
        } else if (booking.getPaymentStatus() == Booking.PaymentStatus.IN_PROGRESS) {
            // in_: tiền ứng trước = 20% billTotal
            actualPaidAmount = depositRequired;
            bookingInfo.getChildren().add(new Label("Tiền ứng trước: " + CurrencyFormatter.formatVND(actualPaidAmount)));
            double remainingBeforeExtras = Math.max(0, billTotal - actualPaidAmount);
            bookingInfo.getChildren().add(new Label("Số tiền còn lại (trước phát sinh): " + CurrencyFormatter.formatVND(remainingBeforeExtras)));
        } else if (booking.getPaymentStatus() == Booking.PaymentStatus.COMPLETED) {
            // completed: giữ nguyên paidAmount (nếu có), nếu không thì = 20%
            actualPaidAmount = booking.getPaidAmount() > 0 ? booking.getPaidAmount() : depositRequired;
            bookingInfo.getChildren().add(new Label("Tiền ứng trước: " + CurrencyFormatter.formatVND(actualPaidAmount)));
            Label completedLabel = new Label("Đã thanh toán đủ");
            completedLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #10b981;");
            bookingInfo.getChildren().add(completedLabel);
        }

        content.getChildren().add(bookingInfo);

        // Extras Services Section
        List<ExtrasServiceItem> extrasItems = extrasServiceDAO.findByBookingId(booking.getId());
        VBox extrasSection = new VBox(8);
        Label extrasHeader = new Label("💰 Phát sinh & Dịch vụ");
        extrasHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
        extrasSection.getChildren().add(extrasHeader);

        if (extrasItems.isEmpty()) {
            extrasSection.getChildren().add(new Label("Không có phát sinh/dịch vụ"));
        } else {
            TableView<ExtrasServiceItem> extrasTable = new TableView<>();
            extrasTable.setItems(FXCollections.observableArrayList(extrasItems));
            extrasTable.setPrefHeight(200);

            TableColumn<ExtrasServiceItem, String> typeCol = new TableColumn<>("Loại");
            typeCol.setCellValueFactory(item -> {
                ExtrasServiceItem i = item.getValue();
                return new SimpleStringProperty(i.getType() == ExtrasServiceItem.Type.EXTRA_TRAY ? "Thêm bàn" : "Dịch vụ");
            });

            TableColumn<ExtrasServiceItem, String> nameCol = new TableColumn<>("Tên");
            nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

            TableColumn<ExtrasServiceItem, Integer> quantityCol = new TableColumn<>("Số lượng");
            quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

            TableColumn<ExtrasServiceItem, String> unitPriceCol = new TableColumn<>("Đơn giá");
            unitPriceCol.setCellValueFactory(item -> 
                new SimpleStringProperty(CurrencyFormatter.formatVND(item.getValue().getUnitPrice())));

            TableColumn<ExtrasServiceItem, String> totalCol = new TableColumn<>("Thành tiền");
            totalCol.setCellValueFactory(item -> 
                new SimpleStringProperty(CurrencyFormatter.formatVND(item.getValue().getLineTotal())));

            extrasTable.getColumns().add(typeCol);
            extrasTable.getColumns().add(nameCol);
            extrasTable.getColumns().add(quantityCol);
            extrasTable.getColumns().add(unitPriceCol);
            extrasTable.getColumns().add(totalCol);
            extrasSection.getChildren().add(extrasTable);

            // Thêm chữ "Tổng kết" nhỏ ở dưới bảng
            Label summaryLabel = new Label("Tổng kết");
            summaryLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #64748b; -fx-padding: 8 0 4 0;");
            extrasSection.getChildren().add(summaryLabel);

            double extrasTotal = extrasItems.stream().mapToDouble(ExtrasServiceItem::getLineTotal).sum();
            Label extrasTotalLabel = new Label("Tổng phát sinh: " + CurrencyFormatter.formatVND(extrasTotal));
            extrasTotalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            extrasSection.getChildren().add(extrasTotalLabel);

            // Tính tổng sau phát sinh
            double totalAfterExtras = billTotal + extrasTotal;
            Label totalAfterExtrasLabel = new Label("Tổng tiền sau phát sinh: " + CurrencyFormatter.formatVND(totalAfterExtras));
            totalAfterExtrasLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            extrasSection.getChildren().add(totalAfterExtrasLabel);
            
            // Hiển thị số tiền đã ứng theo status
            if (booking.getPaymentStatus() != Booking.PaymentStatus.PENDING) {
                Label paidAmountLabel = new Label("Số tiền đã ứng: " + CurrencyFormatter.formatVND(actualPaidAmount));
                paidAmountLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #059669;");
                extrasSection.getChildren().add(paidAmountLabel);
            }
            
            // Tính số tiền còn lại sau phát sinh
            if (booking.getPaymentStatus() == Booking.PaymentStatus.COMPLETED) {
                Label completedLabel = new Label("Đã thanh toán đủ");
                completedLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #10b981;");
                extrasSection.getChildren().add(completedLabel);
            } else {
                double remainingAfterExtras = Math.max(0, totalAfterExtras - actualPaidAmount);
                Label remainingAfterExtrasLabel = new Label("Số tiền còn lại (sau phát sinh): " + CurrencyFormatter.formatVND(remainingAfterExtras));
                remainingAfterExtrasLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #dc2626;");
                extrasSection.getChildren().add(remainingAfterExtrasLabel);
            }
        }

        content.getChildren().add(extrasSection);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void editBooking(Booking booking) {
        try {
            // Navigate to booking page
            showBooking();
            
            // Load booking data into form
            // We need to get the BookingController instance
            // Since we're loading the booking.fxml, we need to access it after loading
            // For now, let's open it in a new window similar to InvoiceController
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/booking.fxml"));
            Parent root = loader.load();
            BookingController controller = loader.getController();
            
            // Load booking data into form
            controller.loadBookingForEdit(booking);
            
            // Show in new window
            Stage stage = new Stage();
            stage.setTitle("Sửa Booking #" + booking.getId());
            stage.setScene(new Scene(root, 1000, 700));
            stage.show();
        } catch (Exception e) {
            showError("Lỗi khi mở form sửa booking: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteBooking(Booking booking) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận xóa");
        confirmAlert.setHeaderText("Xóa booking #" + booking.getId());
        confirmAlert.setContentText("Bạn có chắc chắn muốn xóa booking này?\nKhách hàng: " + booking.getCustomer().getName() + "\nNgày tổ chức: " + booking.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    bookingDAO.delete(booking.getId());
                    reloadDashboardData();
                    showSuccess("Đã xóa booking #" + booking.getId());
                } catch (Exception e) {
                    showError("Lỗi khi xóa booking: " + e.getMessage());
                }
            }
        });
    }
}
