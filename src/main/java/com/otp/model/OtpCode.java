package com.otp.model;

import java.time.LocalDateTime;


public class OtpCode {
    private Long id;
    private Long userId;
    private String operationId;
    private String code;
    private Status status;
    private DeliveryMethod deliveryMethod;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    
    public enum Status {
        ACTIVE, EXPIRED, USED
    }
    
    public enum DeliveryMethod {
        SMS, EMAIL, TELEGRAM, FILE
    }
    
    // Конструкторы
    public OtpCode() {
    }

    public OtpCode(Long userId, String operationId, String code, Status status, 
                   DeliveryMethod deliveryMethod, LocalDateTime createdAt, 
                   LocalDateTime expiresAt) {
        this.userId = userId;
        this.operationId = operationId;
        this.code = code;
        this.status = status;
        this.deliveryMethod = deliveryMethod;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
}