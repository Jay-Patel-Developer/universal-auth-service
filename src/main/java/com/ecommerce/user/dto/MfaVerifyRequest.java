package com.ecommerce.user.dto;

/**
 * DTO for MFA verification requests.
 * Used for verifying MFA codes during login or enrollment.
 */
public class MfaVerifyRequest {
    /** User ID for whom MFA is being verified. */
    private Long userId;
    /** User email (optional, for login). */
    private String email;
    /** User password (optional, for login). */
    private String password;
    /** TOTP code provided by the user. */
    private String totpCode;
    /** Backup code for MFA recovery. */
    private String backupCode;
    /** Generic code field (for SMS/email MFA). */
    private String code;
    /** Device ID for session tracking. */
    private String deviceId;
    
    /** @return User ID. */
    public Long getUserId() {
        return userId;
    }
    
    /** @param userId User ID. */
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    /** @return Generic MFA code. */
    public String getCode() {
        return code;
    }
    
    /** @param code Generic MFA code. */
    public void setCode(String code) {
        this.code = code;
    }
    
    /** @return Device ID. */
    public String getDeviceId() {
        return deviceId;
    }
    
    /** @param deviceId Device ID. */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    /** @return User email. */
    public String getEmail() {
        return email;
    }
    
    /** @param email User email. */
    public void setEmail(String email) {
        this.email = email;
    }
    
    /** @return User password. */
    public String getPassword() {
        return password;
    }
    
    /** @param password User password. */
    public void setPassword(String password) {
        this.password = password;
    }
    
    /** @return TOTP code. */
    public String getTotpCode() {
        return totpCode;
    }
    
    /** @param totpCode TOTP code. */
    public void setTotpCode(String totpCode) {
        this.totpCode = totpCode;
    }
    
    /** @return Backup code. */
    public String getBackupCode() {
        return backupCode;
    }
    
    /** @param backupCode Backup code. */
    public void setBackupCode(String backupCode) {
        this.backupCode = backupCode;
    }
}
