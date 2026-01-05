package com.weddingapp.model;

public class ExtrasServiceItem {
    public enum Type {
        EXTRA_TRAY,  // Thêm bàn
        SERVICE      // Dịch vụ (MC, Music)
    }

    private int id;
    private int bookingId;
    private Type type;
    private String name;
    private String unit;
    private int quantity;
    private double unitPrice;
    private String notes;
    private String serviceCode; // 'MC' hoặc 'MUSIC' cho SERVICE

    public ExtrasServiceItem() {
    }

    public ExtrasServiceItem(int id, int bookingId, Type type, String name, String unit, 
                             int quantity, double unitPrice, String notes, String serviceCode) {
        this.id = id;
        this.bookingId = bookingId;
        this.type = type;
        this.name = name;
        this.unit = unit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.notes = notes;
        this.serviceCode = serviceCode;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public double getLineTotal() {
        return unitPrice * quantity;
    }
}

