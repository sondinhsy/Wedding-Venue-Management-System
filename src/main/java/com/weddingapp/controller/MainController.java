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
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
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
        dialog.getDialogPane().setPrefSize(850, 720);

        // ==== STATE & SELECTION ====
        ObservableList<MenuItem> currentSelected = menuList.getSelectionModel().getSelectedItems();
        Map<Integer, MenuItem> menuById = menus.stream()
                .collect(Collectors.toMap(MenuItem::getId, m -> m, (a, b) -> a));

        // Lưu selection theo ID, tách rõ combo vs món lẻ
        java.util.Set<Integer> selectedComboIds = new java.util.HashSet<>();
        java.util.Set<Integer> selectedSingleIds = new java.util.HashSet<>();
        for (MenuItem item : currentSelected) {
            if ("combo".equalsIgnoreCase(item.getCategory())) {
                selectedComboIds.add(item.getId());
            } else {
                selectedSingleIds.add(item.getId());
            }
        }

        // ==== MODE TOGGLE (COMBO / MÓN LẺ) ====
        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton comboModeBtn = new RadioButton("Combo");
        comboModeBtn.setToggleGroup(modeGroup);
        comboModeBtn.setUserData("combo");
        RadioButton singleModeBtn = new RadioButton("Món lẻ");
        singleModeBtn.setToggleGroup(modeGroup);
        singleModeBtn.setUserData("single");
        // Ưu tiên sync với filter hiện tại ở màn Booking (tab chính)
        if (menuFilterGroup != null && menuFilterGroup.getSelectedToggle() != null) {
            Object ud = menuFilterGroup.getSelectedToggle().getUserData();
            if ("combo".equalsIgnoreCase(String.valueOf(ud))) {
                comboModeBtn.setSelected(true);
            } else if ("single".equalsIgnoreCase(String.valueOf(ud))) {
                singleModeBtn.setSelected(true);
            } else {
                // Nếu đang là "Tất cả" thì dựa vào selection hiện tại
                if (!selectedComboIds.isEmpty()) {
                    comboModeBtn.setSelected(true);
                } else {
                    singleModeBtn.setSelected(true);
                }
            }
        } else if (!selectedComboIds.isEmpty()) {
            comboModeBtn.setSelected(true);
        } else {
            singleModeBtn.setSelected(true);
        }

        HBox modeBox = new HBox(12, new Label("Chế độ:"), comboModeBtn, singleModeBtn);
        modeBox.setStyle("-fx-padding: 0 0 8 0;");

        // ==== SCROLLABLE LAYOUT (grid card) ====
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        VBox sectionsBox = new VBox(16);
        sectionsBox.setStyle("-fx-padding: 16;");
        scrollPane.setContent(sectionsBox);

        // OK button chỉ enable khi có selection
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(selectedComboIds.isEmpty() && selectedSingleIds.isEmpty());

        // Chuẩn bị dữ liệu cho 2 mode
        final List<MenuItem> comboItems = menus.stream()
                .filter(m -> "combo".equalsIgnoreCase(m.getCategory()))
                .collect(Collectors.toList());
        // Đảm bảo chỉ còn đúng 3 combo (THƯỜNG, VIP, PREMIUM) nếu tồn tại
        final List<MenuItem> limitedComboItems = comboItems.stream()
            .filter(m -> {
                String t = m.getTitle() != null ? m.getTitle().toLowerCase() : "";
                return t.contains("thường") || t.contains("thuong")
                        || t.contains("vip")
                        || t.contains("premium");
            })
            .limit(3)
            .collect(Collectors.toList());

        List<MenuItem> singleItems = menus.stream()
                .filter(m -> !"combo".equalsIgnoreCase(m.getCategory()))
                .collect(Collectors.toList());

        // Helper render theo mode
        java.util.function.Consumer<String> renderMode = mode -> {
            sectionsBox.getChildren().clear();

            if ("combo".equalsIgnoreCase(mode)) {
                Label header = new Label("Combo cố định");
                header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");

                FlowPane flow = new FlowPane(10, 10);
                flow.setPrefWrapLength(800);

                for (MenuItem combo : limitedComboItems) {
                    ToggleButton btn = createComboCatalogButton(
                            combo,
                            selectedComboIds.contains(combo.getId()),
                            selectedComboIds,
                            selectedSingleIds,
                            okButton
                    );
                    flow.getChildren().add(btn);
                }

                sectionsBox.getChildren().addAll(header, flow);
            } else {
                // Món lẻ: chia rõ theo group với header
                java.util.function.BiConsumer<String, List<MenuItem>> addSection =
                        (title, items) -> {
                            if (items.isEmpty()) return;

                            Label header = new Label(title);
                            header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");

                            FlowPane flow = new FlowPane(10, 10);
                            flow.setPrefWrapLength(800);

                            for (MenuItem mi : items) {
                                // Giá món lẻ: generate ngẫu nhiên ổn định theo tên
                                generateStablePrice(mi);
                                boolean isSelected = selectedSingleIds.contains(mi.getId());
                                ToggleButton btn = createSingleCatalogButton(
                                        mi,
                                        isSelected,
                                        selectedSingleIds,
                                        okButton
                                );
                                flow.getChildren().add(btn);
                            }

                            sectionsBox.getChildren().addAll(header, flow);
                        };

                List<MenuItem> appetizers = singleItems.stream()
                        .filter(m -> "appetizer".equals(m.getGroup()))
                        .collect(Collectors.toList());
                List<MenuItem> mains = singleItems.stream()
                        .filter(m -> "main".equals(m.getGroup()))
                        .collect(Collectors.toList());
                List<MenuItem> sides = singleItems.stream()
                        .filter(m -> "side".equals(m.getGroup()))
                        .collect(Collectors.toList());
                List<MenuItem> drinks = singleItems.stream()
                        .filter(m -> "drink".equals(m.getGroup()))
                        .collect(Collectors.toList());
                List<MenuItem> desserts = singleItems.stream()
                        .filter(m -> "dessert".equals(m.getGroup()))
                        .collect(Collectors.toList());

                addSection.accept("Khai vị", appetizers);
                addSection.accept("Món chính", mains);
                addSection.accept("Món phụ", sides);
                addSection.accept("Đồ uống", drinks);
                addSection.accept("Tráng miệng", desserts);
            }
        };

        // Lắng nghe đổi mode
        modeGroup.selectedToggleProperty().addListener((obs, old, val) -> {
            String mode = val != null && val.getUserData() != null
                    ? val.getUserData().toString()
                    : "single";

            // Đồng bộ ngược lại ra 3 nút filter bên ngoài (Tất cả / Combo / Món lẻ)
            if (menuFilterGroup != null) {
                if ("combo".equalsIgnoreCase(mode) && filterComboBtn != null) {
                    menuFilterGroup.selectToggle(filterComboBtn);
                } else if ("single".equalsIgnoreCase(mode) && filterSingleBtn != null) {
                    menuFilterGroup.selectToggle(filterSingleBtn);
                } else if (filterAllBtn != null) {
                    menuFilterGroup.selectToggle(filterAllBtn);
                }
                // applyMenuFilter() đã được gắn listener trong initialize()
            }

            renderMode.accept(mode);
        });

        // Render lần đầu
        String initialMode = modeGroup.getSelectedToggle() != null
                && modeGroup.getSelectedToggle().getUserData() != null
                ? modeGroup.getSelectedToggle().getUserData().toString()
                : "single";
        renderMode.accept(initialMode);

        VBox mainContainer = new VBox(8);
        mainContainer.getChildren().addAll(modeBox, scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        dialog.getDialogPane().setContent(mainContainer);

        // Show dialog và apply chọn
        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                menuList.getSelectionModel().clearSelection();
                for (MenuItem item : menus) {
                    if (selectedComboIds.contains(item.getId()) || selectedSingleIds.contains(item.getId())) {
                        int idx = menus.indexOf(item);
                        if (idx >= 0) {
                            menuList.getSelectionModel().select(idx);
                        }
                    }
                }
                updateTotalPreview();
            }
            // Sau khi đóng popup, sync lại filter theo mode cuối cùng
            if (modeGroup.getSelectedToggle() != null && menuFilterGroup != null) {
                Object md = modeGroup.getSelectedToggle().getUserData();
                if ("combo".equalsIgnoreCase(String.valueOf(md)) && filterComboBtn != null) {
                    menuFilterGroup.selectToggle(filterComboBtn);
                } else if ("single".equalsIgnoreCase(String.valueOf(md)) && filterSingleBtn != null) {
                    menuFilterGroup.selectToggle(filterSingleBtn);
                }
            }
        });
    }

    /**
     * Card cho một combo (click mở dialog chi tiết combo, không chọn ngay).
     */
    private ToggleButton createComboCatalogButton(MenuItem item,
                                                  boolean isSelected,
                                                  java.util.Set<Integer> selectedComboIds,
                                                  java.util.Set<Integer> selectedSingleIds,
                                                  Button okButton) {
        ToggleButton btn = new ToggleButton();
        btn.getStyleClass().add("card-btn");
        btn.setUserData(item);
        btn.setSelected(isSelected);

        HBox contentBox = new HBox(8);
        contentBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Tick icon
        Label checkIcon = new Label(isSelected ? "✓" : "");
        checkIcon.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #059669;");
        checkIcon.setPrefWidth(16);

        VBox textBox = new VBox(4);
        Label title = new Label(item.getTitle());
        title.setStyle("-fx-font-weight: bold;");
        Label price = new Label(String.format("$%.0f", item.getPrice()));
        Label tag = new Label("Combo");
        tag.getStyleClass().add("chip");
        textBox.getChildren().addAll(title, price, tag);

        contentBox.getChildren().addAll(checkIcon, textBox);
        btn.setGraphic(contentBox);

        btn.setOnAction(e -> {
            // Mở dialog chi tiết combo, không chọn ngay từ card
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/combo-detail-dialog.fxml"));
                DialogPane pane = loader.load();
                com.weddingapp.controller.ComboDetailDialogController controller = loader.getController();
                controller.setCombo(item);

                Dialog<ButtonType> detailDialog = new Dialog<>();
                detailDialog.setTitle("Chi tiết combo");
                detailDialog.setDialogPane(pane);

                detailDialog.showAndWait().ifPresent(result -> {
                    if (result.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                        // Người dùng xác nhận "Chọn combo này"
                        selectedSingleIds.clear();
                        selectedComboIds.clear();
                        selectedComboIds.add(item.getId());
                        btn.setSelected(true);
                        updateCheckIcon(btn, true);
                        if (okButton != null) {
                            okButton.setDisable(false);
                        }
                    } else {
                        // Không chọn combo -> giữ trạng thái cũ
                        btn.setSelected(selectedComboIds.contains(item.getId()));
                        updateCheckIcon(btn, btn.isSelected());
                    }
                });
            } catch (IOException ex) {
                showError("Không thể mở chi tiết combo: " + ex.getMessage());
                btn.setSelected(false);
                updateCheckIcon(btn, false);
            }
        });

        return btn;
    }

    /**
     * Card cho một món lẻ (select trực tiếp, có limit theo category).
     */
    private ToggleButton createSingleCatalogButton(MenuItem item,
                                                   boolean isSelected,
                                                   java.util.Set<Integer> selectedSingleIds,
                                                   Button okButton) {
        ToggleButton btn = new ToggleButton();
        btn.getStyleClass().add("card-btn");
        btn.setUserData(item);
        btn.setSelected(isSelected);

        HBox contentBox = new HBox(8);
        contentBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label checkIcon = new Label(isSelected ? "✓" : "");
        checkIcon.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #059669;");
        checkIcon.setPrefWidth(16);

        VBox textBox = new VBox(4);
        Label title = new Label(item.getTitle());
        title.setStyle("-fx-font-weight: bold;");
        Label price = new Label(String.format("$%.0f", item.getPrice()));
        String group = item.getGroup();
        Label tag = new Label(getGroupLabel(group));
        tag.getStyleClass().add("chip");
        textBox.getChildren().addAll(title, price, tag);

        contentBox.getChildren().addAll(checkIcon, textBox);
        btn.setGraphic(contentBox);

        btn.setOnAction(e -> {
            boolean wasSelected = btn.isSelected();

            String g = item.getGroup();
            int currentCount = (int) selectedSingleIds.stream()
                    .map(id -> menus.stream().filter(m -> m.getId() == id).findFirst().orElse(null))
                    .filter(m -> m != null && g.equals(m.getGroup()))
                    .count();

            int limit = getGroupLimit(g);

            if (wasSelected) {
                // Bỏ chọn
                selectedSingleIds.remove(item.getId());
                btn.setSelected(false);
            } else {
                if (currentCount >= limit) {
                    btn.setSelected(false);
                    showError("Đã đạt tối đa " + limit + " cho " + getGroupLabel(g));
                    e.consume();
                    return;
                }
                selectedSingleIds.add(item.getId());
                btn.setSelected(true);
            }

            updateCheckIcon(btn, btn.isSelected());
            if (okButton != null) {
                boolean hasSelection = !selectedSingleIds.isEmpty();
                okButton.setDisable(!hasSelection);
            }
        });

        return btn;
    }
    
    private int getGroupLimit(String group) {
        switch (group) {
            case "appetizer": return 2;
            case "main": return 6;
            case "side": return 2;
            case "drink": return 1;
            case "dessert": return 1;
            default: return 999;
        }
    }
    
    private String getGroupLabel(String group) {
        switch (group) {
            case "appetizer": return "Khai vị";
            case "main": return "Món chính";
            case "side": return "Món phụ";
            case "drink": return "Đồ uống";
            case "dessert": return "Tráng miệng";
            default: return group;
        }
    }
    
    private void updateCheckIcon(ToggleButton btn, boolean selected) {
        HBox contentBox = (HBox) btn.getGraphic();
        Label checkIcon = (Label) contentBox.getChildren().get(0);
        checkIcon.setText(selected ? "✓" : "");
    }

    /**
     * Sinh giá món lẻ ngẫu nhiên nhưng ổn định theo tên, theo khoảng giá từng loại.
     */
    private double generateStablePrice(MenuItem item) {
        String name = item.getTitle() != null ? item.getTitle() : "";
        int hash = Math.abs(name.hashCode());
        String group = item.getGroup();
        String lower = name.toLowerCase();

        java.util.function.BiFunction<Integer, Integer, Double> pickInRange = (min, max) -> {
            int range = max - min + 1;
            return (double) (min + (hash % range));
        };

        double price;
        if ("drink".equals(group)) {
            // Đồ uống: chia nhỏ theo loại
            if (lower.contains("bia") || lower.contains("beer")) {
                price = pickInRange.apply(2, 8);
            } else if (lower.contains("rượu") || lower.contains("vodka") || lower.contains("vang") || lower.contains("wine")) {
                price = pickInRange.apply(10, 60);
            } else {
                // Nước ngọt
                price = pickInRange.apply(2, 6);
            }
        } else if ("appetizer".equals(group)) {
            price = pickInRange.apply(8, 20);
        } else if ("side".equals(group)) {
            price = pickInRange.apply(6, 18);
        } else {
            // Món chính
            boolean premium = lower.contains("hải sản") || lower.contains("seafood")
                    || lower.contains("bò mỹ") || lower.contains("us beef")
                    || lower.contains("wagyu")
                    || lower.contains("tôm hùm") || lower.contains("lobster");
            if (premium) {
                price = pickInRange.apply(35, 120);
            } else {
                price = pickInRange.apply(15, 40);
            }
        }

        item.setPrice(price);
        return price;
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


