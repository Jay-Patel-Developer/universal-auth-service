package com.ecommerce.user.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Service for logging security-related events
 */
@Service
public class SecurityAuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditService.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    @Value("${security.audit.log.enabled:true}")
    private boolean auditLogEnabled;
    
    /**
     * Log a security event
     * 
     * @param userEmail The user's email (can be null for anonymous events)
     * @param clientIp The client IP address (can be null)
     * @param userAgent The user agent string (can be null)
     * @param eventData Additional event data
     */
    public void logSecurityEvent(String userEmail, String clientIp, String userAgent, Map<String, Object> eventData) {
        if (!auditLogEnabled) {
            return;
        }
        
        try {
            String eventId = UUID.randomUUID().toString();
            LocalDateTime timestamp = LocalDateTime.now();
            
            // Store in database
            storeEventInDatabase(eventId, timestamp, userEmail, clientIp, userAgent, eventData);
            
            // Cache in Redis if available (for real-time monitoring)
            if (redisTemplate != null) {
                cacheEventInRedis(eventId, timestamp, userEmail, clientIp, userAgent, eventData);
            }
            
            // Log event
            logger.info("Security event: {}, user: {}, IP: {}, data: {}", 
                    eventData.getOrDefault("action", "UNKNOWN_ACTION"),
                    userEmail != null ? userEmail : "anonymous",
                    clientIp != null ? clientIp : "unknown",
                    eventData);
            
        } catch (Exception e) {
            logger.error("Failed to log security event: {}", e.getMessage());
        }
    }
    
    /**
     * Log a successful login
     */
    public void logSuccessfulLogin(String userEmail, String clientIp, String userAgent) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("action", "LOGIN_SUCCESS");
        eventData.put("userAgent", userAgent);
        logSecurityEvent(userEmail, clientIp, userAgent, eventData);
    }
    
    /**
     * Log a failed login
     */
    public void logFailedLogin(String userEmail, String clientIp, String userAgent, String reason) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("action", "LOGIN_FAILED");
        eventData.put("reason", reason);
        eventData.put("userAgent", userAgent);
        logSecurityEvent(userEmail, clientIp, userAgent, eventData);
    }
    
    /**
     * Log a password change
     */
    public void logPasswordChange(String userEmail, String clientIp) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("action", "PASSWORD_CHANGED");
        logSecurityEvent(userEmail, clientIp, null, eventData);
    }
    
    /**
     * Log a suspicious activity
     */
    public void logSuspiciousActivity(String userEmail, String clientIp, String description) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("action", "SUSPICIOUS_ACTIVITY");
        eventData.put("description", description);
        logSecurityEvent(userEmail, clientIp, null, eventData);
    }
    
    /**
     * Log an account lockout
     */
    public void logAccountLockout(String userEmail, String clientIp, String reason) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("action", "ACCOUNT_LOCKOUT");
        eventData.put("reason", reason);
        logSecurityEvent(userEmail, clientIp, null, eventData);
    }
    
    /**
     * Store event in the database
     */
    private void storeEventInDatabase(String eventId, LocalDateTime timestamp, 
            String userEmail, String clientIp, String userAgent, Map<String, Object> eventData) {
        
        try {
            String action = eventData.getOrDefault("action", "UNKNOWN").toString();
            String details = eventData.toString();
            
            jdbcTemplate.update(
                "INSERT INTO security_audit_log (event_id, timestamp, user_email, client_ip, " +
                "user_agent, action, details) VALUES (?, ?, ?, ?, ?, ?, ?)",
                eventId, timestamp, userEmail, clientIp, userAgent, action, details
            );
        } catch (Exception e) {
            logger.error("Failed to store security event in database: {}", e.getMessage());
        }
    }
    
    /**
     * Cache event in Redis for real-time monitoring
     */
    private void cacheEventInRedis(String eventId, LocalDateTime timestamp, 
            String userEmail, String clientIp, String userAgent, Map<String, Object> eventData) {
        
        try {
            String key = "security_event:" + eventId;
            
            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("id", eventId);
            eventMap.put("timestamp", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            eventMap.put("userEmail", userEmail);
            eventMap.put("clientIp", clientIp);
            eventMap.put("userAgent", userAgent);
            eventMap.putAll(eventData);
            
            redisTemplate.opsForValue().set(key, eventMap);
            redisTemplate.expire(key, 7, java.util.concurrent.TimeUnit.DAYS);
            
            // Add to recent events list
            redisTemplate.opsForList().leftPush("recent_security_events", eventId);
            redisTemplate.opsForList().trim("recent_security_events", 0, 999);
        } catch (Exception e) {
            logger.error("Failed to cache security event in Redis: {}", e.getMessage());
        }
    }
    
    /**
     * Generate security audit report
     */
    public Map<String, Object> generateSecurityReport() {
        Map<String, Object> report = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        
        try {
            // Failed login attempts in the last 24 hours
            int failedLogins = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM security_audit_log WHERE action = 'LOGIN_FAILED' " +
                "AND timestamp BETWEEN ? AND ?",
                Integer.class,
                yesterday, now
            );
            
            // Successful logins in the last 24 hours
            int successfulLogins = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM security_audit_log WHERE action = 'LOGIN_SUCCESS' " +
                "AND timestamp BETWEEN ? AND ?",
                Integer.class,
                yesterday, now
            );
            
            // Suspicious activities in the last 24 hours
            int suspiciousActivities = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM security_audit_log WHERE action = 'SUSPICIOUS_ACTIVITY' " +
                "AND timestamp BETWEEN ? AND ?",
                Integer.class,
                yesterday, now
            );
            
            report.put("reportPeriod", "Last 24 hours");
            report.put("generatedAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            report.put("failedLogins", failedLogins);
            report.put("successfulLogins", successfulLogins);
            report.put("suspiciousActivities", suspiciousActivities);
            report.put("loginSuccessRate", calculateSuccessRate(successfulLogins, failedLogins));
            
            // Add top 5 failed login IPs
            List<Map<String, Object>> topFailedLoginIps = jdbcTemplate.queryForList(
                "SELECT client_ip, COUNT(*) as count FROM security_audit_log " +
                "WHERE action = 'LOGIN_FAILED' AND timestamp BETWEEN ? AND ? " +
                "GROUP BY client_ip ORDER BY count DESC LIMIT 5",
                yesterday, now
            );
            report.put("topFailedLoginIps", topFailedLoginIps);
            
        } catch (Exception e) {
            logger.error("Failed to generate security report: {}", e.getMessage());
            report.put("error", "Failed to generate complete report: " + e.getMessage());
        }
        
        return report;
    }
    
    /**
     * Calculate success rate percentage
     */
    private double calculateSuccessRate(int successes, int failures) {
        int total = successes + failures;
        if (total == 0) {
            return 100.0; // No login attempts, consider 100% success
        }
        return (double) successes / total * 100.0;
    }
}