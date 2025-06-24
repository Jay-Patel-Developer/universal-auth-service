# Feature Toggle and Configuration Management

## Overview

This document describes the feature toggle and configuration management system for the E-Commerce User Authentication Service. The system supports dynamic enabling/disabling of features across environments (development, staging, production, test) using Spring profiles and environment variables.

## Feature Toggle Matrix

| Feature Category      | Config Key / Property                      | Description                                 |
|----------------------|--------------------------------------------|---------------------------------------------|
| MFA                  | FEATURE_MFA_ENABLED                        | Enable/disable multi-factor authentication  |
| Social Login         | FEATURE_SOCIAL_LOGIN_ENABLED               | Enable/disable OAuth providers              |
| Password Reset       | FEATURE_PASSWORD_RESET_ENABLED              | Enable/disable password reset               |
| Rate Limiting        | FEATURE_RATE_LIMITING_ENABLED               | Enable/disable rate limiting                |
| Audit Logging        | FEATURE_AUDIT_LOGGING_ENABLED               | Enable/disable security audit logging       |
| Session Management   | FEATURE_SESSION_MANAGEMENT_ENABLED          | Enable/disable session management           |
| CSRF Protection      | FEATURE_CSRF_PROTECTION_ENABLED             | Enable/disable CSRF protection              |
| Kafka Integration    | FEATURE_KAFKA_ENABLED                       | Enable/disable Kafka event publishing       |
| Redis Integration    | FEATURE_REDIS_ENABLED                       | Enable/disable Redis caching/session store  |
| Email Notifications  | FEATURE_EMAIL_ENABLED                       | Enable/disable email notifications          |
| Actuator Endpoints   | FEATURE_ACTUATOR_ENABLED                    | Enable/disable Spring Boot Actuator         |
| Metrics              | FEATURE_METRICS_ENABLED                     | Enable/disable metrics collection           |
| Health Checks        | FEATURE_HEALTH_CHECKS_ENABLED               | Enable/disable health check endpoints       |
| GDPR Compliance      | FEATURE_GDPR_ENABLED                        | Enable/disable GDPR features                |
| User Analytics       | FEATURE_USER_ANALYTICS_ENABLED              | Enable/disable analytics                    |
| Automated Cleanup    | FEATURE_AUTOMATED_CLEANUP_ENABLED           | Enable/disable scheduled data cleanup       |

## Environment Profiles

- **Development**: Most features off for speed, debug features on, mock services enabled
- **Staging**: All production features enabled, enhanced monitoring
- **Production**: All security and monitoring features enabled, optimized for performance
- **Test**: Minimal features, mocks and in-memory DB, security disabled for test speed

## Configuration Files

```
src/main/resources/
├── application.properties                 # Default
├── application-development.properties     # Development
├── application-staging.properties         # Staging
├── application-production.properties      # Production
└── application-test.properties            # Test
```

## Example Usage

**Conditional Bean Creation:**

```java
@Component
@ConditionalOnProperty(name = "features.auth.mfa.enabled", havingValue = "true")
public class MfaService { /* ... */ }
```

**Runtime Feature Checks:**

```java
if (featureConfig.getAuth().isMfaEnabled()) {
    // MFA logic
}
```

## Best Practices

- Never commit secrets to version control
- Use environment variables for sensitive config
- Test feature toggles in staging before production
- Document feature dependencies and rollback plans
- Monitor feature usage and performance impact

## Troubleshooting

- Use `/actuator/env` and `/actuator/configprops` to verify config
- Check environment variables with `env | grep FEATURE_`
- Validate configuration on startup and monitor logs for warnings

## Updating Feature Toggles

1. Update environment variables or config files
2. Restart the application (or use Spring Boot Actuator refresh)
3. Verify feature state via actuator endpoints