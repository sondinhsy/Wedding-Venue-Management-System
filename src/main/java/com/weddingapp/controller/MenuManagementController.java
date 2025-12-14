package com.weddingapp.controller;

import com.weddingapp.dao.MenuDAO;
import com.weddingapp.model.MenuItem;
import com.weddingapp.util.CurrencyFormatter;
import com.weddingapp.util.Validators;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class MenuManagementController {
    private final MenuDAO menuDAO = new MenuDAO();
    private final ObservableList<MenuItem> menus = FXCollections.observableArrayList();
    private final FilteredList<MenuItem> filteredMenus = new FilteredList<>(menus);
    private MenuItem selectedMenuItem;

    @FXML private TextField menuTitleField;
    @FXML private TextField menuPriceField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TableView<MenuItem> menuTable;
    @FXML private TableColumn<MenuItem, String> colMenuTitle;
    @FXML private TableColumn<MenuItem, String> colMenuPrice;
    @FXML private TableColumn<MenuItem, String> colMenuCategory;
    @FXML private TableColumn<MenuItem, String> colMenuActions;
    @FXML private RadioButton filterAllBtn;
    @FXML private RadioButton filterComboBtn;
    @FXML private RadioButton filterSingleBtn;
    @FXML private ToggleGroup menuFilterGroup;

    @FXML
    public void initialize() {
        setupTable();
        loadData();
        categoryCombo.getItems().addAll("single", "combo");
        categoryCombo.setValue("single");
        
        menuTable.setItems(filteredMenus);
        menuTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadMenuItemToForm(newVal);
            }
        });
        
        menuFilterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        if (filterAllBtn != null) {
            filterAllBtn.setSelected(true);
        }
    }

    private void setupTable() {
        colMenuTitle.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
        colMenuPrice.setCellValueFactory(cell -> 
            new SimpleStringProperty(CurrencyFormatter.formatVND(cell.getValue().getPrice())));
        colMenuCategory.setCellValueFactory(cell -> {
            String cat = cell.getValue().getCategory();
            return new SimpleStringProperty("combo".equalsIgnoreCase(cat) ? "Combo" : "Món lẻ");
        });
        
        colMenuActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Sửa");
            private final Button deleteBtn = new Button("🗑️ Xóa");
            
            {
                editBtn.setOnAction(e -> {
                    MenuItem item = getTableView().getItems().get(getIndex());
                    loadMenuItemToForm(item);
                });
                deleteBtn.setOnAction(e -> {
                    MenuItem item = getTableView().getItems().get(getIndex());
                    handleDelete(item);
                });
                editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                deleteBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8; -fx-background-color: #dc2626;");
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8, editBtn, deleteBtn);
                    setGraphic(box);
                }
            }
        });
    }

    private void loadData() {
        menus.setAll(menuDAO.findAll());
    }

    private void loadMenuItemToForm(MenuItem item) {
        selectedMenuItem = item;
        menuTitleField.setText(item.getTitle());
        menuPriceField.setText(String.valueOf((int)item.getPrice()));
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
                menuTable.refresh();
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
        menuTable.getSelectionModel().clearSelection();
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
