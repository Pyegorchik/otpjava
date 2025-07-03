package com.otp.model;

import java.time.LocalDateTime;

public class OtpConfig {
    private Integer id = 1;
    private Integer codeLength;
    private Integer expiryMinutes;
    private LocalDateTime updatedAt;
    
    // Конструкторы
    public OtpConfig() {
    }

    public OtpConfig(Integer codeLength, Integer expiryMinutes) {
        this.codeLength = codeLength;
        this.expiryMinutes = expiryMinutes;
        this.updatedAt = LocalDateTime.now();
    }

    // Геттеры и сеттеры
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(Integer codeLength) {
        this.codeLength = codeLength;
    }

    public Integer getExpiryMinutes() {
        return expiryMinutes;
    }

    public void setExpiryMinutes(Integer expiryMinutes) {
        this.expiryMinutes = expiryMinutes;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Автоматическое обновление времени при изменении настроек
    public void updateConfig(Integer codeLength, Integer expiryMinutes) {
        this.codeLength = codeLength;
        this.expiryMinutes = expiryMinutes;
        this.updatedAt = LocalDateTime.now();
    }
}