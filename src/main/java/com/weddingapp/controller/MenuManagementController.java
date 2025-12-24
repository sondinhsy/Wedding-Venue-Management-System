package com.weddingapp.controller;

import com.weddingapp.dao.MenuDAO;
import com.weddingapp.model.MenuItem;
import com.weddingapp.util.CurrencyFormatter;
import com.weddingapp.util.Validators;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.geometry.Pos;

public class MenuManagementController {
    private final MenuDAO menuDAO = new MenuDAO();
    private final ObservableList<MenuItem> menus = FXCollections.observableArrayList();
    private final FilteredList<MenuItem> filteredMenus = new FilteredList<>(menus);
    private MenuItem selectedMenuItem;

    @FXML private TextField menuTitleField;
    @FXML private TextField menuPriceField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ScrollPane menuScrollPane;
    @FXML private FlowPane menuGridPane;
    @FXML private RadioButton filterAllBtn;
    @FXML private RadioButton filterComboBtn;
    @FXML private RadioButton filterSingleBtn;
    @FXML private ToggleGroup menuFilterGroup;

    @FXML
    public void initialize() {
        loadData();
        categoryCombo.getItems().addAll("single", "combo");
        categoryCombo.setValue("single");
        
        filteredMenus.addListener((javafx.collections.ListChangeListener.Change<? extends MenuItem> change) -> {
            updateMenuGrid();
        });
        
        menuFilterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            applyFilter();
            updateMenuGrid();
        });
        if (filterAllBtn != null) {
            filterAllBtn.setSelected(true);
        }
        
        updateMenuGrid();
    }

    private void updateMenuGrid() {
        menuGridPane.getChildren().clear();

        // Nếu chưa có món nào, hiển thị một vài khung trống mẫu để sau này gắn ảnh
        if (filteredMenus.isEmpty()) {
            for (int i = 0; i < 4; i++) {
                VBox placeholderCard = createEmptyPlaceholderCard();
                menuGridPane.getChildren().add(placeholderCard);
            }
            return;
        }

        for (MenuItem item : filteredMenus) {
            VBox card = createMenuCard(item);
            menuGridPane.getChildren().add(card);
        }
    }

    private VBox createMenuCard(MenuItem item) {
        VBox card = new VBox(8);
        card.setPrefWidth(180);
        card.setPrefHeight(250);
        card.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12;");
        
        // Image placeholder
        StackPane imagePane = new StackPane();
        imagePane.setPrefSize(156, 120);
        imagePane.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 4;");
        
        ImageView imageView = new ImageView();
        imageView.setFitWidth(156);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);
        
        // Try to load image if available (placeholder for now)
        try {
            String imagePath = "/images/menu/" + item.getId() + ".jpg";
            Image image = new Image(getClass().getResourceAsStream(imagePath));
            imageView.setImage(image);
            imagePane.getChildren().add(imageView);
        } catch (Exception e) {
            // Use placeholder
            Label placeholder = new Label("🍽️");
            placeholder.setStyle("-fx-font-size: 48px;");
            imagePane.getChildren().add(placeholder);
        }
        
        card.getChildren().add(imagePane);
        
        // Title
        Label titleLabel = new Label(item.getTitle());
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        titleLabel.setMaxWidth(156);
        
        // Price
        Label priceLabel = new Label(CurrencyFormatter.formatVND(item.getPrice()));
        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
        
        // Category badge
        Label categoryLabel = new Label("combo".equalsIgnoreCase(item.getCategory()) ? "Combo" : "Món lẻ");
        categoryLabel.setStyle("-fx-font-size: 11px; -fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 4 8; -fx-background-radius: 12;");
        
        // Buttons
        HBox buttonBox = new HBox(4);
        Button editBtn = new Button("✏️");
        editBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 8; -fx-background-color: #3b82f6; -fx-text-fill: white;");
        editBtn.setOnAction(e -> loadMenuItemToForm(item));
        
        Button deleteBtn = new Button("🗑️");
        deleteBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 8; -fx-background-color: #dc2626; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> handleDelete(item));
        
        buttonBox.getChildren().addAll(editBtn, deleteBtn);
        
        card.getChildren().addAll(titleLabel, priceLabel, categoryLabel, buttonBox);
        card.setAlignment(Pos.TOP_CENTER);
        
        return card;
    }

    /**
     * Tạo khung trống mẫu (card trắng có ô ảnh rỗng) dùng khi chưa có dữ liệu menu.
     * Chỉ mang tính minh họa để bạn dễ hình dung vị trí ảnh món ăn sau này.
     */
    private VBox createEmptyPlaceholderCard() {
        VBox card = new VBox(8);
        card.setPrefWidth(180);
        card.setPrefHeight(250);
        card.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12; -fx-border-style: segments(10, 4); -fx-border-width: 1;");

        StackPane imagePane = new StackPane();
        imagePane.setPrefSize(156, 120);
        imagePane.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 4; -fx-border-color: #d1d5db; -fx-border-radius: 4; -fx-border-style: dashed;");

        Label icon = new Label("Thêm ảnh món ăn");
        icon.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        imagePane.getChildren().add(icon);

        Label title = new Label("Khung món ăn (trống)");
        title.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        Label hint = new Label("Sau này bạn có thể gắn ảnh thật cho từng món ở đây.");
        hint.setWrapText(true);
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");

        card.getChildren().addAll(imagePane, title, hint);
        card.setAlignment(Pos.TOP_CENTER);
        return card;
    }

    private void loadData() {
        menus.setAll(menuDAO.findAll());
    }

    private void loadMenuItemToForm(MenuItem item) {
        selectedMenuItem = item;
        menuTitleField.setText(item.getTitle());
        // Price is already stored in USD, display directly
        menuPriceField.setText(String.format("%.2f", item.getPrice()));
        categoryCombo.setValue(item.getCategory());
    }

    @FXML
    public void handleSave() {
        String title = menuTitleField.getText().trim();
        String priceText = menuPriceField.getText().trim();
        String category = categoryCombo.getValue();
        
        if (!Validators.isNotEmpty(title)) {
            showError("Vui lòng nhập tên món");
            menuTitleField.requestFocus();
            return;
        }
        
        if (priceText.isEmpty()) {
            showError("Vui lòng nhập giá");
            menuPriceField.requestFocus();
            return;
        }
        
        double price;
        try {
            price = CurrencyFormatter.parseVND(priceText);
            if (price == 0.0 && !priceText.equals("0")) {
                price = Double.parseDouble(priceText.replace(".", "").replace(",", "."));
            }
        } catch (NumberFormatException ex) {
            showError("Giá không hợp lệ");
            menuPriceField.requestFocus();
            return;
        }
        
        if (!Validators.isPositive(price)) {
            showError("Giá phải lớn hơn 0");
            menuPriceField.requestFocus();
            return;
        }
        
        try {
            if (selectedMenuItem == null) {
                // Add new
                MenuItem item = new MenuItem();
                item.setTitle(title);
                item.setPrice(price);
                item.setCategory(category);
                menuDAO.save(item);
                menus.add(item);
                showSuccess("Đã thêm món: " + title);
            } else {
                // Update
                selectedMenuItem.setTitle(title);
                selectedMenuItem.setPrice(price);
                selectedMenuItem.setCategory(category);
                menuDAO.update(selectedMenuItem);
                updateMenuGrid();
                showSuccess("Đã cập nhật món: " + title);
            }
            handleReset();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    @FXML
    public void handleReset() {
        selectedMenuItem = null;
        menuTitleField.clear();
        menuPriceField.clear();
        categoryCombo.setValue("single");
    }

    private void applyFilter() {
        String filter = "all";
        if (menuFilterGroup.getSelectedToggle() != null) {
            Object userData = menuFilterGroup.getSelectedToggle().getUserData();
            if (userData != null) {
                filter = userData.toString();
            }
        }
        String finalFilter = filter;
        filteredMenus.setPredicate(item -> {
            if ("combo".equalsIgnoreCase(finalFilter)) return "combo".equalsIgnoreCase(item.getCategory());
            if ("single".equalsIgnoreCase(finalFilter)) return !"combo".equalsIgnoreCase(item.getCategory());
            return true;
        });
    }

    private void handleDelete(MenuItem item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, 
            "Bạn có chắc muốn xóa món \"" + item.getTitle() + "\"?", 
            ButtonType.YES, ButtonType.NO);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    menuDAO.delete(item.getId());
                    menus.remove(item);
                    updateMenuGrid();
                    handleReset();
                    showSuccess("Đã xóa món: " + item.getTitle());
                } catch (Exception e) {
                    showError("Lỗi khi xóa: " + e.getMessage());
                }
            }
        });
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
