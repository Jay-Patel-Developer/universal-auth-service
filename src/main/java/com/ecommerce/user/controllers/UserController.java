package com.ecommerce.user.controllers;

import com.ecommerce.user.dto.UserRegistrationRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * REST controller for user management endpoints.
 * Handles user CRUD operations and role management.
 */
@RestController
@RequestMapping("/api/auth/user")
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * GET /api/auth/user/{id}
     * Retrieves a user by their unique ID.
     * @param id User ID
     * @return UserResponse if found, 404 otherwise
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    /**
     * GET /api/auth/user/email/{email}
     * Retrieves a user by their email address.
     * @param email User email
     * @return UserResponse if found, 404 otherwise
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        UserResponse user = userService.getUserByEmail(email);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    /**
     * PUT /api/auth/user/{id}
     * Updates user information.
     * @param id User ID
     * @param request UserRegistrationRequest with updated info
     * @param httpRequest HTTP request for client info
     * @return Updated UserResponse or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserRegistrationRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        UserResponse updated = userService.updateUser(id, request, clientIp, userAgent);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    /**
     * POST /api/auth/user/deactivate/{id}
     * Deactivates a user account (admin action).
     * @param id User ID
     * @param body Map with optional reason and adminEmail
     * @return Success or error message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/deactivate/{id}")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "No reason provided");
        String adminEmail = body.getOrDefault("adminEmail", "system");
        boolean result = userService.deactivateUser(id, reason, adminEmail);
        if (result) {
            return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found or already deactivated"));
        }
    }

    /**
     * POST /api/auth/user/reactivate/{id}
     * Reactivates a user account (admin action).
     * @param id User ID
     * @param body Map with optional adminEmail
     * @return Success or error message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reactivate/{id}")
    public ResponseEntity<?> reactivateUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String adminEmail = body.getOrDefault("adminEmail", "system");
        boolean result = userService.reactivateUser(id, adminEmail);
        if (result) {
            return ResponseEntity.ok(Map.of("message", "User reactivated successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found or already active"));
        }
    }

    /**
     * POST /api/auth/user/add-role
     * Adds a role to a user (admin action).
     * @param body Map with userId, role, and optional adminEmail
     * @return Success or error message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add-role")
    public ResponseEntity<?> addRole(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String role = body.get("role").toString();
        String adminEmail = body.getOrDefault("adminEmail", "system").toString();
        boolean result = userService.addRole(userId, role, adminEmail);
        if (result) {
            return ResponseEntity.ok(Map.of("message", "Role added successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Role already exists or user not found"));
        }
    }

    /**
     * POST /api/auth/user/remove-role
     * Removes a role from a user (admin action).
     * @param body Map with userId, role, and optional adminEmail
     * @return Success or error message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/remove-role")
    public ResponseEntity<?> removeRole(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String role = body.get("role").toString();
        String adminEmail = body.getOrDefault("adminEmail", "system").toString();
        boolean result = userService.removeRole(userId, role, adminEmail);
        if (result) {
            return ResponseEntity.ok(Map.of("message", "Role removed successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Role not found or user not found"));
        }
    }
}
