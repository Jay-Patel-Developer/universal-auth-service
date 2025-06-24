package com.ecommerce.user.models;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a user in the system.
 * Includes authentication, profile, roles, MFA, and GDPR-related fields.
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {
    /** Unique user ID (primary key). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** User's email address (unique). */
    @Column(nullable = false, unique = true)
    private String email;
    /** Hashed password for email-based sign-up. */
    private String password;
    /** Full name of the user. */
    private String name;
    /** Phone number of the user. */
    private String phoneNumber;
    /** Current status (ACTIVE, INACTIVE, BANNED, etc.). */
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    /** List of roles assigned to the user. */
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles;
    /** Timestamp when the user was created. */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    /** Timestamp when the user was last updated. */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    /** Timestamp of the user's last login. */
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    /** List of OAuth providers linked to the user. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<OAuthProvider> oauthProviders;
    /** True if MFA is enabled for the user. */
    @Column(name = "mfa_enabled")
    private boolean mfaEnabled = false;
    /** Secret for TOTP MFA. */
    @Column(name = "mfa_secret")
    private String mfaSecret;
    /** Set of backup codes for MFA recovery. */
    @Column(name = "backup_codes")
    @ElementCollection
    private Set<String> backupCodes;

    // GDPR-related fields
    /** Timestamp when deletion was requested. */
    @Column(name = "deletion_requested_at")
    private LocalDateTime deletionRequestedAt;
    /** Timestamp when deletion is scheduled. */
    @Column(name = "scheduled_deletion_at")
    private LocalDateTime scheduledDeletionAt;
    /** Timestamp when the user was deleted. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    /** Email of the admin who deleted the user. */
    @Column(name = "deleted_by")
    private String deletedBy;
    /** Reason for deletion. */
    @Column(name = "deletion_reason")
    private String deletionReason;
    /** True if user requested GDPR data deletion. */
    @Column(name = "data_deletion_requested")
    private boolean dataDeletionRequested = false;
    /** Timestamp when GDPR data deletion was requested. */
    @Column(name = "data_deletion_request_date")
    private LocalDateTime dataDeletionRequestDate;

    /**
     * Set creation and update timestamps before persisting.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    /**
     * Update the last modified timestamp before updating.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public List<OAuthProvider> getOauthProviders() {
        return oauthProviders;
    }

    public void setOauthProviders(List<OAuthProvider> oauthProviders) {
        this.oauthProviders = oauthProviders;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public String getMfaSecret() {
        return mfaSecret;
    }

    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
    }

    public Set<String> getBackupCodes() {
        return backupCodes;
    }

    public void setBackupCodes(Set<String> backupCodes) {
        this.backupCodes = backupCodes;
    }

    public LocalDateTime getDeletionRequestedAt() {
        return deletionRequestedAt;
    }

    public void setDeletionRequestedAt(LocalDateTime deletionRequestedAt) {
        this.deletionRequestedAt = deletionRequestedAt;
    }

    public LocalDateTime getScheduledDeletionAt() {
        return scheduledDeletionAt;
    }

    public void setScheduledDeletionAt(LocalDateTime scheduledDeletionAt) {
        this.scheduledDeletionAt = scheduledDeletionAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    public String getDeletionReason() {
        return deletionReason;
    }

    public void setDeletionReason(String deletionReason) {
        this.deletionReason = deletionReason;
    }

    public boolean isDataDeletionRequested() {
        return dataDeletionRequested;
    }

    public void setDataDeletionRequested(boolean dataDeletionRequested) {
        this.dataDeletionRequested = dataDeletionRequested;
    }

    public LocalDateTime getDataDeletionRequestDate() {
        return dataDeletionRequestDate;
    }

    public void setDataDeletionRequestDate(LocalDateTime dataDeletionRequestDate) {
        this.dataDeletionRequestDate = dataDeletionRequestDate;
    }
}
