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
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MainController {
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final HallDAO hallDAO = new HallDAO();
    private final MenuDAO menuDAO = new MenuDAO();
    private final BookingDAO bookingDAO = new BookingDAO();

    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final ObservableList<MenuItem> menus = FXCollections.observableArrayList();
    private final FilteredList<MenuItem> filteredMenus = new FilteredList<>(menus);
    private final ObservableList<Booking> bookings = FXCollections.observableArrayList();

    @FXML private TabPane tabPane;
    @FXML private Label customerCountLabel;
    @FXML private Label bookingCountLabel;
    @FXML private Label revenueLabel;
    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, String> colBookingCustomer;
    @FXML private TableColumn<Booking, String> colBookingHall;
    @FXML private TableColumn<Booking, String> colBookingDate;
    @FXML private TableColumn<Booking, Number> colBookingTables;
    @FXML private TableColumn<Booking, String> colBookingTotal;
    @FXML private TableColumn<Booking, String> colBookingMenu;
    @FXML private PieChart hallRevenueChart;
    @FXML private PieChart menuRevenueChart;

    @FXML private ComboBox<Customer> customerCombo;
    @FXML private ComboBox<Hall> hallCombo;
    @FXML private DatePicker datePicker;
    @FXML private Spinner<Integer> tableSpinner;
    @FXML private TextArea notesArea;
    @FXML private ListView<MenuItem> menuList;
    @FXML private Label totalLabel;
    @FXML private Label hallInfoLabel;
    @FXML private RadioButton filterAllBtn;
    @FXML private RadioButton filterComboBtn;
    @FXML private RadioButton filterSingleBtn;
    @FXML private ToggleGroup menuFilterGroup;

    @FXML private TextField customerNameField;
    @FXML private TextField customerPhoneField;
    @FXML private TextField customerEmailField;
    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, String> colCustomerName;
    @FXML private TableColumn<Customer, String> colCustomerPhone;
    @FXML private TableColumn<Customer, String> colCustomerEmail;

    @FXML private TextField menuTitleField;
    @FXML private TextField menuPriceField;
    @FXML private TableView<MenuItem> menuTable;
    @FXML private TableColumn<MenuItem, String> colMenuTitle;
    @FXML private TableColumn<MenuItem, String> colMenuPrice;

    @FXML private TextArea exportStatusArea;

    @FXML private Button navDashboard;
    @FXML private Button navBooking;
    @FXML private Button navCustomers;
    @FXML private Button navMenu;
    @FXML private Button navReport;

    @FXML
    public void initialize() {
        setupTables();
        setupMenuList();
        loadStaticLists();
        reloadData();
        tableSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateTotalPreview());
        menuList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        menuList.getSelectionModel().getSelectedItems().addListener((ListChangeListener<MenuItem>) change -> updateTotalPreview());
        hallCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTotalPreview());
        hallCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateHallInfo(newVal));
        menuFilterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> applyMenuFilter());
    }

    private void setupTables() {
        colBookingCustomer.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCustomer().getName()));
        colBookingHall.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getHall().getName()));
        colBookingDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEventDate().format(DateTimeFormatter.ISO_DATE)));
        colBookingTables.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getTables()));
        colBookingTotal.setCellValueFactory(cell -> 
            new SimpleStringProperty(CurrencyFormatter.formatVND(cell.getValue().getTotal())));
        colBookingMenu.setCellValueFactory(cell -> {
            String menuTitles = cell.getValue().getMenuItems().stream()
                    .map(MenuItem::getTitle)
                    .collect(Collectors.joining(", "));
            return new SimpleStringProperty(menuTitles);
        });
        bookingTable.setItems(bookings);

        colCustomerName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        colCustomerPhone.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPhone()));
        colCustomerEmail.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEmail()));
        customerTable.setItems(customers);

        colMenuTitle.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
        colMenuPrice.setCellValueFactory(cell -> 
            new SimpleStringProperty(CurrencyFormatter.formatVND(cell.getValue().getPrice())));
        menuTable.setItems(menus);
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
        if (menuFilterGroup != null && menuFilterGroup.getSelectedToggle() == null && filterAllBtn != null) {
            filterAllBtn.setSelected(true);
        }
    }

    private void loadStaticLists() {
        customers.setAll(customerDAO.findAll());
        menus.setAll(menuDAO.findAll());
        customerCombo.setItems(customers);
        hallCombo.setItems(FXCollections.observableArrayList(hallDAO.findAll()));
        applyMenuFilter();
        // Mặc định tối thiểu 30 bàn
        tableSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 500, 30));
        updateTotalsHeader();
    }

    @FXML
    public void reloadData() {
        bookings.setAll(bookingDAO.findAll());
        loadStaticLists();
        updateTotalsHeader();
        updateCharts();
    }

    private void updateTotalsHeader() {
        customerCountLabel.setText(String.valueOf(customers.size()));
        bookingCountLabel.setText(String.valueOf(bookings.size()));
        double revenue = bookings.stream().mapToDouble(Booking::getTotal).sum();
        revenueLabel.setText(CurrencyFormatter.formatVND(revenue));
    }

    private void updateCharts() {
        hallRevenueChart.setData(buildPieDataByHall());
        menuRevenueChart.setData(buildPieDataByMenu());
    }

    private ObservableList<PieChart.Data> buildPieDataByHall() {
        Map<String, Double> totals = new HashMap<>();
        for (Booking b : bookings) {
            totals.merge(b.getHall().getName(), b.getTotal(), Double::sum);
        }
        if (totals.isEmpty()) {
            return FXCollections.observableArrayList(new PieChart.Data("Chưa có dữ liệu", 1));
        }
        return totals.entrySet().stream()
                .map(e -> new PieChart.Data(e.getKey(), e.getValue()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), FXCollections::observableArrayList));
    }

    private ObservableList<PieChart.Data> buildPieDataByMenu() {
        Map<String, Double> totals = new HashMap<>();
        for (Booking b : bookings) {
            if (b.getMenuItems().isEmpty()) {
                totals.merge("Chưa chọn menu", b.getTotal(), Double::sum);
            }
            b.getMenuItems().forEach(mi ->
                    totals.merge(mi.getTitle(), mi.getPrice() * b.getTables(), Double::sum));
        }
        if (totals.isEmpty()) {
            return FXCollections.observableArrayList(new PieChart.Data("Chưa có dữ liệu", 1));
        }
        return totals.entrySet().stream()
                .map(e -> new PieChart.Data(e.getKey(), e.getValue()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), FXCollections::observableArrayList));
    }

    @FXML
    public void handleAddCustomer() {
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
            Customer customer = new Customer();
            customer.setName(name);
            customer.setPhone(phone.isEmpty() ? null : phone);
            customer.setEmail(email.isEmpty() ? null : email);
            customerDAO.save(customer);
            customers.add(customer);
            customerNameField.clear();
            customerPhoneField.clear();
            customerEmailField.clear();
            updateTotalsHeader();
            updateCharts();
            showSuccess("Đã thêm khách hàng: " + name);
        } catch (Exception e) {
            showError("Lỗi khi thêm khách hàng: " + e.getMessage());
        }
    }

    @FXML
    public void handleAddMenu() {
        String title = menuTitleField.getText().trim();
        String priceText = menuPriceField.getText().trim();
        
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
            // Hỗ trợ parse cả số thường và format VNĐ
            price = CurrencyFormatter.parseVND(priceText);
            if (price == 0.0 && !priceText.equals("0")) {
                price = Double.parseDouble(priceText.replace(".", "").replace(",", "."));
            }
        } catch (NumberFormatException ex) {
            showError("Giá không hợp lệ. Vui lòng nhập số. Ví dụ: 1500000 hoặc 1.500.000");
            menuPriceField.requestFocus();
            return;
        }
        
        if (!Validators.isPositive(price)) {
            showError("Giá phải lớn hơn 0");
            menuPriceField.requestFocus();
            return;
        }
        
        try {
            MenuItem item = new MenuItem();
            item.setTitle(title);
            item.setPrice(price);
            menuDAO.save(item);
            menus.add(item);
            menuTitleField.clear();
            menuPriceField.clear();
            updateTotalPreview();
            updateCharts();
            showSuccess("Đã thêm món: " + title);
        } catch (Exception e) {
            showError("Lỗi khi thêm món: " + e.getMessage());
        }
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
            bookings.add(0, booking);
            updateTotalsHeader();
            updateCharts();
            showSuccess("Đã lưu booking #" + booking.getId() + " - Tổng tiền: " + CurrencyFormatter.formatVND(booking.getTotal()));
            tabPane.getSelectionModel().selectFirst();
            clearBookingForm();
        } catch (Exception e) {
            showError("Lỗi khi lưu booking: " + e.getMessage());
        }
    }

    private double calculateTotal(Booking booking) {
        // Tổng giá món/combo trên mỗi mâm
        double perTableMenuPrice = booking.getMenuItems().stream()
                .mapToDouble(MenuItem::getPrice)
                .sum();
        // Phí sảnh cố định (50$ cho mỗi sảnh mặc định, hoặc theo cấu hình)
        double hallFee = booking.getHall().getPricePerTable();
        return perTableMenuPrice * booking.getTables() + hallFee;
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

    private void applyMenuFilter() {
        String filter = "all";
        if (menuFilterGroup != null && menuFilterGroup.getSelectedToggle() != null) {
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
        if (hallInfoLabel == null) return;
        if (hall == null) {
            hallInfoLabel.setText("Chọn sảnh để xem chi tiết");
            return;
        }
        hallInfoLabel.setText(String.format("%s • %d bàn • Phí sảnh: %s", 
            hall.getName(), hall.getCapacity(), CurrencyFormatter.formatVND(hall.getPricePerTable())));
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

    private void clearBookingForm() {
        datePicker.setValue(null);
        tableSpinner.getValueFactory().setValue(30); // Minimum value is 30
        notesArea.clear();
        menuList.getSelectionModel().clearSelection();
        totalLabel.setText("0 đ");
    }

    @FXML
    public void handleExportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn nơi lưu CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = chooser.showSaveDialog(tabPane.getScene().getWindow());
        if (file == null) {
            return;
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Id,Customer,Hall,Date,Tables,Total,Menu,Notes\n");
            for (Booking b : bookings) {
                String menuTitles = b.getMenuItems().stream().map(MenuItem::getTitle).collect(Collectors.joining("|"));
                writer.write(String.format("%d,%s,%s,%s,%d,%s,%s,%s\n",
                        b.getId(),
                        sanitize(b.getCustomer().getName()),
                        sanitize(b.getHall().getName()),
                        b.getEventDate().format(DateTimeFormatter.ISO_DATE),
                        b.getTables(),
                        CurrencyFormatter.formatVND(b.getTotal()),
                        sanitize(menuTitles),
                        sanitize(b.getNotes())));
            }
            exportStatusArea.setText("Đã xuất: " + file.getAbsolutePath());
        } catch (IOException ex) {
            exportStatusArea.setText("Lỗi xuất file: " + ex.getMessage());
        }
    }

    private String sanitize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace(",", ";");
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

    // Nav buttons to mimic top bar shortcuts
    @FXML
    public void showDashboard() {
        tabPane.getSelectionModel().select(0);
    }

    @FXML
    public void showBooking() {
        tabPane.getSelectionModel().select(1);
    }

    @FXML
    public void showCustomers() {
        tabPane.getSelectionModel().select(2);
    }

    @FXML
    public void showMenu() {
        tabPane.getSelectionModel().select(3);
    }

    @FXML
    public void showReports() {
        tabPane.getSelectionModel().select(4);
    }
}

