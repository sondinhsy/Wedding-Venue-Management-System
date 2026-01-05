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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

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
    @FXML private Label viewDetailsLink;
    
    private Booking editingBooking = null; // Track if we're editing an existing booking

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
        dialog.getDialogPane().setPrefWidth(850);
        dialog.getDialogPane().setPrefHeight(720);

        // ==== STATE & SELECTION ====
        ObservableList<MenuItem> currentSelected = menuList.getSelectionModel().getSelectedItems();
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
        // Ưu tiên sync với filter hiện tại trên màn Booking
        if (menuFilterGroup != null && menuFilterGroup.getSelectedToggle() != null) {
            Object ud = menuFilterGroup.getSelectedToggle().getUserData();
            if ("combo".equalsIgnoreCase(String.valueOf(ud))) {
                comboModeBtn.setSelected(true);
            } else if ("single".equalsIgnoreCase(String.valueOf(ud))) {
                singleModeBtn.setSelected(true);
            } else {
                // Nếu đang ở "Tất cả" thì dựa vào selection hiện tại
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

        // ==== SCROLLABLE GRID ====
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        VBox sectionsBox = new VBox(16);
        sectionsBox.setStyle("-fx-padding: 16;");
        scrollPane.setContent(sectionsBox);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(selectedComboIds.isEmpty() && selectedSingleIds.isEmpty());

        // Dữ liệu 2 mode
        final java.util.List<MenuItem> comboItems = menus.stream()
                .filter(m -> "combo".equalsIgnoreCase(m.getCategory()))
                .toList();
        // Áp giá cố định và sort theo giá tăng dần
        final java.util.List<MenuItem> limitedComboItems = comboItems.stream()
                .filter(m -> {
                    String t = m.getTitle() != null ? m.getTitle().toLowerCase() : "";
                    return t.contains("thường") || t.contains("thuong")
                            || t.contains("vip")
                            || t.contains("premium");
                })
                .peek(m -> {
                    String t = m.getTitle() != null ? m.getTitle().toLowerCase() : "";
                    if (t.contains("premium")) {
                        m.setPrice(500);
                    } else if (t.contains("vip")) {
                        m.setPrice(300);
                    } else {
                        m.setPrice(200); // THƯỜNG
                    }
                })
                .sorted(java.util.Comparator.comparingDouble(MenuItem::getPrice))
                .limit(3)
                .toList();

        java.util.List<MenuItem> singleItems = menus.stream()
                .filter(m -> !"combo".equalsIgnoreCase(m.getCategory()))
                .toList();

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
                java.util.function.BiConsumer<String, java.util.List<MenuItem>> addSection =
                        (title, items) -> {
                            if (items.isEmpty()) return;

                            Label header = new Label(title);
                            header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");

                            FlowPane flow = new FlowPane(10, 10);
                            flow.setPrefWrapLength(800);

                            for (MenuItem mi : items) {
                                generateStablePrice(mi);
                                boolean isSelected = selectedSingleIds.contains(mi.getId());
                                ToggleButton btn = createSingleCatalogButton(
                                        mi,
                                        isSelected,
                                        selectedSingleIds,
                                        selectedComboIds,
                                        okButton
                                );
                                flow.getChildren().add(btn);
                            }

                            sectionsBox.getChildren().addAll(header, flow);
                        };

                java.util.List<MenuItem> appetizers = singleItems.stream()
                        .filter(m -> "appetizer".equals(m.getGroup()))
                        .sorted(java.util.Comparator.comparingDouble(MenuItem::getPrice))
                        .toList();
                java.util.List<MenuItem> mains = singleItems.stream()
                        .filter(m -> "main".equals(m.getGroup()))
                        .sorted(java.util.Comparator.comparingDouble(MenuItem::getPrice))
                        .toList();
                java.util.List<MenuItem> sides = singleItems.stream()
                        .filter(m -> "side".equals(m.getGroup()))
                        .sorted(java.util.Comparator.comparingDouble(MenuItem::getPrice))
                        .toList();
                java.util.List<MenuItem> drinks = singleItems.stream()
                        .filter(m -> "drink".equals(m.getGroup()))
                        .sorted(java.util.Comparator.comparingDouble(MenuItem::getPrice))
                        .toList();
                java.util.List<MenuItem> desserts = singleItems.stream()
                        .filter(m -> "dessert".equals(m.getGroup()))
                        .sorted(java.util.Comparator.comparingDouble(MenuItem::getPrice))
                        .toList();

                addSection.accept("Khai vị", appetizers);
                addSection.accept("Món chính", mains);
                addSection.accept("Món phụ", sides);
                addSection.accept("Đồ uống", drinks);
                addSection.accept("Tráng miệng", desserts);
            }
        };

        modeGroup.selectedToggleProperty().addListener((obs, old, val) -> {
            String mode = val != null && val.getUserData() != null
                    ? val.getUserData().toString()
                    : "single";

            // Đồng bộ ngược lại ra 3 nút filter ngoài Booking
            if (menuFilterGroup != null) {
                if ("combo".equalsIgnoreCase(mode) && filterComboBtn != null) {
                    menuFilterGroup.selectToggle(filterComboBtn);
                } else if ("single".equalsIgnoreCase(mode) && filterSingleBtn != null) {
                    menuFilterGroup.selectToggle(filterSingleBtn);
                }
                // applyMenuFilter() sẽ tự được gọi qua listener đã gắn trong setupListeners()
            }

            renderMode.accept(mode);
        });

        String initialMode = modeGroup.getSelectedToggle() != null
                && modeGroup.getSelectedToggle().getUserData() != null
                ? modeGroup.getSelectedToggle().getUserData().toString()
                : "single";
        renderMode.accept(initialMode);

        VBox container = new VBox(8, modeBox, scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        dialog.getDialogPane().setContent(container);

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
            // Sync filter buttons theo mode cuối cùng trong popup
            if (modeGroup.getSelectedToggle() != null) {
                Object modeData = modeGroup.getSelectedToggle().getUserData();
                if (menuFilterGroup != null) {
                    if ("combo".equalsIgnoreCase(String.valueOf(modeData)) && filterComboBtn != null) {
                        menuFilterGroup.selectToggle(filterComboBtn);
                    } else if ("single".equalsIgnoreCase(String.valueOf(modeData)) && filterSingleBtn != null) {
                        menuFilterGroup.selectToggle(filterSingleBtn);
                    } else if (filterAllBtn != null) {
                        menuFilterGroup.selectToggle(filterAllBtn);
                    }
                }
            }
        });
    }

    /**
     * Card cho combo: click để mở dialog chi tiết combo, không chọn ngay.
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
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/combo-detail-dialog.fxml"));
                DialogPane pane = loader.load();
                ComboDetailDialogController controller = loader.getController();
                controller.setCombo(item);

                Dialog<ButtonType> detailDialog = new Dialog<>();
                detailDialog.setTitle("Chi tiết combo");
                detailDialog.setDialogPane(pane);

                detailDialog.showAndWait().ifPresent(result -> {
                    if (result.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                        selectedSingleIds.clear();
                        selectedComboIds.clear();
                        selectedComboIds.add(item.getId());
                        btn.setSelected(true);
                        updateCheckIcon(btn, true);
                        if (okButton != null) {
                            okButton.setDisable(false);
                        }
                    } else {
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
     * Card cho món lẻ: chọn trực tiếp, có limit theo group.
     */
    private ToggleButton createSingleCatalogButton(MenuItem item,
                                                   boolean isSelected,
                                                   java.util.Set<Integer> selectedSingleIds,
                                                   java.util.Set<Integer> selectedComboIds,
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
            // ToggleButton tự động toggle, nên isSelected() là state MỚI sau khi click
            boolean isNowSelected = btn.isSelected();

            String g = item.getGroup();
            int currentCount = (int) selectedSingleIds.stream()
                    .map(id -> menus.stream().filter(m -> m.getId() == id).findFirst().orElse(null))
                    .filter(m -> m != null && g != null && g.equals(m.getGroup()))
                    .count();

            int limit = getGroupLimit(g);

            if (isNowSelected) {
                // Đang muốn chọn món lẻ: clear combo trước (combo và món lẻ không thể cùng lúc)
                if (!selectedComboIds.isEmpty()) {
                    selectedComboIds.clear();
                }
                
                // Check limit trước khi add
                if (currentCount >= limit) {
                    // Vượt limit: revert lại
                    btn.setSelected(false);
                    updateCheckIcon(btn, false);
                    showError("Đã đạt tối đa " + limit + " món cho " + getGroupLabel(g));
                    e.consume();
                    return;
                }
                // OK: add vào selection
                selectedSingleIds.add(item.getId());
            } else {
                // Đang muốn bỏ chọn: remove khỏi selection
                selectedSingleIds.remove(item.getId());
            }

            updateCheckIcon(btn, isNowSelected);
            if (okButton != null) {
                boolean hasSelection = !selectedComboIds.isEmpty() || !selectedSingleIds.isEmpty();
                okButton.setDisable(!hasSelection);
            }
        });

        return btn;
    }

    private int getGroupLimit(String group) {
        if (group == null) return 999;
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
        if (group == null) return "";
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
     * Sinh giá món lẻ ngẫu nhiên nhưng ổn định theo tên, theo khoảng cho từng loại.
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
            if (lower.contains("bia") || lower.contains("beer")) {
                price = pickInRange.apply(2, 8);
            } else if (lower.contains("rượu") || lower.contains("vodka") || lower.contains("vang") || lower.contains("wine")) {
                price = pickInRange.apply(10, 60);
            } else {
                price = pickInRange.apply(2, 6);
            }
        } else if ("appetizer".equals(group)) {
            price = pickInRange.apply(8, 20);
        } else if ("side".equals(group)) {
            price = pickInRange.apply(6, 18);
        } else {
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
            Booking booking;
            if (editingBooking != null) {
                // Update existing booking
                booking = editingBooking;
                booking.setCustomer(customerCombo.getValue());
                booking.setHall(hallCombo.getValue());
                booking.setEventDate(datePicker.getValue());
                booking.setTables(tables);
                booking.setMenuItems(FXCollections.observableArrayList(menuList.getSelectionModel().getSelectedItems()));
                booking.setNotes(notesArea.getText().trim());
                booking.setTotal(calculateTotal(booking));
                bookingDAO.save(booking);
                showSuccess("Đã cập nhật booking #" + booking.getId() + " - Tổng tiền: " + CurrencyFormatter.formatVND(booking.getTotal()));
                editingBooking = null;
                if (saveBookingButton != null) {
                    saveBookingButton.setText("✅ Lưu booking");
                }
            } else {
                // Create new booking
                booking = new Booking();
                booking.setCustomer(customerCombo.getValue());
                booking.setHall(hallCombo.getValue());
                booking.setEventDate(datePicker.getValue());
                booking.setTables(tables);
                booking.setMenuItems(FXCollections.observableArrayList(menuList.getSelectionModel().getSelectedItems()));
                booking.setNotes(notesArea.getText().trim());
                booking.setTotal(calculateTotal(booking));
                bookingDAO.save(booking);
                showSuccess("Đã lưu booking #" + booking.getId() + " - Tổng tiền: " + CurrencyFormatter.formatVND(booking.getTotal()));
            }
            clearForm();
        } catch (Exception e) {
            showError("Lỗi khi lưu booking: " + e.getMessage());
        }
    }

    private void clearForm() {
        editingBooking = null;
        datePicker.setValue(null);
        tableSpinner.getValueFactory().setValue(30);
        notesArea.clear();
        menuList.getSelectionModel().clearSelection();
        totalLabel.setText("0 đ");
        if (saveBookingButton != null) {
            saveBookingButton.setText("✅ Lưu booking");
        }
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

    @FXML
    public void handleViewDetailsMouseEnter() {
        if (viewDetailsLink != null) {
            viewDetailsLink.setStyle("-fx-font-size: 10px; -fx-font-style: italic; -fx-text-fill: #2563eb; -fx-underline: true; -fx-cursor: hand; -fx-padding: 4 0 0 0;");
        }
    }

    @FXML
    public void handleViewDetailsMouseExit() {
        if (viewDetailsLink != null) {
            viewDetailsLink.setStyle("-fx-font-size: 10px; -fx-font-style: italic; -fx-text-fill: #3b82f6; -fx-underline: true; -fx-cursor: hand; -fx-padding: 4 0 0 0;");
        }
    }

    @FXML
    public void handleViewBookingDetails() {
        // Kiểm tra xem đã có đủ thông tin để hiển thị chi tiết chưa
        if (hallCombo.getValue() == null) {
            showError("Vui lòng chọn sảnh để xem chi tiết");
            return;
        }
        
        if (datePicker.getValue() == null) {
            showError("Vui lòng chọn ngày tổ chức để xem chi tiết");
            return;
        }
        
        // Tính tổng tiền hiện tại
        Booking tempBooking = new Booking();
        tempBooking.setHall(hallCombo.getValue());
        tempBooking.setMenuItems(menuList.getSelectionModel().getSelectedItems());
        tempBooking.setTables(tableSpinner.getValue());
        double billTotal = calculateTotal(tempBooking);
        
        // Tính 20% deposit
        double depositRequired = billTotal * 0.20;
        
        // Kiểm tra nếu đang edit booking cũ
        double paidAmount = 0.0;
        Booking.PaymentStatus currentStatus = Booking.PaymentStatus.PENDING;
        
        if (editingBooking != null) {
            paidAmount = editingBooking.getPaidAmount();
            currentStatus = editingBooking.getPaymentStatus();
        }
        
        // Tính toán theo status
        double actualPaidAmount = 0.0;
        if (currentStatus == Booking.PaymentStatus.PENDING) {
            actualPaidAmount = 0.0;
        } else if (currentStatus == Booking.PaymentStatus.IN_PROGRESS) {
            actualPaidAmount = depositRequired;
        } else if (currentStatus == Booking.PaymentStatus.COMPLETED) {
            actualPaidAmount = paidAmount > 0 ? paidAmount : depositRequired;
        }
        
        // Tạo dialog hiển thị chi tiết
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết đơn đặt chỗ");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(500, 400);
        
        VBox content = new VBox(16);
        content.setStyle("-fx-padding: 20;");
        
        // Header
        Label header = new Label("📋 Chi tiết đơn đặt chỗ");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
        content.getChildren().add(header);
        
        // Thông tin sảnh
        VBox hallInfo = new VBox(4);
        Label hallLabel = new Label("Sảnh đã chọn:");
        hallLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        Label hallValue = new Label(hallCombo.getValue().getName());
        hallValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        hallInfo.getChildren().addAll(hallLabel, hallValue);
        content.getChildren().add(hallInfo);
        
        // Tổng tiền
        VBox totalInfo = new VBox(4);
        Label totalLabel = new Label("Tổng số tiền:");
        totalLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        Label totalValue = new Label(CurrencyFormatter.formatVND(billTotal));
        totalValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
        totalInfo.getChildren().addAll(totalLabel, totalValue);
        content.getChildren().add(totalInfo);
        
        // Hiển thị theo status
        if (currentStatus == Booking.PaymentStatus.PENDING) {
            // pending: chỉ hiển thị tiền cần ứng trước = 0
            VBox depositInfo = new VBox(4);
            Label depositLabel = new Label("Tiền cần ứng trước:");
            depositLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            Label depositValue = new Label(CurrencyFormatter.formatVND(0.0));
            depositValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            depositInfo.getChildren().addAll(depositLabel, depositValue);
            content.getChildren().add(depositInfo);
        } else if (currentStatus == Booking.PaymentStatus.IN_PROGRESS) {
            // in_: hiển thị tiền ứng trước và số tiền còn lại
            VBox paidInfo = new VBox(4);
            Label paidLabel = new Label("Tiền ứng trước:");
            paidLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            Label paidValue = new Label(CurrencyFormatter.formatVND(actualPaidAmount));
            paidValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #059669;");
            paidInfo.getChildren().addAll(paidLabel, paidValue);
            content.getChildren().add(paidInfo);
            
            double remaining = Math.max(0, billTotal - actualPaidAmount);
            VBox remainingInfo = new VBox(4);
            Label remainingLabel = new Label("Số tiền còn lại cần ứng:");
            remainingLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            Label remainingValue = new Label(CurrencyFormatter.formatVND(remaining));
            remainingValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
            remainingInfo.getChildren().addAll(remainingLabel, remainingValue);
            content.getChildren().add(remainingInfo);
        } else if (currentStatus == Booking.PaymentStatus.COMPLETED) {
            // completed: hiển thị tiền ứng trước và "Đã thanh toán đủ"
            VBox paidInfo = new VBox(4);
            Label paidLabel = new Label("Tiền ứng trước:");
            paidLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            Label paidValue = new Label(CurrencyFormatter.formatVND(actualPaidAmount));
            paidValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #059669;");
            paidInfo.getChildren().addAll(paidLabel, paidValue);
            content.getChildren().add(paidInfo);
            
            Label completedLabel = new Label("Đã thanh toán đủ");
            completedLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
            content.getChildren().add(completedLabel);
        }
        
        // Trạng thái hiện tại (nếu đang edit)
        if (editingBooking != null) {
            VBox statusInfo = new VBox(4);
            Label statusLabel = new Label("Trạng thái thanh toán:");
            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            Label statusValue = new Label(currentStatus.getDisplayName());
            statusValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            statusInfo.getChildren().addAll(statusLabel, statusValue);
            content.getChildren().add(statusInfo);
        }
        
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    /**
     * Load booking data into form for editing
     */
    public void loadBookingForEdit(Booking booking) {
        if (booking == null) {
            return;
        }
        
        try {
            // Store the booking being edited
            editingBooking = booking;
            
            // Set customer
            customerCombo.setValue(booking.getCustomer());
            
            // Set hall
            hallCombo.setValue(booking.getHall());
            
            // Set date
            datePicker.setValue(booking.getEventDate());
            
            // Set tables
            tableSpinner.getValueFactory().setValue(booking.getTables());
            
            // Set notes
            notesArea.setText(booking.getNotes() != null ? booking.getNotes() : "");
            
            // Set menu items
            menuList.getSelectionModel().clearSelection();
            for (MenuItem menuItem : booking.getMenuItems()) {
                // Find menu item by ID since we might have different instances
                for (int i = 0; i < menus.size(); i++) {
                    if (menus.get(i).getId() == menuItem.getId()) {
                        menuList.getSelectionModel().select(i);
                        break;
                    }
                }
            }
            
            // Update total preview
            updateTotalPreview();
            
            // Change save button text to indicate editing
            if (saveBookingButton != null) {
                saveBookingButton.setText("💾 Cập nhật booking");
            }
        } catch (Exception e) {
            showError("Lỗi khi tải dữ liệu booking: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
