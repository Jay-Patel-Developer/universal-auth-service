package com.ecommerce.user.controllers;

import com.ecommerce.user.dto.PasswordResetRequest;
import com.ecommerce.user.services.PasswordResetService;
import com.ecommerce.user.services.RateLimitService;
import com.ecommerce.user.services.SecurityAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * REST controller for password reset endpoints.
 * Handles password reset requests, token validation, and password updates.
 */
@RestController
@RequestMapping("/api/auth/password")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;
    
    @Autowired
    private SecurityAuditService securityAuditService;
    
    @Autowired
    private RateLimitService rateLimitService;
    
    /**
     * POST /api/auth/password/request-reset
     * Initiates a password reset by sending a reset email if allowed by rate limits.
     * Always returns a generic message to prevent email enumeration.
     */
    @PostMapping("/request-reset")
    public ResponseEntity<?> requestPasswordReset(@RequestBody Map<String, String> request, 
                                                HttpServletRequest httpRequest) {
        String email = request.get("email");
        String clientIp = httpRequest.getRemoteAddr();
        
        // Check rate limits
        if (!rateLimitService.isAllowed(clientIp, RateLimitService.RateLimitType.PASSWORD_RESET)) {
            securityAuditService.logSuspiciousActivity(email, clientIp, "Password reset rate limit exceeded");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Too many password reset attempts. Try again later."));
        }
        
        try {
            boolean sent = passwordResetService.sendPasswordResetEmail(email);
            
            // Always return success to prevent email enumeration
            return ResponseEntity.ok(Map.of("message", 
                "If your email exists in our system, you will receive password reset instructions."));
            
        } catch (Exception e) {
            // Log error but don't expose it
            return ResponseEntity.ok(Map.of("message", 
                "If your email exists in our system, you will receive password reset instructions."));
        }
    }
    
    /**
     * GET /api/auth/password/verify-token/{token}
     * Verifies the validity of a password reset token.
     * @param token Reset token
     * @return JSON with valid=true/false and error if invalid
     */
    @GetMapping("/verify-token/{token}")
    public ResponseEntity<?> verifyResetToken(@PathVariable String token, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        
        try {
            boolean valid = passwordResetService.validatePasswordResetToken(token);
            if (valid) {
                return ResponseEntity.ok(Map.of("valid", true));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("valid", false, "error", "Invalid or expired token"));
            }
        } catch (Exception e) {
            securityAuditService.logSuspiciousActivity(null, clientIp, 
                "Invalid password reset token validation attempt: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("valid", false, "error", "Invalid or expired token"));
        }
    }
    
    /**
     * POST /api/auth/password/reset
     * Resets the user's password using a valid token.
     * @param request PasswordResetRequest with token and new password
     * @return Success or error message
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest request, 
                                          HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        
        try {
            String email = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            
            if (email != null) {
                securityAuditService.logPasswordChange(email, clientIp);
                return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to reset password. Invalid or expired token."));
            }
        } catch (Exception e) {
            securityAuditService.logSuspiciousActivity(null, clientIp, 
                "Failed password reset attempt: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Failed to reset password. " + e.getMessage()));
        }
    }
}
