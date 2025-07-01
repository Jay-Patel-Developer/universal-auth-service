package com.ecommerce.user.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis service with fallback mechanisms and circuit breaker pattern
 */
@Service
public class RedisFallbackService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisFallbackService.class);
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    @Value("${redis.fallback.enabled:true}")
    private boolean fallbackEnabled;
    
    @Value("${redis.circuit-breaker.failure-threshold:5}")
    private int failureThreshold;
    
    @Value("${redis.circuit-breaker.recovery-timeout:30000}")
    private long recoveryTimeout;
    
    private final CircuitBreaker circuitBreaker;
    
    public RedisFallbackService() {
        // Configure circuit breaker
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(30000))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .build();
                
        this.circuitBreaker = CircuitBreaker.of("redis", config);
        
        // Add event listeners
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    logger.info("Redis Circuit breaker state transition: {}", event))
                .onFailureRateExceeded(event -> 
                    logger.warn("Redis Circuit breaker failure rate exceeded: {}", event))
                .onCallNotPermitted(event -> 
                    logger.warn("Redis Circuit breaker call not permitted: {}", event));
    }
    
    /**
     * Set a value in Redis with fallback
     */
    public boolean set(String key, Object value) {
        return executeWithFallback(() -> {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(key, value);
                return true;
            }
            return false;
        }, () -> {
            logger.debug("Redis unavailable, fallback for SET key: {}", key);
            return false; // Fallback: operation considered successful but not stored
        });
    }
    
    /**
     * Set a value in Redis with expiration and fallback
     */
    public boolean set(String key, Object value, long timeout, TimeUnit unit) {
        return executeWithFallback(() -> {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(key, value, timeout, unit);
                return true;
            }
            return false;
        }, () -> {
            logger.debug("Redis unavailable, fallback for SET with TTL key: {}", key);
            return false; // Fallback: operation considered successful but not stored
        });
    }
    
    /**
     * Get a value from Redis with fallback
     */
    public Object get(String key) {
        return executeWithFallback(() -> {
            if (redisTemplate != null) {
                return redisTemplate.opsForValue().get(key);
            }
            return null;
        }, () -> {
            logger.debug("Redis unavailable, fallback for GET key: {}", key);
            return null; // Fallback: return null as if key doesn't exist
        });
    }
    
    /**
     * Check if key exists in Redis with fallback
     */
    public boolean exists(String key) {
        return executeWithFallback(() -> {
            if (redisTemplate != null) {
                return Boolean.TRUE.equals(redisTemplate.hasKey(key));
            }
            return false;
        }, () -> {
            logger.debug("Redis unavailable, fallback for EXISTS key: {}", key);
            return false; // Fallback: assume key doesn't exist
        });
    }
    
    /**
     * Delete a key from Redis with fallback
     */
    public boolean delete(String key) {
        return executeWithFallback(() -> {
            if (redisTemplate != null) {
                return Boolean.TRUE.equals(redisTemplate.delete(key));
            }
            return false;
        }, () -> {
            logger.debug("Redis unavailable, fallback for DELETE key: {}", key);
            return true; // Fallback: consider deletion successful
        });
    }
    
    /**
     * Increment a value in Redis with fallback
     */
    public Long increment(String key) {
        return executeWithFallback(() -> {
            if (redisTemplate != null) {
                return redisTemplate.opsForValue().increment(key);
            }
            return null;
        }, () -> {
            logger.debug("Redis unavailable, fallback for INCREMENT key: {}", key);
            return 1L; // Fallback: return 1 as initial increment
        });
    }
    
    /**
     * Set expiration on a key with fallback
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return executeWithFallback(() -> {
            if (redisTemplate != null) {
                return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
            }
            return false;
        }, () -> {
            logger.debug("Redis unavailable, fallback for EXPIRE key: {}", key);
            return true; // Fallback: consider expiration set successfully
        });
    }
    
    /**
     * Get time to live of a key with fallback
     */
    public Long getExpire(String key) {
        return executeWithFallback(() -> {
            if (redisTemplate != null) {
                return redisTemplate.getExpire(key);
            }
            return null;
        }, () -> {
            logger.debug("Redis unavailable, fallback for GET_EXPIRE key: {}", key);
            return -1L; // Fallback: return -1 indicating no expiration
        });
    }
    
    /**
     * Push to a Redis list with fallback
     */
    public Long listPush(String key, Object value) {
        return executeWithFallback(() -> {
            if (redisTemplate != null) {
                return redisTemplate.opsForList().leftPush(key, value);
            }
            return null;
        }, () -> {
            logger.debug("Redis unavailable, fallback for LIST_PUSH key: {}", key);
            return 1L; // Fallback: return 1 as if one item was added
        });
    }
    
    /**
     * Execute operation with circuit breaker and fallback
     */
    private <T> T executeWithFallback(Supplier<T> operation, Supplier<T> fallback) {
        if (!fallbackEnabled) {
            try {
                return operation.get();
            } catch (Exception e) {
                logger.error("Redis operation failed and fallback disabled: {}", e.getMessage());
                throw new RuntimeException("Redis operation failed", e);
            }
        }
        
        try {
            return circuitBreaker.executeSupplier(() -> {
                try {
                    return operation.get();
                } catch (Exception e) {
                    logger.warn("Redis operation failed: {}", e.getMessage());
                    throw e;
                }
            });
        } catch (Exception e) {
            logger.debug("Redis operation failed, using fallback: {}", e.getMessage());
            return fallback.get();
        }
    }
    
    /**
     * Check if Redis is available
     */
    public boolean isRedisAvailable() {
        try {
            if (redisTemplate == null) {
                return false;
            }
            
            // Try a simple ping operation
            String testKey = "health_check_" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(testKey, "ping", 1, TimeUnit.SECONDS);
            Object result = redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);
            
            return "ping".equals(result);
        } catch (Exception e) {
            logger.debug("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get circuit breaker state
     */
    public String getCircuitBreakerState() {
        return circuitBreaker.getState().toString();
    }
    
    /**
     * Get circuit breaker metrics
     */
    public String getCircuitBreakerMetrics() {
        var metrics = circuitBreaker.getMetrics();
        return String.format(
            "Failure Rate: %.2f%%, Successful Calls: %d, Failed Calls: %d, State: %s",
            metrics.getFailureRate(),
            metrics.getNumberOfSuccessfulCalls(),
            metrics.getNumberOfFailedCalls(),
            circuitBreaker.getState()
        );
    }
}