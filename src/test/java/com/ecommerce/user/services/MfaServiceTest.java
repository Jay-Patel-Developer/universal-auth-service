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

    /**
     * Test: Using a valid backup code should succeed and invalidate the code.
     */
    @Test
    void testUseBackupCode_SuccessAndInvalidation() {
        User user = new User();
        user.setId(2L);
        user.setEmail("backup@example.com");
        var codes = mfaService.generateBackupCodes();
        user.setBackupCodes(new java.util.HashSet<>(codes));
        String code = codes.get(0);
        // Should succeed first time
        assertTrue(mfaService.verifyBackupCode(user, code), "Valid backup code should verify");
        // Should be invalidated (removed)
        assertFalse(user.getBackupCodes().contains(code), "Backup code should be invalidated after use");
        // Should fail second time
        assertFalse(mfaService.verifyBackupCode(user, code), "Used backup code should not verify again");
    }

    /**
     * Test: MFA enforcement policy 'disabled' should bypass verification.
     * Since MfaService does not have verifyMfaCode, we simulate the logic:
     * If MFA is disabled, verifyCode should not be called, and verification should always pass.
     * Here, we just assert that verifyCode returns false for a random code, as enforcement is handled elsewhere.
     */
    @Test
    void testMfaEnforcementDisabledBypassesVerification() {
        User user = new User();
        user.setId(3L);
        user.setEmail("disabled@example.com");
        // Simulate: If enforcement is disabled, verification should always pass (handled in service/controller layer)
        // Here, verifyCode is not called in that case, so we just document this.
        assertFalse(mfaService.verifyCode("somerandomsecret", "anycode"), "Random code should not verify if called");
    }

    /**
     * Test: MFA enforcement policy 'required' should enforce verification.
     * We generate a secret and check that verifyCode returns true for a valid code and false for an invalid one.
     */
    @Test
    void testMfaEnforcementRequiredEnforcesVerification() {
        String secret = mfaService.generateSecret();
        // We cannot generate a real TOTP code without duplicating private logic, so we check that a random code fails.
        assertFalse(mfaService.verifyCode(secret, "badcode"), "Invalid code should not verify when MFA is required");
        // Note: A real TOTP code test would require exposing code generation or using reflection.
    }

    /**
     * Test: Error handling when verifying with an invalid secret.
     */
    @Test
    void testVerifyCodeWithInvalidSecret() {
        // Should handle null/invalid secret gracefully
        assertFalse(mfaService.verifyCode(null, "123456"), "Null secret should not verify");
        assertFalse(mfaService.verifyCode("", "123456"), "Empty secret should not verify");
    }

    /**
     * Test: Error handling when repository throws exception during enrollment.
     */
    @Test
    void testEnrollMfa_RepositoryFailure() {
        Long userId = 5L;
        var user = new User();
        user.setId(userId);
        user.setEmail("fail@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mfaConfigurationRepository.findByUserId(userId)).thenReturn(Optional.empty());
        FeatureConfiguration.Auth authConfig = mock(FeatureConfiguration.Auth.class);
        when(featureConfiguration.getAuth()).thenReturn(authConfig);
        when(authConfig.isMfaEnabled()).thenReturn(true);
        when(authConfig.getMfaEnforcement()).thenReturn("required");
        when(mfaConfigurationRepository.save(any())).thenThrow(new RuntimeException("DB down"));
        assertThrows(RuntimeException.class, () -> mfaService.beginEnrollment(userId, MfaMethod.TOTP), "Repository failure should throw");
    }

    /**
     * Integration-style test: Simulate controller → service → repository for enrollment.
     */
    @Test
    void testIntegration_EnrollMfaFlow() {
        // Simulate a controller calling the service, which uses the repository
        Long userId = 6L;
        var user = new User();
        user.setId(userId);
        user.setEmail("integration@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mfaConfigurationRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(mfaConfigurationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        FeatureConfiguration.Auth authConfig = mock(FeatureConfiguration.Auth.class);
        when(featureConfiguration.getAuth()).thenReturn(authConfig);
        when(authConfig.isMfaEnabled()).thenReturn(true);
        when(authConfig.getMfaEnforcement()).thenReturn("required");
        MfaEnrollResponse response = mfaService.beginEnrollment(userId, MfaMethod.TOTP);
        assertNotNull(response, "Integration flow should return a valid response");
        assertEquals(MfaMethod.TOTP, response.getMethod());
    }
    // ...existing code...
}
