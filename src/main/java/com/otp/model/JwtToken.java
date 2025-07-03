package com.otp.model;

import java.time.LocalDateTime;

public class JwtToken {
    private Long id;
    private String jti;
    private Long userId;
    private String token;
    private LocalDateTime expiresAt;
    private boolean revoked;
    private LocalDateTime createdAt;

    // Конструкторы
    public JwtToken() {}
    
    public JwtToken(String jti, Long userId, String token, LocalDateTime expiresAt) {
        this.jti = jti;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}