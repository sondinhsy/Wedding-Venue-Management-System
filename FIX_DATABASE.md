# Hướng dẫn sửa lỗi Database

## Vấn đề
Database thiếu các cột voucher: `voucher_code`, `voucher_type`, `voucher_value` trong bảng `bookings`.

## Giải pháp tự động
Ứng dụng sẽ TỰ ĐỘNG thêm các cột này khi khởi động lần tiếp theo. Chỉ cần:
1. Đóng ứng dụng hoàn toàn
2. Mở lại ứng dụng
3. Database sẽ tự động được cập nhật

## Giải pháp thủ công (nếu cần)
Nếu vẫn gặp lỗi, chạy script SQL sau:

```sql
-- Thêm các cột voucher vào bảng bookings
ALTER TABLE bookings ADD COLUMN voucher_code TEXT;
ALTER TABLE bookings ADD COLUMN voucher_type TEXT;
ALTER TABLE bookings ADD COLUMN voucher_value REAL;
```

Hoặc xóa file database và để ứng dụng tạo lại:
- Xóa file: `data/wedding.db`
- Mở lại ứng dụng

## Voucher cố định hiện có
Các mã voucher có sẵn (hardcoded):
- **GIAM10**: Giảm 10%
- **GIAM20**: Giảm 20%
- **GIAM50K**: Giảm 50,000 VND
- **GIAM100K**: Giảm 100,000 VND

