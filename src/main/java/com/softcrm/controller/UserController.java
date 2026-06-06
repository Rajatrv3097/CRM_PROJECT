package com.softcrm.controller;

// Complete UserController.java with all profile editing features

import com.softcrm.dto.request.UpdateProfileRequest;
import com.softcrm.dto.response.ApiResponse;
import com.softcrm.dto.response.UserProfileDTO;
import com.softcrm.entity.user.User;
import com.softcrm.service.AuthService;
import com.softcrm.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    // Helper method to get current user ID
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();

        if (principal instanceof User) {
            return ((User) principal).getId();
        }

        String email = null;
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            email = (String) principal;
        }

        if (email != null) {
            try {
                return userService.getUserByEmail(email).getId();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private User getCurrentUser() {
        Long userId = getCurrentUserId();
        if (userId != null) {
            return userService.getUserById(userId);
        }
        return null;
    }

    // ============ PROFILE MANAGEMENT (All Roles) ============

    // Get current user profile (any authenticated user)
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getMyProfile() {
        try {
            User user = getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("User not found"));
            }
            UserProfileDTO profile = userService.getUserProfile(user.getId());
            return ResponseEntity.ok(ApiResponse.success(profile, "Profile fetched successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // Update current user profile (any authenticated user)
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateMyProfile(@RequestBody UpdateProfileRequest request) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("User not found"));
            }
            UserProfileDTO updated = userService.updateUserProfile(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.success(updated, "Profile updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // Upload profile picture (any authenticated user)
    @PostMapping("/profile/picture")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> uploadProfilePicture(@RequestParam MultipartFile file) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("User not found"));
            }
            String pictureUrl = userService.uploadProfilePicture(user.getId(), file);
            return ResponseEntity.ok(ApiResponse.success(pictureUrl, "Profile picture updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // Change own password (any authenticated user)
    @PostMapping("/profile/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> changeMyPassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("User not found"));
            }

            if (!newPassword.equals(confirmPassword)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("New password and confirm password do not match"));
            }

            userService.changeUserPassword(user.getId(), currentPassword, newPassword);
            return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // Get user by ID (with permission check)
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getUser(@PathVariable Long id) {
        try {
            Long currentUserId = getCurrentUserId();
            User currentUser = getCurrentUser();

            // Allow if: viewing own profile, OR is ADMIN, OR is SUPER_ADMIN
            boolean isOwnProfile = (currentUserId != null && currentUserId.equals(id));
            boolean isAdmin = currentUser != null && ("ADMIN".equals(currentUser.getUserType()) || "SUPER_ADMIN".equals(currentUser.getUserType()));

            if (!isOwnProfile && !isAdmin) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied. You can only view your own profile."));
            }

            UserProfileDTO user = userService.getUserProfile(id);
            return ResponseEntity.ok(ApiResponse.success(user, "User fetched successfully"));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Update user by ID (ADMIN or SUPER_ADMIN only)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")

    public ResponseEntity<ApiResponse<UserProfileDTO>> updateUser(@PathVariable Long id, @RequestBody UpdateProfileRequest request) {
        try {
            UserProfileDTO updated = userService.updateUserProfile(id, request);
            return ResponseEntity.ok(ApiResponse.success(updated, "User updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // Delete user (ADMIN or SUPER_ADMIN only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUserPermanently(id);
            return ResponseEntity.ok(ApiResponse.success(null, "User deleted permanently successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}