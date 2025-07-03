package com.otp.service;

import java.sql.SQLException;
import java.util.Optional;

import com.otp.dao.UserDao;
import com.otp.model.User;
import com.otp.model.User.Role;

import io.jsonwebtoken.JwtException;

public class AuthService {
    private final UserDao userDao;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserDao userDao, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public User authenticate(String token) throws SecurityException, SQLException {
        try {
            return jwtService.validateToken(token);
        } catch (JwtException e) {
            throw new SecurityException("Invalid token", e);
        }
    }

    public boolean adminExists() throws SQLException {
        return userDao.hasAdmin();
    }

    public User register(String username, String password, Role role) throws SQLException {
        if (role == Role.ADMIN && adminExists()) {
            throw new IllegalStateException("Admin already exists");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        
        userDao.save(user);
        return user;
    }

    public String login(String username, String password) throws SQLException {
        Optional<User> userOpt = userDao.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new SecurityException("User not found");
        }
        
        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new SecurityException("Invalid password");
        }
        
        return jwtService.generateToken(user);
    }
}