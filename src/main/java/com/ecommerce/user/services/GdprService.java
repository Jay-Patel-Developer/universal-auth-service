package com.ecommerce.user.services;

import com.ecommerce.user.config.FeatureConfiguration;
import com.ecommerce.user.models.User;
import com.ecommerce.user.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class GdprService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SecurityAuditService securityAuditService;
    
    @Autowired
    private FeatureConfiguration featureConfiguration;
    
    @Value("${gdpr.retention.days:365}")
    private int retentionDays;
    
    /**
     * Request user data export (GDPR Right to Access)
     */
    public Map<String, Object> exportUserData(Long userId) {
        if (!featureConfiguration.getBusiness().isGdprComplianceEnabled()) {
            throw new RuntimeException("GDPR export is currently disabled by configuration");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("personalData", Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "phoneNumber", user.getPhoneNumber(),
            "status", user.getStatus(),
            "createdAt", user.getCreatedAt(),
            "updatedAt", user.getUpdatedAt()
        ));
        
        exportData.put("securityData", Map.of(
            "mfaEnabled", user.isMfaEnabled(),
            "lastLogin", user.getLastLogin(),
            "roles", user.getRoles()
        ));
        
        exportData.put("exportedAt", LocalDateTime.now());
        exportData.put("exportFormat", "JSON");
        
        // Log the data export request
        securityAuditService.logSecurityEvent(user.getEmail(), null, null, 
            Map.of("action", "DATA_EXPORT_REQUESTED"));
        
        return exportData;
    }
    
    /**
     * Request account deletion (GDPR Right to be Forgotten)
     */
    @Transactional
    public boolean requestDeletion(Long userId, String clientIp, String userAgent) {
        if (!featureConfiguration.getBusiness().isGdprComplianceEnabled()) {
            throw new RuntimeException("GDPR deletion is currently disabled by configuration");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Mark for deletion after grace period
        user.setDeletionRequestedAt(LocalDateTime.now());
        user.setScheduledDeletionAt(LocalDateTime.now().plusDays(30)); // 30-day grace period
        userRepository.save(user);
        
        // Log deletion request
        securityAuditService.logSecurityEvent(user.getEmail(), clientIp, userAgent,
            Map.of("action", "DELETION_REQUESTED", "scheduledFor", user.getScheduledDeletionAt()));
        
        return true;
    }
    
    /**
     * Cancel deletion request
     */
    @Transactional
    public boolean cancelDeletion(Long userId, String clientIp, String userAgent) {
        if (!featureConfiguration.getBusiness().isGdprComplianceEnabled()) {
            throw new RuntimeException("GDPR cancellation is currently disabled by configuration");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getDeletionRequestedAt() == null) {
            throw new RuntimeException("No deletion request found for this user");
        }
        
        user.setDeletionRequestedAt(null);
        user.setScheduledDeletionAt(null);
        userRepository.save(user);
        
        // Log deletion cancellation
        securityAuditService.logSecurityEvent(user.getEmail(), clientIp, userAgent,
            Map.of("action", "DELETION_CANCELLED"));
        
        return true;
    }
    
    /**
     * Immediately delete user account (admin function)
     */
    @Transactional
    public boolean deleteImmediately(Long userId, String adminEmail, String reason) {
        if (!featureConfiguration.getBusiness().isGdprComplianceEnabled()) {
            throw new RuntimeException("GDPR immediate deletion is currently disabled by configuration");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String userEmail = user.getEmail();
        
        // Anonymize user data instead of hard delete to maintain referential integrity
        user.setName("DELETED_USER_" + userId);
        user.setEmail("deleted_" + userId + "@deleted.local");
        user.setPassword("DELETED");
        user.setPhoneNumber(null);
        user.setMfaSecret(null);
        user.setBackupCodes(null);
        user.setMfaEnabled(false);
        user.setDeletionRequestedAt(null);
        user.setScheduledDeletionAt(null);
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(adminEmail);
        user.setDeletionReason(reason);
        
        userRepository.save(user);
        
        // Log immediate deletion
        securityAuditService.logSecurityEvent(userEmail, null, null,
            Map.of("action", "USER_DELETED_IMMEDIATELY", "deletedBy", adminEmail, "reason", reason));
        
        return true;
    }
    
    /**
     * Process scheduled deletions (called by scheduled task)
     */
    @Transactional
    public void processScheduledDeletions() {
        if (!featureConfiguration.getBusiness().isGdprComplianceEnabled()) {
            return;
        }
        
        List<User> usersToDelete = userRepository.findUsersScheduledForDeletion(LocalDateTime.now());
        
        for (User user : usersToDelete) {
            try {
                deleteImmediately(user.getId(), "SYSTEM", "Scheduled deletion after grace period");
            } catch (Exception e) {
                // Log error but continue processing other users
                securityAuditService.logSecurityEvent(user.getEmail(), null, null,
                    Map.of("action", "DELETION_FAILED", "error", e.getMessage()));
            }
        }
    }
    
    /**
     * Clean up old anonymized user records (after retention period)
     */
    @Transactional
    public void cleanupOldRecords() {
        if (!featureConfiguration.getBusiness().isGdprComplianceEnabled()) {
            return;
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<User> expiredUsers = userRepository.findDeletedUsersOlderThan(cutoff);
        
        for (User user : expiredUsers) {
            userRepository.delete(user);
        }
        
        if (!expiredUsers.isEmpty()) {
            securityAuditService.logSecurityEvent(null, null, null,
                Map.of("action", "OLD_RECORDS_CLEANED", "count", expiredUsers.size()));
        }
    }
    
    /**
     * Request data deletion - alias for requestDeletion for controller compatibility
     */
    public boolean requestDataDeletion(Long userId, String clientIp, String userAgent) {
        return requestDeletion(userId, clientIp, userAgent);
    }
    
    /**
     * Cancel data deletion - alias for cancelDeletion for controller compatibility
     */
    public boolean cancelDataDeletion(Long userId, String clientIp, String userAgent) {
        return cancelDeletion(userId, clientIp, userAgent);
    }
    
    /**
     * Scheduled cleanup task for GDPR compliance
     */
    @Scheduled(cron = "${gdpr.deletion.schedule:0 0 2 * * ?}") // Daily at 2 AM
    public void scheduledCleanup() {
        processScheduledDeletions();
        cleanupOldRecords();
    }
}
