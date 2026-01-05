package com.weddingapp.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private static final String DB_FOLDER = "data";
    private static final String DB_FILE = "wedding.db";
    private static Path dbPath;
    private static String jdbcUrl;

    static {
        // Sử dụng đường dẫn tuyệt đối để xử lý tốt với tên folder có khoảng trắng
        Path projectDir = Paths.get(System.getProperty("user.dir"));
        dbPath = projectDir.resolve(DB_FOLDER).resolve(DB_FILE);
        // Chuyển đổi Path sang String an toàn cho JDBC URL (xử lý khoảng trắng)
        jdbcUrl = "jdbc:sqlite:" + dbPath.toString().replace("\\", "/");
    }

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    public static void initialize() {
        try {
            // Tạo thư mục data nếu chưa tồn tại
            Files.createDirectories(dbPath.getParent());
            createTables();
            seedData();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize database", ex);
        }
    }

    private static void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS users(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL UNIQUE,
                        password TEXT NOT NULL,
                        full_name TEXT NOT NULL,
                        role TEXT DEFAULT 'staff'
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS customers(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        phone TEXT,
                        email TEXT
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS halls(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        capacity INTEGER,
                        price_per_table REAL
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS menu_items(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        price REAL NOT NULL,
                        category TEXT DEFAULT 'single'
                    )
                    """);
            // Bảng chi tiết thành phần combo: combo_id tham chiếu menu_items (category = 'combo'),
            // item_id tham chiếu menu_items (thường là 'single'), quantity là số lượng món trong combo.
            st.execute("""
                    CREATE TABLE IF NOT EXISTS combo_items(
                        combo_id INTEGER NOT NULL,
                        item_id INTEGER NOT NULL,
                        quantity INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(combo_id, item_id),
                        FOREIGN KEY(combo_id) REFERENCES menu_items(id),
                        FOREIGN KEY(item_id) REFERENCES menu_items(id)
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS bookings(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        customer_id INTEGER NOT NULL,
                        hall_id INTEGER NOT NULL,
                        event_date TEXT NOT NULL,
                        tables INTEGER NOT NULL,
                        total REAL NOT NULL,
                        paid_amount REAL DEFAULT 0.0,
                        notes TEXT,
                        FOREIGN KEY(customer_id) REFERENCES customers(id),
                        FOREIGN KEY(hall_id) REFERENCES halls(id)
                    )
                    """);
            try {
                st.execute("ALTER TABLE bookings ADD COLUMN paid_amount REAL DEFAULT 0.0");
            } catch (SQLException ignore) {
                // column already exists
            }
            try {
                st.execute("ALTER TABLE bookings ADD COLUMN payment_status TEXT DEFAULT 'PENDING'");
            } catch (SQLException ignore) {
                // column already exists
            }
            try {
                st.execute("ALTER TABLE bookings ADD COLUMN booking_code TEXT");
            } catch (SQLException ignore) {
                // column already exists
            }
            st.execute("""
                    CREATE TABLE IF NOT EXISTS booking_menu(
                        booking_id INTEGER NOT NULL,
                        menu_item_id INTEGER NOT NULL,
                        PRIMARY KEY(booking_id, menu_item_id),
                        FOREIGN KEY(booking_id) REFERENCES bookings(id),
                        FOREIGN KEY(menu_item_id) REFERENCES menu_items(id)
                    )
                    """);
            try {
                st.execute("ALTER TABLE menu_items ADD COLUMN category TEXT DEFAULT 'single'");
            } catch (SQLException ignore) {
                // column already exists
            }
            try {
                st.execute("ALTER TABLE menu_items ADD COLUMN sub_category TEXT");
            } catch (SQLException ignore) {
                // column already exists
            }
            st.execute("""
                    CREATE TABLE IF NOT EXISTS extras_services(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        booking_id INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        name TEXT NOT NULL,
                        unit TEXT NOT NULL,
                        quantity INTEGER NOT NULL DEFAULT 1,
                        unit_price REAL NOT NULL,
                        notes TEXT,
                        service_code TEXT,
                        FOREIGN KEY(booking_id) REFERENCES bookings(id) ON DELETE CASCADE
                    )
                    """);
        }
    }

    private static void seedData() throws SQLException {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            // Seed default admin user (password: admin123)
            st.executeUpdate("""
                    INSERT INTO users(username, password, full_name, role)
                    SELECT 'admin', 'admin123', 'Quản trị viên', 'admin' 
                    WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin')
                    """);
            // Seed default staff user (password: staff123)
            st.executeUpdate("""
                    INSERT INTO users(username, password, full_name, role)
                    SELECT 'staff', 'staff123', 'Nhân viên', 'staff' 
                    WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'staff')
                    """);
            // Mỗi sảnh mặc định 100 bàn và phí sảnh cố định 50 USD
            st.executeUpdate("""
                    INSERT INTO halls(name, capacity, price_per_table)
                    SELECT 'Sảnh Tầng 1', 100, 50.0 WHERE NOT EXISTS (SELECT 1 FROM halls WHERE name = 'Sảnh Tầng 1')
                    """);
            st.executeUpdate("""
                    INSERT INTO halls(name, capacity, price_per_table)
                    SELECT 'Sảnh Tầng 2', 100, 50.0 WHERE NOT EXISTS (SELECT 1 FROM halls WHERE name = 'Sảnh Tầng 2')
                    """);
            // Đảm bảo cập nhật cả dữ liệu cũ (nếu DB đã tồn tại trước đó)
            st.executeUpdate("""
                    UPDATE halls
                    SET capacity = 100, price_per_table = 50.0
                    WHERE name IN ('Sảnh Tầng 1', 'Sảnh Tầng 2')
                    """);
            // Xóa combo cũ
            st.executeUpdate("DELETE FROM combo_items WHERE combo_id IN (SELECT id FROM menu_items WHERE title IN ('Combo Tiệc 1.5tr/mâm', 'Combo Tiệc 2tr/mâm', 'Combo VIP 3tr/mâm'))");
            st.executeUpdate("DELETE FROM menu_items WHERE title IN ('Combo Tiệc 1.5tr/mâm', 'Combo Tiệc 2tr/mâm', 'Combo VIP 3tr/mâm')");
            
            // Seed 3 combo chuẩn với giá cố định
            seedCombo(st, "Combo THƯỜNG", 200.0);
            seedCombo(st, "Combo VIP", 300.0);
            seedCombo(st, "Combo PREMIUM", 500.0);
            
            // Seed món lẻ với giá hợp lý và ổn định
            seedSingleItems(st);
            st.executeUpdate("""
                    INSERT INTO customers(name, phone, email)
                    SELECT 'Demo Customer', '0123456789', 'demo@example.com' WHERE NOT EXISTS (SELECT 1 FROM customers)
                    """);

            // Seed thành phần của các combo mới
            seedComboItems(st);
        }
    }
    
    private static void seedCombo(Statement st, String comboName, double price) throws SQLException {
        st.executeUpdate(String.format("""
                INSERT INTO menu_items(title, price, category, sub_category)
                SELECT '%s', %.1f, 'combo', NULL 
                WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE title = '%s')
                """, comboName, price, comboName));
    }
    
    private static void seedSingleItems(Statement st) throws SQLException {
        // Khai vị (8-20$)
        seedSingleItem(st, "Gỏi cuốn", 12.0, "KHAI_VI");
        seedSingleItem(st, "Nem nướng", 15.0, "KHAI_VI");
        seedSingleItem(st, "Chả giò", 10.0, "KHAI_VI");
        seedSingleItem(st, "Salad tôm thịt", 18.0, "KHAI_VI");
        seedSingleItem(st, "Gỏi đu đủ tôm thịt", 14.0, "KHAI_VI");
        
        // Món chính thường (15-40$)
        seedSingleItem(st, "Gà nướng mật ong", 25.0, "MON_CHINH");
        seedSingleItem(st, "Vịt quay", 30.0, "MON_CHINH");
        seedSingleItem(st, "Heo quay", 35.0, "MON_CHINH");
        seedSingleItem(st, "Cá chiên giòn", 22.0, "MON_CHINH");
        seedSingleItem(st, "Thịt kho tàu", 20.0, "MON_CHINH");
        
        // Hải sản/bò Mỹ (35-120$)
        seedSingleItem(st, "Tôm hùm nướng", 85.0, "MON_CHINH");
        seedSingleItem(st, "Cua rang me", 65.0, "MON_CHINH");
        seedSingleItem(st, "Bò Mỹ nướng", 55.0, "MON_CHINH");
        seedSingleItem(st, "Lẩu hải sản", 75.0, "MON_CHINH");
        seedSingleItem(st, "Cá hồi nướng", 45.0, "MON_CHINH");
        
        // Món phụ (6-18$)
        seedSingleItem(st, "Xôi gấc", 8.0, "MON_PHU");
        seedSingleItem(st, "Xôi đậu xanh", 7.0, "MON_PHU");
        seedSingleItem(st, "Canh chua cá", 12.0, "MON_PHU");
        seedSingleItem(st, "Canh khổ qua", 10.0, "MON_PHU");
        seedSingleItem(st, "Rau muống xào tỏi", 6.0, "MON_PHU");
        seedSingleItem(st, "Rau cải xào", 6.0, "MON_PHU");
        
        // Đồ uống
        seedSingleItem(st, "Bia Sài Gòn", 3.0, "DO_UONG");
        seedSingleItem(st, "Bia Tiger", 4.0, "DO_UONG");
        seedSingleItem(st, "Bia Heineken", 5.0, "DO_UONG");
        seedSingleItem(st, "Bia Budweiser", 6.0, "DO_UONG");
        seedSingleItem(st, "Coca Cola", 3.0, "DO_UONG");
        seedSingleItem(st, "Pepsi", 3.0, "DO_UONG");
        seedSingleItem(st, "Schweppes", 4.0, "DO_UONG");
        seedSingleItem(st, "Nước trái cây đóng chai", 5.0, "DO_UONG");
        seedSingleItem(st, "Rượu trắng", 15.0, "DO_UONG");
        seedSingleItem(st, "Rượu nếp", 20.0, "DO_UONG");
        seedSingleItem(st, "Vodka", 25.0, "DO_UONG");
        seedSingleItem(st, "Vang đỏ", 45.0, "DO_UONG");
        seedSingleItem(st, "Vang trắng", 40.0, "DO_UONG");
        
        // Tráng miệng
        seedSingleItem(st, "Kem dừa", 8.0, "DESSERT");
        seedSingleItem(st, "Chè đậu xanh", 6.0, "DESSERT");
        seedSingleItem(st, "Trái cây theo mùa", 10.0, "DESSERT");
    }
    
    private static void seedSingleItem(Statement st, String title, double price, String subCategory) throws SQLException {
        st.executeUpdate(String.format("""
                INSERT INTO menu_items(title, price, category, sub_category)
                SELECT '%s', %.1f, 'single', '%s'
                WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE title = '%s')
                """, title, price, subCategory, title));
    }
    
    private static void seedComboItems(Statement st) throws SQLException {
        // Combo THƯỜNG (200$)
        seedComboItem(st, "Combo THƯỜNG", "Gỏi cuốn", 1);
        seedComboItem(st, "Combo THƯỜNG", "Nem nướng", 1);
        seedComboItem(st, "Combo THƯỜNG", "Gà nướng mật ong", 1);
        seedComboItem(st, "Combo THƯỜNG", "Cá chiên giòn", 1);
        seedComboItem(st, "Combo THƯỜNG", "Xôi gấc", 1);
        seedComboItem(st, "Combo THƯỜNG", "Canh chua cá", 1);
        seedComboItem(st, "Combo THƯỜNG", "Rau muống xào tỏi", 1);
        seedComboItem(st, "Combo THƯỜNG", "Bia Sài Gòn", 2);
        seedComboItem(st, "Combo THƯỜNG", "Coca Cola", 2);
        seedComboItem(st, "Combo THƯỜNG", "Rượu trắng", 1);
        
        // Combo VIP (300$)
        seedComboItem(st, "Combo VIP", "Salad tôm thịt", 1);
        seedComboItem(st, "Combo VIP", "Gỏi đu đủ tôm thịt", 1);
        seedComboItem(st, "Combo VIP", "Vịt quay", 1);
        seedComboItem(st, "Combo VIP", "Cá hồi nướng", 1);
        seedComboItem(st, "Combo VIP", "Xôi đậu xanh", 1);
        seedComboItem(st, "Combo VIP", "Canh khổ qua", 1);
        seedComboItem(st, "Combo VIP", "Rau cải xào", 1);
        seedComboItem(st, "Combo VIP", "Bia Tiger", 2);
        seedComboItem(st, "Combo VIP", "Schweppes", 2);
        seedComboItem(st, "Combo VIP", "Rượu nếp", 1);
        
        // Combo PREMIUM (500$)
        seedComboItem(st, "Combo PREMIUM", "Salad tôm thịt", 1);
        seedComboItem(st, "Combo PREMIUM", "Nem nướng", 1);
        seedComboItem(st, "Combo PREMIUM", "Tôm hùm nướng", 1);
        seedComboItem(st, "Combo PREMIUM", "Bò Mỹ nướng", 1);
        seedComboItem(st, "Combo PREMIUM", "Cá hồi nướng", 1);
        seedComboItem(st, "Combo PREMIUM", "Xôi gấc", 1);
        seedComboItem(st, "Combo PREMIUM", "Canh chua cá", 1);
        seedComboItem(st, "Combo PREMIUM", "Rau muống xào tỏi", 1);
        seedComboItem(st, "Combo PREMIUM", "Bia Heineken", 2);
        seedComboItem(st, "Combo PREMIUM", "Nước trái cây đóng chai", 2);
        seedComboItem(st, "Combo PREMIUM", "Vang đỏ", 1);
    }
    
    private static void seedComboItem(Statement st, String comboName, String itemName, int quantity) throws SQLException {
        st.executeUpdate(String.format("""
                INSERT INTO combo_items(combo_id, item_id, quantity)
                SELECT c.id, i.id, %d
                FROM menu_items c, menu_items i
                WHERE c.title = '%s' AND i.title = '%s'
                  AND NOT EXISTS (
                    SELECT 1 FROM combo_items ci
                    WHERE ci.combo_id = c.id AND ci.item_id = i.id
                  )
                """, quantity, comboName, itemName));
    }
}

