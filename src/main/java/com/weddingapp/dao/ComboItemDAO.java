package com.weddingapp.dao;

import com.weddingapp.model.ComboItem;
import com.weddingapp.model.MenuItem;
import com.weddingapp.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO để lấy danh sách món chi tiết nằm trong một combo.
 */
public class ComboItemDAO {

    public List<ComboItem> findByComboId(int comboId) {
        List<ComboItem> items = new ArrayList<>();
        String sql = """
                SELECT ci.combo_id, ci.item_id, ci.quantity,
                       m.title, m.price, COALESCE(m.category, 'single') AS category
                FROM combo_items ci
                JOIN menu_items m ON ci.item_id = m.id
                WHERE ci.combo_id = ?
                ORDER BY m.title
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, comboId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MenuItem item = new MenuItem(
                        rs.getInt("item_id"),
                        rs.getString("title"),
                        rs.getDouble("price"),
                        rs.getString("category"));
                ComboItem comboItem = new ComboItem(
                        rs.getInt("combo_id"),
                        item,
                        rs.getInt("quantity"));
                items.add(comboItem);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return items;
    }
}


