package com.ecommerce.user.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "features.integration.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.redis.password:}")
    private String redisPassword;
    
    @Value("${spring.redis.timeout:2000}")
    private int timeout;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (!redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }
        
        // Configure socket options for improved resilience
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(timeout))
                .build();
        
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .build();
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofMillis(timeout))
                .build();
        
        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
    
    @Bean
    public CircuitBreakerConfig redisCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(10000))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slidingWindowSize(10)
                .build();
    }
    
    @Bean
    public RetryConfig redisRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1000))
                .build();
    }
}
