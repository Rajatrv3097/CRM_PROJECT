package com.softcrm.service;

import com.softcrm.dto.request.CreateAdminWithCompanyRequest;
import com.softcrm.dto.request.CreateUserRequest;
import com.softcrm.dto.response.UserResponse;
import com.softcrm.entity.Company;
import com.softcrm.entity.Role;
import com.softcrm.entity.user.User;
import com.softcrm.enums.UserStatus;
import com.softcrm.repository.CompanyRepository;
import com.softcrm.repository.RoleRepository;
import com.softcrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CompanyRepository companyRepository;

    // ============ Helper Methods ============

    private String getCurrentUserEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return "System";
    }

    private Long getCurrentUserCompanyId() {
        String email = getCurrentUserEmail();
        if ("System".equals(email)) {
            log.debug("Current user is System, returning null companyId");
            return null;
        }

        log.info("🔍 Getting company ID for user email: {}", email);

        return userRepository.findByEmail(email)
                .map(user -> {
                    log.info("✅ User found: {} (ID: {}), Company ID: {}", user.getUsername(), user.getId(), user.getCompanyId());
                    return user.getCompanyId();
                })
                .orElseGet(() -> {
                    log.warn("❌ User not found for email: {}", email);
                    return null;
                });
    }

    private User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email).orElse(null);
    }

    private String getCurrentUserFullName() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email)
                    .map(User::getFullName)
                    .orElse("System");
        }
        return "System";
    }

    // Check if company has module enabled
    private boolean isModuleEnabledForCompany(Long companyId, String moduleName) {
        if (companyId == null) return true; // SUPER_ADMIN has all access
        return companyRepository.findById(companyId)
                .map(company -> company.isModuleEnabled(moduleName))
                .orElse(false);
    }

    // ============ Create ADMIN (Regular - for SUPER_ADMIN without company) ============

    @Transactional
    public UserResponse createAdmin(CreateUserRequest request) {
        log.info("Creating new ADMIN user: {}", request.getEmail());

        validateUserUniqueness(request);

        String rawPassword = request.getPassword();

        User admin = new User();
        admin.setUsername(request.getUsername());
        admin.setEmail(request.getEmail());
        admin.setPhone(request.getPhone());
        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());
        admin.setStatus(UserStatus.ACTIVE);
        admin.setIsEmailVerified(true);
        admin.setUserType("ADMIN");  // ✅ Set user type
        admin.setDepartment(request.getDepartment() != null ? request.getDepartment() : "Administration");

        // Admin specific fields
        admin.setAccessLevel(8);
        admin.setCanManageUsers(true);
        admin.setCanManageRoles(true);
        admin.setAdminCode("ADMIN" + System.currentTimeMillis());

        Long creatorCompanyId = getCurrentUserCompanyId();
        admin.setCompanyId(creatorCompanyId);
        admin.setManagedCompanyId(creatorCompanyId);

        log.info("Setting company_id: {} for new ADMIN", creatorCompanyId);

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
        admin.setRoles(Set.of(adminRole));

        User savedUser = userRepository.save(admin);
        log.info("Admin created successfully with ID: {}, Company ID: {}", savedUser.getId(), savedUser.getCompanyId());

        String createdBy = getCurrentUserFullName();
        String fullName = admin.getFullName();
        if (fullName.trim().isEmpty()) {
            fullName = request.getUsername();
        }

        emailService.sendWelcomeEmail(
                request.getEmail(),
                fullName,
                "ADMIN",
                request.getEmail(),
                rawPassword,
                createdBy
        );

        log.info("📧 Welcome email sent to new admin: {}", request.getEmail());

        return convertToResponse(savedUser);
    }

    // ============ Create ADMIN with Company Assignment (for SUPER_ADMIN) ============

    @Transactional
    public UserResponse createAdminWithCompany(CreateAdminWithCompanyRequest request) {
        log.info("Creating new ADMIN user with company assignment: {}", request.getEmail());

        validateUserUniqueness(request);

        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + request.getCompanyId()));

        if (!company.getIsActive()) {
            throw new RuntimeException("Company is not active. Cannot create admin.");
        }

        long currentUsers = userRepository.countActiveUsersByCompany(request.getCompanyId());
        if (currentUsers >= company.getMaxUsers()) {
            throw new RuntimeException("Company has reached maximum user limit: " + company.getMaxUsers());
        }

        String rawPassword = request.getPassword();

        User admin = new User();
        admin.setUsername(request.getUsername());
        admin.setEmail(request.getEmail());
        admin.setPhone(request.getPhone());
        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());
        admin.setStatus(UserStatus.ACTIVE);
        admin.setIsEmailVerified(true);
        admin.setUserType("ADMIN");  // ✅ Set user type
        admin.setDepartment(request.getDepartment() != null ? request.getDepartment() : "Administration");

        // Admin specific fields
        admin.setAccessLevel(8);
        admin.setCanManageUsers(true);
        admin.setCanManageRoles(true);
        admin.setAdminCode("ADMIN" + System.currentTimeMillis());
        admin.setCompanyId(request.getCompanyId());
        admin.setManagedCompanyId(request.getCompanyId());

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
        admin.setRoles(Set.of(adminRole));

        User savedUser = userRepository.save(admin);
        log.info("Admin created successfully with ID: {} for company: {}", savedUser.getId(), company.getName());

        String createdBy = getCurrentUserFullName();
        String fullName = admin.getFullName();
        if (fullName.trim().isEmpty()) {
            fullName = request.getUsername();
        }

        emailService.sendWelcomeEmail(
                request.getEmail(),
                fullName,
                "ADMIN",
                request.getEmail(),
                rawPassword,
                createdBy
        );

        log.info("📧 Welcome email sent to new admin: {}", request.getEmail());

        return convertToResponse(savedUser);
    }

    // ============ Create MANAGER ============

    @Transactional
    public UserResponse createManager(CreateUserRequest request) {
        log.info("Creating new MANAGER user: {}", request.getEmail());

        validateUserUniqueness(request);

        Long companyId = getCurrentUserCompanyId();

        log.info("📌 Creator company ID: {}", companyId);

        // If creator has no company, assign to default company
        if (companyId == null) {
            Company defaultCompany = companyRepository.findFirstByIsActiveTrue()
                    .orElseThrow(() -> new RuntimeException("No active company found. Please create a company first."));
            companyId = defaultCompany.getId();
            log.warn("⚠️ Creator has no company, assigning new manager to default company: {} (ID: {})",
                    defaultCompany.getName(), companyId);
        }

        // Check if company has HR module enabled
//        if (!isModuleEnabledForCompany(companyId, "HR_MODULE")) {
//            throw new RuntimeException("HR module is not enabled for your company. Please contact your administrator.");
//        }

        String rawPassword = request.getPassword();

        User manager = new User();
        manager.setUsername(request.getUsername());
        manager.setEmail(request.getEmail());
        manager.setPhone(request.getPhone());
        manager.setPasswordHash(passwordEncoder.encode(rawPassword));
        manager.setFirstName(request.getFirstName());
        manager.setLastName(request.getLastName());
        manager.setStatus(UserStatus.ACTIVE);
        manager.setIsEmailVerified(true);
        manager.setUserType("MANAGER");  // ✅ Set user type

        // Manager specific fields
        manager.setTeamName(request.getDepartment() != null ? request.getDepartment() : "General Team");
        manager.setTeamSize(0);
        manager.setDepartment(request.getDepartment());
        manager.setMonthlyTarget(0.0);
        manager.setAchievedTarget(0.0);
        manager.setCanApproveLeaves(true);

        manager.setCompanyId(companyId);
        log.info("✅ Setting company_id: {} for new manager", companyId);

        Role managerRole = roleRepository.findByName("MANAGER")
                .orElseThrow(() -> new RuntimeException("MANAGER role not found"));
        manager.setRoles(Set.of(managerRole));

        User savedUser = userRepository.save(manager);
        log.info("Manager created successfully with ID: {}, Company ID: {}", savedUser.getId(), savedUser.getCompanyId());

        String createdBy = getCurrentUserFullName();
        String fullName = manager.getFullName();
        if (fullName.trim().isEmpty()) {
            fullName = request.getUsername();
        }

        emailService.sendWelcomeEmail(
                request.getEmail(),
                fullName,
                "MANAGER",
                request.getEmail(),
                rawPassword,
                createdBy
        );

        log.info("📧 Welcome email sent to new manager: {}", request.getEmail());

        return convertToResponse(savedUser);
    }

    // ============ Create EMPLOYEE ============

    @Transactional
    public UserResponse createEmployee(CreateUserRequest request) {
        log.info("Creating new EMPLOYEE user: {}", request.getEmail());

        validateUserUniqueness(request);

        Long companyId = getCurrentUserCompanyId();

        log.info("📌 Creator company ID: {}", companyId);

        // If creator has no company, assign to default company
        if (companyId == null) {
            Company defaultCompany = companyRepository.findFirstByIsActiveTrue()
                    .orElseThrow(() -> new RuntimeException("No active company found. Please create a company first."));
            companyId = defaultCompany.getId();
            log.warn("⚠️ Creator has no company, assigning new employee to default company: {} (ID: {})",
                    defaultCompany.getName(), companyId);
        }

        // Check if company has HR module enabled
//        if (!isModuleEnabledForCompany(companyId, "HR_MODULE")) {
//            throw new RuntimeException("HR module is not enabled for your company. Please contact your administrator.");
//        }

        String rawPassword = request.getPassword();

        User employee = new User();
        employee.setUsername(request.getUsername());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setPasswordHash(passwordEncoder.encode(rawPassword));
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setStatus(UserStatus.ACTIVE);
        employee.setIsEmailVerified(true);
        employee.setUserType("EMPLOYEE");  // ✅ Set user type

        // Employee specific fields
        employee.setEmployeeId("EMP" + System.currentTimeMillis());
        employee.setDesignation(request.getDesignation() != null ? request.getDesignation() : "Staff");
        employee.setDepartment(request.getDepartment());
        employee.setSalary(request.getSalary());

        employee.setCompanyId(companyId);
        log.info("✅ Setting company_id: {} for new employee", companyId);

        // Set manager relationship
        String currentUserEmail = getCurrentUserEmail();
        userRepository.findByEmail(currentUserEmail).ifPresent(manager -> {
            if ("MANAGER".equals(manager.getUserType())) {
                employee.setManager(manager);
                log.info("✅ Set manager for new employee: {}", manager.getUsername());
            }
        });

        Role employeeRole = roleRepository.findByName("EMPLOYEE")
                .orElseThrow(() -> new RuntimeException("EMPLOYEE role not found"));
        employee.setRoles(Set.of(employeeRole));

        User savedUser = userRepository.save(employee);
        log.info("Employee created successfully with ID: {}, Company ID: {}", savedUser.getId(), savedUser.getCompanyId());

        String createdBy = getCurrentUserFullName();
        String fullName = employee.getFullName();
        if (fullName.trim().isEmpty()) {
            fullName = request.getUsername();
        }

        emailService.sendWelcomeEmail(
                request.getEmail(),
                fullName,
                "EMPLOYEE",
                request.getEmail(),
                rawPassword,
                createdBy
        );

        log.info("📧 Welcome email sent to new employee: {}", request.getEmail());

        return convertToResponse(savedUser);
    }

    // ============ Create CUSTOMER ============

    @Transactional
    public UserResponse createCustomer(CreateUserRequest request) {
        log.info("Creating new CUSTOMER user: {}", request.getEmail());

        validateUserUniqueness(request);

        Long companyId = getCurrentUserCompanyId();

        if (companyId == null) {
            Company defaultCompany = companyRepository.findFirstByIsActiveTrue()
                    .orElseThrow(() -> new RuntimeException("No active company found. Please create a company first."));
            companyId = defaultCompany.getId();
        }

        String rawPassword = request.getPassword();

        User customer = new User();
        customer.setUsername(request.getUsername());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setPasswordHash(passwordEncoder.encode(rawPassword));
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setStatus(UserStatus.ACTIVE);
        customer.setIsEmailVerified(true);
        customer.setUserType("CUSTOMER");  // ✅ Set user type

        // Customer specific fields
        customer.setCompanyName(request.getCompanyName());
        customer.setCustomerTier("BRONZE");
        customer.setCustomerStatus("ACTIVE");
        customer.setLoyaltyPoints(0);
        customer.setTotalPurchases(java.math.BigDecimal.ZERO);
        customer.setTotalOrders(0);

        customer.setCompanyId(companyId);

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("CUSTOMER role not found"));
        customer.setRoles(Set.of(customerRole));

        User savedUser = userRepository.save(customer);
        log.info("Customer created successfully with ID: {}", savedUser.getId());

        String createdBy = getCurrentUserFullName();
        String fullName = customer.getFullName();

        emailService.sendWelcomeEmail(
                request.getEmail(),
                fullName,
                "CUSTOMER",
                request.getEmail(),
                rawPassword,
                createdBy
        );

        return convertToResponse(savedUser);
    }

    // ============ Get All Users (with filters and company isolation) ============

    public List<UserResponse> getAllUsers(String userType) {
        Long currentUserCompanyId = getCurrentUserCompanyId();
        User currentUser = getCurrentUser();

        List<User> users;

        if (userType != null && !userType.isEmpty()) {
            users = userRepository.findByUserType(userType.toUpperCase());
        } else {
            users = userRepository.findAll();
        }

        // Filter by company for non-SUPER_ADMIN users
        if (currentUserCompanyId != null && !isSuperAdmin()) {
            users = users.stream()
                    .filter(u -> currentUserCompanyId.equals(u.getCompanyId()))
                    .collect(Collectors.toList());
            log.info("Filtered users by company ID: {}, found {} users", currentUserCompanyId, users.size());
        }

        return users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Check if current user is SUPER_ADMIN
    private boolean isSuperAdmin() {
        User currentUser = getCurrentUser();
        return currentUser != null && "SUPER_ADMIN".equals(currentUser.getUserType());
    }

    // ============ Get Users by Company (For SUPER_ADMIN) ============

    public List<UserResponse> getUsersByCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + companyId));

        List<User> users = userRepository.findAllByCompanyId(companyId);
        log.info("Found {} users for company: {}", users.size(), company.getName());

        return users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ============ Get Users by Role within Company ============

    public List<UserResponse> getUsersByRole(String role) {
        Long currentUserCompanyId = getCurrentUserCompanyId();

        List<User> users = userRepository.findByUserType(role.toUpperCase());

        if (currentUserCompanyId != null && !isSuperAdmin()) {
            users = users.stream()
                    .filter(u -> currentUserCompanyId.equals(u.getCompanyId()))
                    .collect(Collectors.toList());
        }

        return users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ============ Get Team Members (For MANAGER) ============

    public List<UserResponse> getTeamMembers() {
        User currentUser = getCurrentUser();

        if (currentUser == null || !"MANAGER".equals(currentUser.getUserType())) {
            throw new RuntimeException("Only managers can view team members");
        }

        List<User> teamMembers = userRepository.findTeamMembersByManagerId(currentUser.getId());

        return teamMembers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ============ Get Users with Module Filtering ============

    public List<UserResponse> getUsersByModule(String moduleName) {
        Long currentUserCompanyId = getCurrentUserCompanyId();

        if (currentUserCompanyId != null && !isModuleEnabledForCompany(currentUserCompanyId, moduleName)) {
            log.warn("Module {} is not enabled for company {}", moduleName, currentUserCompanyId);
            return List.of();
        }

        return getAllUsers(null);
    }

    // ============ Get Single User ============

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        Long currentUserCompanyId = getCurrentUserCompanyId();
        if (currentUserCompanyId != null && !isSuperAdmin() && !currentUserCompanyId.equals(user.getCompanyId())) {
            throw new RuntimeException("You don't have permission to view this user");
        }

        return convertToResponse(user);
    }

    // ============ Update User ============

    @Transactional
    public UserResponse updateUser(Long id, CreateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        Long currentUserCompanyId = getCurrentUserCompanyId();
        if (currentUserCompanyId != null && !isSuperAdmin() && !currentUserCompanyId.equals(user.getCompanyId())) {
            throw new RuntimeException("You don't have permission to update this user");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setDepartment(request.getDepartment());

        // Update role-specific fields
        if ("EMPLOYEE".equals(user.getUserType()) && request.getSalary() != null) {
            user.setSalary(request.getSalary());
        }

        if ("MANAGER".equals(user.getUserType()) && request.getDepartment() != null) {
            user.setTeamName(request.getDepartment());
        }

        if ("CUSTOMER".equals(user.getUserType())) {
            if (request.getCompanyName() != null) user.setCompanyName(request.getCompanyName());
            if (request.getGstNumber() != null) user.setGstNumber(request.getGstNumber());
            if (request.getIndustry() != null) user.setIndustry(request.getIndustry());
        }

        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", savedUser.getId());

        return convertToResponse(savedUser);
    }

    // ============ Delete/Deactivate User ============

    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        Long currentUserCompanyId = getCurrentUserCompanyId();
        if (currentUserCompanyId != null && !isSuperAdmin() && !currentUserCompanyId.equals(user.getCompanyId())) {
            throw new RuntimeException("You don't have permission to deactivate this user");
        }

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        log.info("User deactivated: {}", user.getEmail());
    }

    @Transactional
    public void activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        Long currentUserCompanyId = getCurrentUserCompanyId();
        if (currentUserCompanyId != null && !isSuperAdmin() && !currentUserCompanyId.equals(user.getCompanyId())) {
            throw new RuntimeException("You don't have permission to activate this user");
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("User activated: {}", user.getEmail());
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        Long currentUserCompanyId = getCurrentUserCompanyId();
        if (currentUserCompanyId != null && !isSuperAdmin() && !currentUserCompanyId.equals(user.getCompanyId())) {
            throw new RuntimeException("You don't have permission to delete this user");
        }

        user.setIsDeleted(true);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        log.info("User deleted (soft): {}", user.getEmail());
    }

    // ============ Validation Methods ============

    private void validateUserUniqueness(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number already in use: " + request.getPhone());
        }
    }

    private void validateUserUniqueness(CreateAdminWithCompanyRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number already in use: " + request.getPhone());
        }
    }

    // ============ Conversion Methods ============

    private UserResponse convertToResponse(User user) {
        String companyName = null;
        if (user.getCompanyId() != null) {
            companyName = companyRepository.findById(user.getCompanyId())
                    .map(Company::getName)
                    .orElse(null);
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userType(user.getUserType())
                .status(user.getStatus() != null ? user.getStatus().name() : "ACTIVE")
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .companyId(user.getCompanyId())
                .companyName(companyName)
                .department(user.getDepartment())
                .designation(user.getDesignation())
                .build();
    }
}