package com.ecommerce.user.security;

import com.ecommerce.user.config.FeatureConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;
    private final FeatureConfiguration featureConfiguration;
    
    @Value("${security.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;
    
    @Value("${security.rate-limit.requests-per-hour:1000}")
    private int requestsPerHour;
    
    @Value("${security.rate-limit.burst-limit:10}")
    private int burstLimit;

    public RateLimitingFilter(RedisTemplate<String, String> redisTemplate, FeatureConfiguration featureConfiguration) {
        this.redisTemplate = redisTemplate;
        this.featureConfiguration = featureConfiguration;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        // Check if rate limiting is enabled
        if (!featureConfiguration.getAuth().isRateLimitingEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String clientIp = getClientIpAddress(request);
        String endpoint = request.getRequestURI();
        
        // Skip rate limiting for health checks
        if (endpoint.startsWith("/actuator/health")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Apply different rate limits for different endpoints
        if (isAuthEndpoint(endpoint)) {
            if (!checkAuthRateLimit(clientIp)) {
                sendRateLimitExceededResponse(response);
                return;
            }
        } else {
            if (!checkGeneralRateLimit(clientIp)) {
                sendRateLimitExceededResponse(response);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean isAuthEndpoint(String endpoint) {
        return endpoint.startsWith("/api/auth/login") || 
               endpoint.startsWith("/api/auth/register") ||
               endpoint.startsWith("/api/auth/forgot-password");
    }

    private boolean checkAuthRateLimit(String clientIp) {
        String minuteKey = "rate_limit:auth:" + clientIp + ":" + getCurrentMinute();
        String hourKey = "rate_limit:auth:" + clientIp + ":" + getCurrentHour();
        
        // Stricter limits for authentication endpoints
        return checkRateLimit(minuteKey, 5, 60) && checkRateLimit(hourKey, 50, 3600);
    }

    private boolean checkGeneralRateLimit(String clientIp) {
        String minuteKey = "rate_limit:general:" + clientIp + ":" + getCurrentMinute();
        String hourKey = "rate_limit:general:" + clientIp + ":" + getCurrentHour();
        
        return checkRateLimit(minuteKey, requestsPerMinute, 60) && 
               checkRateLimit(hourKey, requestsPerHour, 3600);
    }

    private boolean checkRateLimit(String key, int limit, int windowSeconds) {
        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);
            if (currentCount == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            return currentCount <= limit;
        } catch (Exception e) {
            // If Redis is unavailable, allow the request (fail open)
            logger.warn("Rate limiting check failed, allowing request", e);
            return true;
        }
    }

    private String getCurrentMinute() {
        return String.valueOf(System.currentTimeMillis() / 60000);
    }

    private String getCurrentHour() {
        return String.valueOf(System.currentTimeMillis() / 3600000);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private void sendRateLimitExceededResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429); // HTTP 429 Too Many Requests
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}"
        );
    }
}