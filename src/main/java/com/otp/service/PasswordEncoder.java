package com.otp.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordEncoder {
    private static final SecureRandom random = new SecureRandom();
    
    public String encode(String password) {
        try {
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            
            return Base64.getEncoder().encodeToString(salt) + "$" +
                   Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }
    
    public boolean matches(String password, String encodedPassword) {
        try {
            String[] parts = encodedPassword.split("\\$");
            if (parts.length != 2) return false;
            
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] storedHash = Base64.getDecoder().decode(parts[1]);
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] testHash = md.digest(password.getBytes());
            
            return MessageDigest.isEqual(storedHash, testHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Password verification failed", e);
        }
    }
}