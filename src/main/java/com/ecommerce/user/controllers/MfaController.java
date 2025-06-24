package com.ecommerce.user.controllers;

import com.ecommerce.user.dto.MfaEnrollRequest;
import com.ecommerce.user.dto.MfaEnrollResponse;
import com.ecommerce.user.dto.MfaVerifyRequest;
import com.ecommerce.user.services.MfaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/mfa")
public class MfaController {

    @Autowired
    private MfaService mfaService;

    /**
     * Begin MFA enrollment process
     */
    @PostMapping("/enroll")
    public ResponseEntity<?> beginEnrollment(@RequestBody MfaEnrollRequest request) {
        try {
            MfaEnrollResponse response = mfaService.beginEnrollment(request.getUserId(), request.getMethod());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Complete MFA enrollment with verification
     */
    @PostMapping("/verify-enrollment")
    public ResponseEntity<?> verifyEnrollment(
            @RequestBody MfaVerifyRequest request,
            HttpServletRequest httpRequest) {

        try {
            String clientIp = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");

            boolean success = mfaService.completeEnrollment(request, clientIp, userAgent);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "MFA enrollment completed successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid verification code"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verify MFA code during login
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(
            @RequestBody MfaVerifyRequest request,
            HttpServletRequest httpRequest) {

        try {
            String clientIp = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");

            boolean success = mfaService.verifyLogin(request.getUserId(), request.getCode(), clientIp, userAgent);
            if (success) {
                return ResponseEntity.ok(Map.of("valid", true));
            } else {
                return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Invalid code"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Request SMS or Email verification code
     */
    @PostMapping("/send-code")
    public ResponseEntity<?> sendVerificationCode(
            @RequestBody Map<String, Long> request,
            HttpServletRequest httpRequest) {

        try {
            String clientIp = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");

            boolean sent = mfaService.sendVerificationCode(request.get("userId"), clientIp, userAgent);
            if (sent) {
                return ResponseEntity.ok(Map.of("message", "Verification code sent"));
            } else {
                return ResponseEntity.internalServerError().body(Map.of("error", "Failed to send verification code"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Disable MFA
     */
    @PostMapping("/disable")
    public ResponseEntity<?> disableMfa(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {

        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String password = (String) request.get("password");
            String clientIp = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");

            boolean disabled = mfaService.disableMfa(userId, password, clientIp, userAgent);
            if (disabled) {
                return ResponseEntity.ok(Map.of("message", "MFA disabled successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to disable MFA"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generate new recovery codes
     */
    @PostMapping("/recovery-codes")
    public ResponseEntity<?> regenerateRecoveryCodes(
            @RequestBody Map<String, Long> request,
            HttpServletRequest httpRequest) {

        try {
            String clientIp = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");

            List<String> recoveryCodes = mfaService.regenerateRecoveryCodes(
                    request.get("userId"), clientIp, userAgent);

            return ResponseEntity.ok(Map.of("recoveryCodes", recoveryCodes));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
