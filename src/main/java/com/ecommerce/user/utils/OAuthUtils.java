package com.ecommerce.user.utils;

import java.util.Map;

public class OAuthUtils {
    public static String extractProviderName(Map<String, Object> oauthData) {
        return (String) oauthData.get("providerName");
    }

    public static String extractProviderId(Map<String, Object> oauthData) {
        return (String) oauthData.get("providerId");
    }
}
