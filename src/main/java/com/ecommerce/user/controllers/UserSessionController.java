package com.ecommerce.user.controllers;

import com.ecommerce.user.services.AdvancedJwtService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth/sessions")
public class UserSessionController {

    @Autowired
    private AdvancedJwtService advancedJwtService;

    /**
     * Get all active sessions for the current user
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveSessions(@RequestHeader("Authorization") String authHeader,
                                              HttpServletRequest httpRequest) {
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();

            List<AdvancedJwtService.ActiveSession> activeSessions = advancedJwtService.getActiveSessions(email);
            
            // Convert ActiveSession objects to Maps for JSON response
            List<Map<String, Object>> sessions = activeSessions.stream()
                .map(session -> {
                    Map<String, Object> sessionMap = new HashMap<>();
                    sessionMap.put("sessionId", session.getSessionId() != null ? session.getSessionId() : "");
                    sessionMap.put("deviceId", session.getDeviceId() != null ? session.getDeviceId() : "");
                    sessionMap.put("ipAddress", session.getIpAddress() != null ? session.getIpAddress() : "");
                    sessionMap.put("lastActivity", session.getLastActivity() != null ? session.getLastActivity() : "");
                    sessionMap.put("userAgent", session.getUserAgent() != null ? session.getUserAgent() : "");
                    return sessionMap;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("sessions", sessions));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid token"));
        }
    }

    /**
     * Revoke a specific session
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> revokeSession(@PathVariable String sessionId,
                                          @RequestHeader("Authorization") String authHeader,
                                          HttpServletRequest httpRequest) {
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();

            boolean revoked = advancedJwtService.revokeSession(email, sessionId);

            if (revoked) {
                return ResponseEntity.ok(Map.of("message", "Session revoked successfully"));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Session not found or already revoked"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid token"));
        }
    }

    /**
     * Revoke all sessions except the current one
     */
    @DeleteMapping("/revoke-all")
    public ResponseEntity<?> revokeAllOtherSessions(@RequestHeader("Authorization") String authHeader,
                                                    HttpServletRequest httpRequest) {
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();
            String deviceId = (String) claims.get("deviceId");

            int revokedCount = advancedJwtService.revokeAllSessionsExcept(email, deviceId);

            return ResponseEntity.ok(Map.of(
                "message", "All other sessions revoked successfully",
                "revokedCount", revokedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid token"));
        }
    }

    /**
     * List all active sessions for the current user (strict compliance: GET /api/auth/sessions)
     */
    @GetMapping("")
    public ResponseEntity<?> getActiveSessionsStrict(@RequestHeader("Authorization") String authHeader,
                                                    HttpServletRequest httpRequest) {
        return getActiveSessions(authHeader, httpRequest);
    }

    /**
     * Revoke all sessions except the current one (strict compliance: DELETE /api/auth/sessions/all-except-current)
     */
    @DeleteMapping("/all-except-current")
    public ResponseEntity<?> revokeAllOtherSessionsStrict(@RequestHeader("Authorization") String authHeader,
                                                         HttpServletRequest httpRequest) {
        return revokeAllOtherSessions(authHeader, httpRequest);
    }
}
