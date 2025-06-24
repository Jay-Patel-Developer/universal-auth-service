package com.ecommerce.user.config;

import com.ecommerce.user.services.RedisFallbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration for Redis with fallback support
 */
@Configuration
public class RedisFallbackConfig {

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        
        if (redisConnectionFactory != null) {
            template.setConnectionFactory(redisConnectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.afterPropertiesSet();
        }
        
        return template;
    }

    @Bean
    public HealthIndicator redisHealthIndicator(RedisFallbackService redisFallbackService) {
        return () -> {
            try {
                boolean isAvailable = redisFallbackService.isRedisAvailable();
                String circuitBreakerState = redisFallbackService.getCircuitBreakerState();
                String metrics = redisFallbackService.getCircuitBreakerMetrics();
                
                return Health.status(isAvailable ? "UP" : "DOWN")
                        .withDetail("circuitBreakerState", circuitBreakerState)
                        .withDetail("metrics", metrics)
                        .withDetail("fallbackEnabled", true)
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .withDetail("fallbackEnabled", true)
                        .build();
            }
        };
    }
}
