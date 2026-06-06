package com.softcrm.entity.user;

import com.softcrm.entity.base.BaseEntity;
import com.softcrm.entity.Role;
import com.softcrm.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_company", columnList = "company_id"),
        @Index(name = "idx_user_type", columnList = "user_type"),
        @Index(name = "idx_user_manager", columnList = "manager_id"),
        @Index(name = "idx_user_verification_token", columnList = "email_verification_token")
})
public class User extends BaseEntity {

    // ============ Core Fields (from User.java) ============
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "is_email_verified")
    private Boolean isEmailVerified = false;

    @Column(name = "is_phone_verified")
    private Boolean isPhoneVerified = false;

    // ============ EMAIL VERIFICATION TOKEN ============
    @Column(name = "email_verification_token", length = 255)
    private String emailVerificationToken;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip")
    private String lastLoginIp;

    @Column(name = "last_login_device")
    private String lastLoginDevice;

    @Column(name = "is_two_factor_enabled")
    private Boolean isTwoFactorEnabled = false;

    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    // ============ User Type Discriminator ============
    @Column(name = "user_type", nullable = false)
    private String userType; // "ADMIN", "CUSTOMER", "EMPLOYEE", "MANAGER", "SUPER_ADMIN"

    // ============ Company Association ============
    @Column(name = "company_id")
    private Long companyId;

    // ============ Common Fields (from various user types) ============
    @Column(name = "department")
    private String department;

    @Column(name = "designation")
    private String designation;

    // ============ Manager/Employee Relationship ============
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @OneToMany(mappedBy = "manager", fetch = FetchType.LAZY)
    private Set<User> subordinates = new HashSet<>();

    // ============ Roles (Many-to-Many) ============
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // ============ Admin Specific Fields ============
    @Column(name = "access_level")
    private Integer accessLevel;  // Admin: 10, SuperAdmin: 100

    @Column(name = "can_manage_users")
    private Boolean canManageUsers;

    @Column(name = "can_manage_roles")
    private Boolean canManageRoles;

    @Column(name = "admin_code")
    private String adminCode;

    @Column(name = "managed_company_id")
    private Long managedCompanyId;

    @Column(name = "can_manage_company_settings")
    private Boolean canManageCompanySettings;

    // ============ SuperAdmin Specific Fields ============
    @Column(name = "system_access_level")
    private Integer systemAccessLevel;

    @Column(name = "can_manage_all_tenants")
    private Boolean canManageAllTenants;

    @Column(name = "can_audit_logs")
    private Boolean canAuditLogs;

    @Column(name = "can_configure_system")
    private Boolean canConfigureSystem;

    // ============ Employee Specific Fields ============
    @Column(name = "employee_id", unique = true)
    private String employeeId;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "reporting_manager")
    private String reportingManager;  // Name of reporting manager

    @Column(name = "salary")
    private Double salary;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "pan_number")
    private String panNumber;

    // ============ Manager Specific Fields ============
    @Column(name = "team_name")
    private String teamName;

    @Column(name = "team_size")
    private Integer teamSize;

    @Column(name = "reporting_to")
    private String reportingTo;

    @Column(name = "monthly_target")
    private Double monthlyTarget;

    @Column(name = "achieved_target")
    private Double achievedTarget;

    @Column(name = "can_approve_leaves")
    private Boolean canApproveLeaves;

    // ============ Customer Specific Fields ============
    @Column(name = "company_name", length = 200)
    private String companyName;  // Customer's company name

    @Column(name = "gst_number", length = 50)
    private String gstNumber;

    @Column(length = 100)
    private String industry;

    @Column(length = 200)
    private String website;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(length = 20)
    private String pincode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;  // Sales person assigned to this customer

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user")
    private User createdByUser;

    @Column(name = "total_purchases")
    private BigDecimal totalPurchases = BigDecimal.ZERO;

    @Column(name = "total_orders")
    private Integer totalOrders = 0;

    @Column(name = "last_purchase_date")
    private LocalDateTime lastPurchaseDate;

    @Column(name = "customer_source", length = 50)
    private String customerSource;

    @Column(name = "loyalty_points")
    private Integer loyaltyPoints = 0;

    @Column(name = "customer_tier", length = 20)
    private String customerTier = "BRONZE";

    @Column(name = "customer_status", length = 20)
    private String customerStatus = "ACTIVE";

    // ============ Helper Methods ============

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username;
    }

    public String getName() {
        return getFullName();
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    // ============ Role Check Methods ============
    public boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(userType);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(userType);
    }

    public boolean isManager() {
        return "MANAGER".equals(userType);
    }

    public boolean isEmployee() {
        return "EMPLOYEE".equals(userType);
    }

    public boolean isCustomer() {
        return "CUSTOMER".equals(userType);
    }

    // ============ Manager Specific Methods ============
    public Double getTargetAchievementPercentage() {
        if (monthlyTarget == null || monthlyTarget == 0) return 0.0;
        return (achievedTarget / monthlyTarget) * 100;
    }

    // ============ Customer Specific Methods ============
    public void addLoyaltyPoints(int points) {
        if (this.loyaltyPoints == null) this.loyaltyPoints = 0;
        this.loyaltyPoints += points;
    }

    public void deductLoyaltyPoints(int points) {
        if (this.loyaltyPoints == null) this.loyaltyPoints = 0;
        this.loyaltyPoints = Math.max(0, this.loyaltyPoints - points);
    }

    public void updateTier() {
        if (loyaltyPoints == null) loyaltyPoints = 0;
        if (loyaltyPoints >= 1000) {
            customerTier = "PLATINUM";
        } else if (loyaltyPoints >= 500) {
            customerTier = "GOLD";
        } else if (loyaltyPoints >= 100) {
            customerTier = "SILVER";
        } else {
            customerTier = "BRONZE";
        }
    }
}