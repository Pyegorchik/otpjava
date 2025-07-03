package com.otp.api;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.otp.model.User;
import com.otp.service.AuthService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    public void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method not allowed");
            return;
        }

        try {
            Map<String,String> request = parseRequestBody(exchange);
            String username = request.get("username");
            String password = request.get("password");
            String role = request.get("role");

            // Проверка существования администратора
            if (User.Role.ADMIN.name().equalsIgnoreCase(role) && authService.adminExists()) {
                sendResponse(exchange, 409, "Admin already exists");
                return;
            }

            User user = authService.register(username, password, User.Role.valueOf(role.toUpperCase()));
            sendJsonResponse(exchange, 201, Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole().name()
            ));
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, "Invalid role");
        } catch (Exception e) {
            sendResponse(exchange, 500, "Registration failed");
        }
    }

    public void handleLogin(HttpExchange exchange) throws IOException {
        try {
            Map<String, String> request = parseRequestBody(exchange);
            String username = request.get("username");
            String password = request.get("password");

            String token = authService.login(username, password);
            sendJsonResponse(exchange, 200, Map.of("token", token));
        } catch (SecurityException e) {
            sendResponse(exchange, 401, "Invalid credentials");
        } catch (Exception e) {
            sendResponse(exchange, 500, "Login failed");
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

    public Map<String, String> parseRequestBody(HttpExchange exchange) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode rootNode = objectMapper.readTree(body);
            
            if (!rootNode.isObject()) {
                throw new IOException("Expected JSON object");
            }

            Map<String, String> resultMap = new HashMap<>();
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

}