package com.ecommerce.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Feature toggle configuration for enabling/disabling application features
 * across different environments (development, staging, production, test)
 */
@Configuration
@ConfigurationProperties(prefix = "features")
public class FeatureConfiguration {

    private Auth auth = new Auth();
    private Security security = new Security();
    private Integration integration = new Integration();
    private Monitoring monitoring = new Monitoring();
    private Business business = new Business();
    private Dev dev = new Dev();
    private Prod prod = new Prod();
    private Test test = new Test();

    // Authentication Features
    public static class Auth {
        private boolean mfaEnabled = true;
        private String mfaEnforcement = "optional"; // required, optional, disabled
        private boolean socialLoginEnabled = true;
        private boolean passwordResetEnabled = true;
        private boolean rateLimitingEnabled = true;
        // Per-provider toggles
        private boolean googleEnabled = true;
        private boolean facebookEnabled = true;
        private boolean githubEnabled = true;

        // Getters and setters
        public boolean isMfaEnabled() { return mfaEnabled; }
        public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }
        
        public String getMfaEnforcement() { return mfaEnforcement; }
        public void setMfaEnforcement(String mfaEnforcement) { this.mfaEnforcement = mfaEnforcement; }

        public boolean isSocialLoginEnabled() { return socialLoginEnabled; }
        public void setSocialLoginEnabled(boolean socialLoginEnabled) { this.socialLoginEnabled = socialLoginEnabled; }
        
        public boolean isPasswordResetEnabled() { return passwordResetEnabled; }
        public void setPasswordResetEnabled(boolean passwordResetEnabled) { this.passwordResetEnabled = passwordResetEnabled; }
        
        public boolean isRateLimitingEnabled() { return rateLimitingEnabled; }
        public void setRateLimitingEnabled(boolean rateLimitingEnabled) { this.rateLimitingEnabled = rateLimitingEnabled; }

