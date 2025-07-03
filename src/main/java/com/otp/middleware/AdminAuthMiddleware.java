package com.otp.middleware;

import java.io.IOException;
import java.io.OutputStream;

import com.otp.model.User;
import com.otp.service.AuthService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class AdminAuthMiddleware implements HttpHandler {
    private final HttpHandler next;
    private final AuthService authService;

    public AdminAuthMiddleware(HttpHandler next, AuthService authService) {
        this.next = next;
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            // Проверка заголовка Authorization
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendResponse(exchange, 401, "Missing Authorization header");
                return;
            }

            // Валидация токена
            String token = authHeader.substring(7);
            User user = authService.authenticate(token);

            // Проверка роли
            if (user.getRole() != User.Role.ADMIN) {
                sendResponse(exchange, 403, "Admin access required");
                return;
            }

            // Если проверки пройдены, передаем запрос дальше
            next.handle(exchange);

        } catch (SecurityException e) {
            sendResponse(exchange, 401, "Invalid token: " + e.getMessage());
        } catch (Exception e) {
            sendResponse(exchange, 500, "Internal server error");
            e.printStackTrace();
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}