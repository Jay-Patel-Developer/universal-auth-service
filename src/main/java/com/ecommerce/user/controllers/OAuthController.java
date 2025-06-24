package com.ecommerce.user.controllers;

import com.ecommerce.user.config.FeatureConfiguration;
import com.ecommerce.user.dto.OAuthRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.models.User;
import com.ecommerce.user.models.OAuthProvider;
import com.ecommerce.user.models.OAuthProviderType;
import com.ecommerce.user.services.UserService;
import com.ecommerce.user.services.AdvancedJwtService;
import com.ecommerce.user.services.SecurityAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for OAuth authentication endpoints.
 * Handles OAuth login, provider validation, and user linking.
 */
@RestController
@RequestMapping("/api/auth/oauth")
public class OAuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AdvancedJwtService advancedJwtService;

    @Autowired
    private SecurityAuditService securityAuditService;

    @Autowired
    private FeatureConfiguration featureConfiguration;

    /**
     * POST /api/auth/oauth/login
     * Handles OAuth login/registration for supported providers.
     * @param request OAuthRequest with provider and access token
     * @return JWT tokens and user info on success, error otherwise
     */
    @PostMapping("/login")
    public ResponseEntity<?> oauthLogin(@RequestBody OAuthRequest request,
                                       HttpServletRequest httpRequest) {
        try {
            String clientIp = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            String deviceId = request.getDeviceId() != null ? 
                request.getDeviceId() : "oauth-" + UUID.randomUUID().toString();

            // Validate OAuth token with provider
            Map<String, Object> userInfo = validateOAuthToken(request.getProvider(), request.getAccessToken());
            
            if (userInfo == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid OAuth token"));
            }

            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            String providerId = (String) userInfo.get("id");

            // Check if user exists
            UserResponse existingUser = userService.getUserByEmail(email);
            User user;

            if (existingUser == null) {
                // Create new user
                user = createOAuthUser(email, name, request.getProvider(), providerId);
            } else {
                // Update OAuth provider info if needed
                user = userService.getUserByEmailInternal(email); // Use internal method to get User object
                updateOAuthProvider(user, request.getProvider(), providerId);
            }

            // Log OAuth login - use logSuccessfulLogin instead of missing logOAuthLogin
            securityAuditService.logSuccessfulLogin(email, clientIp, userAgent + " via " + request.getProvider().toString());

            // Generate tokens
            AdvancedJwtService.TokenPair tokenPair = 
                advancedJwtService.generateTokenPair(email, deviceId, clientIp);

            return ResponseEntity.ok(Map.of(
                "message", "OAuth login successful",
                "user", userService.convertToUserResponse(user),
                "accessToken", tokenPair.getAccessToken(),
                "refreshToken", tokenPair.getRefreshToken(),
                "expiresAt", tokenPair.getAccessTokenExpiry().getTime()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Validate OAuth token with the provider, checking per-provider config.
     */
    private Map<String, Object> validateOAuthToken(OAuthProviderType provider, String accessToken) {
        boolean realIntegration = featureConfiguration.getAuth().isSocialLoginEnabled();
        // Per-provider enablement
        boolean googleEnabled = featureConfiguration.getAuth().isGoogleEnabled();
        boolean facebookEnabled = featureConfiguration.getAuth().isFacebookEnabled();
        boolean githubEnabled = featureConfiguration.getAuth().isGithubEnabled();
        switch (provider) {
            case GOOGLE:
                if (!googleEnabled) return null;
                return realIntegration ? validateGoogleTokenReal(accessToken) : validateGoogleToken(accessToken);
            case FACEBOOK:
                if (!facebookEnabled) return null;
                return realIntegration ? validateFacebookTokenReal(accessToken) : validateFacebookToken(accessToken);
            case GITHUB:
                if (!githubEnabled) return null;
                return realIntegration ? validateGitHubTokenReal(accessToken) : validateGitHubToken(accessToken);
            default:
                return null;
        }
    }

    // Real HTTP call to Google
    private Map<String, Object> validateGoogleTokenReal(String accessToken) {
        // Example: Use Google tokeninfo endpoint
        try {
            java.net.URL url = new java.net.URL("https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=" + accessToken);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            int status = conn.getResponseCode();
            if (status == 200) {
                try (java.io.InputStream is = conn.getInputStream();
                     java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A")) {
                    String json = s.hasNext() ? s.next() : "";
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> map = mapper.readValue(json, Map.class);
                    return Map.of(
                        "id", map.get("sub"),
                        "email", map.get("email"),
                        "name", map.getOrDefault("name", "Google User")
                    );
                }
            }
        } catch (Exception e) {
            // Log error if needed
        }
        return null;
    }

    // Real HTTP call to Facebook
    private Map<String, Object> validateFacebookTokenReal(String accessToken) {
        // STUB: Facebook token validation not implemented yet.
        // In production, use Facebook's debug_token endpoint with your app access token.
        // See: https://developers.facebook.com/docs/facebook-login/access-tokens/debugging-and-error-handling
        return null;
    }

    // Real HTTP call to GitHub
    private Map<String, Object> validateGitHubTokenReal(String accessToken) {
        try {
            java.net.URL url = new java.net.URL("https://api.github.com/user");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "token " + accessToken);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            int status = conn.getResponseCode();
            if (status == 200) {
                try (java.io.InputStream is = conn.getInputStream();
                     java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A")) {
                    String json = s.hasNext() ? s.next() : "";
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> map = mapper.readValue(json, Map.class);
                    return Map.of(
                        "id", map.get("id").toString(),
                        "email", map.getOrDefault("email", ""),
                        "name", map.getOrDefault("name", "GitHub User")
                    );
                }
            }
        } catch (Exception e) {
            // Log error if needed
        }
        return null;
    }

    private Map<String, Object> validateGoogleToken(String accessToken) {
        // Mock implementation for Google OAuth validation
        return Map.of(
            "id", "google_123456",
            "email", "user@gmail.com",
            "name", "OAuth User"
        );
    }

    private Map<String, Object> validateFacebookToken(String accessToken) {
        // Mock implementation for Facebook OAuth validation
        return Map.of(
            "id", "facebook_123456",
            "email", "user@facebook.com",
            "name", "OAuth User"
        );
    }

    private Map<String, Object> validateGitHubToken(String accessToken) {
        // Mock implementation for GitHub OAuth validation
        return Map.of(
            "id", "github_123456",
            "email", "user@github.com",
            "name", "OAuth User"
        );
    }

    private User createOAuthUser(String email, String name, OAuthProviderType provider, String providerId) {
        // Create user with OAuth provider
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(""); // No password for OAuth users
        
        // Save user first
        user = userService.saveUser(user);
        
        // Add OAuth provider
        addOAuthProvider(user, provider, providerId);
        
        return user;
    }

    private void addOAuthProvider(User user, OAuthProviderType provider, String providerId) {
        // Prevent duplicate provider for this user
        if (user.getOauthProviders() != null) {
            boolean exists = user.getOauthProviders().stream()
                .anyMatch(p -> p.getProviderName() == provider.name() && p.getProviderId().equals(providerId));
            if (exists) return;
        }
        // Prevent linking providerId to multiple users (enforced at DB or service layer)
        OAuthProvider oauthProvider = new OAuthProvider();
        oauthProvider.setUser(user);
        oauthProvider.setProviderName(provider.name());
        oauthProvider.setProviderId(providerId);
        // Optionally set created/updated timestamps
        user.getOauthProviders().add(oauthProvider);
        userService.saveUser(user); // Save user with new provider
    }

    private void updateOAuthProvider(User user, OAuthProviderType provider, String providerId) {
        if (user.getOauthProviders() == null) return;
        OAuthProvider existing = user.getOauthProviders().stream()
            .filter(p -> p.getProviderName() == provider.name())
            .findFirst().orElse(null);
        if (existing == null) {
            addOAuthProvider(user, provider, providerId);
        } else if (!existing.getProviderId().equals(providerId)) {
            existing.setProviderId(providerId);
            // Optionally update timestamps
            userService.saveUser(user);
        }
    }
}
