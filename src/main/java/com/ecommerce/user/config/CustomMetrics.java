package com.ecommerce.user.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class CustomMetrics {

    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * Initializes custom authentication and registration metrics.
     */
    @PostConstruct
    public void initMetrics() {
        Timer.builder("auth.login.duration")
                .description("Time taken for user authentication")
                .register(meterRegistry);
                
        Timer.builder("auth.registration.duration")
                .description("Time taken for user registration")
                .register(meterRegistry);
                
        Timer.builder("auth.token.validation.duration")
                .description("Time taken for token validation")
                .register(meterRegistry);
    }

    /**
     * Starts a timer for user authentication duration measurement.
     * @return Timer.Sample instance for tracking duration
     */
    public Timer.Sample startAuthenticationTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Records the result and duration of an authentication attempt.
     * @param sample Timer sample to stop
     * @param success True if authentication succeeded
     */
    public void recordAuthentication(Timer.Sample sample, boolean success) {
        sample.stop(Timer.builder("auth.login.duration")
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry));
        
        meterRegistry.counter("auth.login.attempts", 
                "status", success ? "success" : "failure").increment();
    }

    /**
     * Starts a timer for user registration duration measurement.
     * @return Timer.Sample instance for tracking duration
     */
    public Timer.Sample startRegistrationTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Records the result and duration of a registration attempt.
     * @param sample Timer sample to stop
     * @param success True if registration succeeded
     */
    public void recordRegistration(Timer.Sample sample, boolean success) {
        sample.stop(Timer.builder("auth.registration.duration")
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry));
        
        meterRegistry.counter("auth.registration.attempts",
                "status", success ? "success" : "failure").increment();
    }

    /**
     * Starts a timer for token validation duration measurement.
     * @return Timer.Sample instance for tracking duration
     */
    public Timer.Sample startTokenValidationTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Records the result and duration of a token validation attempt.
     * @param sample Timer sample to stop
     * @param valid True if token was valid
     */
    public void recordTokenValidation(Timer.Sample sample, boolean valid) {
        sample.stop(Timer.builder("auth.token.validation.duration")
                .tag("valid", valid ? "true" : "false")
                .register(meterRegistry));
        
        meterRegistry.counter("auth.token.validations",
                "valid", valid ? "true" : "false").increment();
    }
}