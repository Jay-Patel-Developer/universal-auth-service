package com.ecommerce.user.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis service with fallback mechanisms for when Redis is unavailable
 */
@Service
public class RedisServiceWithFallback {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisServiceWithFallback.class);
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    private boolean redisAvailable = true;
    
    /**
     * Set value with TTL, falls back gracefully if Redis is unavailable
     */
    public boolean setValue(String key, Object value, long timeout, TimeUnit unit) {
        if (!isRedisAvailable()) {
            logger.warn("Redis unavailable, skipping cache operation for key: {}", key);
            return false;
        }
        
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            return true;
        } catch (DataAccessException e) {
            handleRedisFailure("setValue", e);
            return false;
        }
    }
    
    /**
     * Get value, returns null if Redis is unavailable
     */
    public Object getValue(String key) {
        if (!isRedisAvailable()) {
            logger.warn("Redis unavailable, returning null for key: {}", key);
            return null;
        }
        
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (DataAccessException e) {
            handleRedisFailure("getValue", e);
            return null;
        }
    }
    
    /**
     * Delete key, fails gracefully if Redis is unavailable
     */
    public boolean deleteKey(String key) {
        if (!isRedisAvailable()) {
            logger.warn("Redis unavailable, skipping delete for key: {}", key);
            return false;
        }
        
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (DataAccessException e) {
            handleRedisFailure("deleteKey", e);
            return false;
        }
    }
    
    /**
     * Increment counter, returns null if Redis is unavailable
     */
    public Long increment(String key) {
        if (!isRedisAvailable()) {
            logger.warn("Redis unavailable, cannot increment key: {}", key);
            return null;
        }
        
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (DataAccessException e) {
            handleRedisFailure("increment", e);
            return null;
        }
    }
    
    /**
     * Add to list, fails gracefully if Redis is unavailable
     */
    public boolean leftPush(String key, Object value) {
        if (!isRedisAvailable()) {
            logger.warn("Redis unavailable, skipping list operation for key: {}", key);
            return false;
        }
        
        try {
            redisTemplate.opsForList().leftPush(key, value);
            return true;
        } catch (DataAccessException e) {
            handleRedisFailure("leftPush", e);
            return false;
        }
    }
    
    /**
     * Check if key exists, returns false if Redis is unavailable
     */
    public boolean hasKey(String key) {
        if (!isRedisAvailable()) {
            logger.warn("Redis unavailable, returning false for key existence: {}", key);
            return false;
        }
        
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (DataAccessException e) {
            handleRedisFailure("hasKey", e);
            return false;
        }
    }
    
    /**
     * Hash operations - increment field
     */
    public boolean hashIncrement(String key, String field, double delta) {
        if (!isRedisAvailable()) {
            logger.warn("Redis unavailable, skipping hash increment for key: {}", key);
            return false;
        }
        
        try {
            redisTemplate.opsForHash().increment(key, field, delta);
            return true;
        } catch (DataAccessException e) {
            handleRedisFailure("hashIncrement", e);
            return false;
        }
    }
    
    /**
     * Set expiration on key
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        if (!isRedisAvailable()) {
            logger.warn("Redis unavailable, skipping expiration for key: {}", key);
            return false;
        }
        
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
        } catch (DataAccessException e) {
            handleRedisFailure("expire", e);
            return false;
        }
    }
    
    private boolean isRedisAvailable() {
        if (redisTemplate == null) {
            return false;
        }
        
        // Simple availability check - if we've recently failed, assume still unavailable
        return redisAvailable;
    }
    
    private void handleRedisFailure(String operation, Exception e) {
        logger.error("Redis operation '{}' failed: {}", operation, e.getMessage());
        redisAvailable = false;
        
        // Schedule a check to see if Redis is back up (simple approach)
        // In production, you might want a more sophisticated circuit breaker pattern
        scheduleAvailabilityCheck();
    }
    
    private void scheduleAvailabilityCheck() {
        // Simple retry mechanism - set available to true after a delay
        // In production, implement proper circuit breaker pattern
        new Thread(() -> {
            try {
                Thread.sleep(30000); // Wait 30 seconds
                redisAvailable = true;
                logger.info("Redis availability check - marking as potentially available");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}