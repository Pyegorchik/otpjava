package com.otp.api;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.otp.model.OtpConfig;
import com.otp.model.User;
import com.otp.service.AdminService;
import com.otp.service.AuthService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminController  {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final AdminService adminService;
    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminController(AdminService adminService, AuthService authService) {
        this.adminService = adminService;
        this.authService = authService;
    }

   public void handleConfig(HttpExchange exchange) throws IOException {
    try {
        if ("GET".equals(exchange.getRequestMethod())) {
            handleGetConfig(exchange);
        } else if ("POST".equals(exchange.getRequestMethod())) {
            handleUpdateConfig(exchange);
        } else {
            sendResponse(exchange, 405, "Method Not Allowed");
        }
    } catch (SQLException e) {
        logger.error("Database error in config operation", e);
        sendResponse(exchange, 500, "Database error: " + e.getMessage());
    } catch (NumberFormatException e) {
        sendResponse(exchange, 400, "Invalid number format");
    } catch (Exception e) {
        logger.error("Unexpected error in config operation", e);
        sendResponse(exchange, 500, "Internal server error");
    }
    }

    public void handleGetConfig(HttpExchange exchange) throws IOException, SQLException {
        OtpConfig config = adminService.getOtpConfig();
        sendJsonResponse(exchange, 200, Map.of(
            "codeLength", config.getCodeLength(),
            "expiryMinutes", config.getExpiryMinutes()
        ));
    }

    public void handleUpdateConfig(HttpExchange exchange) throws IOException, SQLException {
        Map<String,String> request = parseRequestBody(exchange);
        try {
            validateRequiredFields(request, "codeLength", "expiryMinutes");
        }
        catch (ValidationException e) {
            logger.error("Validation error in config update", e);
            sendJsonResponse(exchange, 400, e);
        }
        
        
        int codeLength = Integer.parseInt(request.get("codeLength"));
        int expiryMinutes = Integer.parseInt(request.get("expiryMinutes"));
        
        // Валидация входных значений
        if (codeLength < 4 || codeLength > 8) {
            sendResponse(exchange, 400, "Code length must be between 4 and 8");
            return;
        }
        
        if (expiryMinutes < 1) {
            sendResponse(exchange, 400, "Expiry minutes must be positive");
            return;
        }
        
        adminService.updateOtpConfig(codeLength, expiryMinutes);
        sendResponse(exchange, 200, "Config updated");
    }

   public void handleUsers(HttpExchange exchange) throws IOException {
    try {
        if ("GET".equals(exchange.getRequestMethod())) {
            handleGetUsers(exchange);
        } else if ("DELETE".equals(exchange.getRequestMethod())) {
            handleDeleteUser(exchange);
        } else {
            sendResponse(exchange, 405, "Method Not Allowed");
        }
    } catch (SQLException e) {
        logger.error("Database error in users operation", e);
        sendResponse(exchange, 500, "Database error");
    } catch (NumberFormatException e) {
        sendResponse(exchange, 400, "Invalid user ID format");
    } catch (Exception e) {
        logger.error("Unexpected error in users operation", e);
        sendResponse(exchange, 500, "Internal server error");
    }
}

    private void handleGetUsers(HttpExchange exchange) throws IOException, SQLException {
        List<User> users = adminService.getAllUsers();
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (User user : users) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("email", user.getEmail());
            userMap.put("createdAt", user.getCreatedAt());
            response.add(userMap);
        }
        
        sendJsonResponse(exchange, 200, response);
    }

    private void handleDeleteUser(HttpExchange exchange) throws IOException, SQLException {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.startsWith("id=")) {
            sendResponse(exchange, 400, "Missing user ID");
            return;
        }
        
        long userId = Long.parseLong(query.split("=")[1]);
        adminService.deleteUser(userId);
        sendResponse(exchange, 200, "User deleted");
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


        public Map<String, String> parseRequestBody(HttpExchange exchange) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode rootNode = objectMapper.readTree(body);
            
            if (!rootNode.isObject()) {
                throw new IOException("Expected JSON object");
            }

            Map<String, String > resultMap = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
            
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode valueNode = field.getValue();
                
                if (valueNode.isTextual()) {
                    resultMap.put(field.getKey(), valueNode.asText());
                } else if (valueNode.isNumber() || valueNode.isBoolean()) {
                    resultMap.put(field.getKey(), valueNode.toString());
                }
                // Игнорируем массивы/объекты/null
            }
            
            return resultMap;
        } catch (Exception e) {
            logger.error("Error parsing request body", e);
            throw new IOException("Invalid request format");
        }
    }

    private void validateRequiredFields(Map<String,String> node, String... fields) throws ValidationException {
        Map<String, String> missing = new HashMap<>();
        
        for (String field : fields) {
            if (!node.containsKey(field)) {
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
