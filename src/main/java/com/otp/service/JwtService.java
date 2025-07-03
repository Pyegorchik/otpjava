package com.otp.service;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Date;

import com.otp.dao.UserDao;
import com.otp.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

public class JwtService {
    private final String secretKey;
    private final int expiration;
    private final UserDao userDao;

    public JwtService(String secretKey, int expiration, UserDao userDao) {
        this.secretKey = secretKey;
        this.expiration = expiration;
        this.userDao = userDao;
    }

    public String generateToken(User user) {
        return Jwts.builder()
            .setSubject(user.getUsername())
            .claim("userId", user.getId())
            .claim("role", user.getRole().name())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
            .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), 
                      SignatureAlgorithm.HS512)
            .compact();
    }

    public User validateToken(String token) throws JwtException, SQLException {
        Claims claims = parseToken(token);
        Long userId = claims.get("userId", Long.class);
        
        return userDao.findById(userId)
            .orElseThrow(() -> new JwtException("User not found for token"));
    }

    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}