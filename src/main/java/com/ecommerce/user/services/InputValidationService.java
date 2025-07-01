package com.ecommerce.user.services;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;
import java.util.List;
import java.util.Arrays;

@Service
public class InputValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(InputValidationService.class);
    
    // Common XSS patterns
    private static final List<Pattern> XSS_PATTERNS = Arrays.asList(
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onload", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onerror", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onclick", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<iframe", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<object", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<embed", Pattern.CASE_INSENSITIVE)
    );
    
    // SQL injection patterns
    private static final List<Pattern> SQL_INJECTION_PATTERNS = Arrays.asList(
        Pattern.compile("('|(\\-\\-)|(;)|(\\|)|(\\*)|(%7C))", Pattern.CASE_INSENSITIVE),
        Pattern.compile("((%3D)|(=))[^\\n]*((%27)|(')|(\\-\\-)|((%3B)|(;)))", Pattern.CASE_INSENSITIVE),
        Pattern.compile("w*((%27)|('))*((%6F)|o|(%4F))*((%72)|r|(%52))", Pattern.CASE_INSENSITIVE),
        Pattern.compile("((%27)|('))union", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * Sanitize input by removing potentially dangerous characters
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove null bytes
        String sanitized = input.replace("\0", "");
        
        // Remove control characters except tab, newline, and carriage return
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        // Trim whitespace
        sanitized = sanitized.trim();
        
        return sanitized;
    }
    
    /**
     * Check if input contains malicious content
     */
    public boolean containsMaliciousContent(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        
        // Check for XSS patterns
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(lowerInput).find()) {
                logger.warn("XSS pattern detected in input: {}", input.substring(0, Math.min(50, input.length())));
                return true;
            }
        }
        
        // Check for SQL injection patterns
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(lowerInput).find()) {
                logger.warn("SQL injection pattern detected in input: {}", input.substring(0, Math.min(50, input.length())));
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Validate email format
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        
        Pattern emailPattern = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
        );
        
        return emailPattern.matcher(email).matches();
    }
    
    /**
     * Validate phone number format
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }
        
        // Simple phone number validation (international format)
        Pattern phonePattern = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
        
        return phonePattern.matcher(phoneNumber.replaceAll("[\\s\\-\\(\\)]", "")).matches();
    }
    
    /**
     * Validate password strength
     */
    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        // Check for at least one uppercase, lowercase, digit, and special character
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    }
    
    /**
     * Validate password with detailed validation result
     */
    public ValidationResult validatePassword(String password) {
        ValidationResult result = new ValidationResult();
        
        if (password == null || password.trim().isEmpty()) {
            result.addError("Password is required");
            return result;
        }
        
        if (password.length() < 8) {
            result.addError("Password must be at least 8 characters long");
        }
        
        if (!password.matches(".*[a-z].*")) {
            result.addError("Password must contain at least one lowercase letter");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            result.addError("Password must contain at least one uppercase letter");
        }
        
        if (!password.matches(".*\\d.*")) {
            result.addError("Password must contain at least one digit");
        }
        
        if (!password.matches(".*[@$!%*?&].*")) {
            result.addError("Password must contain at least one special character (@$!%*?&)");
        }
        
        return result;
    }
    
    /**
     * Comprehensive input validation
     */
    public ValidationResult validateInput(String input, String fieldName) {
        ValidationResult result = new ValidationResult();
        
        if (input == null || input.trim().isEmpty()) {
            result.addError(fieldName + " is required");
            return result;
        }
        
        if (containsMaliciousContent(input)) {
            result.addError(fieldName + " contains invalid characters");
        }
        
        return result;
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private boolean valid = true;
        private StringBuilder errors = new StringBuilder();
        
        public void addError(String error) {
            if (errors.length() > 0) {
                errors.append("; ");
            }
            errors.append(error);
            this.valid = false;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public boolean isInvalid() {
            return !valid;
        }
        
        public String getErrors() {
            return errors.toString();
        }
        
        @Override
        public String toString() {
            return valid ? "Valid" : "Invalid: " + errors.toString();
        }
    }
}