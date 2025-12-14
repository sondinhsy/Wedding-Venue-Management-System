package com.weddingapp.dao;

import com.weddingapp.model.Hall;
import com.weddingapp.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HallDAO {
    public List<Hall> findAll() {
        List<Hall> halls = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, capacity, price_per_table FROM halls ORDER BY name")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                halls.add(map(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return halls;
    }

    public Hall save(Hall hall) {
        String sql = "INSERT INTO halls(name, capacity, price_per_table) VALUES(?,?,?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, hall.getName());
            ps.setInt(2, hall.getCapacity());
            ps.setDouble(3, hall.getPricePerTable());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                hall.setId(keys.getInt(1));
            }
            return hall;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void update(Hall hall) {
        String sql = "UPDATE halls SET name=?, capacity=?, price_per_table=? WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hall.getName());
            ps.setInt(2, hall.getCapacity());
            ps.setDouble(3, hall.getPricePerTable());
            ps.setInt(4, hall.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM halls WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Hall map(ResultSet rs) throws SQLException {
        return new Hall(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("capacity"),
                rs.getDouble("price_per_table"));
    }
}

