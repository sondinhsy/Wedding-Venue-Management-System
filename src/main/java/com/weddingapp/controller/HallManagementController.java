package com.weddingapp.controller;

import com.weddingapp.dao.HallDAO;
import com.weddingapp.model.Hall;
import com.weddingapp.util.CurrencyFormatter;
import com.weddingapp.util.Validators;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

public class HallManagementController {
    private final HallDAO hallDAO = new HallDAO();
    private final ObservableList<Hall> halls = FXCollections.observableArrayList();
    private Hall selectedHall;

    @FXML private TextField hallNameField;
    @FXML private Spinner<Integer> capacitySpinner;
    @FXML private TextField priceField;
    @FXML private TableView<Hall> hallTable;
    @FXML private TableColumn<Hall, String> colHallName;
    @FXML private TableColumn<Hall, Number> colHallCapacity;
    @FXML private TableColumn<Hall, String> colHallPrice;
    @FXML private TableColumn<Hall, String> colHallActions;

    @FXML
    public void initialize() {
        setupTable();
        loadData();
        capacitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 50));
        
        hallTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadHallToForm(newVal);
            }
        });
    }

    private void setupTable() {
        colHallName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        colHallCapacity.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getCapacity()));
        colHallPrice.setCellValueFactory(cell -> 
            new SimpleStringProperty(CurrencyFormatter.formatVND(cell.getValue().getPricePerTable())));
        
        colHallActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Sửa");
            private final Button deleteBtn = new Button("🗑️ Xóa");
            
            {
                editBtn.setOnAction(e -> {
                    Hall hall = getTableView().getItems().get(getIndex());
                    loadHallToForm(hall);
                });
                deleteBtn.setOnAction(e -> {
                    Hall hall = getTableView().getItems().get(getIndex());
                    handleDelete(hall);
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
        
        hallTable.setItems(halls);
    }

    private void loadData() {
        halls.setAll(hallDAO.findAll());
    }

    private void loadHallToForm(Hall hall) {
        selectedHall = hall;
        hallNameField.setText(hall.getName());
        capacitySpinner.getValueFactory().setValue(hall.getCapacity());
        priceField.setText(String.valueOf((int)hall.getPricePerTable()));
    }

    @FXML
    public void handleSave() {
        String name = hallNameField.getText().trim();
        String priceText = priceField.getText().trim();
        
        if (!Validators.isNotEmpty(name)) {
            showError("Vui lòng nhập tên sảnh");
            hallNameField.requestFocus();
            return;
        }
        
        if (priceText.isEmpty()) {
            showError("Vui lòng nhập giá mỗi bàn");
            priceField.requestFocus();
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
            priceField.requestFocus();
            return;
        }
        
        if (!Validators.isPositive(price)) {
            showError("Giá phải lớn hơn 0");
            priceField.requestFocus();
            return;
        }
        
        int capacity = capacitySpinner.getValue();
        if (!Validators.isPositive(capacity)) {
            showError("Sức chứa phải lớn hơn 0");
            return;
        }
        
        try {
            if (selectedHall == null) {
                // Add new
                Hall hall = new Hall();
                hall.setName(name);
                hall.setCapacity(capacity);
                hall.setPricePerTable(price);
                hallDAO.save(hall);
                halls.add(hall);
                showSuccess("Đã thêm sảnh: " + name);
            } else {
                // Update
                selectedHall.setName(name);
                selectedHall.setCapacity(capacity);
                selectedHall.setPricePerTable(price);
                hallDAO.update(selectedHall);
                hallTable.refresh();
                showSuccess("Đã cập nhật sảnh: " + name);
            }
            handleReset();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    @FXML
    public void handleReset() {
        selectedHall = null;
        hallNameField.clear();
        capacitySpinner.getValueFactory().setValue(50);
        priceField.clear();
        hallTable.getSelectionModel().clearSelection();
    }

    private void handleDelete(Hall hall) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, 
            "Bạn có chắc muốn xóa sảnh \"" + hall.getName() + "\"?", 
            ButtonType.YES, ButtonType.NO);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    hallDAO.delete(hall.getId());
                    halls.remove(hall);
                    handleReset();
                    showSuccess("Đã xóa sảnh: " + hall.getName());
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
