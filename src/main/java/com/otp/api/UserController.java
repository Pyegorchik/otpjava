package com.otp.api;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.otp.model.OtpCode;
import com.otp.model.User;
import com.otp.service.AuthService;
import com.otp.service.OtpService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.io.IOException;


public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private final OtpService otpService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public UserController(OtpService otpService, AuthService authService) {
        this.otpService = otpService;
        this.authService = authService;
        this.objectMapper = new ObjectMapper();
    }
    
    public void handleGenerateOtp(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method not allowed");
            return;
        }
        
        try {
            // Проверяем авторизацию
            User user = getAuthenticatedUser(exchange);
            if (user.getRole() != User.Role.USER) {
                sendResponse(exchange, 403, "Access denied");
                return;
            }
            
            // Парсим запрос
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode request = objectMapper.readTree(body);
            
            String operationId = request.get("operationId").asText();
            String deliveryMethod = request.get("deliveryMethod").asText();
            
            // Генерируем OTP
            String result = otpService.generateOtp(
                user.getId(), 
                operationId, 
                OtpCode.DeliveryMethod.valueOf(deliveryMethod)
            );
            
            sendJsonResponse(exchange, 200, Map.of("operationId", result));
            
        } catch (Exception e) {
            logger.error("Error generating OTP", e);
            sendResponse(exchange, 500, "Internal server error");
        }
    }
    
    public void handleValidateOtp(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method not allowed");
            return;
        }
        
        try {
            // Проверяем авторизацию
            User user = getAuthenticatedUser(exchange);
            if (user.getRole() != User.Role.USER) {
                sendResponse(exchange, 403, "Access denied");
                return;
            }
            
            // Парсим запрос
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode request = objectMapper.readTree(body);
            
            String operationId = request.get("operationId").asText();
            String code = request.get("code").asText();
            
            // Валидируем OTP
            boolean isValid = otpService.validateOtp(operationId, code);
            
            sendJsonResponse(exchange, 200, Map.of("valid", isValid));
            
        } catch (Exception e) {
            logger.error("Error validating OTP", e);
            sendResponse(exchange, 500, "Internal server error");
        }
    }
    
        /**
     * Отправка JSON-ответа
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object responseData) throws IOException {
        try {
            String json = objectMapper.writeValueAsString(responseData);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, json.getBytes().length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes());
            }
        } catch (Exception e) {
            logger.error("Error sending JSON response", e);
            sendResponse(exchange, 500, "Internal server error");
        }
    }

    /**
     * Отправка текстового ответа
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        try {
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } catch (Exception e) {
            logger.error("Error sending response", e);
            throw new IOException("Failed to send response", e);
        }
    }

    /**
     * Извлечение аутентифицированного пользователя из запроса
     */
    private User getAuthenticatedUser(HttpExchange exchange) throws SecurityException {
        try {
            // Проверяем заголовок Authorization
            List<String> authHeaders = exchange.getRequestHeaders().getOrDefault("Authorization", Collections.emptyList());
            
            if (authHeaders.isEmpty()) {
                throw new SecurityException("Missing authorization header");
            }
            
            String token = authHeaders.get(0).replace("Bearer ", "");
            return authService.authenticate(token);
        } catch (Exception e) {
            logger.warn("Authentication failed", e);
            throw new SecurityException("Invalid credentials");
        }
    }

    /**
     * Парсинг JSON тела запроса
     */
    private JsonNode parseRequestBody(HttpExchange exchange) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            return objectMapper.readTree(body);
        } catch (Exception e) {
            logger.error("Error parsing request body", e);
            throw new IOException("Invalid request format");
        }
    }

    /**
     * Валидация обязательных полей в JSON
     */
    private void validateRequiredFields(JsonNode node, String... fields) throws ValidationException {
        Map<String, String> missing = new HashMap<>();
        
        for (String field : fields) {
            if (!node.has(field)) {
                missing.put(field, "Field is required");
            }
        }
        
        if (!missing.isEmpty()) {
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("error", "Validation failed");
            errorDetails.put("missing", missing);
            throw new ValidationException(errorDetails);
        }
    }


    private static class ValidationException extends Exception {
        private final Map<String, Object> errorDetails;

        public ValidationException(Map<String, Object> errorDetails) {
            this.errorDetails = errorDetails;
        }

        public Map<String, Object> getErrorDetails() {
            return errorDetails;
        }
    }
}