package com.weddingapp.controller;

import com.weddingapp.dao.CustomerDAO;
import com.weddingapp.model.Customer;
import com.weddingapp.util.Validators;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class CustomerManagementController {
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private Customer selectedCustomer;

    @FXML private TextField customerNameField;
    @FXML private TextField customerPhoneField;
    @FXML private TextField customerEmailField;
    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, String> colCustomerName;
    @FXML private TableColumn<Customer, String> colCustomerPhone;
    @FXML private TableColumn<Customer, String> colCustomerEmail;
    @FXML private TableColumn<Customer, String> colCustomerActions;
    @FXML private TextField searchField;

    @FXML
    public void initialize() {
        setupTable();
        loadData();
        
        customerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadCustomerToForm(newVal);
            }
        });
        
        // Search functionality
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterCustomers(newVal));
    }

    private void setupTable() {
        colCustomerName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        colCustomerPhone.setCellValueFactory(cell -> {
            String phone = cell.getValue().getPhone();
            return new SimpleStringProperty(phone != null ? phone : "");
        });
        colCustomerEmail.setCellValueFactory(cell -> {
            String email = cell.getValue().getEmail();
            return new SimpleStringProperty(email != null ? email : "");
        });
        
        colCustomerActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Sửa");
            private final Button deleteBtn = new Button("🗑️ Xóa");
            
            {
                editBtn.setOnAction(e -> {
                    Customer customer = getTableView().getItems().get(getIndex());
                    loadCustomerToForm(customer);
                });
                deleteBtn.setOnAction(e -> {
                    Customer customer = getTableView().getItems().get(getIndex());
                    handleDelete(customer);
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
        
        customerTable.setItems(customers);
    }

    private void loadData() {
        customers.setAll(customerDAO.findAll());
    }

    private void loadCustomerToForm(Customer customer) {
        selectedCustomer = customer;
        customerNameField.setText(customer.getName());
        customerPhoneField.setText(customer.getPhone() != null ? customer.getPhone() : "");
        customerEmailField.setText(customer.getEmail() != null ? customer.getEmail() : "");
    }

    private void filterCustomers(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            customerTable.setItems(customers);
            return;
        }
        
        String lowerSearch = searchText.toLowerCase();
        ObservableList<Customer> filtered = FXCollections.observableArrayList();
        for (Customer customer : customers) {
            if (customer.getName().toLowerCase().contains(lowerSearch) ||
                (customer.getPhone() != null && customer.getPhone().contains(lowerSearch)) ||
                (customer.getEmail() != null && customer.getEmail().toLowerCase().contains(lowerSearch))) {
                filtered.add(customer);
            }
        }
        customerTable.setItems(filtered);
    }

    @FXML
    public void handleSave() {
        String name = customerNameField.getText().trim();
        String phone = customerPhoneField.getText().trim();
        String email = customerEmailField.getText().trim();
        
        if (!Validators.isNotEmpty(name)) {
            showError("Vui lòng nhập tên khách hàng");
            customerNameField.requestFocus();
            return;
        }
        
        if (!phone.isEmpty() && !Validators.isValidPhone(phone)) {
            showError("Số điện thoại không hợp lệ. Ví dụ: 0912345678 hoặc +84912345678");
            customerPhoneField.requestFocus();
            return;
        }
        
        if (!email.isEmpty() && !Validators.isValidEmail(email)) {
            showError("Email không hợp lệ. Ví dụ: example@email.com");
            customerEmailField.requestFocus();
            return;
        }
        
        try {
            if (selectedCustomer == null) {
                // Add new
                Customer customer = new Customer();
                customer.setName(name);
                customer.setPhone(phone.isEmpty() ? null : phone);
                customer.setEmail(email.isEmpty() ? null : email);
                customerDAO.save(customer);
                customers.add(customer);
                showSuccess("Đã thêm khách hàng: " + name);
            } else {
                // Update
                selectedCustomer.setName(name);
                selectedCustomer.setPhone(phone.isEmpty() ? null : phone);
                selectedCustomer.setEmail(email.isEmpty() ? null : email);
                customerDAO.update(selectedCustomer);
                customerTable.refresh();
                showSuccess("Đã cập nhật khách hàng: " + name);
            }
            handleReset();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    @FXML
    public void handleReset() {
        selectedCustomer = null;
        customerNameField.clear();
        customerPhoneField.clear();
        customerEmailField.clear();
        customerTable.getSelectionModel().clearSelection();
        searchField.clear();
    }

    private void handleDelete(Customer customer) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, 
            "Bạn có chắc muốn xóa khách hàng \"" + customer.getName() + "\"?", 
            ButtonType.YES, ButtonType.NO);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    customerDAO.delete(customer.getId());
                    customers.remove(customer);
                    handleReset();
                    showSuccess("Đã xóa khách hàng: " + customer.getName());
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
