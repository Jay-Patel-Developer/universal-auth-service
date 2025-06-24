package com.ecommerce.user.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class ProductionConfigValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductionConfigValidator.class);
    
    @Value("${spring.datasource.url:}")
    private String databaseUrl;
    
    @Value("${spring.datasource.username:}")
    private String databaseUsername;
    
    @Value("${spring.datasource.password:}")
    private String databasePassword;
    
    @Value("${jwt.secret:}")
    private String jwtSecret;
    
    @Value("${cors.allowed-origins:}")
    private String corsAllowedOrigins;
    
    @PostConstruct
    public void validateConfiguration() {
        logger.info("=== PRODUCTION CONFIGURATION VALIDATION ===");
        
        if (databaseUrl.isEmpty()) {
            throw new IllegalStateException("DATABASE_URL environment variable is required in production");
        }
        
        if (databaseUsername.isEmpty()) {
            throw new IllegalStateException("DB_USERNAME environment variable is required in production");
        }
        
        if (databasePassword.isEmpty()) {
            throw new IllegalStateException("DB_PASSWORD environment variable is required in production");
        }
        
        if (jwtSecret.isEmpty() || jwtSecret.equals("development-secret-key-not-for-production")) {
            throw new IllegalStateException("JWT_SECRET environment variable must be set with a secure value in production");
        }
        
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long for security");
        }
        
        if (corsAllowedOrigins.isEmpty()) {
            logger.warn("CORS_ALLOWED_ORIGINS not set - this may cause CORS issues");
        }
        
        logger.info("✅ All required production configuration is valid");
        logger.info("🔗 Database: {}", databaseUrl.replaceAll(":[^:/@]*@", ":***@")); // Hide password in URL
        logger.info("🛡️ JWT Secret: {} characters", jwtSecret.length());
        logger.info("🌐 CORS Origins: {}", corsAllowedOrigins.isEmpty() ? "None configured" : corsAllowedOrigins);
        logger.info("===============================================");
    }
}