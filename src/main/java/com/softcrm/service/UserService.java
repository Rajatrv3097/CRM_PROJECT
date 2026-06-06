package com.softcrm.service;

import com.softcrm.dto.request.ChangePasswordRequest;
import com.softcrm.dto.request.UpdateProfileRequest;
import com.softcrm.dto.response.UserProfileDTO;
import com.softcrm.entity.Company;
import com.softcrm.entity.user.User;
import com.softcrm.enums.UserStatus;
import com.softcrm.repository.CompanyRepository;
import com.softcrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    // ============ PROFILE MANAGEMENT ============

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(Long userId) {
        User user = getUserById(userId);
        return convertToProfileDTO(user);
    }

    @Transactional
    public UserProfileDTO updateUserProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        // Update common fields
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getDepartment() != null) user.setDepartment(request.getDepartment());
        if (request.getDesignation() != null) user.setDesignation(request.getDesignation());

        // Update customer-specific fields
        if ("CUSTOMER".equals(user.getUserType())) {
            if (request.getCompanyName() != null) user.setCompanyName(request.getCompanyName());
            if (request.getGstNumber() != null) user.setGstNumber(request.getGstNumber());
            if (request.getIndustry() != null) user.setIndustry(request.getIndustry());
            if (request.getAddress() != null) user.setAddress(request.getAddress());
            if (request.getCity() != null) user.setCity(request.getCity());
            if (request.getState() != null) user.setState(request.getState());
            if (request.getCountry() != null) user.setCountry(request.getCountry());
            if (request.getPincode() != null) user.setPincode(request.getPincode());
            if (request.getWebsite() != null) user.setWebsite(request.getWebsite());
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updated = userRepository.save(user);

        log.info("Profile updated for user: {}", user.getEmail());
        return convertToProfileDTO(updated);
    }

    @Transactional
    public String uploadProfilePicture(Long userId, MultipartFile file) {
        User user = getUserById(userId);

        // Generate unique filename
        String filename = "profile_" + userId + "_" + System.currentTimeMillis() + ".jpg";

        // Save file (implement your storage logic here)
        // String fileUrl = fileStorageService.saveFile(file, filename);
        String fileUrl = "/uploads/profiles/" + filename;

        user.setAvatarUrl(fileUrl);
        userRepository.save(user);

        log.info("Profile picture uploaded for user: {}", user.getEmail());
        return fileUrl;
    }

    @Transactional
    public void changeUserPassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getEmail());
    }

    // ============ PERMANENT DELETE ============

    @Transactional
    public void deleteUserPermanently(Long userId) {
        User user = getUserById(userId);

        // Check if user has any dependent records
        // You may want to handle cascading deletes here

        userRepository.delete(user);
        log.info("User permanently deleted: {}", user.getEmail());
    }

    // ============ CONVERSION METHODS ============

    private UserProfileDTO convertToProfileDTO(User user) {
        String companyName = null;
        if (user.getCompanyId() != null) {
            companyName = companyRepository.findById(user.getCompanyId())
                    .map(Company::getName)
                    .orElse(null);
        }

        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .userType(user.getUserType())
                .avatarUrl(user.getAvatarUrl())
                .department(user.getDepartment())
                .designation(user.getDesignation())
                .companyName(companyName)
                .companyId(user.getCompanyId())
                .isEmailVerified(user.getIsEmailVerified())
                .isTwoFactorEnabled(user.getIsTwoFactorEnabled())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                // Customer specific
                .gstNumber(user.getGstNumber())
                .industry(user.getIndustry())
                .address(user.getAddress())
                .city(user.getCity())
                .state(user.getState())
                .country(user.getCountry())
                .pincode(user.getPincode())
                .customerTier(user.getCustomerTier())
                .customerStatus(user.getCustomerStatus())
                .totalPurchases(user.getTotalPurchases())
                .totalOrders(user.getTotalOrders())
                .loyaltyPoints(user.getLoyaltyPoints())
                .build();
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);
        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setPhone(userDetails.getPhone());
        user.setAvatarUrl(userDetails.getAvatarUrl());
        user.setDepartment(userDetails.getDepartment());

        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = getUserById(id);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getEmail());
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = getUserById(id);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        log.info("User deactivated: {}", user.getEmail());
    }

    // ============ ROLE-BASED USER FETCHING METHODS ============

    /**
     * Get all users by company ID
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByCompany(Long companyId) {
        return userRepository.findByCompanyId(companyId);
    }

    /**
     * Get users by user type
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByType(String userType) {
        return userRepository.findByUserType(userType);
    }

    /**
     * Get users by company and user type
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByCompanyAndType(Long companyId, String userType) {
        return userRepository.findByCompanyIdAndUserType(companyId, userType);
    }

    /**
     * SUPER_ADMIN: Get all ADMINS
     */
    @Transactional(readOnly = true)
    public List<User> getAllAdmins() {
        return userRepository.findByUserType("ADMIN");
    }

    /**
     * SUPER_ADMIN: Get ADMINS by company
     */
    @Transactional(readOnly = true)
    public List<User> getAdminsByCompany(Long companyId) {
        return userRepository.findByCompanyIdAndUserType(companyId, "ADMIN");
    }

    /**
     * ADMIN: Get MANAGERS of their company
     */
    @Transactional(readOnly = true)
    public List<User> getManagersByCompany(Long companyId) {
        return userRepository.findByCompanyIdAndUserType(companyId, "MANAGER");
    }

    /**
     * ADMIN: Get EMPLOYEES of their company
     */
    @Transactional(readOnly = true)
    public List<User> getEmployeesByCompany(Long companyId) {
        return userRepository.findByCompanyIdAndUserType(companyId, "EMPLOYEE");
    }

    /**
     * ADMIN: Get all non-admin users of a company (MANAGERS + EMPLOYEES)
     */
    @Transactional(readOnly = true)
    public List<User> getCompanyUsers(Long companyId) {
        List<User> allUsers = userRepository.findByCompanyId(companyId);
        return allUsers.stream()
                .filter(user -> !"ADMIN".equals(user.getUserType()))
                .collect(Collectors.toList());
    }

    /**
     * MANAGER: Get their team members (EMPLOYEES under this manager)
     */
    @Transactional(readOnly = true)
    public List<User> getTeamMembers(Long managerId) {
        return userRepository.findByManagerId(managerId);
    }
}