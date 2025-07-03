package com.otp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.otp.model.User;

public class UserDao {
    private final Connection connection;
    
    public UserDao(Connection connection) {
        this.connection = connection;
    }
    
    public void save(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role, email, phone, telegram_chat_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getRole().name());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getPhone());
            stmt.setObject(6, user.getTelegramChatId());
            
            stmt.executeUpdate();
            
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
        }
    }
    
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }
        return Optional.empty();
    }
    
    public boolean hasAdmin() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    
    public List<User> findAllUsers() throws SQLException {
        String sql = "SELECT * FROM users WHERE role = 'USER'";
        List<User> users = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }
        return users;
    }
    
    public void delete(Long userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ? AND role = 'USER'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        }
    }
    
    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(User.Role.valueOf(rs.getString("role")));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setTelegramChatId((Long) rs.getObject("telegram_chat_id"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return user;
    }


    public Optional<User> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }
        return Optional.empty();
    }
    

}
