package com.ecommerce.user.services;

import com.ecommerce.user.config.FeatureConfiguration;
import com.ecommerce.user.dto.UserRegistrationRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.events.UserEventPublisher;
import com.ecommerce.user.models.OAuthProvider;
import com.ecommerce.user.models.User;
import com.ecommerce.user.models.UserStatus;
import com.ecommerce.user.repositories.OAuthProviderRepository;
import com.ecommerce.user.repositories.UserRepository;
import com.ecommerce.user.utils.PasswordUtils;
import com.ecommerce.user.utils.JwtUtils;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class UserServiceTest {
    // Mock repository for isolated unit testing
    @Mock
    private UserRepository userRepository;
    @Mock private OAuthProviderRepository oauthProviderRepository;
    @Mock private JwtUtils jwtUtils;
    @Mock private InputValidationService inputValidationService;
    @Mock private SecurityAuditService securityAuditService;
    @Mock private RateLimitService rateLimitService;
    @Mock private MfaService mfaService;
    @Mock private UserEventPublisher userEventPublisher;
    @Mock private FeatureConfiguration featureConfiguration;
    @Mock private FeatureConfiguration.Auth authConfig;
    @Mock private FeatureConfiguration.Business businessConfig;

    // Inject mocks into the service under test
    @InjectMocks private UserService userService;

    @BeforeEach
    void setUp() {
        // Initialize mocks before each test
        MockitoAnnotations.openMocks(this);
        when(featureConfiguration.getAuth()).thenReturn(authConfig);
        when(featureConfiguration.getBusiness()).thenReturn(businessConfig);
    }

    @Test
    void testRegisterUser_Success() {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setEmail("test@example.com");
        req.setName("Test User");
        req.setPassword("password123");
        req.setPhoneNumber("1234567890");
        when(authConfig.isRateLimitingEnabled()).thenReturn(true);
        when(rateLimitService.isAllowed(anyString(), any())).thenReturn(true);
        when(inputValidationService.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
        when(inputValidationService.containsMaliciousContent(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            u.setCreatedAt(LocalDateTime.now());
            u.setUpdatedAt(LocalDateTime.now());
            return u;
        });
        UserResponse resp = userService.registerUser(req, "1.2.3.4", "JUnit");
        assertNotNull(resp);
        assertEquals("test@example.com", resp.getEmail());
        verify(userEventPublisher).publishUserRegisteredEvent("test@example.com");
    }

    @Test
    void testRegisterUser_DuplicateEmail() {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setEmail("test@example.com");
        req.setName("Test User");
        req.setPassword("password123");
        when(authConfig.isRateLimitingEnabled()).thenReturn(true);
        when(rateLimitService.isAllowed(anyString(), any())).thenReturn(true);
        when(inputValidationService.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
        when(inputValidationService.containsMaliciousContent(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(new User());
        assertThrows(RuntimeException.class, () -> userService.registerUser(req, "1.2.3.4", "JUnit"));
    }

    @Test
    void testLoginUser_Success() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword(PasswordUtils.hashPassword("password123"));
        when(authConfig.isRateLimitingEnabled()).thenReturn(true);
        when(rateLimitService.isAllowed(anyString(), any())).thenReturn(true);
        when(inputValidationService.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByEmail("test@example.com")).thenReturn(user);
        try (var pwMock = mockStatic(PasswordUtils.class)) {
            pwMock.when(() -> PasswordUtils.verifyPassword("password123", user.getPassword())).thenReturn(true);
            User result = userService.loginUser("test@example.com", "password123", "1.2.3.4", "JUnit");
            assertNotNull(result);
            verify(userEventPublisher).publishLoginSuccessEvent("test@example.com", "JUnit");
        }
    }

    @Test
    void testLoginUser_Failure() {
        when(authConfig.isRateLimitingEnabled()).thenReturn(true);
        when(rateLimitService.isAllowed(anyString(), any())).thenReturn(true);
        when(inputValidationService.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByEmail("test@example.com")).thenReturn(null);
        User result = userService.loginUser("test@example.com", "wrong", "1.2.3.4", "JUnit");
        assertNull(result);
        verify(userEventPublisher).publishLoginFailedEvent(eq("test@example.com"), anyString());
    }

    @Test
    void testUpdateUserProfile_Success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setName("Old Name");
        user.setPhoneNumber("111");
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setName("New Name");
        req.setPhoneNumber("222");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(inputValidationService.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
        when(inputValidationService.containsMaliciousContent(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        UserResponse resp = userService.updateUser(1L, req, "1.2.3.4", "JUnit");
        assertNotNull(resp);
        assertEquals("New Name", resp.getName());
        assertEquals("222", resp.getPhoneNumber());
    }

    @Test
    void testUpdateUserProfile_MaliciousInput() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setName("<script>");
        req.setPhoneNumber("222");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(inputValidationService.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
        when(inputValidationService.containsMaliciousContent(anyString())).thenReturn(true);
        assertThrows(RuntimeException.class, () -> userService.updateUser(1L, req, "1.2.3.4", "JUnit"));
    }

    @Test
    void testDeactivateUser_Success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        boolean result = userService.deactivateUser(1L, "test reason", "admin@e.com");
        assertTrue(result);
        assertEquals(UserStatus.INACTIVE, user.getStatus());
        verify(userEventPublisher).publishEvent(eq("ACCOUNT_DEACTIVATED"), eq("test@example.com"), anyMap());
    }

    @Test
    void testReactivateUser_Success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.INACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        boolean result = userService.reactivateUser(1L, "admin@e.com");
        assertTrue(result);
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(userEventPublisher).publishEvent(eq("ACCOUNT_REACTIVATED"), eq("test@example.com"), anyMap());
    }

    @Test
    void testAddRole_Success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setRoles(new ArrayList<>(List.of("USER")));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        boolean result = userService.addRole(1L, "ADMIN", "admin@e.com");
        assertTrue(result);
        assertTrue(user.getRoles().contains("ADMIN"));
        verify(userEventPublisher).publishRoleChangedEvent("test@example.com", "ADDED", "ADMIN");
    }

    @Test
    void testRemoveRole_Success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setRoles(new ArrayList<>(List.of("USER", "ADMIN")));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        boolean result = userService.removeRole(1L, "ADMIN", "admin@e.com");
        assertTrue(result);
        assertFalse(user.getRoles().contains("ADMIN"));
        verify(userEventPublisher).publishRoleChangedEvent("test@example.com", "REMOVED", "ADMIN");
    }

    @Test
    void testChangePassword_Success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword(PasswordUtils.hashPassword("oldpass"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        try (var pwMock = mockStatic(PasswordUtils.class)) {
            pwMock.when(() -> PasswordUtils.verifyPassword("oldpass", user.getPassword())).thenReturn(true);
            pwMock.when(() -> PasswordUtils.hashPassword("newpassword")).thenReturn("hashednew");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            boolean result = userService.changePassword(1L, "oldpass", "newpassword", "1.2.3.4", "JUnit");
            assertTrue(result);
            assertEquals("hashednew", user.getPassword());
            verify(userEventPublisher).publishPasswordChangedEvent("test@example.com");
        }
    }

    @Test
    void testChangePassword_WrongOldPassword() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword(PasswordUtils.hashPassword("oldpass"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        try (var pwMock = mockStatic(PasswordUtils.class)) {
            pwMock.when(() -> PasswordUtils.verifyPassword("wrong", user.getPassword())).thenReturn(false);
            boolean result = userService.changePassword(1L, "wrong", "newpassword", "1.2.3.4", "JUnit");
            assertFalse(result);
        }
    }

    // Add tests for user service logic
    @Test
    void testRegisterUser_InvalidEmail() {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setEmail("invalid-email");
        req.setName("Test User");
        req.setPassword("password123");
        req.setPhoneNumber("1234567890");
        when(authConfig.isRateLimitingEnabled()).thenReturn(true);
        when(rateLimitService.isAllowed(anyString(), any())).thenReturn(true);
        when(inputValidationService.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
        when(inputValidationService.containsMaliciousContent(anyString())).thenReturn(false);
        assertThrows(RuntimeException.class, () -> userService.registerUser(req, "1.2.3.4", "JUnit"));
    }

    @Test
    void testRegisterUser_WeakPassword() {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setEmail("test@example.com");
        req.setName("Test User");
        req.setPassword("123");
        req.setPhoneNumber("1234567890");
        when(authConfig.isRateLimitingEnabled()).thenReturn(true);
        when(rateLimitService.isAllowed(anyString(), any())).thenReturn(true);
        when(inputValidationService.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
        when(inputValidationService.containsMaliciousContent(anyString())).thenReturn(false);
        assertThrows(RuntimeException.class, () -> userService.registerUser(req, "1.2.3.4", "JUnit"));
    }

    @Test
    void testLoginUser_InactiveAccount() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword(PasswordUtils.hashPassword("password123"));
        user.setStatus(UserStatus.INACTIVE);
        when(authConfig.isRateLimitingEnabled()).thenReturn(true);
        when(rateLimitService.isAllowed(anyString(), any())).thenReturn(true);
        when(inputValidationService.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByEmail("test@example.com")).thenReturn(user);
        User result = userService.loginUser("test@example.com", "password123", "1.2.3.4", "JUnit");
        assertNull(result);
        verify(userEventPublisher).publishLoginFailedEvent(eq("test@example.com"), anyString());
    }

    @Test
    void testUpdateUserProfile_NonExistentUser() {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setName("New Name");
        req.setPhoneNumber("222");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> userService.updateUser(1L, req, "1.2.3.4", "JUnit"));
    }

    @Test
    void testDeactivateUser_NonExistentUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        boolean result = userService.deactivateUser(1L, "test reason", "admin@e.com");
        assertFalse(result);
    }

    @Test
    void testReactivateUser_NonExistentUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        boolean result = userService.reactivateUser(1L, "admin@e.com");
        assertFalse(result);
    }

    @Test
    void testAddRole_NonExistentUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        boolean result = userService.addRole(1L, "ADMIN", "admin@e.com");
        assertFalse(result);
    }

    @Test
    void testRemoveRole_NonExistentUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        boolean result = userService.removeRole(1L, "ADMIN", "admin@e.com");
        assertFalse(result);
    }

    @Test
    void testChangePassword_NonExistentUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        boolean result = userService.changePassword(1L, "oldpass", "newpassword", "1.2.3.4", "JUnit");
        assertFalse(result);
    }

    // ...more tests for MFA, GDPR, OAuth, backup codes, etc. can be added similarly...
}
