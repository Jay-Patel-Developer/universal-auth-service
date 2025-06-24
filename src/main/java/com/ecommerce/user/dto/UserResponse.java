package com.ecommerce.user.dto;

import com.ecommerce.user.models.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * DTO representing a user profile returned by the API.
 * Includes core user info, roles, status, MFA, and backup codes.
 */
public class UserResponse {
    /** Unique user ID. */
    private Long id;
    /** Full name of the user. */
    private String name;
    /** Email address of the user. */
    private String email;
    /** Phone number of the user. */
    private String phoneNumber;
    /** Current status (ACTIVE, INACTIVE, etc.). */
    private UserStatus status;
    /** List of roles assigned to the user. */
    private List<String> roles;
    /** Timestamp when the user was created. */
    private LocalDateTime createdAt;
    /** Timestamp when the user was last updated. */
    private LocalDateTime updatedAt;
    /** Set of backup codes for MFA recovery. */
    private Set<String> backupCodes;
    /** Indicates if MFA is enabled for the user. */
    private boolean mfaEnabled;

    /**
     * Default constructor.
     */
    public UserResponse() {}

    /**
     * Constructs a UserResponse with core user fields.
     */
    public UserResponse(Long id, String name, String email, String phoneNumber, 
                       UserStatus status, List<String> roles, 
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.roles = roles;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Constructs a UserResponse with MFA fields.
     */
    public UserResponse(Long id, String name, String email, String phoneNumber, 
                       UserStatus status, List<String> roles, 
                       LocalDateTime createdAt, LocalDateTime updatedAt,
                       Set<String> backupCodes, boolean mfaEnabled) {
        this(id, name, email, phoneNumber, status, roles, createdAt, updatedAt);
        this.backupCodes = backupCodes;
        this.mfaEnabled = mfaEnabled;
    }

    /** @return Unique user ID. */
    public Long getId() { return id; }
    /** @param id Unique user ID. */
    public void setId(Long id) { this.id = id; }
    /** @return Full name of the user. */
    public String getName() { return name; }
    /** @param name Full name of the user. */
    public void setName(String name) { this.name = name; }
    /** @return Email address of the user. */
    public String getEmail() { return email; }
    /** @param email Email address of the user. */
    public void setEmail(String email) { this.email = email; }
    /** @return Phone number of the user. */
    public String getPhoneNumber() { return phoneNumber; }
    /** @param phoneNumber Phone number of the user. */
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    /** @return User status. */
    public UserStatus getStatus() { return status; }
    /** @param status User status. */
    public void setStatus(UserStatus status) { this.status = status; }
    /** @return List of user roles. */
    public List<String> getRoles() { return roles; }
    /** @param roles List of user roles. */
    public void setRoles(List<String> roles) { this.roles = roles; }
    /** @return Creation timestamp. */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** @param createdAt Creation timestamp. */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    /** @return Last update timestamp. */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    /** @param updatedAt Last update timestamp. */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    /** @return Set of MFA backup codes. */
    public Set<String> getBackupCodes() { return backupCodes; }
    /** @param backupCodes Set of MFA backup codes. */
    public void setBackupCodes(Set<String> backupCodes) { this.backupCodes = backupCodes; }
    /** @return True if MFA is enabled. */
    public boolean isMfaEnabled() { return mfaEnabled; }
    /** @param mfaEnabled True if MFA is enabled. */
    public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }
}