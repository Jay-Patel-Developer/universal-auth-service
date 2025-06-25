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

@RestController
@RequestMapping("/api/auth/sessions")
public class UserSessionController {
    @Autowired
    private AdvancedJwtService advancedJwtService;

    @GetMapping("")
    public ResponseEntity<?> getActiveSessions(@RequestHeader("Authorization") String authHeader, HttpServletRequest httpRequest) {
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();
            List<AdvancedJwtService.ActiveSession> activeSessions = advancedJwtService.getActiveSessions(email);
            List<Map<String, Object>> sessions = new java.util.ArrayList<>();
            for (AdvancedJwtService.ActiveSession session : activeSessions) {
                Map<String, Object> sessionMap = new java.util.HashMap<>();
                sessionMap.put("sessionId", session.getSessionId());
                sessionMap.put("deviceId", session.getDeviceId());
                sessionMap.put("ipAddress", session.getIpAddress());
                sessionMap.put("lastActivity", session.getLastActivity());
                sessionMap.put("userAgent", session.getUserAgent());
                sessions.add(sessionMap);
            }
            return ResponseEntity.ok(Map.of("sessions", sessions));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> revokeSession(@PathVariable String sessionId, @RequestHeader("Authorization") String authHeader, HttpServletRequest httpRequest) {
        try {
            String token = authHeader.substring(7);
            Claims claims = advancedJwtService.validateToken(token);
            String email = claims.getSubject();
            boolean revoked = advancedJwtService.revokeSession(email, sessionId);
            if (revoked) {
                return ResponseEntity.ok(Map.of("message", "Session revoked successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Session not found or already revoked"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
    }

    @DeleteMapping("/all-except-current")
    public ResponseEntity<?> revokeAllOtherSessions(@RequestHeader("Authorization") String authHeader, HttpServletRequest httpRequest) {
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
    }
}
