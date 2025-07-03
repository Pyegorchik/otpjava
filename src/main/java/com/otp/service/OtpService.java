package com.otp.service;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

import com.otp.dao.OtpCodeDao;
import com.otp.dao.OtpConfigDao;
import com.otp.model.OtpCode;
import com.otp.notification.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    
    private final OtpCodeDao otpCodeDao;
    private final OtpConfigDao otpConfigDao;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom;
    
    public OtpService(OtpCodeDao otpCodeDao, OtpConfigDao otpConfigDao, NotificationService notificationService) {
        this.otpCodeDao = otpCodeDao;
        this.otpConfigDao = otpConfigDao;
        this.notificationService = notificationService;
        this.secureRandom = new SecureRandom();
    }
    
    public String generateOtp(Long userId, String operationId, OtpCode.DeliveryMethod deliveryMethod) throws SQLException {
        logger.info("Generating OTP for user {} with operation {}", userId, operationId);
        
 
        // Генерируем код
        String code = generateSecureCode(otpConfigDao.getConfig().getCodeLength());
        
        // Создаем OTP запись
        OtpCode otpCode = new OtpCode();
        otpCode.setUserId(userId);
        otpCode.setOperationId(operationId);
        otpCode.setCode(code);
        otpCode.setStatus(OtpCode.Status.ACTIVE);
        otpCode.setDeliveryMethod(deliveryMethod);
        otpCode.setCreatedAt(LocalDateTime.now());
        otpCode.setExpiresAt(LocalDateTime.now().plusMinutes(otpConfigDao.getConfig().getCodeLength()));
        
        // Удаляем старый код для этой операции если есть
        otpCodeDao.findByOperationId(operationId).ifPresent(existing -> {
            try {
                otpCodeDao.updateStatus(existing.getId(), OtpCode.Status.EXPIRED);
            } catch (SQLException e) {
                logger.error("Failed to expire old OTP code", e);
            }
        });
        
        // Сохраняем новый код
        otpCodeDao.save(otpCode);
        
        // Отправляем код
        notificationService.sendOtp(userId, code, deliveryMethod);
        
        logger.info("OTP generated and sent for operation {}", operationId);
        return operationId;
    }
    
    public boolean validateOtp(String operationId, String code) throws SQLException {
        logger.info("Validating OTP for operation {}", operationId);
        
        Optional<OtpCode> otpOpt = otpCodeDao.findByOperationId(operationId);
        if (otpOpt.isEmpty()) {
            logger.warn("OTP not found for operation {}", operationId);
            return false;
        }
        
        OtpCode otp = otpOpt.get();
        
        // Проверяем статус
        if (otp.getStatus() != OtpCode.Status.ACTIVE) {
            logger.warn("OTP is not active for operation {}", operationId);
            return false;
        }
        
        // Проверяем срок действия
        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            otpCodeDao.updateStatus(otp.getId(), OtpCode.Status.EXPIRED);
            logger.warn("OTP expired for operation {}", operationId);
            return false;
        }
        
        // Проверяем код
        if (!otp.getCode().equals(code)) {
            logger.warn("Invalid OTP code for operation {}", operationId);
            return false;
        }
        
        // Помечаем как использованный
        otpCodeDao.updateStatus(otp.getId(), OtpCode.Status.USED);
        logger.info("OTP validated successfully for operation {}", operationId);
        return true;
    }
    
    private String generateSecureCode(int length) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }
}
