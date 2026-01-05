package com.weddingapp.controller;

import com.weddingapp.dao.BookingDAO;
import com.weddingapp.dao.CustomerDAO;
import com.weddingapp.dao.ExtrasServiceDAO;
import com.weddingapp.model.Booking;
import com.weddingapp.model.ExtrasServiceItem;
import com.weddingapp.util.CurrencyFormatter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvoiceController {
    private final BookingDAO bookingDAO = new BookingDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ExtrasServiceDAO extrasServiceDAO = new ExtrasServiceDAO();
    private final ObservableList<Booking> bookings = FXCollections.observableArrayList();

    @FXML private Label totalRevenueLabel;
    @FXML private Label totalBookingsLabel;
    @FXML private Label totalCustomersLabel;
    @FXML private Label averageBookingLabel;
    @FXML private TableView<Booking> invoiceTable;
    @FXML private TableColumn<Booking, String> colInvoiceId;
    @FXML private TableColumn<Booking, String> colInvoiceCustomer;
    @FXML private TableColumn<Booking, String> colInvoiceHall;
    @FXML private TableColumn<Booking, String> colInvoiceDate;
    @FXML private TableColumn<Booking, String> colInvoiceTotal;
    @FXML private TableColumn<Booking, String> colInvoicePaymentStatus;
    @FXML private TableColumn<Booking, Void> colInvoiceActions;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button filterButton;
    @FXML private Button exportPdfButton;
    @FXML private Button exportAllPdfButton;

    @FXML
    public void initialize() {
        setupTable();
        loadData();
        updateStatistics();
        
        // Set default date range (last 30 days)
        toDatePicker.setValue(LocalDate.now());
        fromDatePicker.setValue(LocalDate.now().minusDays(30));
    }

    private void setupTable() {
        colInvoiceId.setCellValueFactory(cell -> {
            String code = cell.getValue().getBookingCode();
            return new SimpleStringProperty(code != null ? code : String.valueOf(cell.getValue().getId()));
        });
        colInvoiceCustomer.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getCustomer().getName()));
        colInvoiceHall.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getHall().getName()));
        colInvoiceDate.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        colInvoiceTotal.setCellValueFactory(cell -> 
            new SimpleStringProperty(CurrencyFormatter.formatVND(cell.getValue().getTotal())));
        
        // Setup payment status column with ComboBox
        colInvoicePaymentStatus.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getPaymentStatus().getDisplayName()));
        colInvoicePaymentStatus.setCellFactory(column -> new TableCell<>() {
            private final ComboBox<Booking.PaymentStatus> comboBox = new ComboBox<>();
            private Booking currentBooking;
            private boolean updating = false;
            
            {
                comboBox.getItems().addAll(Booking.PaymentStatus.values());
                comboBox.setStyle("-fx-pref-width: 140px;");
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
                            loadData();
                            updateStatistics();
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
        colInvoiceActions.setCellFactory(param -> new TableCell<>() {
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
        
        invoiceTable.setItems(bookings);
    }

    private void loadData() {
        bookings.setAll(bookingDAO.findAll());
    }

    @FXML
    public void handleFilter() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        
        if (fromDate == null || toDate == null) {
            showError("Vui lòng chọn cả ngày bắt đầu và ngày kết thúc");
            return;
        }
        
        if (fromDate.isAfter(toDate)) {
            showError("Ngày bắt đầu phải trước ngày kết thúc");
            return;
        }
        
        ObservableList<Booking> filtered = FXCollections.observableArrayList();
        for (Booking booking : bookingDAO.findAll()) {
            LocalDate eventDate = booking.getEventDate();
            if (!eventDate.isBefore(fromDate) && !eventDate.isAfter(toDate)) {
                filtered.add(booking);
            }
        }
        bookings.setAll(filtered);
        updateStatistics();
    }

    @FXML
    public void handleResetFilter() {
        fromDatePicker.setValue(LocalDate.now().minusDays(30));
        toDatePicker.setValue(LocalDate.now());
        loadData();
        updateStatistics();
    }

    @FXML
    public void handleExportPdf() {
        Booking selected = invoiceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Vui lòng chọn một booking để xuất hóa đơn");
            return;
        }
        
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Lưu hóa đơn PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("Invoice_" + selected.getId() + ".pdf");
        File file = chooser.showSaveDialog(invoiceTable.getScene().getWindow());
        
        if (file != null) {
            try {
                exportInvoiceToPdf(selected, file);
                showSuccess("Đã xuất hóa đơn: " + file.getName());
            } catch (IOException e) {
                showError("Lỗi khi xuất PDF: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleExportAllPdf() {
        if (bookings.isEmpty()) {
            showError("Không có dữ liệu để xuất");
            return;
        }
        
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Lưu báo cáo thống kê PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("Report_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf");
        File file = chooser.showSaveDialog(invoiceTable.getScene().getWindow());
        
        if (file != null) {
            try {
                exportStatisticsToPdf(file);
                showSuccess("Đã xuất báo cáo: " + file.getName());
            } catch (IOException e) {
                showError("Lỗi khi xuất PDF: " + e.getMessage());
            }
        }
    }

    private void exportInvoiceToPdf(Booking booking, File file) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float yPosition = 750;
                float margin = 50;
                float lineHeight = 20;
                
                // Header
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("HÓA ĐƠN ĐẶT CHỖ");
                contentStream.endText();
                
                yPosition -= 40;
                
                // Invoice details
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                float leftColumn = margin;
                
                String bookingCode = booking.getBookingCode() != null ? booking.getBookingCode() : String.valueOf(booking.getId());
                yPosition = writeText(contentStream, "Mã booking: " + bookingCode, leftColumn, yPosition, lineHeight);
                yPosition = writeText(contentStream, "Ngày đặt: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), leftColumn, yPosition, lineHeight);
                yPosition -= 10;
                
                // Customer info
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                yPosition = writeText(contentStream, "Thông tin khách hàng:", leftColumn, yPosition, lineHeight);
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                yPosition = writeText(contentStream, "Tên: " + booking.getCustomer().getName(), leftColumn + 20, yPosition, lineHeight);
                if (booking.getCustomer().getPhone() != null) {
                    yPosition = writeText(contentStream, "Điện thoại: " + booking.getCustomer().getPhone(), leftColumn + 20, yPosition, lineHeight);
                }
                if (booking.getCustomer().getEmail() != null) {
                    yPosition = writeText(contentStream, "Email: " + booking.getCustomer().getEmail(), leftColumn + 20, yPosition, lineHeight);
                }
                yPosition -= 10;
                
                // Booking details
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                yPosition = writeText(contentStream, "Chi tiết đặt chỗ:", leftColumn, yPosition, lineHeight);
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                yPosition = writeText(contentStream, "Sảnh: " + booking.getHall().getName(), leftColumn + 20, yPosition, lineHeight);
                yPosition = writeText(contentStream, "Ngày tổ chức: " + booking.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), leftColumn + 20, yPosition, lineHeight);
                yPosition = writeText(contentStream, "Số bàn: " + booking.getTables(), leftColumn + 20, yPosition, lineHeight);
                yPosition -= 10;
                
                // Menu items
                if (!booking.getMenuItems().isEmpty()) {
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    yPosition = writeText(contentStream, "Thực đơn:", leftColumn, yPosition, lineHeight);
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    for (var item : booking.getMenuItems()) {
                        yPosition = writeText(contentStream, "- " + item.getTitle() + " (" + CurrencyFormatter.formatVND(item.getPrice()) + ")", leftColumn + 20, yPosition, lineHeight);
                    }
                    yPosition -= 10;
                }
                
                // Total
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                yPosition = writeText(contentStream, "TỔNG TIỀN: " + CurrencyFormatter.formatVND(booking.getTotal()), leftColumn, yPosition, lineHeight);
                
                if (booking.getNotes() != null && !booking.getNotes().trim().isEmpty()) {
                    yPosition -= 20;
                    contentStream.setFont(PDType1Font.HELVETICA, 10);
                    yPosition = writeText(contentStream, "Ghi chú: " + booking.getNotes(), leftColumn, yPosition, lineHeight);
                }
            }
            
            document.save(file);
        }
    }

    private void exportStatisticsToPdf(File file) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            
            try {
                float yPosition = 750;
                float margin = 50;
                float lineHeight = 20;
                
                // Header
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("BÁO CÁO THỐNG KÊ");
                contentStream.endText();
                
                yPosition -= 30;
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                yPosition = writeText(contentStream, "Ngày xuất: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), margin, yPosition, lineHeight);
                yPosition -= 20;
                
                // Statistics
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                yPosition = writeText(contentStream, "Tổng quan:", margin, yPosition, lineHeight);
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                
                double totalRevenue = bookings.stream().mapToDouble(Booking::getTotal).sum();
                int totalBookings = bookings.size();
                double average = totalBookings > 0 ? totalRevenue / totalBookings : 0;
                
                yPosition = writeText(contentStream, "Tổng doanh thu: " + CurrencyFormatter.formatVND(totalRevenue), margin + 20, yPosition, lineHeight);
                yPosition = writeText(contentStream, "Tổng số booking: " + totalBookings, margin + 20, yPosition, lineHeight);
                yPosition = writeText(contentStream, "Trung bình/booking: " + CurrencyFormatter.formatVND(average), margin + 20, yPosition, lineHeight);
                yPosition -= 20;
                
                // Revenue by hall
                Map<String, Double> hallRevenue = new HashMap<>();
                for (Booking b : bookings) {
                    hallRevenue.merge(b.getHall().getName(), b.getTotal(), Double::sum);
                }
                
                if (!hallRevenue.isEmpty()) {
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    yPosition = writeText(contentStream, "Doanh thu theo sảnh:", margin, yPosition, lineHeight);
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    for (var entry : hallRevenue.entrySet()) {
                        yPosition = writeText(contentStream, entry.getKey() + ": " + CurrencyFormatter.formatVND(entry.getValue()), margin + 20, yPosition, lineHeight);
                    }
                    yPosition -= 20;
                }
                
                // Booking list
                if (yPosition < 200) {
                    contentStream.close();
                    page = new PDPage();
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = 750;
                }
                
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                yPosition = writeText(contentStream, "Danh sách booking:", margin, yPosition, lineHeight);
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                
                for (Booking booking : bookings) {
                    if (yPosition < 50) {
                        contentStream.close();
                        page = new PDPage();
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = 750;
                    }
                    String line = String.format("#%d - %s - %s - %s - %s",
                        booking.getId(),
                        booking.getCustomer().getName(),
                        booking.getHall().getName(),
                        booking.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        CurrencyFormatter.formatVND(booking.getTotal()));
                    yPosition = writeText(contentStream, line, margin, yPosition, lineHeight - 5);
                }
            } finally {
                contentStream.close();
            }
            
            document.save(file);
        }
    }

    private float writeText(PDPageContentStream contentStream, String text, float x, float y, float lineHeight) throws IOException {
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
        return y - lineHeight;
    }

    private void updateStatistics() {
        double totalRevenue = bookings.stream().mapToDouble(Booking::getTotal).sum();
        int totalBookings = bookings.size();
        int totalCustomers = customerDAO.findAll().size();
        double average = totalBookings > 0 ? totalRevenue / totalBookings : 0;
        
        totalRevenueLabel.setText(CurrencyFormatter.formatVND(totalRevenue));
        totalBookingsLabel.setText(String.valueOf(totalBookings));
        totalCustomersLabel.setText(String.valueOf(totalCustomers));
        averageBookingLabel.setText(CurrencyFormatter.formatVND(average));
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle("Thành công");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Lỗi");
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

        // Booking Info Section
        VBox bookingInfo = new VBox(8);
        Label bookingHeader = new Label("📋 Thông tin Booking");
        bookingHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
        
        bookingInfo.getChildren().add(bookingHeader);
        String bookingCode = booking.getBookingCode() != null ? booking.getBookingCode() : String.valueOf(booking.getId());
        bookingInfo.getChildren().add(new Label("Mã booking: " + bookingCode));
        bookingInfo.getChildren().add(new Label("Khách hàng: " + booking.getCustomer().getName()));
        if (booking.getCustomer().getPhone() != null) {
            bookingInfo.getChildren().add(new Label("Điện thoại: " + booking.getCustomer().getPhone()));
        }
        bookingInfo.getChildren().add(new Label("Sảnh: " + booking.getHall().getName()));
        bookingInfo.getChildren().add(new Label("Ngày tổ chức: " + booking.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        bookingInfo.getChildren().add(new Label("Số bàn: " + booking.getTables()));
        double billTotal = booking.getTotal();
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
            // Load booking page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/booking.fxml"));
            Parent root = loader.load();
            BookingController controller = loader.getController();
            
            // Load booking data into form
            controller.loadBookingForEdit(booking);
            
            // Show in new window or navigate
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
                    loadData();
                    updateStatistics();
                    showSuccess("Đã xóa booking #" + booking.getId());
                } catch (Exception e) {
                    showError("Lỗi khi xóa booking: " + e.getMessage());
                }
            }
        });
    }
}
