package com.otp;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.otp.api.AdminController;
import com.otp.api.AuthController;
import com.otp.api.UserController;
import com.otp.dao.OtpCodeDao;
import com.otp.dao.OtpConfigDao;
import com.otp.dao.UserDao;
import com.otp.middleware.AdminAuthMiddleware;
import com.otp.notification.NotificationService;
import com.otp.service.AdminService;
import com.otp.service.AuthService;
import com.otp.service.JwtService;
import com.otp.service.OtpService;
import com.otp.service.PasswordEncoder;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        try {
            // Инициализация конфигурации
            Properties config = loadConfiguration();
            
            // Подключение к БД
            Connection connection = createDatabaseConnection(config);
            
            
            // Инициализация DAO
            UserDao userDao = new UserDao(connection);
            OtpCodeDao otpCodeDao = new OtpCodeDao(connection);
            OtpConfigDao otpConfigDao = new OtpConfigDao(connection);
           
            NotificationService notificationService = new NotificationService(userDao, config);
            PasswordEncoder passwordEncoder = new PasswordEncoder();
            // Инициализация сервисов
            JwtService jwtService = new JwtService(
                config.getProperty("jwt.secret"),
                Integer.parseInt(config.getProperty("jwt.expiration")),
                userDao
            );
            
            AuthService authService = new AuthService(userDao, jwtService, passwordEncoder);
            OtpService otpService = new OtpService(otpCodeDao, otpConfigDao, notificationService);
            AdminService adminService = new AdminService(userDao, otpConfigDao, otpCodeDao);
            
            // Запуск фонового процесса очистки просроченных кодов
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleWithFixedDelay(
                () -> {
                    try {
                        int expired = otpCodeDao.expireOldCodes();
                        if (expired > 0) {
                            logger.info("Expired {} old OTP codes", expired);
                        }
                    } catch (SQLException e) {
                        logger.error("Error expiring old OTP codes", e);
                    }
                },
                0, 60, TimeUnit.SECONDS
            );
            
            // Запуск HTTP сервера
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            AdminController adminController = new AdminController(adminService, authService);

            // Обертываем в middleware
            HttpHandler adminConfigHandler = new AdminAuthMiddleware(
                adminController::handleConfig,
                authService
            );

            HttpHandler adminUsersHandler = new AdminAuthMiddleware(
                adminController::handleUsers,
                authService
            );
            
            // Регистрация обработчиков
            server.createContext("/api/auth/register", new AuthController(authService)::handleRegister);
            server.createContext("/api/auth/login", new AuthController(authService)::handleLogin);
            server.createContext("/api/user/otp/generate", new UserController(otpService, authService)::handleGenerateOtp);
            server.createContext("/api/user/otp/validate", new UserController(otpService, authService)::handleValidateOtp);
            server.createContext("/api/admin/config", adminConfigHandler);
            server.createContext("/api/admin/users", adminUsersHandler);
            
            server.setExecutor(null);
            server.start();
            
            logger.info("OTP Service started on port 8080");
            
            // Graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down OTP Service...");
                server.stop(5);
                scheduler.shutdown();
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Error closing database connection", e);
                }
            }));
            
        } catch (Exception e) {
            logger.error("Failed to start OTP Service", e);
            System.exit(1);
        }
    }
    
    private static Connection createDatabaseConnection(Properties config) throws SQLException {
        String url = config.getProperty("db.url");
        String username = config.getProperty("db.username");
        String password = config.getProperty("db.password");
        
        return DriverManager.getConnection(url, username, password);
    }

    private static Properties loadConfiguration() {
        Properties properties = new Properties();
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("application.properties")) {
            
            if (input == null) {
                throw new IllegalStateException("Config file not found! Please create config.properties in resources");
            }
            
            properties.load(input);
            return properties;
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }
}