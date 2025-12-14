package com.weddingapp.controller;

import com.weddingapp.dao.BookingDAO;
import com.weddingapp.dao.CustomerDAO;
import com.weddingapp.dao.HallDAO;
import com.weddingapp.dao.MenuDAO;
import com.weddingapp.model.Booking;
import com.weddingapp.model.Customer;
import com.weddingapp.model.Hall;
import com.weddingapp.model.MenuItem;
import com.weddingapp.util.CurrencyFormatter;
import com.weddingapp.util.Validators;
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

    @FXML
    public void initialize() {
        loadData();
        setupMenuList();
        setupListeners();
        tableSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 10));
        if (filterAllBtn != null) {
            filterAllBtn.setSelected(true);
        }
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
        tableSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateTotalPreview());
        menuList.getSelectionModel().getSelectedItems().addListener((ListChangeListener<MenuItem>) change -> updateTotalPreview());
        hallCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTotalPreview();
            updateHallInfo(newVal);
        });
        menuFilterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> applyMenuFilter());
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
            return;
        }
        hallInfoLabel.setText(String.format("%s • %d bàn • %s/bàn", 
            hall.getName(), hall.getCapacity(), CurrencyFormatter.formatVND(hall.getPricePerTable())));
    }

    private void updateTotalPreview() {
        if (hallCombo.getValue() == null) {
            totalLabel.setText("0 đ");
            return;
        }
        Booking temp = new Booking();
        temp.setHall(hallCombo.getValue());
        temp.setMenuItems(menuList.getSelectionModel().getSelectedItems());
        temp.setTables(tableSpinner.getValue());
        double total = calculateTotal(temp);
        totalLabel.setText(CurrencyFormatter.formatVND(total));
    }

    private double calculateTotal(Booking booking) {
        double menuTotal = booking.getMenuItems().stream().mapToDouble(MenuItem::getPrice).sum();
        double perTable = booking.getHall().getPricePerTable() + menuTotal;
        return perTable * booking.getTables();
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
        if (!Validators.isPositive(tables)) {
            showError("Số bàn phải lớn hơn 0");
            tableSpinner.requestFocus();
            return;
        }
        
        if (tables > hallCombo.getValue().getCapacity()) {
            showError("Số bàn vượt quá sức chứa của sảnh (" + hallCombo.getValue().getCapacity() + " bàn)");
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
        tableSpinner.getValueFactory().setValue(10);
        notesArea.clear();
        menuList.getSelectionModel().clearSelection();
        totalLabel.setText("0 đ");
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
