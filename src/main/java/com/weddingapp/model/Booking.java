package com.weddingapp.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Booking {
    public enum PaymentStatus {
        PENDING("pending"),
        IN_PROGRESS("in_"),
        COMPLETED("completed");
        
        private final String displayName;
        
        PaymentStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static PaymentStatus fromString(String status) {
            if (status == null) return PENDING;
            try {
                return valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return PENDING;
            }
        }
        
        public String getColorStyle() {
            switch (this) {
                case IN_PROGRESS:
                    return "-fx-background-color: #fbbf24; -fx-text-fill: white;"; // Vàng
                case COMPLETED:
                    return "-fx-background-color: #10b981; -fx-text-fill: white;"; // Xanh lá
                case PENDING:
                default:
                    return ""; // Giữ màu mặc định
            }
        }
    }
    
    private int id;
    private String bookingCode; // Mã booking ngẫu nhiên 5 chữ số
    private Customer customer;
    private Hall hall;
    private LocalDate eventDate;
    private int tables;
    private double total;
    private double paidAmount; // Số tiền đã trả trước
    private PaymentStatus paymentStatus = PaymentStatus.PENDING; // Trạng thái thanh toán
    private String notes;
    private List<MenuItem> menuItems = new ArrayList<>();

    public Booking() {
    }

    public Booking(int id, Customer customer, Hall hall, LocalDate eventDate, int tables, double total, String notes) {
        this.id = id;
        this.customer = customer;
        this.hall = hall;
        this.eventDate = eventDate;
        this.tables = tables;
        this.total = total;
        this.paidAmount = 0.0;
        this.notes = notes;
    }

    public Booking(int id, Customer customer, Hall hall, LocalDate eventDate, int tables, double total, double paidAmount, String notes) {
        this.id = id;
        this.customer = customer;
        this.hall = hall;
        this.eventDate = eventDate;
        this.tables = tables;
        this.total = total;
        this.paidAmount = paidAmount;
        this.paymentStatus = PaymentStatus.PENDING;
        this.notes = notes;
    }
    
    public Booking(int id, Customer customer, Hall hall, LocalDate eventDate, int tables, double total, double paidAmount, PaymentStatus paymentStatus, String notes) {
        this.id = id;
        this.customer = customer;
        this.hall = hall;
        this.eventDate = eventDate;
        this.tables = tables;
        this.total = total;
        this.paidAmount = paidAmount;
        this.paymentStatus = paymentStatus != null ? paymentStatus : PaymentStatus.PENDING;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Hall getHall() {
        return hall;
    }

    public void setHall(Hall hall) {
        this.hall = hall;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public int getTables() {
        return tables;
    }

    public void setTables(int tables) {
        this.tables = tables;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<MenuItem> getMenuItems() {
        return menuItems;
    }

    public void setMenuItems(List<MenuItem> menuItems) {
        this.menuItems = menuItems;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus != null ? paymentStatus : PaymentStatus.PENDING;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }
}

