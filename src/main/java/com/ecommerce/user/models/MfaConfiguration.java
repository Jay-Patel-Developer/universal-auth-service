package com.ecommerce.user.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a user's MFA (Multi-Factor Authentication) configuration.
 * Stores method, secret, recovery codes, and status.
 */
@Entity
@Table(name = "mfa_configurations")
public class MfaConfiguration {
    /** Unique ID for the MFA configuration. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** The user associated with this MFA configuration. */
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    /** MFA method (e.g., TOTP, SMS, EMAIL). */
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    private MfaMethod method;
    /** Secret key for TOTP MFA. */
    @Column(name = "secret_key")
    private String secretKey;
    /** Phone number for SMS MFA. */
    @Column(name = "phone_number")
    private String phoneNumber;
    /** Comma-separated recovery codes for MFA recovery. */
    @Column(name = "recovery_codes", columnDefinition = "TEXT")
    private String recoveryCodes;
    /** True if MFA is enabled. */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;
    /** Timestamp of last successful MFA verification. */
    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;
    /** Timestamp when the MFA configuration was created. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    /** Timestamp when the MFA configuration was last updated. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    /**
     * Set creation and update timestamps before persisting.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    /**
     * Update the last modified timestamp before updating.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public MfaMethod getMethod() {
        return method;
    }

    public void setMethod(MfaMethod method) {
        this.method = method;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRecoveryCodes() {
        return recoveryCodes;
    }

    public void setRecoveryCodes(String recoveryCodes) {
        this.recoveryCodes = recoveryCodes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(LocalDateTime lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
