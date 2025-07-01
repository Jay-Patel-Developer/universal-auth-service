package com.ecommerce.user.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
public class SecurityAuditFilter extends OncePerRequestFilter {

    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE");

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestId = UUID.randomUUID().toString();
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String sessionId = request.getSession(false) != null ? request.getSession().getId() : "anonymous";
        
        // Add to MDC for structured logging
        MDC.put("requestId", requestId);
        MDC.put("clientIp", clientIp);
        MDC.put("method", method);
        MDC.put("uri", uri);
        MDC.put("sessionId", sessionId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Log incoming request
            logSecurityEvent("REQUEST_RECEIVED", request, null);
            
            filterChain.doFilter(request, response);
            
            // Log successful response
            long duration = System.currentTimeMillis() - startTime;
            logPerformanceMetrics(request, response, duration);
            
            if (isSecuritySensitiveEndpoint(uri)) {
                logSecurityEvent("REQUEST_COMPLETED", request, response);
            }
            
        } catch (Exception e) {
            // Log security exceptions
            logSecurityEvent("REQUEST_FAILED", request, response, e);
            throw e;
        } finally {
            // Clean up MDC
            MDC.clear();
        }
    }

    private void logSecurityEvent(String eventType, HttpServletRequest request, 
                                HttpServletResponse response) {
        logSecurityEvent(eventType, request, response, null);
    }

    private void logSecurityEvent(String eventType, HttpServletRequest request, 
                                HttpServletResponse response, Exception exception) {
        try {
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String referer = request.getHeader("Referer");
            String authorization = request.getHeader("Authorization");
            boolean hasAuth = authorization != null && !authorization.isEmpty();
            
            StringBuilder logMessage = new StringBuilder()
                .append("Security Event: ").append(eventType)
                .append(" | IP: ").append(clientIp)
                .append(" | Method: ").append(request.getMethod())
                .append(" | URI: ").append(request.getRequestURI())
                .append(" | UserAgent: ").append(userAgent != null ? userAgent : "Unknown")
                .append(" | HasAuth: ").append(hasAuth)
                .append(" | Referer: ").append(referer != null ? referer : "None");
            
            if (response != null) {
                logMessage.append(" | Status: ").append(response.getStatus());
            }
            
            if (exception != null) {
                logMessage.append(" | Error: ").append(exception.getClass().getSimpleName())
                         .append(" - ").append(exception.getMessage());
            }
            
            // Log at appropriate level based on event type and response status
            if (exception != null || (response != null && response.getStatus() >= 400)) {
                securityLogger.warn(logMessage.toString());
            } else if (isSecuritySensitiveEndpoint(request.getRequestURI())) {
                securityLogger.info(logMessage.toString());
            } else {
                securityLogger.debug(logMessage.toString());
            }
            
        } catch (Exception e) {
            securityLogger.error("Failed to log security event", e);
        }
    }

    private void logPerformanceMetrics(HttpServletRequest request, HttpServletResponse response, long duration) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        int status = response.getStatus();
        
        StringBuilder perfMessage = new StringBuilder()
            .append("Performance: ")
            .append(method).append(" ").append(uri)
            .append(" | Duration: ").append(duration).append("ms")
            .append(" | Status: ").append(status);
        
        // Log slow requests as warnings
        if (duration > 2000) { // > 2 seconds
            performanceLogger.warn(perfMessage.toString());
        } else if (duration > 1000) { // > 1 second
            performanceLogger.info(perfMessage.toString());
        } else {
            performanceLogger.debug(perfMessage.toString());
        }
    }

    private boolean isSecuritySensitiveEndpoint(String uri) {
        return uri.startsWith("/api/auth/") || 
               uri.startsWith("/api/admin/") || 
               uri.startsWith("/actuator/") ||
               uri.contains("password") ||
               uri.contains("token");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}