package com.softcrm.service;

import com.softcrm.config.JwtUtil;
import com.softcrm.dto.request.LoginRequest;
import com.softcrm.dto.request.RegisterRequest;
import com.softcrm.dto.response.AuthResponse;
import com.softcrm.entity.LoginLog;
import com.softcrm.entity.Role;
import com.softcrm.entity.user.User;
import com.softcrm.enums.UserStatus;
import com.softcrm.repository.CompanyRepository;
import com.softcrm.repository.LoginLogRepository;
import com.softcrm.repository.RoleRepository;
import com.softcrm.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final LoginLogRepository loginLogRepository;
    private final EmailService emailService;
    private final CompanyRepository companyRepository;

    private final Map<String, OtpData> otpStore = new HashMap<>();

    private static class OtpData {
        String otp;
        long expiryTime;
        OtpData(String otp, long expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }

    // ============ EMAIL VERIFICATION ============

    @Transactional
    public void verifyEmail(String token) {
        log.info("Verifying email with token: {}", token);

        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired verification token"));

        user.setIsEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);

        log.info("Email verified successfully for: {}", user.getEmail());
    }

    // Resend verification email
    @Transactional
    public void resendVerificationEmail(String email, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new RuntimeException("Email already verified");
        }

        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        userRepository.save(user);

        String verificationUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() +
                ":" + httpRequest.getServerPort() + "/api/auth/verify-email?token=" + verificationToken;

        emailService.sendVerificationEmail(user.getEmail(), verificationUrl);
        log.info("Verification email resent to: {}", email);
    }

    // ============ FORGOT PASSWORD / RESET PASSWORD ============

    @Transactional
    public void sendPasswordResetOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = generateOtp();
        long expiry = System.currentTimeMillis() + (15 * 60 * 1000); // 15 minutes
        otpStore.put("RESET_" + email, new OtpData(otp, expiry));

        String subject = "SoftCRM - Password Reset OTP";
        String body = "Your OTP to reset your password is: " + otp +
                "\n\nThis OTP is valid for 15 minutes.\n\n" +
                "If you didn't request this, please ignore this email.";
        emailService.sendSimpleEmail(email, subject, body);

        log.info("Password reset OTP sent to: {}", email);
    }

    @Transactional
    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        // Verify OTP
        OtpData otpData = otpStore.get("RESET_" + email);
        if (otpData == null) {
            throw new RuntimeException("No OTP found. Please request a new one.");
        }
        if (System.currentTimeMillis() > otpData.expiryTime) {
            otpStore.remove("RESET_" + email);
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }
        if (!otpData.otp.equals(otp)) {
            throw new RuntimeException("Invalid OTP. Please try again.");
        }

        // Reset password
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Remove OTP from store
        otpStore.remove("RESET_" + email);

        log.info("Password reset successfully for: {}", email);
    }

    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed successfully for: {}", email);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use!");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken!");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number already in use!");
        }

        User user = new User();
        String roleName = request.getRole() != null ? request.getRole().toUpperCase() : "CUSTOMER";

        // Set core fields
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setIsEmailVerified(false);
        user.setIsTwoFactorEnabled(false);
        user.setUserType(roleName);

        // ✅ Generate email verification token
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);

        // Set role-specific fields based on user type
        switch (roleName) {
            case "ADMIN":
                user.setDepartment("General");
                user.setAccessLevel(5);
                user.setCanManageUsers(true);
                user.setCanManageRoles(true);
                user.setAdminCode("ADMIN" + System.currentTimeMillis());
                break;

            case "MANAGER":
                user.setTeamName("Default Team");
                user.setDepartment("General");
                user.setTeamSize(0);
                user.setMonthlyTarget(0.0);
                user.setAchievedTarget(0.0);
                user.setCanApproveLeaves(true);
                break;

            case "EMPLOYEE":
                user.setEmployeeId("EMP" + System.currentTimeMillis());
                user.setDesignation("Staff");
                user.setSalary(0.0);
                break;

            case "CUSTOMER":
            default:
                user.setCustomerSource("Website");
                user.setTotalPurchases(BigDecimal.ZERO);
                user.setTotalOrders(0);
                user.setLoyaltyPoints(0);
                user.setCustomerTier("BRONZE");
                user.setCustomerStatus("ACTIVE");
                break;
        }

        // Auto-assign company for non-customer users
        if (!"CUSTOMER".equals(roleName)) {
            companyRepository.findFirstByIsActiveTrue().ifPresent(company -> {
                user.setCompanyId(company.getId());
                log.info("Auto-assigned company {} to new {} user", company.getName(), roleName);
            });
        }

        // Assign role
        Role userRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        // ✅ Send verification email
        String verificationUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() +
                ":" + httpRequest.getServerPort() + "/api/auth/verify-email?token=" + verificationToken;
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationUrl);
        log.info("Verification email sent to: {}", savedUser.getEmail());

        logLogin(savedUser, httpRequest, "REGISTERED");

        // Generate token
        String accessToken = jwtUtil.generateToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .userType(savedUser.getUserType())
                .role(roleName)
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("Account is not active. Please contact administrator.");
        }

        // Check if 2FA is enabled
        if (Boolean.TRUE.equals(user.getIsTwoFactorEnabled())) {
            if (request.getOtp() == null || request.getOtp().isEmpty()) {
                generateAndSendOtp(user.getEmail());
                log.info("2FA OTP sent to: {}", user.getEmail());
                throw new RuntimeException("2FA_REQUIRED: OTP sent to your email. Please verify.");
            } else {
                if (!verifyOtp(user.getEmail(), request.getOtp())) {
                    throw new RuntimeException("Invalid or expired OTP. Please try again.");
                }
                log.info("2FA OTP verified successfully for: {}", user.getEmail());
            }
        }

        // Authenticate password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Update last login info
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(httpRequest.getRemoteAddr());
        user.setLastLoginDevice(httpRequest.getHeader("User-Agent"));
        userRepository.save(user);

        logLogin(user, httpRequest, "SUCCESS");

        // Generate tokens
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtUtil.generateToken(user.getEmail(), userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // Get primary role name
        String roleName = user.getRoles().isEmpty() ? user.getUserType() : user.getRoles().iterator().next().getName();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .userType(user.getUserType())
                .role(roleName)
                .companyId(user.getCompanyId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    @Transactional
    public void logout(String email, HttpServletRequest httpRequest) {
        log.info("Logout attempt for email: {}", email);

        userRepository.findByEmail(email).ifPresent(user -> {
            logLogin(user, httpRequest, "LOGOUT");
            log.info("User logged out successfully: {}", email);
        });
    }

    private String generateAndSendOtp(String email) {
        String otp = generateOtp();
        long expiry = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes
        otpStore.put(email, new OtpData(otp, expiry));

        String subject = "SoftCRM - Two Factor Authentication OTP";
        String body = "Your OTP for login is: " + otp + "\n\nThis OTP is valid for 5 minutes.\n\nIf you didn't request this, please ignore this email.";
        emailService.sendSimpleEmail(email, subject, body);

        log.info("OTP sent to: {}", email);
        return otp;
    }

    private String generateOtp() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private boolean verifyOtp(String email, String otp) {
        OtpData otpData = otpStore.get(email);
        if (otpData == null) {
            log.warn("No OTP found for email: {}", email);
            return false;
        }
        if (System.currentTimeMillis() > otpData.expiryTime) {
            otpStore.remove(email);
            log.warn("OTP expired for email: {}", email);
            return false;
        }
        if (otpData.otp.equals(otp)) {
            otpStore.remove(email);
            log.info("OTP verified successfully for: {}", email);
            return true;
        }
        log.warn("Invalid OTP for email: {}", email);
        return false;
    }

    @Transactional
    public void toggleTwoFactor(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsTwoFactorEnabled(enabled);
        userRepository.save(user);

        if (enabled) {
            log.info("2FA enabled for user: {}", user.getEmail());
        } else {
            log.info("2FA disabled for user: {}", user.getEmail());
        }
    }

    public boolean getTwoFactorStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return Boolean.TRUE.equals(user.getIsTwoFactorEnabled());
    }

    @Transactional
    public void sendTwoFactorSetupOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = generateAndSendOtp(email);
        log.info("2FA setup OTP sent to: {}", email);
    }

    @Transactional
    public boolean verifyTwoFactorSetupOtp(String email, String otp) {
        return verifyOtp(email, otp);
    }

    private void logLogin(User user, HttpServletRequest request, String status) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUser(user);
        loginLog.setEmail(user.getEmail());
        loginLog.setIpAddress(request.getRemoteAddr());
        loginLog.setUserAgent(request.getHeader("User-Agent"));
        loginLog.setStatus(status);

        if ("FAILURE".equals(status) || "REGISTERED".equals(status)) {
            // No failure reason for these cases
        }

        loginLogRepository.save(loginLog);
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset successfully for user: {}", email);
    }

    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean isPhoneExists(String phone) {
        return userRepository.existsByPhone(phone);
    }
}