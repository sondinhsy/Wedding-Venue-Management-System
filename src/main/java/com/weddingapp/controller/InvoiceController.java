package com.weddingapp.controller;

import com.weddingapp.dao.BookingDAO;
import com.weddingapp.dao.CustomerDAO;
import com.weddingapp.model.Booking;
import com.weddingapp.util.CurrencyFormatter;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class InvoiceController {
    private final BookingDAO bookingDAO = new BookingDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ObservableList<Booking> bookings = FXCollections.observableArrayList();

    @FXML private Label totalRevenueLabel;
    @FXML private Label totalBookingsLabel;
    @FXML private Label totalCustomersLabel;
    @FXML private Label averageBookingLabel;
    @FXML private TableView<Booking> invoiceTable;
    @FXML private TableColumn<Booking, Number> colInvoiceId;
    @FXML private TableColumn<Booking, String> colInvoiceCustomer;
    @FXML private TableColumn<Booking, String> colInvoiceHall;
    @FXML private TableColumn<Booking, String> colInvoiceDate;
    @FXML private TableColumn<Booking, String> colInvoiceTotal;
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
        colInvoiceId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()));
        colInvoiceCustomer.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getCustomer().getName()));
        colInvoiceHall.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getHall().getName()));
        colInvoiceDate.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        colInvoiceTotal.setCellValueFactory(cell -> 
            new SimpleStringProperty(CurrencyFormatter.formatVND(cell.getValue().getTotal())));
        
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
                
                yPosition = writeText(contentStream, "Mã booking: #" + booking.getId(), leftColumn, yPosition, lineHeight);
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
}
