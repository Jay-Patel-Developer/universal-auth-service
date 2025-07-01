package com.ecommerce.user.services;

import com.ecommerce.user.config.FeatureConfiguration;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private FeatureConfiguration featureConfiguration;

    @Value("${rate.limit.login.per-minute:5}")
    private int loginAttemptsPerMinute;
    @Value("${rate.limit.registration.per-hour:3}")
    private int registrationAttemptsPerHour;
    @Value("${rate.limit.token-validation.per-minute:100}")
    private int tokenValidationPerMinute;
    @Value("${rate.limit.password-reset.per-hour:2}")
    private int passwordResetPerHour;

    private final ConcurrentHashMap<String, Bucket> cache = new ConcurrentHashMap<>();

    public boolean isLoginAllowed(String clientIdentifier) {
        if (!featureConfiguration.getAuth().isRateLimitingEnabled()) {
            return true;
        }
        return tryConsume(getLoginBucket(clientIdentifier));
    }

    public boolean isRegistrationAllowed(String clientIdentifier) {
        if (!featureConfiguration.getAuth().isRateLimitingEnabled()) {
            return true;
        }
        return tryConsume(getRegistrationBucket(clientIdentifier));
    }

    public boolean isTokenValidationAllowed(String clientIdentifier) {
        if (!featureConfiguration.getAuth().isRateLimitingEnabled()) {
            return true;
        }
        return tryConsume(getTokenValidationBucket(clientIdentifier));
    }

    public boolean isPasswordResetAllowed(String clientIdentifier) {
        if (!featureConfiguration.getAuth().isRateLimitingEnabled()) {
            return true;
        }
        return tryConsume(getPasswordResetBucket(clientIdentifier));
    }

    private Bucket getLoginBucket(String key) {
        return cache.computeIfAbsent("login:" + key, k -> createBucket(loginAttemptsPerMinute, Duration.ofMinutes(1)));
    }

    private Bucket getRegistrationBucket(String key) {
        return cache.computeIfAbsent("register:" + key, k -> createBucket(registrationAttemptsPerHour, Duration.ofHours(1)));
    }

    private Bucket getTokenValidationBucket(String key) {
        return cache.computeIfAbsent("token:" + key, k -> createBucket(tokenValidationPerMinute, Duration.ofMinutes(1)));
    }

    private Bucket getPasswordResetBucket(String key) {
        return cache.computeIfAbsent("reset:" + key, k -> createBucket(passwordResetPerHour, Duration.ofHours(1)));
    }

    private Bucket createBucket(int capacity, Duration refillDuration) {
        Bandwidth bandwidth = Bandwidth.classic(capacity, Refill.intervally(capacity, refillDuration));
        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    private boolean tryConsume(Bucket bucket) {
        return bucket.tryConsume(1);
    }

    public long getRemainingTokens(String operation, String clientIdentifier) {
        Bucket bucket = switch (operation) {
            case "login" -> getLoginBucket(clientIdentifier);
            case "register" -> getRegistrationBucket(clientIdentifier);
            case "token" -> getTokenValidationBucket(clientIdentifier);
            case "reset" -> getPasswordResetBucket(clientIdentifier);
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
        return bucket.getAvailableTokens();
    }

    public void blockUser(String clientIdentifier, Duration duration) {
        String blockKey = "blocked:" + clientIdentifier;
        redisTemplate.opsForValue().set(blockKey, "blocked", duration);
    }

    public boolean isUserBlocked(String clientIdentifier) {
        String blockKey = "blocked:" + clientIdentifier;
        return redisTemplate.hasKey(blockKey);
    }
}