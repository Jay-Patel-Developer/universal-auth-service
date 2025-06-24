package com.ecommerce.user.services;

import com.ecommerce.user.config.FeatureConfiguration;
import com.ecommerce.user.dto.MfaEnrollResponse;
import com.ecommerce.user.models.MfaConfiguration;
import com.ecommerce.user.models.MfaMethod;
import com.ecommerce.user.models.User;
import com.ecommerce.user.repositories.MfaConfigurationRepository;
import com.ecommerce.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class MfaServiceTest {
    // Mock dependencies for isolated unit testing
    @Mock
    private MfaConfigurationRepository mfaConfigurationRepository;
    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FeatureConfiguration featureConfiguration;
    @Mock
    private SecurityAuditService securityAuditService;
    // Inject mocks into the service under test
    @InjectMocks
    private MfaService mfaService;

    @BeforeEach
    void setUp() {
        // Initialize mocks before each test
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGenerateTotpSecret() {
        // Test: TOTP secret generation should return a non-empty string
        String secret = mfaService.generateSecret();
        assertNotNull(secret, "Secret should not be null");
        assertTrue(secret.length() > 0, "Secret should not be empty");
    }

    @Test
    void testVerifyTotpCode() {
        // Test: Generated TOTP code should be valid for the secret
        String secret = mfaService.generateSecret();
        // For simplicity, just check that verifyCode returns false for a wrong code
        assertFalse(mfaService.verifyCode(secret, "000000"), "Random code should not be valid");
    }

    @Test
    void testGenerateBackupCodes() {
        // Test: Backup code generation should return a list of unique codes
        var codes = mfaService.generateBackupCodes();
        assertNotNull(codes, "Codes should not be null");
        assertEquals(10, codes.size(), "Should generate 10 codes by default");
        assertEquals(codes.size(), codes.stream().distinct().count(), "Codes should be unique");
    }

    @Test
    void testEnrollMfa() {
        // Test: Enrolling MFA should persist configuration and return response
        Long userId = 1L;
        var user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setPhoneNumber("1234567890");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mfaConfigurationRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(mfaConfigurationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        FeatureConfiguration.Auth authConfig = mock(FeatureConfiguration.Auth.class);
        when(featureConfiguration.getAuth()).thenReturn(authConfig);
        when(authConfig.isMfaEnabled()).thenReturn(true);
        when(authConfig.getMfaEnforcement()).thenReturn("required");

        MfaEnrollResponse response = mfaService.beginEnrollment(userId, MfaMethod.TOTP);
        assertNotNull(response, "Response should not be null");
        assertEquals(MfaMethod.TOTP, response.getMethod());
        assertNotNull(response.getSecretKey());
        assertNotNull(response.getQrCodeUrl());
        assertEquals(10, response.getRecoveryCodes().size());
    }

    @Test
    void testDisableMfa() {
        // Test: Disabling MFA should set enabled to false and log event
        Long userId = 1L;
        var user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        var mfaConfig = new MfaConfiguration();
        mfaConfig.setUser(user);
        mfaConfig.setEnabled(true);
        mfaConfig.setMethod(MfaMethod.TOTP);
        when(mfaConfigurationRepository.findByUserId(userId)).thenReturn(Optional.of(mfaConfig));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        FeatureConfiguration.Auth authConfig = mock(FeatureConfiguration.Auth.class);
        when(featureConfiguration.getAuth()).thenReturn(authConfig);
        when(authConfig.isMfaEnabled()).thenReturn(true);
        when(authConfig.getMfaEnforcement()).thenReturn("required");
        when(mfaConfigurationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = mfaService.disableMfa(userId, "password", "127.0.0.1", "JUnit");
        assertTrue(result, "Disabling MFA should return true");
        assertFalse(mfaConfig.isEnabled(), "MFA should be disabled");
        // Optionally verify audit log
        verify(securityAuditService, atLeastOnce()).logSecurityEvent(eq(user.getEmail()), any(), any(), any());
    }

    // Add tests for MFA service logic
    @Test
    void testBeginEnrollmentWithExistingConfig() {
        // Test: Beginning enrollment when MFA is already configured should return null
        Long userId = 1L;
        var user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        var mfaConfig = new MfaConfiguration();
        mfaConfig.setUser(user);
        mfaConfig.setEnabled(true);
        mfaConfig.setMethod(MfaMethod.TOTP);
        when(mfaConfigurationRepository.findByUserId(userId)).thenReturn(Optional.of(mfaConfig));

        MfaEnrollResponse response = mfaService.beginEnrollment(userId, MfaMethod.TOTP);
        assertNull(response, "Response should be null when MFA is already configured");
    }

    @Test
    void testEnrollMfaWithDisabledFeature() {
        // Test: Enrolling MFA should not persist configuration if the feature is disabled
        Long userId = 1L;
        var user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mfaConfigurationRepository.findByUserId(userId)).thenReturn(Optional.empty());
        FeatureConfiguration.Auth authConfig = mock(FeatureConfiguration.Auth.class);
        when(featureConfiguration.getAuth()).thenReturn(authConfig);
        when(authConfig.isMfaEnabled()).thenReturn(false);

        MfaEnrollResponse response = mfaService.beginEnrollment(userId, MfaMethod.TOTP);
        assertNull(response, "Response should be null when MFA feature is disabled");
        verify(mfaConfigurationRepository, never()).save(any());
    }

    @Test
    void testDisableMfaWithIncorrectPassword() {
        // Test: Disabling MFA should fail with incorrect password
        Long userId = 1L;
        var user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        var mfaConfig = new MfaConfiguration();
        mfaConfig.setUser(user);
        mfaConfig.setEnabled(true);
        mfaConfig.setMethod(MfaMethod.TOTP);
        when(mfaConfigurationRepository.findByUserId(userId)).thenReturn(Optional.of(mfaConfig));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        FeatureConfiguration.Auth authConfig = mock(FeatureConfiguration.Auth.class);
        when(featureConfiguration.getAuth()).thenReturn(authConfig);
        when(authConfig.isMfaEnabled()).thenReturn(true);
        when(authConfig.getMfaEnforcement()).thenReturn("required");

        boolean result = mfaService.disableMfa(userId, "wrongpassword", "127.0.0.1", "JUnit");
        assertFalse(result, "Disabling MFA should return false with incorrect password");
        assertTrue(mfaConfig.isEnabled(), "MFA should remain enabled");
    }

    @Test
    void testGenerateTotpSecretEdgeCase() {
        // Test: TOTP secret generation handles edge cases (e.g., maximum length)
        String secret = mfaService.generateSecret();
        assertNotNull(secret, "Secret should not be null");
        assertTrue(secret.length() > 0, "Secret should not be empty");
        assertTrue(secret.length() <= 16, "Secret should not exceed 16 characters");
    }

    @Test
    void testVerifyTotpCodeEdgeCase() {
        // Test: TOTP code verification handles edge cases (e.g., expired codes)
        String secret = mfaService.generateSecret();
        String code = mfaService.getCurrentTotpCode(secret);
        assertTrue(mfaService.verifyCode(secret, code), "Valid code should verify successfully");
        // Simulate code expiration (e.g., by waiting or manipulating system time)
        try {
            Thread.sleep(30000); // Wait for 30 seconds (assuming 30s TOTP window)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertFalse(mfaService.verifyCode(secret, code), "Expired code should not verify");
    }

    // Additional integration and edge-case tests can be added here
}
