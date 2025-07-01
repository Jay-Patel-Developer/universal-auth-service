package com.ecommerce.user.models;

/**
 * Enum representing supported MFA (Multi-Factor Authentication) methods.
 */
public enum MfaMethod {
    /** No MFA enabled. */
    NONE,
    /** Time-based One-Time Password (TOTP) MFA. */
    TOTP,
    /** SMS-based MFA. */
    SMS,
    /** Email-based MFA. */
    EMAIL
}
