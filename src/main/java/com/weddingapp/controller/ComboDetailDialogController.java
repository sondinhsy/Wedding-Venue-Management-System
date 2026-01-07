package com.weddingapp.controller;

import com.weddingapp.dao.ComboItemDAO;
import com.weddingapp.model.ComboItem;
import com.weddingapp.model.MenuItem;
import javafx.fxml.FXML;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ComboDetailDialogController {
    @FXML private Label comboTitleLabel;
    @FXML private Label comboPriceLabel;
    @FXML private VBox itemsVBox;
    @FXML private DialogPane dialogPane;

    private MenuItem combo;
    private final ComboItemDAO comboItemDAO = new ComboItemDAO();

    public void setCombo(MenuItem combo) {
        this.combo = combo;
        loadComboDetails();
    }

    private void loadComboDetails() {
        if (combo == null) return;

        // Tên combo + giá combo (giá cố định, hiển thị $)
        comboTitleLabel.setText(combo.getTitle());
        comboPriceLabel.setText(String.format("Price: $%.0f / table", combo.getPrice()));

        // Load combo items từ DB
        List<ComboItem> comboItems = comboItemDAO.findByComboId(combo.getId());

        // Group by subCategory
        Map<String, List<ComboItem>> grouped = comboItems.stream()
                .collect(Collectors.groupingBy(
                        ci -> ci.getItem().getSubCategory() != null ? ci.getItem().getSubCategory() : "KHÁC"
                ));

        itemsVBox.getChildren().clear();

        // 1) Khai vị
        List<ComboItem> appetizers = grouped.get("KHAI_VI");
        if (appetizers != null && !appetizers.isEmpty()) {
            addCategorySection("🥗 Khai vị", appetizers);
        }

        // 2) Món chính
        List<ComboItem> mains = grouped.get("MON_CHINH");
        if (mains != null && !mains.isEmpty()) {
            addCategorySection("🍖 Món chính", mains);
        }

        // 3) Món phụ
        List<ComboItem> sides = grouped.get("MON_PHU");
        if (sides != null && !sides.isEmpty()) {
            addCategorySection("🍚 Món phụ (xôi / canh / rau)", sides);
        }

        // 4) Đồ uống: luôn hiển thị theo rule fixed
        addDrinksSection();

        // Các nhóm khác (nếu có)
        List<ComboItem> others = new ArrayList<>();
        grouped.forEach((k, v) -> {
            if (!"KHAI_VI".equals(k) && !"MON_CHINH".equals(k) && !"MON_PHU".equals(k) && !"DO_UONG".equals(k)) {
                others.addAll(v);
            }
        });
        if (!others.isEmpty()) {
            addCategorySection("📋 Khác", others);
        }
    }

    private void addCategorySection(String categoryLabel, List<ComboItem> items) {
        VBox categoryBox = new VBox(8);
        categoryBox.setStyle("-fx-padding: 8 0;");

        Label categoryTitle = new Label(categoryLabel);
        categoryTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
        categoryBox.getChildren().add(categoryTitle);

        for (ComboItem comboItem : items) {
            MenuItem item = comboItem.getItem();
            HBox itemRow = new HBox(8);
            itemRow.setStyle("-fx-padding: 4 0 4 16;");

            Label itemName = new Label(item.getTitle());
            itemName.setStyle("-fx-font-size: 13px;");

            String catText = resolveCategoryLabel(item.getSubCategory());
            Label categoryTag = new Label(catText);
            categoryTag.setStyle("-fx-font-size: 11px; -fx-text-fill: #4b5563;");

            Label quantityLabel = new Label();
            if (comboItem.getQuantity() > 1) {
                quantityLabel.setText("(x" + comboItem.getQuantity() + ")");
                quantityLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            }

            Label includedLabel = new Label("Included");
            includedLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #059669; -fx-font-style: italic;");

            HBox.setHgrow(itemName, javafx.scene.layout.Priority.ALWAYS);
            itemRow.getChildren().addAll(itemName, categoryTag, quantityLabel, includedLabel);
            categoryBox.getChildren().add(itemRow);
        }

        itemsVBox.getChildren().add(categoryBox);
    }

    private void addDrinksSection() {
        VBox categoryBox = new VBox(8);
        categoryBox.setStyle("-fx-padding: 8 0;");

        Label categoryTitle = new Label("🥤 Đồ uống");
        categoryTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
        categoryBox.getChildren().add(categoryTitle);

        String titleLower = combo.getTitle() != null ? combo.getTitle().toLowerCase() : "";

        if (titleLower.contains("vip")) {
            addDrinkRow(categoryBox, "Tiger/Heineken");
            addDrinkRow(categoryBox, "Coca/Pepsi/Schweppes");
            addDrinkRow(categoryBox, "Vodka / rượu nếp ngon");
        } else if (titleLower.contains("premium")) {
            addDrinkRow(categoryBox, "Heineken/Budweiser");
            addDrinkRow(categoryBox, "Nước ngọt có gas");
            addDrinkRow(categoryBox, "Nước trái cây đóng chai");
            addDrinkRow(categoryBox, "Vang đỏ / vang trắng");
        } else {
            // Combo THƯỜNG
            addDrinkRow(categoryBox, "Bia Sài Gòn");
            addDrinkRow(categoryBox, "Coca/Pepsi");
            addDrinkRow(categoryBox, "Rượu trắng");
        }

        itemsVBox.getChildren().add(categoryBox);
    }

    private void addDrinkRow(VBox categoryBox, String name) {
        HBox itemRow = new HBox(8);
        itemRow.setStyle("-fx-padding: 4 0 4 16;");

        Label itemName = new Label(name);
        itemName.setStyle("-fx-font-size: 13px;");

        Label categoryTag = new Label("Đồ uống");
        categoryTag.setStyle("-fx-font-size: 11px; -fx-text-fill: #4b5563;");

        Label includedLabel = new Label("Included");
        includedLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #059669; -fx-font-style: italic;");

        HBox.setHgrow(itemName, javafx.scene.layout.Priority.ALWAYS);
        itemRow.getChildren().addAll(itemName, categoryTag, includedLabel);
        categoryBox.getChildren().add(itemRow);
    }

    private String resolveCategoryLabel(String subCategory) {
        if (subCategory == null) return "";
        switch (subCategory) {
            case "KHAI_VI":
                return "Khai vị";
            case "MON_CHINH":
                return "Món chính";
            case "MON_PHU":
                return "Món phụ";
            case "DO_UONG":
                return "Đồ uống";
            default:
                return subCategory;
        }
    }
}

