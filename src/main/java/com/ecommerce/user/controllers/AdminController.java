package com.ecommerce.user.controllers;

import com.ecommerce.user.services.SecurityAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for admin and monitoring endpoints.
 * Provides security audit reports and health checks.
 */
@RestController
@RequestMapping("/api/auth/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private SecurityAuditService securityAuditService;

    /**
     * GET /api/auth/admin/security-report
     * Returns a security audit report for admin monitoring.
     * @return Security report as JSON
     */
    @GetMapping("/security-report")
    public ResponseEntity<?> getSecurityReport() {
        Map<String, Object> report = securityAuditService.generateSecurityReport();
        return ResponseEntity.ok(report);
    }

    /**
     * GET /api/auth/admin/health
     * Returns a simple health check status.
     * @return Service status as JSON
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        // Simple health check, can be extended for more details
        return ResponseEntity.ok(Map.of("status", "UP", "timestamp", System.currentTimeMillis()));
    }
}
