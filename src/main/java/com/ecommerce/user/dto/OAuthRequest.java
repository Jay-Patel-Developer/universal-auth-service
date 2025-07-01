package com.ecommerce.user.dto;

import com.ecommerce.user.models.OAuthProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for OAuth login requests.
 * Contains provider, access token, and optional device/client info.
 */
public class OAuthRequest {
    /** OAuth provider type (e.g., GOOGLE, FACEBOOK, GITHUB). */
    @NotNull(message = "OAuth provider is required")
    private OAuthProviderType provider;
    /** Access token from the OAuth provider. */
    @NotBlank(message = "Access token is required")
    private String accessToken;
    /** Optional device ID for session tracking. */
    private String deviceId;
    /** Optional device name for session tracking. */
    private String deviceName;
    /** Optional user agent string. */
    private String userAgent;

    // Constructors
    public OAuthRequest() {}

    public OAuthRequest(OAuthProviderType provider, String accessToken) {
        this.provider = provider;
        this.accessToken = accessToken;
    }

    // Getters and setters
    /** @return OAuth provider type. */
    public OAuthProviderType getProvider() { return provider; }
    /** @param provider OAuth provider type. */
    public void setProvider(OAuthProviderType provider) { this.provider = provider; }
    /** @return Access token from the OAuth provider. */
    public String getAccessToken() { return accessToken; }
    /** @param accessToken Access token from the OAuth provider. */
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    /** @return Device ID for session tracking. */
    public String getDeviceId() { return deviceId; }
    /** @param deviceId Device ID for session tracking. */
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    /** @return Device name for session tracking. */
    public String getDeviceName() { return deviceName; }
    /** @param deviceName Device name for session tracking. */
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    /** @return User agent string. */
    public String getUserAgent() { return userAgent; }
    /** @param userAgent User agent string. */
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    @Override
    public String toString() {
        return "OAuthRequest{" +
                "provider=" + provider +
                ", deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                '}';
    }
}