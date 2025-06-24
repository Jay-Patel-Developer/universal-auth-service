package com.ecommerce.user.dto;

/**
 * DTO for MFA setup/verification requests.
 * Contains the TOTP code provided by the user for verification.
 */
public class MfaSetupRequest {
    /** TOTP code provided by the user for MFA verification. */
    private String totpCode;

    /** @return TOTP code for verification. */
    public String getTotpCode() {
        return totpCode;
    }

    /** @param totpCode TOTP code for verification. */
    public void setTotpCode(String totpCode) {
        this.totpCode = totpCode;
    }
}
