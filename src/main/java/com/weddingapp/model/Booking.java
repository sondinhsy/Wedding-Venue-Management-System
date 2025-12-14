package com.weddingapp.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Booking {
    private int id;
    private Customer customer;
    private Hall hall;
    private LocalDate eventDate;
    private int tables;
    private double total;
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
}

