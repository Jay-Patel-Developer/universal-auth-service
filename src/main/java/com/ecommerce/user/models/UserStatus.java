package com.ecommerce.user.models;

/**
 * Enum representing possible user account statuses.
 */
public enum UserStatus {
    /** User is active and can access the system. */
    ACTIVE,
    /** User is inactive (e.g., deactivated or suspended). */
    INACTIVE,
    /** User is banned from the system. */
    BANNED,
    /** User is pending email or admin verification. */
    PENDING_VERIFICATION
}