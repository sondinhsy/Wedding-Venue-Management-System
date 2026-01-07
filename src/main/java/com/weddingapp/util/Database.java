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
            // Thêm các cột voucher
            try {
                st.execute("ALTER TABLE bookings ADD COLUMN voucher_code TEXT");
            } catch (SQLException ignore) {
                // column already exists
            }
            try {
                st.execute("ALTER TABLE bookings ADD COLUMN voucher_type TEXT");
            } catch (SQLException ignore) {
                // column already exists
            }
            try {
                st.execute("ALTER TABLE bookings ADD COLUMN voucher_value REAL");
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
        seedSingleItem(st, "Gỏi ngó sen tôm thịt", 16.0, "KHAI_VI");
        seedSingleItem(st, "Súp cua", 19.0, "KHAI_VI");
        seedSingleItem(st, "Gỏi bò bóp thấu", 17.0, "KHAI_VI");
        
        // Món chính thường (15-40$)
        seedSingleItem(st, "Gà nướng mật ong", 25.0, "MON_CHINH");
        seedSingleItem(st, "Vịt quay Bắc Kinh", 30.0, "MON_CHINH");
        seedSingleItem(st, "Heo quay da giòn", 35.0, "MON_CHINH");
        seedSingleItem(st, "Cá chiên giòn sốt chua ngọt", 22.0, "MON_CHINH");
        seedSingleItem(st, "Thịt kho tàu", 20.0, "MON_CHINH");
        seedSingleItem(st, "Gà hấp muối tiêu", 28.0, "MON_CHINH");
        seedSingleItem(st, "Vịt tiềm thuốc bắc", 32.0, "MON_CHINH");
        seedSingleItem(st, "Cá kho tộ miền Tây", 24.0, "MON_CHINH");
        seedSingleItem(st, "Sườn heo nướng BBQ", 26.0, "MON_CHINH");
        seedSingleItem(st, "Gà xé phay chua ngọt", 23.0, "MON_CHINH");
        
        // Hải sản/bò Mỹ (35-120$)
        seedSingleItem(st, "Tôm hùm nướng phô mai", 85.0, "MON_CHINH");
        seedSingleItem(st, "Cua rang me (size vừa)", 65.0, "MON_CHINH");
        seedSingleItem(st, "Bò Mỹ nướng sốt tiêu đen", 55.0, "MON_CHINH");
        seedSingleItem(st, "Lẩu hải sản Thái Lan", 75.0, "MON_CHINH");
        seedSingleItem(st, "Cá hồi nướng sốt teriyaki", 45.0, "MON_CHINH");
        seedSingleItem(st, "Tôm sú nướng muối ớt", 48.0, "MON_CHINH");
        seedSingleItem(st, "Cua biển rang me (size lớn)", 70.0, "MON_CHINH");
        seedSingleItem(st, "Mực nướng sa tế", 38.0, "MON_CHINH");
        seedSingleItem(st, "Cá mú hấp xì dầu", 52.0, "MON_CHINH");
        
        // Món cao cấp PREMIUM (80-150$)
        seedSingleItem(st, "Tôm hùm Alaska nướng bơ tỏi", 120.0, "MON_CHINH");
        seedSingleItem(st, "Bò Wagyu nướng than hoa", 95.0, "MON_CHINH");
        seedSingleItem(st, "Cua hoàng đế hấp bia", 110.0, "MON_CHINH");
        seedSingleItem(st, "Cá hồi Na Uy nướng bơ tỏi", 88.0, "MON_CHINH");
        seedSingleItem(st, "Tôm hùm xanh sốt phô mai", 105.0, "MON_CHINH");
        seedSingleItem(st, "Bò nướng kiểu Kobe", 130.0, "MON_CHINH");
        seedSingleItem(st, "Lobster Thermidor", 125.0, "MON_CHINH");
        seedSingleItem(st, "Cá mú đỏ hấp Hong Kong", 92.0, "MON_CHINH");
        
        // Món phụ (6-18$)
        seedSingleItem(st, "Xôi gấc", 8.0, "MON_PHU");
        seedSingleItem(st, "Xôi đậu xanh", 7.0, "MON_PHU");
        seedSingleItem(st, "Canh chua cá", 12.0, "MON_PHU");
        seedSingleItem(st, "Canh khổ qua", 10.0, "MON_PHU");
        seedSingleItem(st, "Rau muống xào tỏi", 6.0, "MON_PHU");
        seedSingleItem(st, "Rau cải xào", 6.0, "MON_PHU");
        seedSingleItem(st, "Canh bí đỏ tôm thịt", 11.0, "MON_PHU");
        seedSingleItem(st, "Canh chua tôm", 13.0, "MON_PHU");
        seedSingleItem(st, "Rau lang xào", 7.0, "MON_PHU");
        seedSingleItem(st, "Đậu phụ chiên", 8.0, "MON_PHU");
        
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
        seedSingleItem(st, "Champagne", 60.0, "DO_UONG");
        seedSingleItem(st, "Whisky", 50.0, "DO_UONG");
        
        // Tráng miệng
        seedSingleItem(st, "Kem dừa", 8.0, "DESSERT");
        seedSingleItem(st, "Chè đậu xanh", 6.0, "DESSERT");
        seedSingleItem(st, "Trái cây theo mùa", 10.0, "DESSERT");
        seedSingleItem(st, "Chè trôi nước", 7.0, "DESSERT");
        seedSingleItem(st, "Bánh flan", 9.0, "DESSERT");
        seedSingleItem(st, "Kem chanh dây", 8.0, "DESSERT");
    }
    
    private static void seedSingleItem(Statement st, String title, double price, String subCategory) throws SQLException {
        st.executeUpdate(String.format("""
                INSERT INTO menu_items(title, price, category, sub_category)
                SELECT '%s', %.1f, 'single', '%s'
                WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE title = '%s')
                """, title, price, subCategory, title));
    }
    
    private static void seedComboItems(Statement st) throws SQLException {
        // Combo THƯỜNG (200$) - Tổng giá món ~180-220$
        // 1 món khai vị
        seedComboItem(st, "Combo THƯỜNG", "Gỏi cuốn", 1);           // 12$
        // 6 món chính (chọn món giá vừa phải)
        seedComboItem(st, "Combo THƯỜNG", "Gà nướng mật ong", 1);   // 25$
        seedComboItem(st, "Combo THƯỜNG", "Cá chiên giòn", 1);      // 22$
        seedComboItem(st, "Combo THƯỜNG", "Thịt kho tàu", 1);       // 20$
        seedComboItem(st, "Combo THƯỜNG", "Vịt quay Bắc Kinh", 1);          // 30$
        seedComboItem(st, "Combo THƯỜNG", "Heo quay da giòn", 1);          // 35$
        seedComboItem(st, "Combo THƯỜNG", "Gà nướng mật ong", 1);   // 25$ (tổng ~169$ - tăng số lượng để đạt ~200$)
        // 3 món phụ
        seedComboItem(st, "Combo THƯỜNG", "Xôi gấc", 1);           // 8$
        seedComboItem(st, "Combo THƯỜNG", "Canh chua cá", 1);     // 12$
        seedComboItem(st, "Combo THƯỜNG", "Rau muống xào tỏi", 1); // 6$ (tổng ~190$)
        // 1 món tráng miệng
        seedComboItem(st, "Combo THƯỜNG", "Kem dừa", 1);          // 8$ (tổng ~198$)
        // 1 nước (đủ: bia + rượu + nước ngọt)
        seedComboItem(st, "Combo THƯỜNG", "Bia Sài Gòn", 1);       // 3$ (bia)
        seedComboItem(st, "Combo THƯỜNG", "Coca Cola", 1);         // 3$ (nước ngọt)
        seedComboItem(st, "Combo THƯỜNG", "Rượu trắng", 1);        // 15$ (rượu) - Tổng ~219$ (gần 200$, OK)
        
        // Combo VIP (300$) - Tổng giá món ~280-320$
        // 1 món khai vị
        seedComboItem(st, "Combo VIP", "Salad tôm thịt", 1);         // 18$
        // 6 món chính
        seedComboItem(st, "Combo VIP", "Vịt quay Bắc Kinh", 1);              // 30$
        seedComboItem(st, "Combo VIP", "Cá hồi nướng sốt teriyaki", 1);          // 45$
        seedComboItem(st, "Combo VIP", "Heo quay da giòn", 1);              // 35$
        seedComboItem(st, "Combo VIP", "Bò Mỹ nướng sốt tiêu đen", 1);           // 55$
        seedComboItem(st, "Combo VIP", "Lẩu hải sản Thái Lan", 1);           // 75$
        seedComboItem(st, "Combo VIP", "Gà nướng mật ong", 1);      // 25$ (tổng ~283$)
        // 3 món phụ
        seedComboItem(st, "Combo VIP", "Xôi đậu xanh", 1);          // 7$
        seedComboItem(st, "Combo VIP", "Canh khổ qua", 1);          // 10$
        seedComboItem(st, "Combo VIP", "Rau cải xào", 1);           // 6$ (tổng ~306$)
        // 1 món tráng miệng
        seedComboItem(st, "Combo VIP", "Trái cây theo mùa", 1);     // 10$ (tổng ~316$)
        // 1 nước (đủ: bia + rượu + nước ngọt)
        seedComboItem(st, "Combo VIP", "Bia Tiger", 1);             // 4$ (bia)
        seedComboItem(st, "Combo VIP", "Schweppes", 1);             // 4$ (nước ngọt)
        seedComboItem(st, "Combo VIP", "Rượu nếp", 1);             // 20$ (rượu) - Tổng ~344$ (hơi cao, nhưng OK)
        
        // Combo PREMIUM (500$) - Tổng giá món <505$ (điều chỉnh để tránh lỗi vốn)
        // 1 món khai vị
        seedComboItem(st, "Combo PREMIUM", "Salad tôm thịt", 1);    // 18$
        // 6 món chính (chọn món cao cấp nhưng điều chỉnh để tổng <505$)
        seedComboItem(st, "Combo PREMIUM", "Tôm hùm nướng phô mai", 1);    // 85$
        seedComboItem(st, "Combo PREMIUM", "Bò Wagyu nướng than hoa", 1); // 95$ (món cao cấp)
        seedComboItem(st, "Combo PREMIUM", "Cua rang me (size vừa)", 1);       // 65$
        seedComboItem(st, "Combo PREMIUM", "Cá hồi Na Uy nướng bơ tỏi", 1); // 88$ (món cao cấp)
        seedComboItem(st, "Combo PREMIUM", "Lẩu hải sản Thái Lan", 1);      // 75$
        seedComboItem(st, "Combo PREMIUM", "Bò Mỹ nướng sốt tiêu đen", 1);      // 55$ (tổng ~472$)
        // 3 món phụ
        seedComboItem(st, "Combo PREMIUM", "Xôi gấc", 1);          // 8$
        seedComboItem(st, "Combo PREMIUM", "Canh chua cá", 1);     // 12$
        seedComboItem(st, "Combo PREMIUM", "Rau muống xào tỏi", 1); // 6$ (tổng ~498$)
        // 1 món tráng miệng
        seedComboItem(st, "Combo PREMIUM", "Trái cây theo mùa", 1); // 10$ (tổng ~508$)
        // 1 nước (đủ: bia + rượu + nước ngọt) - Thay Vang đỏ bằng Vang trắng để giảm 5$
        seedComboItem(st, "Combo PREMIUM", "Bia Heineken", 1);     // 5$ (bia)
        seedComboItem(st, "Combo PREMIUM", "Nước trái cây đóng chai", 1); // 5$ (nước ngọt)
        seedComboItem(st, "Combo PREMIUM", "Vang trắng", 1);       // 40$ (rượu, thay Vang đỏ 45$ để giảm 5$) - Tổng ~503$ (<505$ OK)
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

