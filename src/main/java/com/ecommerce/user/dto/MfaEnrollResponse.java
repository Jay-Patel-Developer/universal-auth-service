package com.ecommerce.user.dto;

import com.ecommerce.user.models.MfaMethod;

import java.util.List;

/**
 * DTO for MFA enrollment responses.
 * Contains the enrolled method, secret, QR code, recovery codes, and verification status.
 */
public class MfaEnrollResponse {
    /** MFA method enrolled (e.g., TOTP, SMS). */
    private MfaMethod method;
    /** Secret key for TOTP (if applicable). */
    private String secretKey;
    /** QR code URL for TOTP enrollment (if applicable). */
    private String qrCodeUrl;
    /** List of recovery codes for account recovery. */
    private List<String> recoveryCodes;
    /** True if verification code was sent (for SMS/Email). */
    private boolean verificationSent;

    /** @return MFA method enrolled. */
    public MfaMethod getMethod() {
        return method;
    }

    /** @param method MFA method enrolled. */
    public void setMethod(MfaMethod method) {
        this.method = method;
    }

    /** @return Secret key for TOTP. */
    public String getSecretKey() {
        return secretKey;
    }

    /** @param secretKey Secret key for TOTP. */
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    /** @return QR code URL for TOTP enrollment. */
    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    /** @param qrCodeUrl QR code URL for TOTP enrollment. */
    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    /** @return List of recovery codes. */
    public List<String> getRecoveryCodes() {
        return recoveryCodes;
    }

    /** @param recoveryCodes List of recovery codes. */
    public void setRecoveryCodes(List<String> recoveryCodes) {
        this.recoveryCodes = recoveryCodes;
    }

    /** @return True if verification code was sent. */
    public boolean isVerificationSent() {
        return verificationSent;
    }

    /** @param verificationSent True if verification code was sent. */
    public void setVerificationSent(boolean verificationSent) {
        this.verificationSent = verificationSent;
    }
}
