package com.weddingapp.model;

/**
 * Đại diện cho một món lẻ nằm trong một combo.
 * comboId tham chiếu tới MenuItem có category = 'combo'.
 */
public class ComboItem {
    private int comboId;
    private MenuItem item;
    private int quantity;

    public ComboItem(int comboId, MenuItem item, int quantity) {
        this.comboId = comboId;
        this.item = item;
        this.quantity = quantity;
    }

    public int getComboId() {
        return comboId;
    }

    public MenuItem getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }
}


