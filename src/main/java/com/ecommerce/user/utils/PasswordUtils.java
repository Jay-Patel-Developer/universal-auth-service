package com.ecommerce.user.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtils {
    
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public static String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    
    public static boolean verifyPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Raw password cannot be null");
        }
        try {
            return passwordEncoder.matches(rawPassword, hashedPassword);
        } catch (Exception e) {
            return false; // Don't throw exception for invalid hash, just return false
        }
    }
}
