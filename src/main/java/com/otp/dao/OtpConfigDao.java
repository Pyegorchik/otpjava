package com.otp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.otp.model.OtpConfig;

public class OtpConfigDao {
    private final Connection connection;
    
    public OtpConfigDao(Connection connection) {
        this.connection = connection;
    }
    
    public OtpConfig getConfig() throws SQLException {
        String sql = "SELECT * FROM otp_config WHERE id = 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return mapConfig(rs);
            }
            throw new SQLException("OTP config not found");
        }
    }
    
    public void updateConfig(int codeLength, int expiryMinutes) throws SQLException {
        String sql = "UPDATE public.otp_config SET code_length = ?, expiry_minutes = ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, codeLength);
            stmt.setInt(2, expiryMinutes);
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Failed to update OTP config");
            }
        }
    }
    
    private OtpConfig mapConfig(ResultSet rs) throws SQLException {
        OtpConfig config = new OtpConfig();
        config.setId(rs.getInt("id"));
        config.setCodeLength(rs.getInt("code_length"));
        config.setExpiryMinutes(rs.getInt("expiry_minutes"));
        config.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return config;
    }
}