        public boolean isGoogleEnabled() { return googleEnabled; }
        public void setGoogleEnabled(boolean googleEnabled) { this.googleEnabled = googleEnabled; }
        public boolean isFacebookEnabled() { return facebookEnabled; }
        public void setFacebookEnabled(boolean facebookEnabled) { this.facebookEnabled = facebookEnabled; }
        public boolean isGithubEnabled() { return githubEnabled; }
        public void setGithubEnabled(boolean githubEnabled) { this.githubEnabled = githubEnabled; }
    }

    // Security Features
    public static class Security {
        private boolean auditLoggingEnabled = true;
        private boolean sessionManagementEnabled = true;
        private boolean csrfProtectionEnabled = true;

        // Getters and setters
        public boolean isAuditLoggingEnabled() { return auditLoggingEnabled; }
        public void setAuditLoggingEnabled(boolean auditLoggingEnabled) { this.auditLoggingEnabled = auditLoggingEnabled; }
        
        public boolean isSessionManagementEnabled() { return sessionManagementEnabled; }
        public void setSessionManagementEnabled(boolean sessionManagementEnabled) { this.sessionManagementEnabled = sessionManagementEnabled; }
        
        public boolean isCsrfProtectionEnabled() { return csrfProtectionEnabled; }
        public void setCsrfProtectionEnabled(boolean csrfProtectionEnabled) { this.csrfProtectionEnabled = csrfProtectionEnabled; }
    }

    // Integration Features
    public static class Integration {
        private boolean kafkaEnabled = true;
        private boolean redisEnabled = false;
        private boolean emailEnabled = false;

        // Getters and setters
        public boolean isKafkaEnabled() { return kafkaEnabled; }
        public void setKafkaEnabled(boolean kafkaEnabled) { this.kafkaEnabled = kafkaEnabled; }
        
        public boolean isRedisEnabled() { return redisEnabled; }
        public void setRedisEnabled(boolean redisEnabled) { this.redisEnabled = redisEnabled; }
        
        public boolean isEmailEnabled() { return emailEnabled; }
        public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    }

    // Monitoring Features
    public static class Monitoring {
        private boolean actuatorEnabled = true;
        private boolean metricsEnabled = true;
        private boolean healthChecksEnabled = true;

        // Getters and setters
        public boolean isActuatorEnabled() { return actuatorEnabled; }
        public void setActuatorEnabled(boolean actuatorEnabled) { this.actuatorEnabled = actuatorEnabled; }
        
        public boolean isMetricsEnabled() { return metricsEnabled; }
        public void setMetricsEnabled(boolean metricsEnabled) { this.metricsEnabled = metricsEnabled; }
        
        public boolean isHealthChecksEnabled() { return healthChecksEnabled; }
        public void setHealthChecksEnabled(boolean healthChecksEnabled) { this.healthChecksEnabled = healthChecksEnabled; }
    }

    // Business Features
    public static class Business {
        private boolean gdprComplianceEnabled = true;
        private boolean userAnalyticsEnabled = false;
        private boolean automatedCleanupEnabled = false;

        // Getters and setters
        public boolean isGdprComplianceEnabled() { return gdprComplianceEnabled; }
        public void setGdprComplianceEnabled(boolean gdprComplianceEnabled) { this.gdprComplianceEnabled = gdprComplianceEnabled; }
        
        public boolean isUserAnalyticsEnabled() { return userAnalyticsEnabled; }
        public void setUserAnalyticsEnabled(boolean userAnalyticsEnabled) { this.userAnalyticsEnabled = userAnalyticsEnabled; }
        
        public boolean isAutomatedCleanupEnabled() { return automatedCleanupEnabled; }
        public void setAutomatedCleanupEnabled(boolean automatedCleanupEnabled) { this.automatedCleanupEnabled = automatedCleanupEnabled; }
    }

    // Development Features
    public static class Dev {
        private boolean mockServicesEnabled = false;
        private boolean testDataEnabled = false;
        private boolean swaggerUiEnabled = false;

        // Getters and setters
        public boolean isMockServicesEnabled() { return mockServicesEnabled; }
        public void setMockServicesEnabled(boolean mockServicesEnabled) { this.mockServicesEnabled = mockServicesEnabled; }
        
        public boolean isTestDataEnabled() { return testDataEnabled; }
        public void setTestDataEnabled(boolean testDataEnabled) { this.testDataEnabled = testDataEnabled; }
        
        public boolean isSwaggerUiEnabled() { return swaggerUiEnabled; }
        public void setSwaggerUiEnabled(boolean swaggerUiEnabled) { this.swaggerUiEnabled = swaggerUiEnabled; }
    }

    // Production Features
    public static class Prod {
        private boolean circuitBreakerEnabled = false;
        private boolean performanceMonitoringEnabled = false;
        private boolean backupAutomationEnabled = false;

        // Getters and setters
        public boolean isCircuitBreakerEnabled() { return circuitBreakerEnabled; }
        public void setCircuitBreakerEnabled(boolean circuitBreakerEnabled) { this.circuitBreakerEnabled = circuitBreakerEnabled; }
        
        public boolean isPerformanceMonitoringEnabled() { return performanceMonitoringEnabled; }
        public void setPerformanceMonitoringEnabled(boolean performanceMonitoringEnabled) { this.performanceMonitoringEnabled = performanceMonitoringEnabled; }
        
        public boolean isBackupAutomationEnabled() { return backupAutomationEnabled; }
        public void setBackupAutomationEnabled(boolean backupAutomationEnabled) { this.backupAutomationEnabled = backupAutomationEnabled; }
    }

    // Test Features
    public static class Test {
        private boolean mockExternalServicesEnabled = false;
        private boolean embeddedDatabaseEnabled = false;
        private boolean disableSecurityEnabled = false;

        // Getters and setters
        public boolean isMockExternalServicesEnabled() { return mockExternalServicesEnabled; }
        public void setMockExternalServicesEnabled(boolean mockExternalServicesEnabled) { this.mockExternalServicesEnabled = mockExternalServicesEnabled; }
        
        public boolean isEmbeddedDatabaseEnabled() { return embeddedDatabaseEnabled; }
        public void setEmbeddedDatabaseEnabled(boolean embeddedDatabaseEnabled) { this.embeddedDatabaseEnabled = embeddedDatabaseEnabled; }
        
        public boolean isDisableSecurityEnabled() { return disableSecurityEnabled; }
        public void setDisableSecurityEnabled(boolean disableSecurityEnabled) { this.disableSecurityEnabled = disableSecurityEnabled; }
    }

    // Main getters and setters
    public Auth getAuth() { return auth; }
    public void setAuth(Auth auth) { this.auth = auth; }

    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }

    public Integration getIntegration() { return integration; }
    public void setIntegration(Integration integration) { this.integration = integration; }

    public Monitoring getMonitoring() { return monitoring; }
    public void setMonitoring(Monitoring monitoring) { this.monitoring = monitoring; }

    public Business getBusiness() { return business; }
    public void setBusiness(Business business) { this.business = business; }

    public Dev getDev() { return dev; }
    public void setDev(Dev dev) { this.dev = dev; }

    public Prod getProd() { return prod; }
    public void setProd(Prod prod) { this.prod = prod; }

    public Test getTest() { return test; }
    public void setTest(Test test) { this.test = test; }
}