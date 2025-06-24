package com.ecommerce.user.dto;

/**
 * DTO for password reset requests.
 * Contains the reset token and the new password to be set.
 */
public class PasswordResetRequest {
    /** Password reset token sent to the user's email. */
    private String token;
    /** New password to set for the user. */
    private String newPassword;
    
    /** @return Password reset token. */
    public String getToken() {
        return token;
    }
    
    /** @param token Password reset token. */
    public void setToken(String token) {
        this.token = token;
    }
    
    /** @return New password to set. */
    public String getNewPassword() {
        return newPassword;
    }
    
    /** @param newPassword New password to set. */
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
