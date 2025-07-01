package com.ecommerce.user.services;

import com.ecommerce.user.config.FeatureConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for sending SMS messages
 * This is a mock implementation that can be replaced with actual SMS providers
 */
@Service
public class SmsService {
    
    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);
    
    @Value("${sms.enabled:false}")
    private boolean smsEnabled;
    
    @Value("${sms.provider:mock}")
    private String smsProvider;
    
    @Value("${sms.api.key:}")
    private String smsApiKey;
    
    @Value("${sms.api.secret:}")
    private String smsApiSecret;
    
    @Autowired
    private FeatureConfiguration featureConfiguration;
    
    /**
     * Send SMS message to the specified phone number
     * 
     * @param phoneNumber The recipient's phone number
     * @param message The message to send
     * @return true if SMS was sent successfully, false otherwise
     */
    public boolean sendSms(String phoneNumber, String message) {
        if (!smsEnabled || !featureConfiguration.getIntegration().isEmailEnabled()) {
            logger.info("SMS disabled. Would send SMS to {}: {}", phoneNumber, message);
            return true; // Simulate success when disabled
        }
        
        try {
            switch (smsProvider.toLowerCase()) {
                case "twilio":
                    return sendViaTwilio(phoneNumber, message);
                case "aws":
                    return sendViaAwsSns(phoneNumber, message);
                case "nexmo":
                    return sendViaNexmo(phoneNumber, message);
                case "mock":
                default:
                    return sendViaMock(phoneNumber, message);
            }
        } catch (Exception e) {
            logger.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }
    
    /**
     * Send SMS via Twilio (stub for real integration)
     * Replace this with actual Twilio API logic.
     */
    private boolean sendViaTwilio(String phoneNumber, String message) {
        logger.info("Sending SMS via Twilio to {}: {}", phoneNumber, message);
        // STUB: Twilio integration not implemented yet.
        return true; // Mock success
    }
    
    /**
     * Send SMS via AWS SNS (stub for real integration)
     * Replace this with actual AWS SNS API logic.
     */
    private boolean sendViaAwsSns(String phoneNumber, String message) {
        logger.info("Sending SMS via AWS SNS to {}: {}", phoneNumber, message);
        // STUB: AWS SNS integration not implemented yet.
        return true; // Mock success
    }
    
    /**
     * Send SMS via Nexmo/Vonage (stub for real integration)
     * Replace this with actual Nexmo API logic.
     */
    private boolean sendViaNexmo(String phoneNumber, String message) {
        logger.info("Sending SMS via Nexmo to {}: {}", phoneNumber, message);
        // STUB: Nexmo integration not implemented yet.
        return true; // Mock success
    }
    
    /**
     * Mock SMS sending for development/testing
     */
    private boolean sendViaMock(String phoneNumber, String message) {
        logger.info("MOCK SMS to {}: {}", phoneNumber, message);
        
        // Simulate some processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return true;
    }
    
    /**
     * Validate phone number format
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // Remove all non-digit characters for validation
        String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");
        
        // Basic validation - should start with + and have 10-15 digits
        return cleanNumber.matches("^\\+?[1-9]\\d{9,14}$");
    }
}

// TODOs for SMS providers are now wrapped with feature toggles (see FeatureConfiguration)
