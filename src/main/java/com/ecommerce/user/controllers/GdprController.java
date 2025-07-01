package com.ecommerce.user.controllers;

import com.ecommerce.user.config.FeatureConfiguration;
import com.ecommerce.user.services.AdvancedJwtService;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.services.UserService;
import com.ecommerce.user.services.GdprService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * REST controller for GDPR compliance endpoints.
 * Handles user data export, deletion requests, and cancellation.
 */
@RestController
@RequestMapping("/api/auth/gdpr")
public class GdprController {

    @Autowired
    private GdprService gdprService;

    @Autowired
    private AdvancedJwtService advancedJwtService;

    @Autowired
    private UserService userService;

    @Autowired
    private FeatureConfiguration featureConfiguration;

    /**
     * POST /api/auth/gdpr/export
     * Exports all user data for GDPR compliance (Right to Access).
     * @param authHeader Bearer token for authentication
     * @return User data as JSON or error
     */
    @PostMapping("/export")
    public ResponseEntity<?> exportUserData(
            @RequestHeader("Authorization") String authHeader) {
        if (!featureConfiguration.getBusiness().isGdprComplianceEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "GDPR export is currently disabled by configuration"));
        }
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();
            
            UserResponse user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }
            
            Map<String, Object> userData = gdprService.exportUserData(user.getId());
            
            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    /**
     * POST /api/auth/gdpr/request-deletion
     * Requests account deletion (Right to be Forgotten).
     * @param authHeader Bearer token for authentication
     * @return Success or error message
     */
    @PostMapping("/request-deletion")
    public ResponseEntity<?> requestDataDeletion(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {
        if (!featureConfiguration.getBusiness().isGdprComplianceEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "GDPR deletion is currently disabled by configuration"));
        }
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();
            
            UserResponse user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }
            
            String clientIp = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            
            boolean requested = gdprService.requestDataDeletion(user.getId(), clientIp, userAgent);
            
            if (requested) {
                return ResponseEntity.ok(Map.of(
                    "message", "Deletion request submitted successfully",
                    "gracePeriod", "30 days"
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Deletion request failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    /**
     * POST /api/auth/gdpr/cancel-deletion
     * Cancels a pending account deletion request.
     * @param authHeader Bearer token for authentication
     * @return Success or error message
     */
    @PostMapping("/cancel-deletion")
    public ResponseEntity<?> cancelDataDeletion(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {
        if (!featureConfiguration.getBusiness().isGdprComplianceEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "GDPR cancellation is currently disabled by configuration"));
        }
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();
            
            UserResponse user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }
            
            String clientIp = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            
            boolean cancelled = gdprService.cancelDataDeletion(user.getId(), clientIp, userAgent);
            
            if (cancelled) {
                return ResponseEntity.ok(Map.of(
                    "message", "Deletion request cancelled successfully"
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No active deletion request found"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
