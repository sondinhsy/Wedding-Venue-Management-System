package com.weddingapp.dao;

import com.weddingapp.model.MenuItem;
import com.weddingapp.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MenuDAO {
    public List<MenuItem> findAll() {
        List<MenuItem> menuItems = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, title, price, COALESCE(category,'single') AS category FROM menu_items ORDER BY category, title")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                menuItems.add(map(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return menuItems;
    }

    public MenuItem save(MenuItem item) {
        String sql = "INSERT INTO menu_items(title, price, category) VALUES(?,?,COALESCE(?, 'single'))";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getTitle());
            ps.setDouble(2, item.getPrice());
            ps.setString(3, item.getCategory());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                item.setId(keys.getInt(1));
            }
            return item;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void update(MenuItem item) {
        String sql = "UPDATE menu_items SET title=?, price=?, category=COALESCE(?, 'single') WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getTitle());
            ps.setDouble(2, item.getPrice());
            ps.setString(3, item.getCategory());
            ps.setInt(4, item.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM menu_items WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private MenuItem map(ResultSet rs) throws SQLException {
        return new MenuItem(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getDouble("price"),
                rs.getString("category"));
    }
}

