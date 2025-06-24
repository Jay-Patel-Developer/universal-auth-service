package com.ecommerce.user.controllers;

import com.ecommerce.user.dto.*;
import com.ecommerce.user.models.User;
import com.ecommerce.user.services.UserService;
import com.ecommerce.user.services.AdvancedJwtService;
import com.ecommerce.user.services.GdprService;
import com.ecommerce.user.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private AdvancedJwtService advancedJwtService;
    
    @Autowired
    private GdprService gdprService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationRequest request, HttpServletRequest httpRequest) {
        try {
            // Manual validation for registration
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
            }
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
            }
            
            // Get client info for security logging
            String clientIp = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            
            UserResponse userResponse = userService.registerUser(request, clientIp, userAgent);
            return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
                "user", userResponse
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody UserLoginRequest request, HttpServletRequest httpRequest) {
        try {
            // Get client info for security logging
            String clientIp = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            String deviceId = request.getDeviceId() != null ? 
                request.getDeviceId() : "default-" + UUID.randomUUID().toString();
            
            User user = userService.loginUser(
                request.getEmail(), 
                request.getPassword(),
                clientIp,
                userAgent
            );
            
            if (user != null) {
                // Check if MFA is enabled
                if (user.isMfaEnabled()) {
                    // Return challenge response requiring MFA code
                    return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                        "message", "MFA verification required",
                        "userId", user.getId(),
                        "requiresMfa", true
                    ));
                }
                
                // MFA not enabled, generate tokens
                AdvancedJwtService.TokenPair tokenPair = 
                    advancedJwtService.generateTokenPair(user.getEmail(), deviceId, clientIp);
                
                return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "user", userService.convertToUserResponse(user),
                    "accessToken", tokenPair.getAccessToken(),
                    "refreshToken", tokenPair.getRefreshToken(),
                    "expiresAt", tokenPair.getAccessTokenExpiry().getTime()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/verify-mfa")
    public ResponseEntity<?> verifyMfa(@RequestBody MfaVerifyRequest request, HttpServletRequest httpRequest) {
        try {
            // Get client info for security logging
            String clientIp = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            String deviceId = request.getDeviceId() != null ? 
                request.getDeviceId() : "default-" + UUID.randomUUID().toString();
            
            User user = userService.loginUser(
                request.getEmail(), 
                request.getPassword(),
                clientIp,
                userAgent
            );
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
            }
            
            // Verify MFA code
            boolean isValidCode = false;
            
            if (request.getTotpCode() != null) {
                // Verify TOTP code
                isValidCode = userService.verifyMfaCode(user, request.getTotpCode());
            } else if (request.getBackupCode() != null) {
                // Verify backup code
                isValidCode = userService.verifyBackupCode(user, request.getBackupCode());
            }
            
            if (!isValidCode) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid MFA code"));
            }
            
            // Generate tokens
            AdvancedJwtService.TokenPair tokenPair = 
                advancedJwtService.generateTokenPair(user.getEmail(), deviceId, clientIp);
            
            return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "user", userService.convertToUserResponse(user),
                "accessToken", tokenPair.getAccessToken(),
                "refreshToken", tokenPair.getRefreshToken(),
                "expiresAt", tokenPair.getAccessTokenExpiry().getTime()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/mfa/setup")
    public ResponseEntity<?> setupMfa(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();
            
            UserResponse user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
            }
            
            Map<String, Object> mfaSetup = userService.setupMfa(user.getId());
            
            return ResponseEntity.ok(mfaSetup);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyAndEnableMfa(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody MfaSetupRequest request) {
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();
            
            UserResponse user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
            }
            
            boolean verified = userService.verifyAndEnableMfa(user.getId(), request.getTotpCode());
            
            if (verified) {
                return ResponseEntity.ok(Map.of(
                    "message", "MFA enabled successfully",
                    "backupCodes", userService.getUserById(user.getId()).getBackupCodes()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid verification code"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/mfa/disable")
    public ResponseEntity<?> disableMfa(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody MfaSetupRequest request) {
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();
            
            UserResponse user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
            }
            
            boolean disabled = userService.disableMfa(user.getId(), request.getTotpCode());
            
            if (disabled) {
                return ResponseEntity.ok(Map.of("message", "MFA disabled successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid verification code"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/gdpr/export")
    public ResponseEntity<?> exportUserData(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();
            
            UserResponse user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
            }
            
            Map<String, Object> exportData = gdprService.exportUserData(user.getId());
            
            return ResponseEntity.ok(exportData);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/gdpr/request-deletion")
    public ResponseEntity<?> requestDataDeletion(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();
            
            UserResponse user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
            }
            
            String clientIp = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            
            boolean scheduled = gdprService.requestDeletion(user.getId(), clientIp, userAgent);
            
            if (scheduled) {
                return ResponseEntity.ok(Map.of("message", "Deletion request submitted successfully", 
                                               "scheduledFor", "30 days from now"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to schedule deletion"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/gdpr/cancel-deletion")
    public ResponseEntity<?> cancelDataDeletion(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();
            
            UserResponse user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
            }
            
            String clientIp = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            
            boolean cancelled = gdprService.cancelDeletion(user.getId(), clientIp, userAgent);
            
            if (cancelled) {
                return ResponseEntity.ok(Map.of("message", "Deletion request cancelled successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to cancel deletion"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();
            
            UserResponse user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
            }
            
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            String clientIp = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            
            boolean changed = userService.changePassword(user.getId(), oldPassword, newPassword, clientIp, userAgent);
            
            if (changed) {
                return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid old password"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/sessions")
    public ResponseEntity<?> getUserSessions(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();
            
            // Get user sessions from JWT service
            List<Map<String, Object>> sessions = advancedJwtService.getUserSessions(email);
            
            return ResponseEntity.ok(Map.of("sessions", sessions));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();
            
            // Get user details
            UserResponse user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
            }
            
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "user", user
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "valid", false,
                "error", e.getMessage()
            ));
        }
    }
}