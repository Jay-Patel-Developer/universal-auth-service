package com.ecommerce.user.services;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class AdvancedJwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.access-token-expiration}")
    private int accessTokenExpiration;
    
    @Value("${jwt.refresh-token-expiration}")
    private int refreshTokenExpiration;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public TokenPair generateTokenPair(String username, String deviceId, String ipAddress) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date accessExpiry = new Date(now.getTime() + accessTokenExpiration * 1000);
        Date refreshExpiry = new Date(now.getTime() + refreshTokenExpiration * 1000);

        // Access Token
        String accessToken = Jwts.builder()
                .setSubject(username)
                .setId(jti)
                .setIssuedAt(now)
                .setExpiration(accessExpiry)
                .claim("type", "access")
                .claim("deviceId", deviceId)
                .claim("ipAddress", ipAddress)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        // Refresh Token
        String refreshTokenId = UUID.randomUUID().toString();
        String refreshToken = Jwts.builder()
                .setSubject(username)
                .setId(refreshTokenId)
                .setIssuedAt(now)
                .setExpiration(refreshExpiry)
                .claim("type", "refresh")
                .claim("accessTokenId", jti)
                .claim("deviceId", deviceId)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        // Store token metadata in Redis
        storeTokenMetadata(jti, username, deviceId, ipAddress, accessExpiry);
        storeRefreshTokenMetadata(refreshTokenId, username, jti, refreshExpiry);

        return new TokenPair(accessToken, refreshToken, accessExpiry, refreshExpiry);
    }

    public Claims validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String tokenId = claims.getId();
            
            // Check if token is blacklisted
            if (isTokenBlacklisted(tokenId)) {
                throw new JwtException("Token is blacklisted");
            }

            // Check if token metadata exists in Redis
            if (!tokenExists(tokenId)) {
                throw new JwtException("Token metadata not found");
            }

            return claims;
        } catch (ExpiredJwtException e) {
            throw new JwtException("Token has expired");
        } catch (UnsupportedJwtException e) {
            throw new JwtException("Token is not supported");
        } catch (MalformedJwtException e) {
            throw new JwtException("Token is malformed");
        } catch (SecurityException e) {
            throw new JwtException("Token signature validation failed");
        }
    }

    public TokenPair refreshToken(String refreshToken, String deviceId, String ipAddress) {
        Claims claims = validateToken(refreshToken);
        
        if (!"refresh".equals(claims.get("type"))) {
            throw new JwtException("Invalid token type for refresh");
        }

        String username = claims.getSubject();
        String oldAccessTokenId = (String) claims.get("accessTokenId");
        
        // Invalidate old tokens
        blacklistToken(oldAccessTokenId, "Token refreshed");
        blacklistToken(claims.getId(), "Refresh token used");
        
        // Generate new token pair
        return generateTokenPair(username, deviceId, ipAddress);
    }

    public void revokeAllUserTokens(String username, String reason) {
        String pattern = "token_metadata:" + username + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        
        if (keys != null) {
            for (String key : keys) {
                String tokenId = key.substring(key.lastIndexOf(":") + 1);
                blacklistToken(tokenId, reason);
            }
        }
    }

    public void revokeDeviceTokens(String username, String deviceId, String reason) {
        String pattern = "token_metadata:" + username + ":" + deviceId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        
        if (keys != null) {
            for (String key : keys) {
                String tokenId = key.substring(key.lastIndexOf(":") + 1);
                blacklistToken(tokenId, reason);
            }
        }
    }

    /**
     * Revoke a specific session by session ID
     */
    public boolean revokeSession(String username, String sessionId) {
        try {
            String pattern = "token_metadata:" + username + ":*:" + sessionId;
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                blacklistToken(sessionId, "User requested session revocation");
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Revoke all sessions except the current device
     */
    public int revokeAllSessionsExcept(String username, String currentDeviceId) {
        try {
            Set<String> allSessionKeys = redisTemplate.keys("token_metadata:" + username + ":*");
            int revokedCount = 0;
            
            if (allSessionKeys != null) {
                for (String key : allSessionKeys) {
                    String[] keyParts = key.split(":");
                    if (keyParts.length >= 4) {
                        String deviceId = keyParts[2];
                        String tokenId = keyParts[3];
                        
                        // Skip current device
                        if (!currentDeviceId.equals(deviceId)) {
                            blacklistToken(tokenId, "User requested logout from all other devices");
                            revokedCount++;
                        }
                    }
                }
            }
            
            return revokedCount;
        } catch (Exception e) {
            return 0;
        }
    }

    public void blacklistToken(String tokenId, String reason) {
        String blacklistKey = "blacklisted_token:" + tokenId;
        Map<String, Object> blacklistInfo = new HashMap<>();
        blacklistInfo.put("reason", reason);
        blacklistInfo.put("timestamp", LocalDateTime.now().toString());
        
        redisTemplate.opsForValue().set(blacklistKey, blacklistInfo, 24, TimeUnit.HOURS);
        
        // Remove token metadata
        removeTokenMetadata(tokenId);
    }

    private void storeTokenMetadata(String tokenId, String username, String deviceId, String ipAddress, Date expiry) {
        String key = "token_metadata:" + username + ":" + deviceId + ":" + tokenId;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", username);
        metadata.put("deviceId", deviceId);
        metadata.put("ipAddress", ipAddress);
        metadata.put("issuedAt", LocalDateTime.now().toString());
        metadata.put("expiry", expiry.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString());
        
        long ttl = (expiry.getTime() - System.currentTimeMillis()) / 1000;
        redisTemplate.opsForValue().set(key, metadata, ttl, TimeUnit.SECONDS);
    }

    private void storeRefreshTokenMetadata(String refreshTokenId, String username, String accessTokenId, Date expiry) {
        String key = "refresh_token:" + refreshTokenId;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", username);
        metadata.put("accessTokenId", accessTokenId);
        metadata.put("issuedAt", LocalDateTime.now().toString());
        
        long ttl = (expiry.getTime() - System.currentTimeMillis()) / 1000;
        redisTemplate.opsForValue().set(key, metadata, ttl, TimeUnit.SECONDS);
    }

    private boolean isTokenBlacklisted(String tokenId) {
        return redisTemplate.hasKey("blacklisted_token:" + tokenId);
    }

    private boolean tokenExists(String tokenId) {
        String pattern = "token_metadata:*:" + tokenId;
        Set<String> keys = redisTemplate.keys(pattern);
        return keys != null && !keys.isEmpty();
    }

    private void removeTokenMetadata(String tokenId) {
        String pattern = "token_metadata:*:" + tokenId;
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * Get active sessions for a user
     */
    public List<Map<String, Object>> getUserSessions(String username) {
        try {
            Set<String> sessionIds = getAllUserSessions(username);
            List<Map<String, Object>> sessions = new ArrayList<>();
            
            for (String sessionId : sessionIds) {
                Object sessionData = redisTemplate.opsForValue().get("session:" + sessionId);
                if (sessionData != null) {
                    Map<String, Object> session = new HashMap<>();
                    session.put("sessionId", sessionId);
                    session.put("username", username);
                    session.put("active", true);
                    sessions.add(session);
                }
            }
            
            return sessions;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Get all active sessions for a user (enhanced implementation)
     */
    public List<ActiveSession> getActiveSessions(String username) {
        try {
            Set<String> sessionKeys = redisTemplate.keys("session:" + username + ":*");
            List<ActiveSession> sessions = new ArrayList<>();
            
            for (String key : sessionKeys) {
                Object sessionData = redisTemplate.opsForValue().get(key);
                if (sessionData != null) {
                    // Parse session data and create ActiveSession object
                    ActiveSession session = new ActiveSession();
                    session.setSessionId(key.substring(key.lastIndexOf(":") + 1));
                    session.setUsername(username);
                    session.setLastActivity(new Date());
                    sessions.add(session);
                }
            }
            
            return sessions;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    private Set<String> getAllUserSessions(String username) {
        try {
            String pattern = "token_metadata:" + username + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            Set<String> sessionIds = new HashSet<>();
            
            if (keys != null) {
                for (String key : keys) {
                    String[] parts = key.split(":");
                    if (parts.length >= 4) {
                        sessionIds.add(parts[3]); // Extract token ID
                    }
                }
            }
            
            return sessionIds;
        } catch (Exception e) {
            return new HashSet<>();
        }
    }
    
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;
        private final Date accessTokenExpiry;
        private final Date refreshTokenExpiry;

        public TokenPair(String accessToken, String refreshToken, Date accessTokenExpiry, Date refreshTokenExpiry) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.accessTokenExpiry = accessTokenExpiry;
            this.refreshTokenExpiry = refreshTokenExpiry;
        }

        // Getters
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public Date getAccessTokenExpiry() { return accessTokenExpiry; }
        public Date getRefreshTokenExpiry() { return refreshTokenExpiry; }
    }

    public static class ActiveSession {
        private String sessionId;
        private String username;
        private String deviceId;
        private String ipAddress;
        private Date lastActivity;
        private String userAgent;
        
        // Getters and setters
        public String getSessionId() {
            return sessionId;
        }
        
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getDeviceId() {
            return deviceId;
        }
        
        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }
        
        public String getIpAddress() {
            return ipAddress;
        }
        
        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }
        
        public Date getLastActivity() {
            return lastActivity;
        }
        
        public void setLastActivity(Date lastActivity) {
            this.lastActivity = lastActivity;
        }
        
        public String getUserAgent() {
            return userAgent;
        }
        
        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }
    }
}