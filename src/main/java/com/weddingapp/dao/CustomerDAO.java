package com.weddingapp.dao;

import com.weddingapp.model.Customer;
import com.weddingapp.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {
    public List<Customer> findAll() {
        List<Customer> customers = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, phone, email FROM customers ORDER BY name")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                customers.add(map(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return customers;
    }

    public Customer save(Customer customer) {
        String sql = "INSERT INTO customers(name, phone, email) VALUES(?,?,?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getEmail());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                customer.setId(keys.getInt(1));
            }
            return customer;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void update(Customer customer) {
        String sql = "UPDATE customers SET name=?, phone=?, email=? WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getEmail());
            ps.setInt(4, customer.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM customers WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Customer map(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("email"));
    }
}

