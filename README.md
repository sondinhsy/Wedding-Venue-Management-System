# Wedding Venue Manager (JavaFX + SQLite)

Ứng dụng demo quản lý sảnh cưới: booking, khách hàng, thực đơn, thống kê nhanh và xuất CSV. Công nghệ: Java 17, JavaFX (FXML/Controller/CSS), JDBC SQLite, DAO, MVC đơn giản.

## Cách chạy trên Windows (Intel)
1. Cài đặt:
   - JDK 17+: https://adoptium.net
   - Maven 3.9+ thêm vào `PATH`.
2. Mở PowerShell tại thư mục dự án:
   ```powershell
   mvn clean javafx:run
   ```
   Maven sẽ tải JavaFX và driver SQLite, sau đó khởi chạy ứng dụng.

## Sử dụng nhanh
- **Dashboard**: xem tổng số khách, booking và doanh thu ước tính.
- **Booking mới**: chọn khách hàng, sảnh, ngày, số bàn, thực đơn → xem tổng tiền ước tính → Lưu.
- **Khách hàng**: thêm khách hàng mới, bảng liệt kê toàn bộ khách.
- **Thực đơn**: thêm món/set, hiển thị danh sách.
- **Xuất báo cáo**: lưu CSV chứa booking + thực đơn đã chọn.

## Thư mục chính
- `src/main/java/com/weddingapp/MainApp.java` — khởi động JavaFX.
- `controller/MainController.java` — logic UI (MVC controller).
- `dao/*` — lớp truy cập dữ liệu (JDBC).
- `model/*` — entity.
- `util/Database.java` — khởi tạo SQLite, seed mẫu.
- `src/main/resources/fxml/main.fxml` — layout FXML.
- `src/main/resources/styles/app.css` — giao diện CSS.

## Cơ sở dữ liệu
- SQLite file lưu tại `data/wedding.db` (tự tạo khi chạy lần đầu).
- Seed sẵn 1 sảnh, 1 menu, 1 khách để thử nhanh.

## Xuất/nhập file
- Nút **Export CSV** tạo file chứa Booking + thực đơn đã chọn.
- Mẫu code mở rộng: thêm ghi/đọc CSV, hoặc tích hợp PDF/Excel theo nhu cầu.

## Tùy biến thêm
- Bổ sung DAO cho cập nhật/xóa.
- Thêm thống kê theo ngày/tháng.
- Kết nối MySQL: chỉnh JDBC URL/driver trong `Database.java` và `pom.xml`.

