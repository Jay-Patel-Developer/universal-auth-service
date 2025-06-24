package com.ecommerce.user.events;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a user-related event for publishing or auditing.
 * Contains event type, user info, timestamp, and additional data.
 */
public class UserEvent {
    /** Type of the event (e.g., USER_REGISTERED, MFA_ENABLED). */
    private String eventType;
    /** User ID associated with the event. */
    private Long userId;
    /** User email associated with the event. */
    private String userEmail;
    /** Source of the event (e.g., auth-service). */
    private String eventSource;
    /** Timestamp when the event occurred. */
    private LocalDateTime timestamp;
    /** Additional event data. */
    private Map<String, Object> data;
    
    /**
     * Default constructor.
     */
    public UserEvent() {
    }
    
    /**
     * Constructs a UserEvent with all main fields.
     */
    public UserEvent(String eventType, Long userId, String userEmail, Map<String, Object> data) {
        this.eventType = eventType;
        this.userId = userId;
        this.userEmail = userEmail;
        this.eventSource = "auth-service";
        this.timestamp = LocalDateTime.now();
        this.data = data;
    }
    
    /** @return Event type. */
    public String getEventType() {
        return eventType;
    }
    
    /** @param eventType Event type. */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    /** @return User ID. */
    public Long getUserId() {
        return userId;
    }
    
    /** @param userId User ID. */
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    /** @return User email. */
    public String getUserEmail() {
        return userEmail;
    }
    
    /** @param userEmail User email. */
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    /** @return Event source. */
    public String getEventSource() {
        return eventSource;
    }
    
    /** @param eventSource Event source. */
    public void setEventSource(String eventSource) {
        this.eventSource = eventSource;
    }
    
    /** @return Event timestamp. */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /** @param timestamp Event timestamp. */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    /** @return Additional event data. */
    public Map<String, Object> getData() {
        return data;
    }
    
    /** @param data Additional event data. */
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
