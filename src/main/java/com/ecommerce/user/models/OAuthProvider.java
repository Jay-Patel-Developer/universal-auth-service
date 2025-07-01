package com.ecommerce.user.models;

import jakarta.persistence.*;

/**
 * Entity representing an OAuth provider linked to a user account.
 * Stores provider name, provider user ID, and tokens.
 */
@Entity
@Table(name = "oauth_providers")
public class OAuthProvider {
    /** Unique ID for the OAuth provider record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user associated with this OAuth provider. */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Name of the OAuth provider (e.g., GOOGLE, FACEBOOK). */
    @Column(nullable = false)
    private String providerName; // e.g., "GOOGLE", "FACEBOOK"

    /** Unique user ID from the OAuth provider. */
    @Column(nullable = false)
    private String providerId; // Unique ID from the provider

    /** Access token from the OAuth provider. */
    private String accessToken;

    /** Refresh token from the OAuth provider. */
    private String refreshToken;

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

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
