package com.ecommerce.user.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Component for processing user events from Kafka
 * Can be used for internal services that need to react to user events
 */
@Component
public class UserEventListener {
    private static final Logger logger = LoggerFactory.getLogger(UserEventListener.class);
    
    // For testing
    private CountDownLatch latch = new CountDownLatch(1);
    private Map<String, Object> receivedEvent;
    
    /**
     * Listen for general user events
     * 
     * @param event The received event payload
     */
    @KafkaListener(topics = "user-events", groupId = "user-service-internal-group")
    public void handleUserEvent(@Payload Map<String, Object> event) {
        logger.info("Received user event: {}", event.get("eventType"));
        processEvent(event);
    }
    
    /**
     * Listen for authentication-specific user events
     * 
     * @param event The received event payload
     */
    @KafkaListener(topics = "user-auth-events", groupId = "user-service-internal-group")
    public void handleAuthEvent(@Payload Map<String, Object> event) {
        logger.info("Received user auth event: {}", event.get("eventType"));
        processEvent(event);
    }
    
    /**
     * Listen for GDPR-related user events
     * 
     * @param event The received event payload
     */
    @KafkaListener(topics = "user-gdpr-events", groupId = "user-service-internal-group")
    public void handleGdprEvent(@Payload Map<String, Object> event) {
        logger.info("Received user GDPR event: {}", event.get("eventType"));
        processEvent(event);
    }
    
    /**
     * Process received event based on type
     * 
     * @param event The event to process
     */
    private void processEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            String subject = (String) event.get("subject");
            
            switch (eventType) {
                case "USER_REGISTERED":
                    // Handle user registration event
                    logger.debug("Processing registration for: {}", subject);
                    // Additional processing logic here
                    break;
                    
                case "MFA_ENABLED":
                case "MFA_DISABLED":
                    // Handle MFA status change events
                    logger.debug("Processing MFA status change for: {}", subject);
                    // Additional processing logic here
                    break;
                    
                case "DATA_DELETION_REQUESTED":
                    // Handle GDPR deletion request
                    logger.debug("Processing deletion request for: {}", subject);
                    // Additional processing logic here
                    break;
                    
                case "USER_DELETED":
                    // Handle user deletion confirmation
                    logger.debug("Processing user deletion for: {}", subject);
                    // Additional processing logic here
                    break;
                    
                default:
                    logger.debug("Unhandled event type: {}", eventType);
            }
            
            // For testing
            this.receivedEvent = event;
            latch.countDown();
            
        } catch (Exception e) {
            logger.error("Error processing event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Reset the latch for testing purposes
     */
    public void resetLatch() {
        this.latch = new CountDownLatch(1);
    }
    
    /**
     * Get the latch for testing purposes
     */
    public CountDownLatch getLatch() {
        return latch;
    }
    
    /**
     * Get the last received event for testing
     */
    public Map<String, Object> getReceivedEvent() {
        return receivedEvent;
    }
}