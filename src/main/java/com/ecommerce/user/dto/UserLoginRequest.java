package com.ecommerce.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for user login requests.
 * Contains email, password, and optional device/session info.
 */
public class UserLoginRequest {
    /** User's email address. */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /** User's password. */
    @NotBlank(message = "Password is required")
    private String password;

    /** Optional device ID for session tracking. */
    private String deviceId;

    /** Remember-me flag for persistent sessions. */
    private boolean rememberMe;

    /**
     * Default constructor.
     */
    public UserLoginRequest() {}

    /**
     * Constructs a UserLoginRequest with email and password.
     */
    public UserLoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /** @return User's email address. */
    public String getEmail() {
        return email;
    }

    /** @param email User's email address. */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return User's password. */
    public String getPassword() {
        return password;
    }

    /** @param password User's password. */
    public void setPassword(String password) {
        this.password = password;
    }
    
    /** @return Device ID for session tracking. */
    public String getDeviceId() {
        return deviceId;
    }
    
    /** @param deviceId Device ID for session tracking. */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    /** @return Remember-me flag. */
    public boolean isRememberMe() {
        return rememberMe;
    }
    
    /** @param rememberMe Remember-me flag. */
    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}