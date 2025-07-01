package com.ecommerce.user.services;

import com.ecommerce.user.models.User;
import com.ecommerce.user.repositories.UserRepository;
import com.ecommerce.user.utils.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling password reset operations.
 * Generates reset tokens, sends emails, validates tokens, and updates passwords.
 */
@Service
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private InputValidationService inputValidationService;
    
    @Value("${password-reset.token-validity-minutes:30}")
    private int tokenValidityMinutes;
    
    /**
     * Generate and send password reset email.
     * @param email User's email address
     * @return true if email sent (or user not found), false if input invalid
     */
    public boolean sendPasswordResetEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        email = inputValidationService.sanitizeInput(email);
        User user = userRepository.findByEmail(email);
        
        // If user doesn't exist, still return true to prevent email enumeration
        if (user == null) {
            return true;
        }
        
        // Generate unique token
        String token = UUID.randomUUID().toString();
        
        // Store token with user email in Redis with expiration
        String tokenKey = "password_reset:" + token;
        redisTemplate.opsForValue().set(tokenKey, email, tokenValidityMinutes, TimeUnit.MINUTES);
        
        // Send email with reset link
        String resetLink = "https://yourdomain.com/reset-password?token=" + token;
        String subject = "Password Reset Request";
        String body = "Hello " + user.getName() + ",\n\n" +
                "You requested to reset your password. Please click the link below to reset your password:\n\n" +
                resetLink + "\n\n" +
                "This link will expire in " + tokenValidityMinutes + " minutes.\n\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "Best regards,\nYour Application Team";
        
        return emailService.sendEmail(email, subject, body);
    }
    
    /**
     * Validate a password reset token.
     * @param token Password reset token
     * @return true if valid, false otherwise
     */
    public boolean validatePasswordResetToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        String tokenKey = "password_reset:" + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
    }
    
    /**
     * Reset password using token.
     * @param token Password reset token
     * @param newPassword New password to set
     * @return email of the user if successful, null otherwise
     */
    public String resetPassword(String token, String newPassword) {
        if (!validatePasswordResetToken(token)) {
            return null;
        }
        
        // Password validation
        if (inputValidationService.validatePassword(newPassword).isInvalid()) {
            throw new RuntimeException("Password does not meet security requirements");
        }
        
        // Get email from token
        String tokenKey = "password_reset:" + token;
        String email = redisTemplate.opsForValue().get(tokenKey);
        
        // Find user and update password
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return null;
        }
        
        // Update password
        user.setPassword(PasswordUtils.hashPassword(newPassword));
        userRepository.save(user);
        
        // Invalidate token
        redisTemplate.delete(tokenKey);
        
        return email;
    }
}
