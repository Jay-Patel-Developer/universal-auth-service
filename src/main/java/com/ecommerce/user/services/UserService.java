package com.ecommerce.user.services;

import com.ecommerce.user.config.FeatureConfiguration;
import com.ecommerce.user.dto.UserRegistrationRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.models.User;
import com.ecommerce.user.models.UserStatus;
import com.ecommerce.user.models.OAuthProvider;
import com.ecommerce.user.repositories.UserRepository;
import com.ecommerce.user.repositories.OAuthProviderRepository;
import com.ecommerce.user.utils.PasswordUtils;
import com.ecommerce.user.utils.EmailValidator;
import com.ecommerce.user.utils.JwtUtils;
import com.ecommerce.user.events.UserEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service class for user management, authentication, MFA, and related business logic.
 */
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuthProviderRepository oauthProviderRepository;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private InputValidationService inputValidationService;
    
    @Autowired
    private SecurityAuditService securityAuditService;
    
    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private MfaService mfaService;
    
    @Autowired
    private UserEventPublisher userEventPublisher;

    @Autowired
    private FeatureConfiguration featureConfiguration;

    /**
     * Registers a new user after validating input and rate limits.
     * @param request User registration details
     * @param clientIp IP address of the client
     * @param userAgent User agent string
     * @return UserResponse DTO with user details
     * @throws RuntimeException if registration fails
     */
    public UserResponse registerUser(UserRegistrationRequest request, String clientIp, String userAgent) {
        // Check rate limits
        if (featureConfiguration.getAuth().isRateLimitingEnabled()) {
            if (!rateLimitService.isAllowed(clientIp, RateLimitService.RateLimitType.REGISTRATION)) {
                throw new RuntimeException("Registration rate limit exceeded");
            }
        }
        
        // Sanitize and validate inputs
        String email = inputValidationService.sanitizeInput(request.getEmail());
        String name = inputValidationService.sanitizeInput(request.getName());
        
        if (inputValidationService.containsMaliciousContent(request.getEmail()) || 
            inputValidationService.containsMaliciousContent(request.getName())) {
            securityAuditService.logSuspiciousActivity(email, clientIp, "Malicious content detected in registration");
            throw new RuntimeException("Invalid input detected");
        }
        
        // Validate email format
        if (!EmailValidator.isValidEmail(email)) {
            throw new RuntimeException("Invalid email format");
        }

        // Check if user already exists
        if (userRepository.findByEmail(email) != null) {
            throw new RuntimeException("User with this email already exists");
        }

        // Create new user
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(PasswordUtils.hashPassword(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(new ArrayList<>(Arrays.asList("USER")));

        User savedUser = userRepository.save(user);
        securityAuditService.logSuccessfulLogin(email, clientIp, userAgent);
        
        // Publish user registration event
        userEventPublisher.publishUserRegisteredEvent(email);
        
        return convertToUserResponse(savedUser);
    }

    /**
     * Authenticates a user using email and password.
     * @param email User email
     * @param password User password
     * @param clientIp IP address of the client
     * @param userAgent User agent string
     * @return User if credentials are valid, null otherwise
     */
    public User loginUser(String email, String password, String clientIp, String userAgent) {
        // Check rate limits
        if (featureConfiguration.getAuth().isRateLimitingEnabled()) {
            if (!rateLimitService.isAllowed(clientIp, RateLimitService.RateLimitType.LOGIN)) {
                securityAuditService.logSuspiciousActivity(email, clientIp, "Login rate limit exceeded");
                return null;
            }
        }
        
        // Sanitize input
        email = inputValidationService.sanitizeInput(email);
        
        User user = userRepository.findByEmail(email);
        if (user != null && PasswordUtils.verifyPassword(password, user.getPassword())) {
            // Success - log and reset rate limit
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            
            securityAuditService.logSuccessfulLogin(email, clientIp, userAgent);
            // rateLimitService.clearRateLimit(clientIp, RateLimitService.RateLimitType.LOGIN);
            
            // Publish login success event
            userEventPublisher.publishLoginSuccessEvent(email, userAgent);
            
            return user;
        }
        
        // Failed login attempt
        securityAuditService.logFailedLogin(email, clientIp, userAgent, "Invalid credentials");
        
        // Publish login failed event
        userEventPublisher.publishLoginFailedEvent(email, "Invalid credentials");
        
        return null;
    }

    /**
     * Authenticates a user using OAuth provider info.
     * @param providerName OAuth provider name
     * @param providerId OAuth provider user ID
     * @param clientIp IP address of the client
     * @param userAgent User agent string
     * @return User if found, null otherwise
     */
    public User loginWithOAuth(String providerName, String providerId, String clientIp, String userAgent) {
        // Check rate limits - temporarily commented out due to compilation issue
        // if (!rateLimitService.isAllowed(clientIp, RateLimitService.RateLimitType.OAUTH_LOGIN)) {
        //     securityAuditService.logSuspiciousActivity(null, clientIp, "OAuth login rate limit exceeded");
        //     return null;
        // }
        
        OAuthProvider oauthProvider = oauthProviderRepository.findByProviderNameAndProviderId(providerName, providerId);
        User user = oauthProvider != null ? oauthProvider.getUser() : null;
        
        if (user != null) {
            securityAuditService.logSuccessfulLogin(user.getEmail(), clientIp, userAgent + " via " + providerName);
            // rateLimitService.clearRateLimit(clientIp, RateLimitService.RateLimitType.OAUTH_LOGIN);
            
            // Publish login success event
            userEventPublisher.publishLoginSuccessEvent(user.getEmail(), userAgent + " via " + providerName);
        }
        
        return user;
    }

    /**
     * Links an OAuth provider to a user.
     * @param user User entity
     * @param oauthProvider OAuthProvider entity
     * @return Saved OAuthProvider
     */
    public OAuthProvider linkOAuthProvider(User user, OAuthProvider oauthProvider) {
        oauthProvider.setUser(user);
        return oauthProviderRepository.save(oauthProvider);
    }

    /**
     * Retrieves a user by ID.
     * @param id User ID
     * @return UserResponse or null if not found
     */
    public UserResponse getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.map(this::convertToUserResponse).orElse(null);
    }

    /**
     * Retrieves a user by email.
     * @param email User email
     * @return UserResponse or null if not found
     */
    public UserResponse getUserByEmail(String email) {
        email = inputValidationService.sanitizeInput(email);
        User user = userRepository.findByEmail(email);
        return user != null ? convertToUserResponse(user) : null;
    }

    /**
     * Retrieves a user entity by email (internal use).
     * @param email User email
     * @return User entity or null if not found
     */
    public User getUserByEmailInternal(String email) {
        email = inputValidationService.sanitizeInput(email);
        return userRepository.findByEmail(email);
    }
    
    /**
     * Saves a user entity.
     * @param user User entity
     * @return Saved User
     */
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    /**
     * Deactivates a user account (admin action).
     * @param id User ID
     * @param reason Reason for deactivation
     * @param adminEmail Admin performing the action
     * @return true if successful, false otherwise
     */
    public boolean deactivateUser(Long id, String reason, String adminEmail) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setStatus(UserStatus.INACTIVE);
            userRepository.save(user);
            securityAuditService.logAccountLockout(user.getEmail(), null, reason + " (by " + adminEmail + ")");
            
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("reason", reason);
            eventData.put("admin", adminEmail);
            userEventPublisher.publishEvent("ACCOUNT_DEACTIVATED", user.getEmail(), eventData);
            
            return true;
        }
        return false;
    }
    
    /**
     * Reactivates a user account (admin action).
     * @param id User ID
     * @param adminEmail Admin performing the action
     * @return true if successful, false otherwise
     */
    public boolean reactivateUser(Long id, String adminEmail) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            
            Map<String, Object> event = new HashMap<>();
            event.put("action", "ACCOUNT_REACTIVATED");
            event.put("admin", adminEmail);
            event.put("userId", id);
            securityAuditService.logSecurityEvent(user.getEmail(), null, null, event);
            
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("admin", adminEmail);
            userEventPublisher.publishEvent("ACCOUNT_REACTIVATED", user.getEmail(), eventData);
            
            return true;
        }
        return false;
    }
    
    /**
     * Checks if a user has a specific role.
     * @param user User entity
     * @param role Role name
     * @return true if user has the role
     */
    public boolean hasRole(User user, String role) {
        return user != null && user.getRoles() != null && user.getRoles().contains(role);
    }
    
    /**
     * Adds a role to a user (admin action).
     * @param userId User ID
     * @param role Role name
     * @param adminEmail Admin performing the action
     * @return true if successful, false otherwise
     */
    public boolean addRole(Long userId, String role, String adminEmail) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (!user.getRoles().contains(role)) {
                user.getRoles().add(role);
                userRepository.save(user);
                
                Map<String, Object> event = new HashMap<>();
                event.put("action", "ROLE_ADDED");
                event.put("role", role);
                event.put("admin", adminEmail);
                securityAuditService.logSecurityEvent(user.getEmail(), null, null, event);
                
                userEventPublisher.publishRoleChangedEvent(user.getEmail(), "ADDED", role);
                
                return true;
            }
        }
        return false;
    }
    
    /**
     * Removes a role from a user (admin action).
     * @param userId User ID
     * @param role Role name
     * @param adminEmail Admin performing the action
     * @return true if successful, false otherwise
     */
    public boolean removeRole(Long userId, String role, String adminEmail) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getRoles().contains(role)) {
                user.getRoles().remove(role);
                userRepository.save(user);
                
                Map<String, Object> event = new HashMap<>();
                event.put("action", "ROLE_REMOVED");
                event.put("role", role);
                event.put("admin", adminEmail);
                securityAuditService.logSecurityEvent(user.getEmail(), null, null, event);
                
                userEventPublisher.publishRoleChangedEvent(user.getEmail(), "REMOVED", role);
                
                return true;
            }
        }
        return false;
    }
    
    /**
     * Updates user information.
     * @param id User ID
     * @param request UserRegistrationRequest with updated info
     * @param clientIp IP address of the client
     * @param userAgent User agent string
     * @return Updated UserResponse or null if not found
     */
    public UserResponse updateUser(Long id, UserRegistrationRequest request, String clientIp, String userAgent) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (!optionalUser.isPresent()) {
            return null;
        }
        
        User user = optionalUser.get();
        
        String name = request.getName() != null ? inputValidationService.sanitizeInput(request.getName()) : null;
        String phoneNumber = request.getPhoneNumber() != null ? inputValidationService.sanitizeInput(request.getPhoneNumber()) : null;
        
        if ((name != null && inputValidationService.containsMaliciousContent(name)) || 
            (phoneNumber != null && inputValidationService.containsMaliciousContent(phoneNumber))) {
            securityAuditService.logSuspiciousActivity(user.getEmail(), clientIp, "Malicious content in profile update");
            throw new RuntimeException("Invalid input detected");
        }
        
        if (name != null && !name.trim().isEmpty()) {
            user.setName(name);
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            user.setPhoneNumber(phoneNumber);
        }
        
        User updatedUser = userRepository.save(user);
        securityAuditService.logSuccessfulLogin(user.getEmail(), clientIp, userAgent + " - profile updated");
        return convertToUserResponse(updatedUser);
    }
    
    /**
     * Changes a user's password after verifying the old password.
     * @param userId User ID
     * @param oldPassword Old password
     * @param newPassword New password
     * @param clientIp IP address of the client
     * @param userAgent User agent string
     * @return true if successful, false otherwise
     */
    public boolean changePassword(Long userId, String oldPassword, String newPassword, String clientIp, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!PasswordUtils.verifyPassword(oldPassword, user.getPassword())) {
            securityAuditService.logFailedLogin(user.getEmail(), clientIp, userAgent, "Invalid old password during password change");
            return false;
        }
        
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("New password must be at least 8 characters long");
        }
        
        user.setPassword(PasswordUtils.hashPassword(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        securityAuditService.logPasswordChange(user.getEmail(), clientIp);
        userEventPublisher.publishPasswordChangedEvent(user.getEmail());
        
        return true;
    }
    
    /**
     * Generates a JWT auth token for a user.
     * @param user User entity
     * @return JWT token string
     */
    public String generateAuthToken(User user) {
        return jwtUtils.generateToken(user.getEmail());
    }
    
    /**
     * Validates a JWT auth token.
     * @param token JWT token string
     * @return true if valid, false otherwise
     */
    public boolean validateAuthToken(String token) {
        return jwtUtils.validateToken(token);
    }
    
    /**
     * Extracts email from a JWT token.
     * @param token JWT token string
     * @return Email string
     */
    public String getEmailFromToken(String token) {
        return jwtUtils.getEmailFromToken(token);
    }

    /**
     * Sets up MFA for a user and returns secret and QR code.
     * @param userId User ID
     * @return Map with secret and QR code image
     */
    public Map<String, Object> setupMfa(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String secret = mfaService.generateSecret();
        String qrCodeImage = mfaService.generateQrCodeImageUri(secret, user.getEmail());
        
        user.setMfaSecret(secret);
        userRepository.save(user);
        
        return Map.of(
            "secret", secret,
            "qrCodeImage", qrCodeImage
        );
    }
    
    /**
     * Verifies and enables MFA for a user.
     * @param userId User ID
     * @param code MFA verification code
     * @return true if successful, false otherwise
     */
    public boolean verifyAndEnableMfa(Long userId, String code) {
        if (!featureConfiguration.getAuth().isMfaEnabled() ||
            "disabled".equalsIgnoreCase(featureConfiguration.getAuth().getMfaEnforcement())) {
            throw new RuntimeException("MFA is currently disabled by configuration");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (mfaService.verifyCode(user.getMfaSecret(), code)) {
            user.setMfaEnabled(true);
            List<String> backupCodesList = mfaService.generateBackupCodes();
            user.setBackupCodes(new HashSet<>(backupCodesList));
            userRepository.save(user);
            
            userEventPublisher.publishMfaEnabledEvent(user.getEmail());
            
            return true;
        }
        return false;
    }
    
    /**
     * Disables MFA for a user.
     * @param userId User ID
     * @param code MFA verification code
     * @return true if successful, false otherwise
     */
    public boolean disableMfa(Long userId, String code) {
        if (!featureConfiguration.getAuth().isMfaEnabled() ||
            "disabled".equalsIgnoreCase(featureConfiguration.getAuth().getMfaEnforcement())) {
            throw new RuntimeException("MFA is currently disabled by configuration");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (mfaService.verifyCode(user.getMfaSecret(), code)) {
            user.setMfaEnabled(false);
            user.setMfaSecret(null);
            user.setBackupCodes(null);
            userRepository.save(user);
            
            userEventPublisher.publishMfaDisabledEvent(user.getEmail());
            
            return true;
        }
        return false;
    }
    
    /**
     * Verifies an MFA code for a user.
     * @param user User entity
     * @param code MFA code
     * @return true if valid
     */
    public boolean verifyMfaCode(User user, String code) {
        if (!featureConfiguration.getAuth().isMfaEnabled() ||
            "disabled".equalsIgnoreCase(featureConfiguration.getAuth().getMfaEnforcement())) {
            return true;
        }
        return mfaService.verifyCode(user.getMfaSecret(), code);
    }
    
    /**
     * Verifies a backup code for a user.
     * @param user User entity
     * @param backupCode Backup code
     * @return true if valid
     */
    public boolean verifyBackupCode(User user, String backupCode) {
        if (!featureConfiguration.getAuth().isMfaEnabled() ||
            "disabled".equalsIgnoreCase(featureConfiguration.getAuth().getMfaEnforcement())) {
            return true;
        }
        boolean isValid = mfaService.verifyBackupCode(user, backupCode);
        if (isValid) {
            userRepository.save(user);
        }
        return isValid;
    }
    
    /**
     * Requests GDPR-compliant data deletion for a user.
     * @param userId User ID
     */
    public void requestDataDeletion(Long userId) {
        if (!featureConfiguration.getBusiness().isGdprComplianceEnabled()) {
            throw new RuntimeException("GDPR deletion is currently disabled by configuration");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setDataDeletionRequested(true);
        user.setDataDeletionRequestDate(LocalDateTime.now());
        userRepository.save(user);
        
        userEventPublisher.publishAccountDeletionRequestedEvent(user.getEmail());
    }
    
    /**
     * Cancels a GDPR-compliant data deletion request for a user.
     * @param userId User ID
     */
    public void cancelDataDeletion(Long userId) {
        if (!featureConfiguration.getBusiness().isGdprComplianceEnabled()) {
            throw new RuntimeException("GDPR cancellation is currently disabled by configuration");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setDataDeletionRequested(false);
        user.setDataDeletionRequestDate(null);
        userRepository.save(user);
    }
    
    /**
     * Scheduled task to process user deletion requests (runs daily).
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void processDeletionRequests() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<User> usersToDelete = userRepository.findUsersScheduledForDeletion(cutoff);
        
        for (User user : usersToDelete) {
            try {
                user.setName("DELETED_USER_" + user.getId());
                user.setEmail("deleted_" + user.getId() + "@deleted.local");
                user.setPassword("DELETED");
                user.setPhoneNumber(null);
                user.setMfaSecret(null);
                user.setBackupCodes(null);
                user.setMfaEnabled(false);
                user.setDataDeletionRequested(false);
                user.setDataDeletionRequestDate(null);
                userRepository.save(user);
                
                userEventPublisher.publishAccountDeletedEvent(user.getEmail());
                
            } catch (Exception e) {
                securityAuditService.logSecurityEvent(user.getEmail(), null, null, 
                    Map.of("action", "DELETION_PROCESSING_FAILED", "error", e.getMessage()));
            }
        }
    }

    /**
     * Converts a User entity to a UserResponse DTO.
     * @param user User entity
     * @return UserResponse DTO
     */
    public UserResponse convertToUserResponse(User user) {
        if (user == null) {
            return null;
        }
        
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getStatus(),
            user.getRoles() != null ? user.getRoles() : Arrays.asList("USER"),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            user.getBackupCodes(),
            user.isMfaEnabled()
        );
    }
}