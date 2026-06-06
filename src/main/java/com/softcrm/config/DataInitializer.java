package com.softcrm.config;

import com.softcrm.entity.Company;
import com.softcrm.entity.Permission;
import com.softcrm.entity.Role;
import com.softcrm.entity.user.User;
import com.softcrm.enums.UserStatus;
import com.softcrm.repository.CompanyRepository;
import com.softcrm.repository.PermissionRepository;
import com.softcrm.repository.RoleRepository;
import com.softcrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.create-default-company:false}")
    private boolean createDefaultCompany;

    @Override
    public void run(String... args) {
        log.info("🚀 Starting Data Initializer...");
        initializePermissions();
        initializeRoles();
        initializeSuperAdmin();
        initializeDefaultCompany();
        log.info("✅ Data Initialization Completed!");
    }

    private void initializePermissions() {
        String[] permissionNames = {
                "USER_CREATE", "USER_READ", "USER_UPDATE", "USER_DELETE",
                "ROLE_CREATE", "ROLE_READ", "ROLE_UPDATE", "ROLE_DELETE",
                "LEAD_CREATE", "LEAD_READ", "LEAD_UPDATE", "LEAD_DELETE",
                "CUSTOMER_CREATE", "CUSTOMER_READ", "CUSTOMER_UPDATE", "CUSTOMER_DELETE",
                "REPORT_READ", "REPORT_EXPORT", "SYSTEM_CONFIG", "SYSTEM_BACKUP",
                "COMPANY_CREATE", "COMPANY_READ", "COMPANY_UPDATE", "COMPANY_DELETE"
        };

        for (String permName : permissionNames) {
            boolean exists = permissionRepository.findAll().stream()
                    .anyMatch(p -> p.getName().equals(permName));

            if (!exists) {
                Permission permission = new Permission();
                permission.setName(permName);
                String[] parts = permName.split("_");
                if (parts.length >= 2) {
                    permission.setResource(parts[0]);
                    permission.setAction(parts[1]);
                }
                permissionRepository.save(permission);
                log.info("✅ Created permission: {}", permName);
            } else {
                log.debug("Permission already exists: {}", permName);
            }
        }
    }

    private void initializeRoles() {
        String[] roles = {"SUPER_ADMIN", "ADMIN", "MANAGER", "EMPLOYEE", "CUSTOMER"};

        for (String roleName : roles) {
            if (!roleRepository.existsByName(roleName)) {
                Role role = new Role();
                role.setName(roleName);
                role.setDescription(roleName + " Role - Full access management");

                // Assign all permissions to SUPER_ADMIN only
                if (roleName.equals("SUPER_ADMIN")) {
                    Set<Permission> allPermissions = new HashSet<>(permissionRepository.findAll());
                    role.setPermissions(allPermissions);
                    log.info("✅ Assigned {} permissions to SUPER_ADMIN role", allPermissions.size());
                }

                roleRepository.save(role);
                log.info("✅ Created role: {}", roleName);
            } else {
                log.debug("Role already exists: {}", roleName);
            }
        }
    }

    private void initializeSuperAdmin() {
        // Check by username instead of email (more reliable)
        if (!userRepository.existsByUsername("superadmin")) {
            User superAdmin = new User();

            // Core fields
            superAdmin.setUsername("superadmin");
            superAdmin.setEmail("cgrover179@gmail.com");
            superAdmin.setPhone("9999999999");
            superAdmin.setFirstName("Super");
            superAdmin.setLastName("Admin");
            superAdmin.setPasswordHash(passwordEncoder.encode("SuperAdmin@123"));
            superAdmin.setStatus(UserStatus.ACTIVE);
            superAdmin.setIsEmailVerified(true);
            superAdmin.setIsPhoneVerified(true);

            // Set user type
            superAdmin.setUserType("SUPER_ADMIN");

            // SuperAdmin specific fields
            superAdmin.setSystemAccessLevel(100);
            superAdmin.setCanManageAllTenants(true);
            superAdmin.setCanAuditLogs(true);
            superAdmin.setCanConfigureSystem(true);

            // No company for SUPER_ADMIN
            superAdmin.setCompanyId(null);

            // Assign SUPER_ADMIN role
            Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                    .orElseThrow(() -> new RuntimeException("SUPER_ADMIN role not found"));
            Set<Role> roles = new HashSet<>();
            roles.add(superAdminRole);
            superAdmin.setRoles(roles);

            userRepository.save(superAdmin);
            log.info("========================================");
            log.info("👑 SUPER ADMIN CREATED SUCCESSFULLY!");
            log.info("📧 Email: cgrover179@gmail.com");
            log.info("🔑 Password: SuperAdmin@123");
            log.info("👤 Username: superadmin");
            log.info("========================================");
        } else {
            log.info("Super Admin already exists, skipping creation.");
        }
    }

    // Optional: Create default company for development/demo purposes
    private void initializeDefaultCompany() {
        if (!createDefaultCompany) {
            log.info("Default company creation is disabled (app.create-default-company=false)");
            return;
        }

        if (!companyRepository.existsByName("Default Company")) {
            Company defaultCompany = Company.builder()
                    .name("Default Company")
                    .code("DEF001")
                    .address("123 Default Street")
                    .city("Default City")
                    .state("Default State")
                    .country("Default Country")
                    .pincode("000000")
                    .phone("0000000000")
                    .email("default@company.com")
                    .isActive(true)
                    .subscriptionPlan("PROFESSIONAL")
                    .maxUsers(100)
                    .maxStorageGb(50)
                    .usedStorageGb(0.0)
                    .subscriptionExpiry(LocalDateTime.now().plusYears(1))
                    .build();

            companyRepository.save(defaultCompany);
            log.info("========================================");
            log.info("🏢 DEFAULT COMPANY CREATED SUCCESSFULLY!");
            log.info("📛 Name: Default Company");
            log.info("🆔 Code: DEF001");
            log.info("========================================");
        } else {
            log.debug("Default company already exists, skipping creation.");
        }
    }
}