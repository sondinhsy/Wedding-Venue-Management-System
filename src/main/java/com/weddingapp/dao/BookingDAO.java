package com.weddingapp.dao;

import com.weddingapp.model.Booking;
import com.weddingapp.model.Customer;
import com.weddingapp.model.Hall;
import com.weddingapp.model.MenuItem;
import com.weddingapp.util.Database;

import java.sql.Connection;
import java.sql.Date;
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
                SELECT b.id as booking_id, b.event_date, b.tables, b.total, b.notes,
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
                Booking booking = bookings.computeIfAbsent(bookingId, id -> mapBooking(rs));
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

    public Booking save(Booking booking) {
        String sql = "INSERT INTO bookings(customer_id, hall_id, event_date, tables, total, notes) VALUES(?,?,?,?,?,?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, booking.getCustomer().getId());
            ps.setInt(2, booking.getHall().getId());
            ps.setDate(3, Date.valueOf(booking.getEventDate()));
            ps.setInt(4, booking.getTables());
            ps.setDouble(5, booking.getTotal());
            ps.setString(6, booking.getNotes());
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

    private Booking mapBooking(ResultSet rs) {
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
            LocalDate date = LocalDate.parse(rs.getString("event_date"));
            Booking booking = new Booking(
                    rs.getInt("booking_id"),
                    customer,
                    hall,
                    date,
                    rs.getInt("tables"),
                    rs.getDouble("total"),
                    rs.getString("notes"));
            booking.setMenuItems(new ArrayList<>());
            return booking;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}

