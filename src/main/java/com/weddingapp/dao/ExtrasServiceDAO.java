package com.weddingapp.dao;

import com.weddingapp.model.ExtrasServiceItem;
import com.weddingapp.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ExtrasServiceDAO {
    
    public List<ExtrasServiceItem> findByBookingId(int bookingId) {
        List<ExtrasServiceItem> items = new ArrayList<>();
        String sql = "SELECT * FROM extras_services WHERE booking_id = ? ORDER BY type, id";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(mapItem(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error loading extras services for booking " + bookingId, ex);
        }
        return items;
    }

    public void saveAll(int bookingId, List<ExtrasServiceItem> items) {
        String deleteSql = "DELETE FROM extras_services WHERE booking_id = ?";
        String insertSql = """
                INSERT INTO extras_services(booking_id, type, name, unit, quantity, unit_price, notes, service_code)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Xóa tất cả items cũ của booking
                try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
                    deletePs.setInt(1, bookingId);
                    deletePs.executeUpdate();
                }
                // Thêm items mới
                if (!items.isEmpty()) {
                    try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                        for (ExtrasServiceItem item : items) {
                            insertPs.setInt(1, bookingId);
                            insertPs.setString(2, item.getType().name());
                            insertPs.setString(3, item.getName());
                            insertPs.setString(4, item.getUnit());
                            insertPs.setInt(5, item.getQuantity());
                            insertPs.setDouble(6, item.getUnitPrice());
                            insertPs.setString(7, item.getNotes());
                            insertPs.setString(8, item.getServiceCode());
                            insertPs.addBatch();
                        }
                        insertPs.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error saving extras services for booking " + bookingId, ex);
        }
    }

    private ExtrasServiceItem mapItem(ResultSet rs) throws SQLException {
        ExtrasServiceItem item = new ExtrasServiceItem();
        item.setId(rs.getInt("id"));
        item.setBookingId(rs.getInt("booking_id"));
        String typeStr = rs.getString("type");
        if (typeStr != null) {
            item.setType(ExtrasServiceItem.Type.valueOf(typeStr));
        }
        item.setName(rs.getString("name"));
        item.setUnit(rs.getString("unit"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getDouble("unit_price"));
        String notes = rs.getString("notes");
        item.setNotes(notes != null ? notes : "");
        String serviceCode = rs.getString("service_code");
        item.setServiceCode(serviceCode != null ? serviceCode : "");
        return item;
    }
}

