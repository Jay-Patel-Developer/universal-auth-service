package com.ecommerce.user.dto;

import com.ecommerce.user.models.MfaMethod;

/**
 * DTO for MFA enrollment requests.
 * Contains the user ID and the MFA method to enroll.
 */
public class MfaEnrollRequest {
    /** User ID for whom MFA is being enrolled. */
    private Long userId;
    /** MFA method to enroll (e.g., TOTP, SMS). */
    private MfaMethod method;

    /** @return User ID. */
    public Long getUserId() {
        return userId;
    }

    /** @param userId User ID. */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** @return MFA method. */
    public MfaMethod getMethod() {
        return method;
    }

    /** @param method MFA method. */
    public void setMethod(MfaMethod method) {
        this.method = method;
    }
}
