package com.ecommerce.user.services;

import com.ecommerce.user.config.FeatureConfiguration;
import com.ecommerce.user.dto.MfaVerifyRequest;
import com.ecommerce.user.dto.MfaEnrollResponse;
import com.ecommerce.user.models.MfaConfiguration;
import com.ecommerce.user.models.MfaMethod;
import com.ecommerce.user.models.User;
import com.ecommerce.user.repositories.MfaConfigurationRepository;
import com.ecommerce.user.repositories.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MfaService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MfaConfigurationRepository mfaConfigurationRepository;

    @Autowired
    private SmsService smsService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SecurityAuditService securityAuditService;

    @Autowired
    private FeatureConfiguration featureConfiguration;

    @Value("${mfa.recovery-codes.count:10}")
    private int recoveryCodesCount;

    @Value("${mfa.secret.size:20}")
    private int secretSize;

    private static final String ISSUER = "ECommerce Auth";

    /**
     * Begin the MFA enrollment process
     */
    public MfaEnrollResponse beginEnrollment(Long userId, MfaMethod method) {
        if (!featureConfiguration.getAuth().isMfaEnabled() ||
            "disabled".equalsIgnoreCase(featureConfiguration.getAuth().getMfaEnforcement())) {
            throw new RuntimeException("MFA is currently disabled by configuration");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if MFA is already configured
        Optional<MfaConfiguration> existingConfig = mfaConfigurationRepository.findByUserId(userId);
        if (existingConfig.isPresent() && existingConfig.get().isEnabled()) {
            throw new RuntimeException("MFA is already enabled for this user");
        }

        // Generate secret key for TOTP
        String secretKey = generateSecretKey();
        
        // Generate QR code URL for TOTP
        String qrCodeUrl = null;
        if (method == MfaMethod.TOTP) {
            String totpUrl = generateTotpUrl(user.getEmail(), secretKey);
            qrCodeUrl = generateQrCodeImage(totpUrl);
        }

        // Generate recovery codes
        List<String> recoveryCodes = generateRecoveryCodes();

        // Save or update MFA configuration
        MfaConfiguration mfaConfig = existingConfig.orElse(new MfaConfiguration());
        mfaConfig.setUser(user);
        mfaConfig.setMethod(method);
        mfaConfig.setSecretKey(secretKey);
        mfaConfig.setEnabled(false); // Not enabled until verified
        mfaConfig.setPhoneNumber(user.getPhoneNumber());
        mfaConfig.setRecoveryCodes(String.join(",", recoveryCodes));
        
        mfaConfigurationRepository.save(mfaConfig);

        // Send verification code if using SMS or Email
        String verificationCode = null;
        if (method == MfaMethod.SMS) {
            verificationCode = generateVerificationCode();
            smsService.sendSms(user.getPhoneNumber(), "Your verification code is: " + verificationCode);
        } else if (method == MfaMethod.EMAIL) {
            verificationCode = generateVerificationCode();
            emailService.sendEmail(user.getEmail(), "MFA Verification Code", 
                    "Your verification code is: " + verificationCode);
        }

        // Return enrollment information
        MfaEnrollResponse response = new MfaEnrollResponse();
        response.setMethod(method);
        response.setQrCodeUrl(qrCodeUrl);
        response.setSecretKey(secretKey);
        response.setRecoveryCodes(recoveryCodes);
        response.setVerificationSent(method == MfaMethod.SMS || method == MfaMethod.EMAIL);

        return response;
    }

    /**
     * Verify and complete MFA enrollment
     */
    public boolean completeEnrollment(MfaVerifyRequest request, String clientIp, String userAgent) {
        if (!featureConfiguration.getAuth().isMfaEnabled() ||
            "disabled".equalsIgnoreCase(featureConfiguration.getAuth().getMfaEnforcement())) {
            throw new RuntimeException("MFA is currently disabled by configuration");
        }
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        MfaConfiguration mfaConfig = mfaConfigurationRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException("MFA enrollment not initiated"));

        boolean verified = false;
        
        // Verify based on method
        if (mfaConfig.getMethod() == MfaMethod.TOTP) {
            verified = verifyTotp(request.getCode(), mfaConfig.getSecretKey());
        } else if (mfaConfig.getMethod() == MfaMethod.SMS || mfaConfig.getMethod() == MfaMethod.EMAIL) {
            // For SMS/Email, the code should be validated against what was sent
            // This would typically involve checking a cached code, but for simplicity:
            verified = validateOtpCode(user.getEmail(), request.getCode());
        }

        if (verified) {
            // Enable MFA
            mfaConfig.setEnabled(true);
            mfaConfig.setLastVerifiedAt(LocalDateTime.now());
            mfaConfigurationRepository.save(mfaConfig);
            
            // Log the successful MFA enrollment
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("action", "MFA_ENROLLED");
            auditDetails.put("method", mfaConfig.getMethod().toString());
            securityAuditService.logSecurityEvent(user.getEmail(), clientIp, userAgent, auditDetails);
            
            return true;
        } else {
            // Log the failed verification attempt
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("action", "MFA_VERIFICATION_FAILED");
            auditDetails.put("method", mfaConfig.getMethod().toString());
            securityAuditService.logSecurityEvent(user.getEmail(), clientIp, userAgent, auditDetails);
            
            return false;
        }
    }

    /**
     * Verify MFA during login
     */
    public boolean verifyLogin(Long userId, String code, String clientIp, String userAgent) {
        if (!featureConfiguration.getAuth().isMfaEnabled() ||
            "disabled".equalsIgnoreCase(featureConfiguration.getAuth().getMfaEnforcement())) {
            return true;
        }
        MfaConfiguration mfaConfig = mfaConfigurationRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("MFA not configured for this user"));
        
        if (!mfaConfig.isEnabled()) {
            return true; // MFA not enabled, so verification passes
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        boolean verified = false;
        
        // Check if it's a recovery code
        if (isRecoveryCode(code, mfaConfig.getRecoveryCodes())) {
            consumeRecoveryCode(mfaConfig, code);
            verified = true;
        } else if (mfaConfig.getMethod() == MfaMethod.TOTP) {
            verified = verifyTotp(code, mfaConfig.getSecretKey());
        } else if (mfaConfig.getMethod() == MfaMethod.SMS || mfaConfig.getMethod() == MfaMethod.EMAIL) {
            verified = validateOtpCode(user.getEmail(), code);
        }
        
        if (verified) {
            // Update last verified timestamp
            mfaConfig.setLastVerifiedAt(LocalDateTime.now());
            mfaConfigurationRepository.save(mfaConfig);
            
            // Log successful verification
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("action", "MFA_VERIFICATION_SUCCESS");
            auditDetails.put("method", mfaConfig.getMethod().toString());
            securityAuditService.logSecurityEvent(user.getEmail(), clientIp, userAgent, auditDetails);
        } else {
            // Log failed verification
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("action", "MFA_VERIFICATION_FAILED");
            auditDetails.put("method", mfaConfig.getMethod().toString());
            securityAuditService.logSecurityEvent(user.getEmail(), clientIp, userAgent, auditDetails);
        }
        
        return verified;
    }

    /**
     * Send verification code for SMS or Email MFA
     */
    public boolean sendVerificationCode(Long userId, String clientIp, String userAgent) {
        MfaConfiguration mfaConfig = mfaConfigurationRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("MFA not configured for this user"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String verificationCode = generateVerificationCode();
        
        // Store verification code (in a real system, this would be in Redis with TTL)
        cacheOtpCode(user.getEmail(), verificationCode);
        
        if (mfaConfig.getMethod() == MfaMethod.SMS) {
            return smsService.sendSms(mfaConfig.getPhoneNumber(), "Your verification code is: " + verificationCode);
        } else if (mfaConfig.getMethod() == MfaMethod.EMAIL) {
            return emailService.sendEmail(user.getEmail(), "MFA Verification Code", 
                    "Your verification code is: " + verificationCode);
        }
        
        return false;
    }

    /**
     * Disable MFA for a user
     */
    public boolean disableMfa(Long userId, String password, String clientIp, String userAgent) {
        if (!featureConfiguration.getAuth().isMfaEnabled() ||
            "disabled".equalsIgnoreCase(featureConfiguration.getAuth().getMfaEnforcement())) {
            throw new RuntimeException("MFA is currently disabled by configuration");
        }
        MfaConfiguration mfaConfig = mfaConfigurationRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("MFA not configured for this user"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // In a real implementation, verify the password here
        
        // Disable MFA
        mfaConfig.setEnabled(false);
        mfaConfigurationRepository.save(mfaConfig);
        
        // Log MFA disable action
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("action", "MFA_DISABLED");
        auditDetails.put("method", mfaConfig.getMethod().toString());
        securityAuditService.logSecurityEvent(user.getEmail(), clientIp, userAgent, auditDetails);
        
        return true;
    }

    /**
     * Generate new recovery codes
     */
    public List<String> regenerateRecoveryCodes(Long userId, String clientIp, String userAgent) {
        if (!featureConfiguration.getAuth().isMfaEnabled() ||
            "disabled".equalsIgnoreCase(featureConfiguration.getAuth().getMfaEnforcement())) {
            throw new RuntimeException("MFA is currently disabled by configuration");
        }
        MfaConfiguration mfaConfig = mfaConfigurationRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("MFA not configured for this user"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<String> recoveryCodes = generateRecoveryCodes();
        mfaConfig.setRecoveryCodes(String.join(",", recoveryCodes));
        mfaConfigurationRepository.save(mfaConfig);
        
        // Log recovery codes regeneration
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("action", "MFA_RECOVERY_CODES_REGENERATED");
        securityAuditService.logSecurityEvent(user.getEmail(), clientIp, userAgent, auditDetails);
        
        return recoveryCodes;
    }

    // Helper methods
    
    private String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[secretSize];
        random.nextBytes(bytes);
        
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes);
    }

    private String generateTotpUrl(String email, String secretKey) {
        try {
            return String.format(
                    "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                    URLEncoder.encode(ISSUER, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(email, StandardCharsets.UTF_8.toString()),
                    secretKey,
                    URLEncoder.encode(ISSUER, StandardCharsets.UTF_8.toString()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate TOTP URL", e);
        }
    }

    private String generateQrCodeImage(String data) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    private List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        
        for (int i = 0; i < recoveryCodesCount; i++) {
            byte[] bytes = new byte[5];  // 10-character recovery code
            random.nextBytes(bytes);
            String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            codes.add(code.substring(0, 10).toUpperCase());  // Take first 10 chars
        }
        
        return codes;
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 6-digit code
        return String.valueOf(code);
    }

    private boolean verifyTotp(String code, String secretKey) {
        try {
            // Get current timestamp in seconds
            long timestamp = Instant.now().getEpochSecond() / 30;
            
            // Check current and one time unit before and after
            for (int i = -1; i <= 1; i++) {
                if (generateTotpCode(secretKey, timestamp + i).equals(code)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify TOTP code", e);
        }
    }

    private String generateTotpCode(String secretKey, long timestamp) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Base32 base32 = new Base32();
        byte[] decodedKey = base32.decode(secretKey);
        
        // Get timestamp bytes
        byte[] data = new byte[8];
        long value = timestamp;
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (value & 0xff);
            value >>= 8;
        }
        
        // Generate HMAC-SHA1 hash
        SecretKeySpec signKey = new SecretKeySpec(decodedKey, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);
        
        // Take the last byte as offset
        int offset = hash[hash.length - 1] & 0xf;
        
        // Get 4 bytes starting at offset
        int binary = ((hash[offset] & 0x7f) << 24) |
                     ((hash[offset + 1] & 0xff) << 16) |
                     ((hash[offset + 2] & 0xff) << 8) |
                     (hash[offset + 3] & 0xff);
        
        // Get 6-digit code
        int code = binary % 1000000;
        return String.format("%06d", code);
    }

    private boolean isRecoveryCode(String inputCode, String storedCodes) {
        if (storedCodes == null || storedCodes.isEmpty()) {
            return false;
        }
        
        String[] codes = storedCodes.split(",");
        return Arrays.asList(codes).contains(inputCode);
    }

    private void consumeRecoveryCode(MfaConfiguration mfaConfig, String usedCode) {
        List<String> codes = new ArrayList<>(Arrays.asList(mfaConfig.getRecoveryCodes().split(",")));
        codes.remove(usedCode);
        mfaConfig.setRecoveryCodes(String.join(",", codes));
        mfaConfigurationRepository.save(mfaConfig);
    }
    
    // In a real implementation, these methods would use Redis with TTL
    private void cacheOtpCode(String key, String code) {
        // Simplified - would use Redis in production
    }
    
    private boolean validateOtpCode(String key, String code) {
        // Simplified - would validate against Redis in production
        return true;  // For demo purposes only
    }
    
    /**
     * Generate secret - alias for generateSecretKey for compatibility
     */
    public String generateSecret() {
        return generateSecretKey();
    }
    
    /**
     * Generate QR code image URI - alias for generateQrCodeImage for compatibility
     */
    public String generateQrCodeImageUri(String secretKey, String email) {
        String totpUrl = generateTotpUrl(email, secretKey);
        return "data:image/png;base64," + generateQrCodeImage(totpUrl);
    }
    
    /**
     * Verify code - alias for verifyTotp for compatibility
     */
    public boolean verifyCode(String secretKey, String code) {
        return verifyTotp(code, secretKey);
    }
    
    /**
     * Generate backup codes - alias for generateRecoveryCodes for compatibility
     */
    public List<String> generateBackupCodes() {
        return generateRecoveryCodes();
    }
    
    /**
     * Verify backup code against user's stored codes
     */
    public boolean verifyBackupCode(User user, String backupCode) {
        Optional<MfaConfiguration> mfaConfigOpt = mfaConfigurationRepository.findByUserId(user.getId());
        if (mfaConfigOpt.isEmpty()) {
            return false;
        }
        
        MfaConfiguration mfaConfig = mfaConfigOpt.get();
        boolean isValid = isRecoveryCode(backupCode, mfaConfig.getRecoveryCodes());
        
        if (isValid) {
            // Consume the backup code
            consumeRecoveryCode(mfaConfig, backupCode);
        }
        
        return isValid;
    }
}
