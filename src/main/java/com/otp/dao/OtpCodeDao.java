package com.otp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import com.otp.model.OtpCode;

public class OtpCodeDao {
    private final Connection connection;
    
    public OtpCodeDao(Connection connection) {
        this.connection = connection;
    }
    
    public void save(OtpCode otpCode) throws SQLException {
        String sql = "INSERT INTO otp_codes (user_id, operation_id, code, status, delivery_method, expires_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, otpCode.getUserId());
            stmt.setString(2, otpCode.getOperationId());
            stmt.setString(3, otpCode.getCode());
            stmt.setString(4, otpCode.getStatus().name());
            stmt.setString(5, otpCode.getDeliveryMethod().name());
            stmt.setTimestamp(6, Timestamp.valueOf(otpCode.getExpiresAt()));
            
            stmt.executeUpdate();
            
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    otpCode.setId(keys.getLong(1));
                }
            }
        }
    }
    
    public Optional<OtpCode> findByOperationId(String operationId) throws SQLException {
        String sql = "SELECT * FROM otp_codes WHERE operation_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, operationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapOtpCode(rs));
                }
            }
        }
        return Optional.empty();
    }
    
    public void updateStatus(Long id, OtpCode.Status status) throws SQLException {
        String sql = "UPDATE otp_codes SET status = ?, used_at = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setTimestamp(2, status == OtpCode.Status.USED ? Timestamp.valueOf(LocalDateTime.now()) : null);
            stmt.setLong(3, id);
            stmt.executeUpdate();
        }
    }
    
    public int expireOldCodes() throws SQLException {
        String sql = "UPDATE otp_codes SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND expires_at < CURRENT_TIMESTAMP";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            return stmt.executeUpdate();
        }
    }
    
    public void deleteByUserId(Long userId) throws SQLException {
        String sql = "DELETE FROM otp_codes WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        }
    }
    
    private OtpCode mapOtpCode(ResultSet rs) throws SQLException {
        OtpCode otpCode = new OtpCode();
        otpCode.setId(rs.getLong("id"));
        otpCode.setUserId(rs.getLong("user_id"));
        otpCode.setOperationId(rs.getString("operation_id"));
        otpCode.setCode(rs.getString("code"));
        otpCode.setStatus(OtpCode.Status.valueOf(rs.getString("status")));
        otpCode.setDeliveryMethod(OtpCode.DeliveryMethod.valueOf(rs.getString("delivery_method")));
        otpCode.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        otpCode.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        
        Timestamp usedAt = rs.getTimestamp("used_at");
        if (usedAt != null) {
            otpCode.setUsedAt(usedAt.toLocalDateTime());
        }
        
        return otpCode;
    }
}