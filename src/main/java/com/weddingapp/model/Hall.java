package com.weddingapp.model;

public class Hall {
    private int id;
    private String name;
    private int capacity;
    private double pricePerTable;

    public Hall() {
    }

    public Hall(int id, String name, int capacity, double pricePerTable) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.pricePerTable = pricePerTable;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getPricePerTable() {
        return pricePerTable;
    }

    public void setPricePerTable(double pricePerTable) {
        this.pricePerTable = pricePerTable;
    }

    @Override
    public String toString() {
        return name;
    }
}

