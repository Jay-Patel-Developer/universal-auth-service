package com.ecommerce.user.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import com.ecommerce.user.config.FeatureConfiguration;

/**
 * Service for rate limiting authentication and user actions.
 * Uses Redis to track and enforce limits for login, registration, OAuth, and password reset.
 */
@Service
public class RateLimitService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private FeatureConfiguration featureConfiguration;
    
    // Rate limit configurations
    @Value("${rate.limit.login.max-attempts:5}")
    private int loginLimit;
    @Value("${rate.limit.registration.max-attempts:3}")
    private int registrationLimit;
    @Value("${rate.limit.oauth-login.max-attempts:10}")
    private int oauthLoginLimit;
    @Value("${rate.limit.password-reset.max-attempts:3}")
    private int passwordResetLimit;
    
    public enum RateLimitType {
        LOGIN("login"),
        REGISTRATION("registration"),
        OAUTH_LOGIN("oauth_login"),
        PASSWORD_RESET("password_reset");
        
        private final String key;
        RateLimitType(String key) {
            this.key = key;
        }
        public String getKey() { return key; }
        public int getLimit(RateLimitService svc) {
            switch (this) {
                case LOGIN: return svc.loginLimit;
                case REGISTRATION: return svc.registrationLimit;
                case OAUTH_LOGIN: return svc.oauthLoginLimit;
                case PASSWORD_RESET: return svc.passwordResetLimit;
                default: return 1;
            }
        }
        public int getWindowHours() { return 1; }
    }
    
    /**
     * Check if the rate limit allows the operation.
     * @param identifier Unique identifier (e.g., IP address)
     * @param type Rate limit type
     * @return true if allowed, false if limit exceeded
     */
    public boolean isAllowed(String identifier, RateLimitType type) {
        if (!featureConfiguration.getAuth().isRateLimitingEnabled()) {
            return true;
        }
        if (redisTemplate == null) {
            // If Redis is not available, allow the operation but log it
            logger.warn("Redis not available for rate limiting, allowing operation");
            return true;
        }
        
        try {
            String key = buildRateLimitKey(identifier, type);
            
            // Get current count
            Object countObj = redisTemplate.opsForValue().get(key);
            int currentCount = countObj != null ? Integer.parseInt(countObj.toString()) : 0;
            
            if (currentCount >= type.getLimit(this)) {
                logger.warn("Rate limit exceeded for {}: {} attempts", type.getKey(), currentCount);
                return false;
            }
            
            // Increment counter
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, type.getWindowHours(), TimeUnit.HOURS);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking rate limit: {}", e.getMessage());
            // Fail open - allow the operation if there's an error
            return true;
        }
    }
    
    /**
     * Clear rate limit for successful operations.
     * @param identifier Unique identifier (e.g., IP address)
     * @param type Rate limit type
     */
    public void clearRateLimit(String identifier, RateLimitType type) {
        if (redisTemplate == null) {
            return;
        }
        
        try {
            String key = buildRateLimitKey(identifier, type);
            redisTemplate.delete(key);
        } catch (Exception e) {
            logger.error("Error clearing rate limit: {}", e.getMessage());
        }
    }
    
    /**
     * Get current rate limit status.
     * @param identifier Unique identifier (e.g., IP address)
     * @param type Rate limit type
     * @return RateLimitStatus object with current count, limit, and TTL
     */
    public RateLimitStatus getRateLimitStatus(String identifier, RateLimitType type) {
        if (redisTemplate == null) {
            return new RateLimitStatus(0, type.getLimit(this), 0);
        }
        
        try {
            String key = buildRateLimitKey(identifier, type);
            
            Object countObj = redisTemplate.opsForValue().get(key);
            int currentCount = countObj != null ? Integer.parseInt(countObj.toString()) : 0;
            
            Long ttl = redisTemplate.getExpire(key);
            long remainingSeconds = ttl != null ? ttl : 0;
            
            return new RateLimitStatus(currentCount, type.getLimit(this), remainingSeconds);
            
        } catch (Exception e) {
            logger.error("Error getting rate limit status: {}", e.getMessage());
            return new RateLimitStatus(0, type.getLimit(this), 0);
        }
    }
    
    /**
     * Helper to build Redis key for rate limiting.
     */
    private String buildRateLimitKey(String identifier, RateLimitType type) {
        return String.format("rate_limit:%s:%s", type.getKey(), identifier);
    }
    
    /**
     * Status object for rate limiting.
     */
    public static class RateLimitStatus {
        private final int currentCount;
        private final int limit;
        private final long remainingSeconds;
        
        public RateLimitStatus(int currentCount, int limit, long remainingSeconds) {
            this.currentCount = currentCount;
            this.limit = limit;
            this.remainingSeconds = remainingSeconds;
        }
        
        public int getCurrentCount() { return currentCount; }
        public int getLimit() { return limit; }
        public long getRemainingSeconds() { return remainingSeconds; }
        public boolean isExceeded() { return currentCount >= limit; }
        public int getRemainingAttempts() { return Math.max(0, limit - currentCount); }
    }
}