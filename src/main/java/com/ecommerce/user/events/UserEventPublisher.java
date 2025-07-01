package com.ecommerce.user.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;

/**
 * Publishes user-related events to Kafka or logs them for audit and integration purposes.
 * Events include registration, login, role changes, password changes, MFA, and account deletion.
 */
@Component
public class UserEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(UserEventPublisher.class);

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${features.integration.kafka-enabled:false}")
    private boolean kafkaEnabled;

    /**
     * Publish a user registration event.
     * @param email User's email address
     */
    public void publishUserRegisteredEvent(String email) {
        publishEvent("USER_REGISTERED", email, null);
    }

    /**
     * Publish a successful login event.
     * @param email User's email address
     * @param userAgent User agent string
     */
    public void publishLoginSuccessEvent(String email, String userAgent) {
        publishEvent("LOGIN_SUCCESS", email, Map.of("userAgent", userAgent));
    }

    /**
     * Publish a failed login event.
     * @param email User's email address
     * @param reason Reason for failure
     */
    public void publishLoginFailedEvent(String email, String reason) {
        publishEvent("LOGIN_FAILED", email, Map.of("reason", reason));
    }

    /**
     * Publish a role change event.
     * @param email User's email address
     * @param action Action performed (ADDED/REMOVED)
     * @param role Role name
     */
    public void publishRoleChangedEvent(String email, String action, String role) {
        publishEvent("ROLE_CHANGED", email, Map.of("action", action, "role", role));
    }

    /**
     * Publish a password changed event.
     * @param email User's email address
     */
    public void publishPasswordChangedEvent(String email) {
        publishEvent("PASSWORD_CHANGED", email, null);
    }

    /**
     * Publish an MFA enabled event.
     * @param email User's email address
     */
    public void publishMfaEnabledEvent(String email) {
        publishEvent("MFA_ENABLED", email, null);
    }

    /**
     * Publish an MFA disabled event.
     * @param email User's email address
     */
    public void publishMfaDisabledEvent(String email) {
        publishEvent("MFA_DISABLED", email, null);
    }

    /**
     * Publish an account deletion requested event.
     * @param email User's email address
     */
    public void publishAccountDeletionRequestedEvent(String email) {
        publishEvent("ACCOUNT_DELETION_REQUESTED", email, null);
    }

    /**
     * Publish an account deleted event.
     * @param email User's email address
     */
    public void publishAccountDeletedEvent(String email) {
        publishEvent("ACCOUNT_DELETED", email, null);
    }

    /**
     * Publish a generic user event.
     * @param eventType Event type string
     * @param email User's email address
     * @param data Additional event data
     */
    public void publishEvent(String eventType, String email, Map<String, Object> data) {
        if (kafkaEnabled && kafkaTemplate != null) {
            try {
                Map<String, Object> event = Map.of(
                    "type", eventType,
                    "email", email,
                    "data", data
                );
                kafkaTemplate.send("user-events", event);
                logger.info("Published event to Kafka: {}", event);
            } catch (Exception e) {
                logger.error("Failed to publish event to Kafka: {}", e.getMessage());
            }
        } else {
            logger.info("Event ({}): email={}, data={}", eventType, email, data);
        }
    }
}
