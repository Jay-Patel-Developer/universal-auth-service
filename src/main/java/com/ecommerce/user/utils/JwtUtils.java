package com.ecommerce.user.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtils {
    
    @Value("${app.jwt.secret}")
    private String secretKeyString;
    
    @Value("${app.jwt.expiration-ms:86400000}") // Default 24 hours if not specified
    private long expirationTimeMs;
    
    private SecretKey key;
    
    // Initialize the key after properties are set
    public void initKey() {
        this.key = Keys.hmacShaKeyFor(secretKeyString.getBytes());
    }
    
    // This will be called after all properties are set
    @jakarta.annotation.PostConstruct
    public void init() {
        initKey();
    }
    
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTimeMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}