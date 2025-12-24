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
                        notes TEXT,
                        FOREIGN KEY(customer_id) REFERENCES customers(id),
                        FOREIGN KEY(hall_id) REFERENCES halls(id)
                    )
                    """);
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
            st.executeUpdate("""
                    INSERT INTO menu_items(title, price, category)
                    SELECT 'Combo Tiệc 1.5tr/mâm', 35.0, 'combo' WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE title = 'Combo Tiệc 1.5tr/mâm')
                    """);
            st.executeUpdate("""
                    INSERT INTO menu_items(title, price, category)
                    SELECT 'Combo Tiệc 2tr/mâm', 45.0, 'combo' WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE title = 'Combo Tiệc 2tr/mâm')
                    """);
            st.executeUpdate("""
                    INSERT INTO menu_items(title, price, category)
                    SELECT 'Combo VIP 3tr/mâm', 65.0, 'combo' WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE title = 'Combo VIP 3tr/mâm')
                    """);
            st.executeUpdate("""
                    INSERT INTO menu_items(title, price, category)
                    SELECT 'Set Hải sản', 25.0, 'single' WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE title = 'Set Hải sản')
                    """);
            st.executeUpdate("""
                    INSERT INTO menu_items(title, price, category)
                    SELECT 'Set Bò Mỹ', 28.0, 'single' WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE title = 'Set Bò Mỹ')
                    """);
            st.executeUpdate("""
                    INSERT INTO menu_items(title, price, category)
                    SELECT 'Set Chay', 18.0, 'single' WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE title = 'Set Chay')
                    """);
            st.executeUpdate("""
                    INSERT INTO customers(name, phone, email)
                    SELECT 'Demo Customer', '0123456789', 'demo@example.com' WHERE NOT EXISTS (SELECT 1 FROM customers)
                    """);

            // Seed thành phần của các combo mẫu nếu chưa có
            st.executeUpdate("""
                    INSERT INTO combo_items(combo_id, item_id, quantity)
                    SELECT c.id, i.id, 1
                    FROM menu_items c, menu_items i
                    WHERE c.title = 'Combo Tiệc 1.5tr/mâm'
                      AND i.title IN ('Set Hải sản', 'Set Chay')
                      AND NOT EXISTS (
                        SELECT 1 FROM combo_items ci
                        WHERE ci.combo_id = c.id AND ci.item_id = i.id
                    )
                    """);
            st.executeUpdate("""
                    INSERT INTO combo_items(combo_id, item_id, quantity)
                    SELECT c.id, i.id, 1
                    FROM menu_items c, menu_items i
                    WHERE c.title = 'Combo Tiệc 2tr/mâm'
                      AND i.title IN ('Set Hải sản', 'Set Bò Mỹ', 'Set Chay')
                      AND NOT EXISTS (
                        SELECT 1 FROM combo_items ci
                        WHERE ci.combo_id = c.id AND ci.item_id = i.id
                    )
                    """);
            st.executeUpdate("""
                    INSERT INTO combo_items(combo_id, item_id, quantity)
                    SELECT c.id, i.id, 1
                    FROM menu_items c, menu_items i
                    WHERE c.title = 'Combo VIP 3tr/mâm'
                      AND i.title IN ('Set Hải sản', 'Set Bò Mỹ', 'Set Chay')
                      AND NOT EXISTS (
                        SELECT 1 FROM combo_items ci
                        WHERE ci.combo_id = c.id AND ci.item_id = i.id
                    )
                    """);
        }
    }
}

