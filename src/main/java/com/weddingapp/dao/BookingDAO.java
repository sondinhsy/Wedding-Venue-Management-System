package com.weddingapp.dao;

import com.weddingapp.model.Booking;
import com.weddingapp.model.Customer;
import com.weddingapp.model.Hall;
import com.weddingapp.model.MenuItem;
import com.weddingapp.util.Database;

import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingDAO {
    public List<Booking> findAll() {
        Map<Integer, Booking> bookings = new HashMap<>();
        String sql = """
                SELECT b.id as booking_id, b.booking_code, b.event_date, b.tables, b.total, b.paid_amount, b.payment_status, b.notes,
                       c.id as customer_id, c.name as customer_name, c.phone, c.email,
                       h.id as hall_id, h.name as hall_name, h.capacity, h.price_per_table,
                       m.id as menu_id, m.title as menu_title, m.price as menu_price, m.category as menu_category
                FROM bookings b
                JOIN customers c ON b.customer_id = c.id
                JOIN halls h ON b.hall_id = h.id
                LEFT JOIN booking_menu bm ON b.id = bm.booking_id
                LEFT JOIN menu_items m ON bm.menu_item_id = m.id
                ORDER BY b.event_date DESC, b.id DESC
                """;
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int bookingId = rs.getInt("booking_id");
                Booking booking = bookings.computeIfAbsent(bookingId, id -> mapBooking(rs, conn));
                int menuId = rs.getInt("menu_id");
                if (menuId > 0) {
                    booking.getMenuItems().add(new MenuItem(
                            menuId,
                            rs.getString("menu_title"),
                            rs.getDouble("menu_price"),
                            rs.getString("menu_category")));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return new ArrayList<>(bookings.values());
    }

    /**
     * Tổng số bàn đã được đặt cho một sảnh trong một ngày cụ thể.
     */
    public int getTotalTablesForHallOnDate(int hallId, LocalDate date) {
        String sql = "SELECT COALESCE(SUM(tables), 0) AS total_tables FROM bookings WHERE hall_id = ? AND event_date = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, hallId);
            ps.setString(2, date.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("total_tables");
            }
            return 0;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Booking save(Booking booking) {
        if (booking.getId() > 0) {
            return update(booking);
        }
        // Chỉ tính 20% deposit nếu status là IN_PROGRESS hoặc COMPLETED
        // Nếu status là PENDING thì paidAmount = 0
        if (booking.getPaymentStatus() == Booking.PaymentStatus.IN_PROGRESS || 
            booking.getPaymentStatus() == Booking.PaymentStatus.COMPLETED) {
            if (booking.getPaidAmount() == 0.0) {
                double deposit = booking.getTotal() * 0.20;
                booking.setPaidAmount(deposit);
            }
        } else {
            // PENDING: reset về 0
            booking.setPaidAmount(0.0);
        }
        
        String sql = "INSERT INTO bookings(customer_id, hall_id, event_date, tables, total, paid_amount, payment_status, notes, booking_code) VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            // Generate random 5-digit booking code if not set
            if (booking.getBookingCode() == null || booking.getBookingCode().isEmpty()) {
                String bookingCode = generateRandomBookingCode(conn);
                booking.setBookingCode(bookingCode);
            }
            ps.setInt(1, booking.getCustomer().getId());
            ps.setInt(2, booking.getHall().getId());
            // Use setString to ensure YYYY-MM-DD format in TEXT column
            ps.setString(3, booking.getEventDate().toString());
            ps.setInt(4, booking.getTables());
            ps.setDouble(5, booking.getTotal());
            ps.setDouble(6, booking.getPaidAmount());
            ps.setString(7, booking.getPaymentStatus() != null ? booking.getPaymentStatus().name() : "PENDING");
            ps.setString(8, booking.getNotes());
            ps.setString(9, booking.getBookingCode());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                booking.setId(keys.getInt(1));
            }
            insertMenuSelections(conn, booking);
            return booking;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Booking update(Booking booking) {
        // Generate booking code if not set
        if (booking.getBookingCode() == null || booking.getBookingCode().isEmpty()) {
            try (Connection conn = Database.getConnection()) {
                String bookingCode = generateRandomBookingCode(conn);
                booking.setBookingCode(bookingCode);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        String sql = "UPDATE bookings SET customer_id=?, hall_id=?, event_date=?, tables=?, total=?, paid_amount=?, payment_status=?, notes=?, booking_code=? WHERE id=?";
        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, booking.getCustomer().getId());
                ps.setInt(2, booking.getHall().getId());
                ps.setString(3, booking.getEventDate().toString());
                ps.setInt(4, booking.getTables());
                ps.setDouble(5, booking.getTotal());
                ps.setDouble(6, booking.getPaidAmount());
                ps.setString(7, booking.getPaymentStatus() != null ? booking.getPaymentStatus().name() : "PENDING");
                ps.setString(8, booking.getNotes());
                ps.setString(9, booking.getBookingCode());
                ps.setInt(10, booking.getId());
                ps.executeUpdate();
            }
            // Delete old menu selections and insert new ones
            deleteMenuSelections(conn, booking.getId());
            insertMenuSelections(conn, booking);
            return booking;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void updatePaymentStatus(int bookingId, Booking.PaymentStatus status) {
        String sql = "UPDATE bookings SET payment_status=? WHERE id=?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void updatePaymentStatusAndAmount(int bookingId, Booking.PaymentStatus status, double paidAmount) {
        String sql = "UPDATE bookings SET payment_status=?, paid_amount=? WHERE id=?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setDouble(2, paidAmount);
            ps.setInt(3, bookingId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public Booking findById(int bookingId) {
        String sql = """
                SELECT b.id as booking_id, b.booking_code, b.event_date, b.tables, b.total, b.paid_amount, b.payment_status, b.notes,
                       c.id as customer_id, c.name as customer_name, c.phone, c.email,
                       h.id as hall_id, h.name as hall_name, h.capacity, h.price_per_table,
                       m.id as menu_id, m.title as menu_title, m.price as menu_price, m.category as menu_category
                FROM bookings b
                JOIN customers c ON b.customer_id = c.id
                JOIN halls h ON b.hall_id = h.id
                LEFT JOIN booking_menu bm ON b.id = bm.booking_id
                LEFT JOIN menu_items m ON bm.menu_item_id = m.id
                WHERE b.id = ?
                """;
        Map<Integer, Booking> bookings = new HashMap<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("booking_id");
                Booking booking = bookings.computeIfAbsent(id, i -> mapBooking(rs, conn));
                int menuId = rs.getInt("menu_id");
                if (menuId > 0) {
                    booking.getMenuItems().add(new MenuItem(
                            menuId,
                            rs.getString("menu_title"),
                            rs.getDouble("menu_price"),
                            rs.getString("menu_category")));
                }
            }
            return bookings.isEmpty() ? null : bookings.values().iterator().next();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void delete(int bookingId) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Xóa extras services (có thể có cascade nhưng để chắc chắn)
                String deleteExtrasSql = "DELETE FROM extras_services WHERE booking_id=?";
                try (PreparedStatement ps = conn.prepareStatement(deleteExtrasSql)) {
                    ps.setInt(1, bookingId);
                    ps.executeUpdate();
                }
                
                // Xóa menu selections
                deleteMenuSelections(conn, bookingId);
                
                // Xóa booking
                String deleteBookingSql = "DELETE FROM bookings WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(deleteBookingSql)) {
                    ps.setInt(1, bookingId);
                    ps.executeUpdate();
                }
                
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error deleting booking " + bookingId, ex);
        }
    }

    private void deleteMenuSelections(Connection conn, int bookingId) throws SQLException {
        String sql = "DELETE FROM booking_menu WHERE booking_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.executeUpdate();
        }
    }

    private void insertMenuSelections(Connection conn, Booking booking) throws SQLException {
        if (booking.getMenuItems().isEmpty()) {
            return;
        }
        String sql = "INSERT INTO booking_menu(booking_id, menu_item_id) VALUES(?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (MenuItem item : booking.getMenuItems()) {
                ps.setInt(1, booking.getId());
                ps.setInt(2, item.getId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private Booking mapBooking(ResultSet rs, Connection conn) {
        try {
            Customer customer = new Customer(
                    rs.getInt("customer_id"),
                    rs.getString("customer_name"),
                    rs.getString("phone"),
                    rs.getString("email"));
            Hall hall = new Hall(
                    rs.getInt("hall_id"),
                    rs.getString("hall_name"),
                    rs.getInt("capacity"),
                    rs.getDouble("price_per_table"));

            String dateStr = rs.getString("event_date");
            LocalDate date;
            try {
                // Try standard ISO format first (YYYY-MM-DD)
                date = LocalDate.parse(dateStr);
            } catch (Exception e) {
                // Handle fallback cases
                try {
                    // Check if it's a numeric timestamp (epoch millis)
                    long timestamp = Long.parseLong(dateStr);
                    date = new java.sql.Date(timestamp).toLocalDate();
                } catch (NumberFormatException nfe) {
                    // Fallback for other potential formats, e.g. "YYYY-MM-DD HH:mm:ss"
                    if (dateStr != null && dateStr.length() >= 10) {
                        try {
                            date = LocalDate.parse(dateStr.substring(0, 10));
                        } catch (Exception ex2) {
                            throw new RuntimeException("Could not parse date: " + dateStr, e);
                        }
                    } else {
                        throw new RuntimeException("Could not parse date: " + dateStr, e);
                    }
                }
            }

            double paidAmount = 0.0;
            try {
                // Try to get paid_amount column, if it doesn't exist, default to 0.0
                paidAmount = rs.getDouble("paid_amount");
                if (rs.wasNull()) {
                    paidAmount = 0.0;
                }
            } catch (SQLException e) {
                // Column might not exist in old database, default to 0.0
                paidAmount = 0.0;
            }
            
            Booking.PaymentStatus paymentStatus = Booking.PaymentStatus.PENDING;
            try {
                String statusStr = rs.getString("payment_status");
                if (statusStr != null) {
                    paymentStatus = Booking.PaymentStatus.fromString(statusStr);
                }
            } catch (SQLException e) {
                // Column might not exist in old database, default to PENDING
                paymentStatus = Booking.PaymentStatus.PENDING;
            }
            
            Booking booking = new Booking(
                    rs.getInt("booking_id"),
                    customer,
                    hall,
                    date,
                    rs.getInt("tables"),
                    rs.getDouble("total"),
                    paidAmount,
                    paymentStatus,
                    rs.getString("notes"));
            
            // Set booking code
            try {
                String bookingCode = rs.getString("booking_code");
                if (bookingCode == null || bookingCode.isEmpty()) {
                    // Generate code for old bookings that don't have one
                    bookingCode = generateRandomBookingCode(conn);
                    booking.setBookingCode(bookingCode);
                    // Update in database
                    try (PreparedStatement updatePs = conn.prepareStatement("UPDATE bookings SET booking_code = ? WHERE id = ?")) {
                        updatePs.setString(1, bookingCode);
                        updatePs.setInt(2, booking.getId());
                        updatePs.executeUpdate();
                    }
                } else {
                    booking.setBookingCode(bookingCode);
                }
            } catch (SQLException e) {
                // Column might not exist, generate new code
                String bookingCode = generateRandomBookingCode(conn);
                booking.setBookingCode(bookingCode);
            }
            
            booking.setMenuItems(new ArrayList<>());
            return booking;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Generate a random 5-digit booking code that doesn't exist in the database
     */
    private String generateRandomBookingCode(Connection conn) throws SQLException {
        java.util.Random random = new java.util.Random();
        String code;
        int attempts = 0;
        do {
            // Generate random 5-digit number (10000-99999)
            int num = 10000 + random.nextInt(90000);
            code = String.valueOf(num);
            attempts++;
            
            // Check if code already exists
            try (PreparedStatement checkPs = conn.prepareStatement("SELECT COUNT(*) FROM bookings WHERE booking_code = ?")) {
                checkPs.setString(1, code);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        return code; // Code is unique
                    }
                }
            }
        } while (attempts < 100); // Prevent infinite loop
        
        // Fallback: use timestamp-based code if random generation fails
        return String.format("%05d", System.currentTimeMillis() % 100000);
    }
}
