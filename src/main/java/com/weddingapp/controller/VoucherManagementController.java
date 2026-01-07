package com.weddingapp.controller;

import com.weddingapp.service.VoucherService;
import com.weddingapp.util.CurrencyFormatter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class VoucherManagementController {
    
    @FXML private TableView<VoucherService.VoucherInfo> voucherTable;
    @FXML private TableColumn<VoucherService.VoucherInfo, String> colVoucherCode;
    @FXML private TableColumn<VoucherService.VoucherInfo, String> colVoucherDescription;
    @FXML private TableColumn<VoucherService.VoucherInfo, String> colVoucherType;
    @FXML private TableColumn<VoucherService.VoucherInfo, String> colVoucherValue;
    @FXML private TableColumn<VoucherService.VoucherInfo, String> colVoucherSeason;
    
    @FXML
    public void initialize() {
        setupTable();
        loadVouchers();
    }
    
    private void setupTable() {
        colVoucherCode.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getCode()));
        
        colVoucherDescription.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getDescription()));
        
        colVoucherType.setCellValueFactory(cell -> {
            String type = cell.getValue().getType();
            return new SimpleStringProperty("percent".equalsIgnoreCase(type) ? "Phần trăm" : "Số tiền");
        });
        
        colVoucherValue.setCellValueFactory(cell -> {
            VoucherService.VoucherInfo voucher = cell.getValue();
            String valueText;
            if ("percent".equalsIgnoreCase(voucher.getType())) {
                valueText = String.format("%.0f%%", voucher.getValue());
            } else {
                valueText = String.format("$%.0f", voucher.getValue());
            }
            return new SimpleStringProperty(valueText);
        });
        
        colVoucherSeason.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getSeason()));
        
        voucherTable.setItems(FXCollections.observableArrayList(VoucherService.getAllVouchers()));
    }
    
    private void loadVouchers() {
        voucherTable.setItems(FXCollections.observableArrayList(VoucherService.getAllVouchers()));
    }
}

