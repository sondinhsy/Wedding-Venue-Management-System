package com.weddingapp.controller;

import com.weddingapp.dao.BookingDAO;
import com.weddingapp.dao.ExtrasServiceDAO;
import com.weddingapp.model.Booking;
import com.weddingapp.model.ExtrasServiceItem;
import com.weddingapp.model.MenuItem;
import com.weddingapp.util.CurrencyFormatter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExtrasServicesController {
    private final BookingDAO bookingDAO = new BookingDAO();
    private final ExtrasServiceDAO extrasServiceDAO = new ExtrasServiceDAO();

    private Booking selectedBooking;
    private final ObservableList<ExtrasServiceItem> extrasItems = FXCollections.observableArrayList();

    @FXML private ComboBox<Booking> bookingCombo;
    @FXML private TextField searchBookingField;
    @FXML private Label emptyStateLabel;
    
    // Row 2 - Left: Add Tables
    @FXML private Button decreaseTableBtn;
    @FXML private TextField tableQuantityField;
    @FXML private Button increaseTableBtn;
    @FXML private Button addTableBtn;
    @FXML private Label pricePerTableLabel;
    
    // Row 2 - Right: Services
    @FXML private CheckBox mcCheckBox;
    @FXML private CheckBox musicCheckBox;
    
    // Row 3 - Financial Info
    @FXML private Label originalTotalLabel;
    @FXML private Label paidAmountLabel;
    @FXML private Label remainingLabel;
    @FXML private Label extrasTotalLabel;
    @FXML private Label finalTotalLabel;
    
    // Row 4
    @FXML private Button viewDetailsBtn;
    @FXML private Button saveBtn;
    
    // Conditional containers
    @FXML private VBox contentContainer;

    private int additionalTables = 0;
    private int maxAddableTables = 0;
    private double pricePerTable = 0.0;

    @FXML
    public void initialize() {
        try {
            loadBookings();
            setupBookingCombo();
            setupSearchField();
            setupTableControls();
            setupServiceCheckboxes();
            updateUI();
        } catch (Exception e) {
            System.err.println("Error initializing ExtrasServicesController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupSearchField() {
        if (searchBookingField != null) {
            searchBookingField.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    if (newVal == null || newVal.trim().isEmpty()) {
                        loadBookings();
                        return;
                    }
                    String search = newVal.trim().toLowerCase();
                    List<Booking> allBookings = bookingDAO.findAll();
                    List<Booking> filtered = allBookings.stream()
                            .filter(booking -> 
                                    booking != null &&
                                    booking.getCustomer() != null &&
                                    (String.valueOf(booking.getId()).contains(search) ||
                                    booking.getCustomer().getName().toLowerCase().contains(search) ||
                                    (booking.getEventDate() != null && booking.getEventDate().toString().contains(search))))
                            .collect(Collectors.toList());
                    if (bookingCombo != null) {
                        bookingCombo.setItems(FXCollections.observableArrayList(filtered));
                    }
                } catch (Exception e) {
                    System.err.println("Error in search: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }

    private void loadBookings() {
        try {
            List<Booking> bookings = bookingDAO.findAll();
            if (bookingCombo != null) {
                bookingCombo.setItems(FXCollections.observableArrayList(bookings));
            }
        } catch (Exception e) {
            System.err.println("Error loading bookings: " + e.getMessage());
            e.printStackTrace();
            if (bookingCombo != null) {
                bookingCombo.setItems(FXCollections.observableArrayList());
            }
        }
    }

    private void setupBookingCombo() {
        bookingCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Booking booking, boolean empty) {
                super.updateItem(booking, empty);
                if (empty || booking == null) {
                    setText(null);
                } else {
                    setText(String.format("Booking #%d - %s - %s (%d bàn)",
                            booking.getId(),
                            booking.getCustomer().getName(),
                            booking.getEventDate(),
                            booking.getTables()));
                }
            }
        });
        bookingCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Booking booking, boolean empty) {
                super.updateItem(booking, empty);
                if (empty || booking == null) {
                    setText("Chọn booking");
                } else {
                    setText(String.format("Booking #%d - %s", booking.getId(), booking.getCustomer().getName()));
                }
            }
        });
        bookingCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            selectedBooking = newVal;
            onBookingSelected();
        });
    }

    private void setupTableControls() {
        if (tableQuantityField != null) {
            tableQuantityField.setEditable(false);
            tableQuantityField.setText("0");
        }
        
        if (decreaseTableBtn != null) {
            decreaseTableBtn.setOnAction(e -> {
                if (additionalTables > 0) {
                    additionalTables--;
                    updateTableControls();
                }
            });
        }
        
        if (increaseTableBtn != null) {
            increaseTableBtn.setOnAction(e -> {
                if (additionalTables < maxAddableTables) {
                    additionalTables++;
                    updateTableControls();
                }
            });
        }
        
        if (addTableBtn != null) {
            addTableBtn.setOnAction(e -> handleAddTable());
        }
    }

    private void setupServiceCheckboxes() {
        if (mcCheckBox != null) {
            mcCheckBox.setOnAction(e -> handleServiceToggle("MC", mcCheckBox.isSelected()));
        }
        if (musicCheckBox != null) {
            musicCheckBox.setOnAction(e -> handleServiceToggle("MUSIC", musicCheckBox.isSelected()));
        }
    }

    private void onBookingSelected() {
        try {
            if (selectedBooking == null) {
                extrasItems.clear();
                additionalTables = 0;
                maxAddableTables = 0;
                pricePerTable = 0.0;
                updateUI();
                return;
            }

            // Load existing extras items
            List<ExtrasServiceItem> existing = extrasServiceDAO.findByBookingId(selectedBooking.getId());
            extrasItems.setAll(existing);

            // Calculate max addable tables
            int bookedTables = selectedBooking.getTables();
            if (selectedBooking.getHall() != null) {
                int hallCapacity = selectedBooking.getHall().getCapacity();
                maxAddableTables = Math.max(0, hallCapacity - bookedTables);
            } else {
                maxAddableTables = 0;
            }

            // Calculate price per table from booking's menu items
            pricePerTable = calculatePricePerTable(selectedBooking);

            // Reset additional tables counter
            additionalTables = 0;

            // Update checkboxes based on existing items
            updateServiceCheckboxes();

            updateUI();
        } catch (Exception e) {
            System.err.println("Error in onBookingSelected: " + e.getMessage());
            e.printStackTrace();
            showError("Lỗi khi tải thông tin booking: " + e.getMessage());
        }
    }

    private double calculatePricePerTable(Booking booking) {
        if (booking.getMenuItems().isEmpty()) {
            return 0.0;
        }
        return booking.getMenuItems().stream()
                .mapToDouble(MenuItem::getPrice)
                .sum();
    }

    private void updateServiceCheckboxes() {
        boolean hasMC = extrasItems.stream()
                .anyMatch(item -> item.getType() == ExtrasServiceItem.Type.SERVICE 
                        && "MC".equals(item.getServiceCode()));
        boolean hasMusic = extrasItems.stream()
                .anyMatch(item -> item.getType() == ExtrasServiceItem.Type.SERVICE 
                        && "MUSIC".equals(item.getServiceCode()));
        
        mcCheckBox.setSelected(hasMC);
        musicCheckBox.setSelected(hasMusic);
    }

    private void updateTableControls() {
        if (tableQuantityField != null) {
            tableQuantityField.setText(String.valueOf(additionalTables));
        }
        if (decreaseTableBtn != null) {
            decreaseTableBtn.setDisable(additionalTables <= 0 || maxAddableTables == 0);
        }
        if (increaseTableBtn != null) {
            increaseTableBtn.setDisable(additionalTables >= maxAddableTables || maxAddableTables == 0);
        }
        if (addTableBtn != null) {
            addTableBtn.setDisable(additionalTables <= 0 || maxAddableTables == 0);
        }
    }

    private void handleAddTable() {
        if (additionalTables <= 0 || selectedBooking == null) {
            return;
        }

        // Tìm item EXTRA_TRAY "Thêm bàn" nếu có
        ExtrasServiceItem existingTray = extrasItems.stream()
                .filter(item -> item.getType() == ExtrasServiceItem.Type.EXTRA_TRAY
                        && "Thêm bàn".equals(item.getName()))
                .findFirst()
                .orElse(null);

        if (existingTray != null) {
            // Cộng dồn quantity
            existingTray.setQuantity(existingTray.getQuantity() + additionalTables);
        } else {
            // Tạo mới
            ExtrasServiceItem newItem = new ExtrasServiceItem();
            newItem.setBookingId(selectedBooking.getId());
            newItem.setType(ExtrasServiceItem.Type.EXTRA_TRAY);
            newItem.setName("Thêm bàn");
            newItem.setUnit("bàn");
            newItem.setQuantity(additionalTables);
            newItem.setUnitPrice(pricePerTable);
            extrasItems.add(newItem);
        }

        additionalTables = 0;
        updateTableControls();
        updateTotal();
    }

    private void handleServiceToggle(String serviceCode, boolean selected) {
        if (selectedBooking == null) {
            return;
        }

        if (selected) {
            // Kiểm tra xem đã có chưa
            boolean exists = extrasItems.stream()
                    .anyMatch(item -> item.getType() == ExtrasServiceItem.Type.SERVICE
                            && serviceCode.equals(item.getServiceCode()));
            
            if (!exists) {
                ExtrasServiceItem item = new ExtrasServiceItem();
                item.setBookingId(selectedBooking.getId());
                item.setType(ExtrasServiceItem.Type.SERVICE);
                item.setServiceCode(serviceCode);
                item.setName(serviceCode.equals("MC") ? "MC" : "Âm nhạc");
                item.setUnit("gói");
                item.setQuantity(1);
                item.setUnitPrice(30.0);
                extrasItems.add(item);
            }
        } else {
            // Xóa item
            extrasItems.removeIf(item -> item.getType() == ExtrasServiceItem.Type.SERVICE
                    && serviceCode.equals(item.getServiceCode()));
        }

        updateTotal();
    }

    @FXML
    private void handleViewDetails() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Danh sách đã thêm");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<ExtrasServiceItem> table = new TableView<>();
        table.setItems(extrasItems);

        TableColumn<ExtrasServiceItem, String> typeCol = new TableColumn<>("Loại");
        typeCol.setCellValueFactory(item -> {
            ExtrasServiceItem i = item.getValue();
            return new SimpleStringProperty(i.getType() == ExtrasServiceItem.Type.EXTRA_TRAY ? "Thêm bàn" : "Dịch vụ");
        });

        TableColumn<ExtrasServiceItem, String> nameCol = new TableColumn<>("Tên");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<ExtrasServiceItem, Integer> quantityCol = new TableColumn<>("Số lượng");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<ExtrasServiceItem, String> unitPriceCol = new TableColumn<>("Đơn giá");
        unitPriceCol.setCellValueFactory(item -> {
            return new SimpleStringProperty(CurrencyFormatter.formatVND(item.getValue().getUnitPrice()));
        });

        TableColumn<ExtrasServiceItem, String> totalCol = new TableColumn<>("Thành tiền");
        totalCol.setCellValueFactory(item -> {
            return new SimpleStringProperty(CurrencyFormatter.formatVND(item.getValue().getLineTotal()));
        });

        TableColumn<ExtrasServiceItem, String> notesCol = new TableColumn<>("Ghi chú");
        notesCol.setCellValueFactory(item -> {
            String notes = item.getValue().getNotes();
            return new SimpleStringProperty(notes != null ? notes : "");
        });

        TableColumn<ExtrasServiceItem, Void> deleteCol = new TableColumn<>("Xóa");
        deleteCol.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Xóa");

            {
                deleteBtn.setOnAction(e -> {
                    ExtrasServiceItem item = getTableView().getItems().get(getIndex());
                    extrasItems.remove(item);
                    updateServiceCheckboxes();
                    updateTotal();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                }
            }
        });

        @SuppressWarnings("unchecked")
        TableColumn<ExtrasServiceItem, ?>[] columns = new TableColumn[] {
            typeCol, nameCol, quantityCol, unitPriceCol, totalCol, notesCol, deleteCol
        };
        table.getColumns().addAll(columns);

        if (extrasItems.isEmpty()) {
            Label emptyLabel = new Label("Chưa có phát sinh/dịch vụ");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-padding: 20;");
            dialog.getDialogPane().setContent(emptyLabel);
        } else {
            VBox content = new VBox(10);
            content.getChildren().add(table);
            dialog.getDialogPane().setContent(content);
        }

        dialog.getDialogPane().setPrefSize(800, 400);
        dialog.showAndWait();
    }

    @FXML
    private void handleSave() {
        if (selectedBooking == null) {
            showError("Vui lòng chọn booking");
            return;
        }

        try {
            extrasServiceDAO.saveAll(selectedBooking.getId(), new ArrayList<>(extrasItems));
            showSuccess("Đã lưu phát sinh & dịch vụ thành công!");
            // Reload để có ID mới nếu có
            onBookingSelected();
        } catch (Exception e) {
            showError("Lỗi khi lưu: " + e.getMessage());
        }
    }

    private void updateUI() {
        boolean hasBooking = selectedBooking != null;
        
        if (contentContainer != null) {
            contentContainer.setVisible(hasBooking);
            contentContainer.setManaged(hasBooking);
        }
        
        if (emptyStateLabel != null) {
            emptyStateLabel.setVisible(!hasBooking);
            emptyStateLabel.setManaged(!hasBooking);
        }

        if (hasBooking) {
            String priceText = String.format("Đơn giá menu đã đặt: %s/bàn", 
                    CurrencyFormatter.formatVND(pricePerTable));
            if (maxAddableTables == 0) {
                priceText += " (Sảnh đã đầy, không thể thêm bàn)";
            } else {
                priceText += String.format(" (Có thể thêm tối đa %d bàn)", maxAddableTables);
            }
            pricePerTableLabel.setText(priceText);
            updateTableControls();
            updateFinancialInfo();
        } else {
            pricePerTableLabel.setText("");
            resetFinancialInfo();
        }

        if (saveBtn != null) {
            saveBtn.setDisable(!hasBooking || extrasItems.isEmpty());
        }
    }

    private void updateTotal() {
        updateFinancialInfo();
        
        if (saveBtn != null) {
            saveBtn.setDisable(selectedBooking == null || extrasItems.isEmpty());
        }
    }
    
    private void resetFinancialInfo() {
        if (originalTotalLabel != null) originalTotalLabel.setText("$0.00");
        if (paidAmountLabel != null) paidAmountLabel.setText("$0.00");
        if (remainingLabel != null) remainingLabel.setText("$0.00");
        if (extrasTotalLabel != null) extrasTotalLabel.setText("$0.00");
        if (finalTotalLabel != null) finalTotalLabel.setText("$0.00");
    }
    
    private void updateFinancialInfo() {
        if (selectedBooking == null) {
            resetFinancialInfo();
            return;
        }
        
        // Số tiền đơn đặt gốc (trước phát sinh)
        double originalTotal = selectedBooking.getTotal();
        if (originalTotalLabel != null) {
            originalTotalLabel.setText(CurrencyFormatter.formatVND(originalTotal) + " (trước phát sinh)");
        }
        
        // Số tiền đã trả trước (20% deposit)
        double paidAmount = selectedBooking.getPaidAmount();
        if (paidAmountLabel != null) {
            paidAmountLabel.setText(CurrencyFormatter.formatVND(paidAmount) + " (20% deposit)");
        }
        
        // Số tiền còn lại chưa trả (trước phát sinh)
        double remainingBeforeExtras = Math.max(0, originalTotal - paidAmount);
        if (remainingLabel != null) {
            remainingLabel.setText(CurrencyFormatter.formatVND(remainingBeforeExtras) + " (trước phát sinh)");
        }
        
        // Tổng phát sinh & dịch vụ
        double extrasTotal = extrasItems.stream()
                .mapToDouble(ExtrasServiceItem::getLineTotal)
                .sum();
        if (extrasTotalLabel != null) {
            extrasTotalLabel.setText(CurrencyFormatter.formatVND(extrasTotal));
        }
        
        // Tổng tiền sau phát sinh = đơn gốc + phát sinh
        double totalAfterExtras = originalTotal + extrasTotal;
        
        // Tổng số tiền cần phải trả = (đơn gốc + phát sinh) - đã trả
        double finalTotal = Math.max(0, totalAfterExtras - paidAmount);
        if (finalTotalLabel != null) {
            finalTotalLabel.setText(CurrencyFormatter.formatVND(finalTotal) + " (sau phát sinh)");
        }
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

