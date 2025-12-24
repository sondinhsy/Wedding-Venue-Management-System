package com.weddingapp.controller;

import com.weddingapp.dao.BookingDAO;
import com.weddingapp.dao.CustomerDAO;
import com.weddingapp.dao.HallDAO;
import com.weddingapp.dao.MenuDAO;
import com.weddingapp.dao.ComboItemDAO;
import com.weddingapp.model.Booking;
import com.weddingapp.model.Customer;
import com.weddingapp.model.Hall;
import com.weddingapp.model.MenuItem;
import com.weddingapp.util.CurrencyFormatter;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class BookingController {
    private final BookingDAO bookingDAO = new BookingDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final HallDAO hallDAO = new HallDAO();
    private final MenuDAO menuDAO = new MenuDAO();
    private final ComboItemDAO comboItemDAO = new ComboItemDAO();

    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final ObservableList<MenuItem> menus = FXCollections.observableArrayList();
    private final FilteredList<MenuItem> filteredMenus = new FilteredList<>(menus);

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<Customer> customerCombo;
    @FXML private ComboBox<Hall> hallCombo;
    @FXML private Label hallInfoLabel;
    @FXML private Spinner<Integer> tableSpinner;
    @FXML private TextArea notesArea;
    @FXML private ListView<MenuItem> menuList;
    @FXML private Label totalLabel;
    @FXML private RadioButton filterAllBtn;
    @FXML private RadioButton filterComboBtn;
    @FXML private RadioButton filterSingleBtn;
    @FXML private ToggleGroup menuFilterGroup;
    @FXML private Button saveBookingButton;
    @FXML private Label summaryHallLabel;
    @FXML private Label summaryMenuLabel;
    @FXML private Label summaryTotalLabel;

    @FXML
    public void initialize() {
        loadData();
        setupMenuList();
        setupListeners();
        // Mặc định tối thiểu 30 bàn
        tableSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 500, 30));
        if (filterAllBtn != null) {
            filterAllBtn.setSelected(true);
        }
        updateSummary();
    }

    private void loadData() {
        customers.setAll(customerDAO.findAll());
        menus.setAll(menuDAO.findAll());
        customerCombo.setItems(customers);
        hallCombo.setItems(FXCollections.observableArrayList(hallDAO.findAll()));
        applyMenuFilter();
    }

    private void setupMenuList() {
        menuList.setItems(filteredMenus);
        menuList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(MenuItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                String tag = item.getCategory().equalsIgnoreCase("combo") ? "[Combo]" : "[Món lẻ]";
                setText(item.getTitle() + " - " + CurrencyFormatter.formatVND(item.getPrice()) + " " + tag);
            }
        });
        menuList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void setupListeners() {
        // Khi đổi ngày tổ chức, kiểm tra xem các sảnh trong ngày đó còn chỗ không
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateAvailabilityForDate(newVal));

        tableSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateTotalPreview());
        menuList.getSelectionModel().getSelectedItems().addListener((ListChangeListener<MenuItem>) change -> updateTotalPreview());
        hallCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTotalPreview();
            updateHallInfo(newVal);
        });
        menuFilterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> applyMenuFilter());
    }

    /**
     * Cập nhật danh sách sảnh còn trống và trạng thái form theo ngày được chọn.
     * Nếu tất cả sảnh đều đã full cho ngày đó thì khóa chức năng đặt chỗ.
     */
    private void updateAvailabilityForDate(java.time.LocalDate date) {
        if (date == null) {
            setBookingFormDisabled(false);
            // Hiển thị lại tất cả sảnh
            hallCombo.setItems(FXCollections.observableArrayList(hallDAO.findAll()));
            hallCombo.getSelectionModel().clearSelection();
            hallInfoLabel.setText("Chọn sảnh để xem chi tiết");
            return;
        }

        ObservableList<Hall> allHalls = FXCollections.observableArrayList(hallDAO.findAll());
        ObservableList<Hall> availableHalls = FXCollections.observableArrayList();

        for (Hall hall : allHalls) {
            int usedTables = bookingDAO.getTotalTablesForHallOnDate(hall.getId(), date);
            if (usedTables < hall.getCapacity()) {
                availableHalls.add(hall);
            }
        }

        if (availableHalls.isEmpty()) {
            // Cả 2 sảnh đã full trong ngày này → khóa chức năng đặt mới
            setBookingFormDisabled(true);
            hallCombo.getItems().clear();
            hallInfoLabel.setText("Cả hai sảnh đã full trong ngày này. Bạn chỉ có thể xem thông tin các đơn đã đặt.");
            totalLabel.setText("0 đ");
        } else {
            setBookingFormDisabled(false);
            hallCombo.setItems(availableHalls);
            hallCombo.getSelectionModel().clearSelection();
            hallInfoLabel.setText("Chọn sảnh để xem chi tiết");
        }
    }

    private void setBookingFormDisabled(boolean disabled) {
        customerCombo.setDisable(disabled);
        hallCombo.setDisable(disabled);
        tableSpinner.setDisable(disabled);
        notesArea.setDisable(disabled);
        menuList.setDisable(disabled);
        filterAllBtn.setDisable(disabled);
        filterComboBtn.setDisable(disabled);
        filterSingleBtn.setDisable(disabled);
        if (saveBookingButton != null) {
            saveBookingButton.setDisable(disabled);
        }
    }

    private void applyMenuFilter() {
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

    private void updateHallInfo(Hall hall) {
        if (hall == null) {
            hallInfoLabel.setText("Chọn sảnh để xem chi tiết");
            updateSummary();
            return;
        }
        hallInfoLabel.setText(String.format("%s • %d bàn • Phí sảnh: %s", 
            hall.getName(), hall.getCapacity(), CurrencyFormatter.formatVND(hall.getPricePerTable())));
        updateSummary();
    }

    private void updateTotalPreview() {
        if (hallCombo.getValue() == null) {
            totalLabel.setText("0 đ");
            updateSummary();
            return;
        }
        Booking temp = new Booking();
        temp.setHall(hallCombo.getValue());
        temp.setMenuItems(menuList.getSelectionModel().getSelectedItems());
        temp.setTables(tableSpinner.getValue());
        double total = calculateTotal(temp);
        totalLabel.setText(CurrencyFormatter.formatVND(total));
        updateSummary();
    }

    /**
     * Tính tổng tiền:
     *  - Phí sảnh cố định: hall.getPricePerTable() (50$)
     *  - Giá mỗi mâm: tổng giá tất cả món/combo đã chọn (menuItems)
     *  - Tổng = (giá mỗi mâm * số bàn) + phí sảnh.
     */
    private double calculateTotal(Booking booking) {
        double perTableMenuPrice = booking.getMenuItems().stream()
                .mapToDouble(MenuItem::getPrice)
                .sum();
        double hallFee = booking.getHall().getPricePerTable();
        return perTableMenuPrice * booking.getTables() + hallFee;
    }

    @FXML
    public void handleAddNewCustomer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/customer-management.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Thêm khách hàng mới");
            stage.setScene(new javafx.scene.Scene(root, 800, 600));
            stage.showAndWait();
            // Reload customers after dialog closes
            loadData();
        } catch (Exception e) {
            showError("Lỗi khi mở form thêm khách hàng: " + e.getMessage());
        }
    }

    @FXML
    public void openMenuCatalog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chọn món / combo");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        FlowPane flow = new FlowPane(10, 10);
        flow.setPrefWrapLength(480);
        flow.getStyleClass().add("catalog-pane");

        Map<MenuItem, ToggleButton> toggleByItem = new HashMap<>();
        menus.forEach(item -> {
            ToggleButton btn = new ToggleButton();
            btn.getStyleClass().add("card-btn");
            btn.setUserData(item);
            btn.setSelected(menuList.getSelectionModel().getSelectedItems().contains(item));

            Label title = new Label(item.getTitle());
            title.setStyle("-fx-font-weight: bold;");
            Label price = new Label(CurrencyFormatter.formatVND(item.getPrice()));
            Label tag = new Label(item.getCategory().equalsIgnoreCase("combo") ? "Combo" : "Món lẻ");
            tag.getStyleClass().add("chip");

            VBox box = new VBox(4, title, price, tag);
            btn.setGraphic(box);
            toggleByItem.put(item, btn);
            flow.getChildren().add(btn);
        });

        dialog.getDialogPane().setContent(flow);
        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                menuList.getSelectionModel().clearSelection();
                toggleByItem.forEach((item, toggle) -> {
                    if (toggle.isSelected()) {
                        int idx = menus.indexOf(item);
                        if (idx >= 0) {
                            menuList.getSelectionModel().select(idx);
                        }
                    }
                });
                updateTotalPreview();
            }
        });
    }

    @FXML
    public void handleSaveBooking() {
        if (customerCombo.getValue() == null) {
            showError("Vui lòng chọn khách hàng");
            return;
        }
        
        if (hallCombo.getValue() == null) {
            showError("Vui lòng chọn sảnh cưới");
            return;
        }
        
        if (datePicker.getValue() == null) {
            showError("Vui lòng chọn ngày tổ chức");
            datePicker.requestFocus();
            return;
        }
        
        if (datePicker.getValue().isBefore(java.time.LocalDate.now())) {
            showError("Ngày tổ chức không thể là ngày trong quá khứ");
            datePicker.requestFocus();
            return;
        }
        
        int tables = tableSpinner.getValue();
        if (tables < 30) {
            showError("Số bàn phải từ 30 bàn trở lên");
            tableSpinner.requestFocus();
            return;
        }
        
        Hall selectedHall = hallCombo.getValue();
        if (tables > selectedHall.getCapacity()) {
            showError("Số bàn vượt quá sức chứa tối đa của sảnh (" + selectedHall.getCapacity() + " bàn)");
            tableSpinner.requestFocus();
            return;
        }

        // Kiểm tra tổng số bàn đã đặt trong ngày đó cho sảnh này
        int existingTablesForHall = bookingDAO.getTotalTablesForHallOnDate(selectedHall.getId(), datePicker.getValue());
        if (existingTablesForHall + tables > selectedHall.getCapacity()) {
            int remaining = Math.max(selectedHall.getCapacity() - existingTablesForHall, 0);
            if (remaining == 0) {
                showError("Sảnh \"" + selectedHall.getName() + "\" đã được đặt hết " + selectedHall.getCapacity() +
                        " bàn cho ngày này. Vui lòng chọn sảnh khác hoặc ngày khác.");
            } else {
                showError("Trong ngày này, sảnh \"" + selectedHall.getName() + "\" chỉ còn tối đa " + remaining +
                        " bàn trống. Vui lòng giảm số bàn hoặc chọn ngày khác.");
            }
            tableSpinner.requestFocus();
            return;
        }
        
        try {
            Booking booking = new Booking();
            booking.setCustomer(customerCombo.getValue());
            booking.setHall(hallCombo.getValue());
            booking.setEventDate(datePicker.getValue());
            booking.setTables(tables);
            booking.setMenuItems(FXCollections.observableArrayList(menuList.getSelectionModel().getSelectedItems()));
            booking.setNotes(notesArea.getText().trim());
            booking.setTotal(calculateTotal(booking));
            bookingDAO.save(booking);
            showSuccess("Đã lưu booking #" + booking.getId() + " - Tổng tiền: " + CurrencyFormatter.formatVND(booking.getTotal()));
            clearForm();
        } catch (Exception e) {
            showError("Lỗi khi lưu booking: " + e.getMessage());
        }
    }

    private void clearForm() {
        datePicker.setValue(null);
        tableSpinner.getValueFactory().setValue(30);
        notesArea.clear();
        menuList.getSelectionModel().clearSelection();
        totalLabel.setText("0 đ");
        updateSummary();
    }

    /**
     * Cập nhật phần tóm tắt: sảnh, số bàn, danh sách món/combo, tổng tiền.
     */
    private void updateSummary() {
        if (summaryHallLabel == null) {
            return;
        }

        String hallText;
        if (hallCombo.getValue() == null || datePicker.getValue() == null) {
            hallText = "Sảnh: Chưa chọn • Ngày: Chưa chọn • Số bàn: " + tableSpinner.getValue();
        } else {
            Hall h = hallCombo.getValue();
            hallText = String.format("Sảnh: %s • Ngày: %s • Số bàn: %d",
                    h.getName(),
                    datePicker.getValue(),
                    tableSpinner.getValue());
        }
        summaryHallLabel.setText(hallText);

        ObservableList<MenuItem> selectedItems = menuList.getSelectionModel().getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty()) {
            summaryMenuLabel.setText("Thực đơn: Chưa chọn món");
        } else {
            long comboCount = selectedItems.stream().filter(mi -> "combo".equalsIgnoreCase(mi.getCategory())).count();
            long singleCount = selectedItems.size() - comboCount;

            StringBuilder details = new StringBuilder();
            for (MenuItem mi : selectedItems) {
                if ("combo".equalsIgnoreCase(mi.getCategory())) {
                    var comboItems = comboItemDAO.findByComboId(mi.getId());
                    details.append(mi.getTitle()).append(" (Combo: ");
                    if (comboItems.isEmpty()) {
                        details.append("chưa cấu hình món");
                    } else {
                        for (int i = 0; i < comboItems.size(); i++) {
                            var ci = comboItems.get(i);
                            if (i > 0) details.append(", ");
                            details.append(ci.getItem().getTitle())
                                   .append(" x").append(ci.getQuantity());
                        }
                    }
                    details.append(")").append("; ");
                } else {
                    details.append(mi.getTitle()).append(" (Món lẻ); ");
                }
            }

            summaryMenuLabel.setText(String.format("Thực đơn (%d mục: %d combo, %d món lẻ): %s",
                    selectedItems.size(), comboCount, singleCount, details.toString()));
        }

        summaryTotalLabel.setText("Tổng tiền dự kiến: " + totalLabel.getText());
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
