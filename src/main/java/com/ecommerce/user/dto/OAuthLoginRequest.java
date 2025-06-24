package com.ecommerce.user.dto;

/**
 * DTO for OAuth login requests (legacy or provider-specific flows).
 * Contains provider ID, access token, user info, and device ID.
 */
public class OAuthLoginRequest {
    /** Provider-specific user ID. */
    private String providerId;
    /** Access token from the OAuth provider. */
    private String accessToken;
    /** User info returned by the OAuth provider. */
    private OAuthUserInfo userInfo;
    /** Optional device ID for session tracking. */
    private String deviceId;
    
    /** @return Provider-specific user ID. */
    public String getProviderId() {
        return providerId;
    }
    
    /** @param providerId Provider-specific user ID. */
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    /** @return Access token from the OAuth provider. */
    public String getAccessToken() {
        return accessToken;
    }
    
    /** @param accessToken Access token from the OAuth provider. */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    /** @return User info from the OAuth provider. */
    public OAuthUserInfo getUserInfo() {
        return userInfo;
    }
    
    /** @param userInfo User info from the OAuth provider. */
    public void setUserInfo(OAuthUserInfo userInfo) {
        this.userInfo = userInfo;
    }
    
    /** @return Device ID for session tracking. */
    public String getDeviceId() {
        return deviceId;
    }
    
    /** @param deviceId Device ID for session tracking. */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
