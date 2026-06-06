package com.softcrm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    // ============ Core Fields (Required for all users) ============
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String fullName;
    private String userType;  // SUPER_ADMIN, ADMIN, MANAGER, EMPLOYEE, CUSTOMER
    private String status;    // ACTIVE, INACTIVE, SUSPENDED
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ============ Company Information ============
    private Long companyId;
    private String companyName;

    // ============ Department & Role Information ============
    private String department;
    private String designation;
    private Set<String> roles;

    // ============ Security Fields ============
    private Boolean emailVerified;
    private Boolean twoFactorEnabled;

    // ============ Assignment Related Fields (For Lead Assignment) ============
    private Long reportingManagerId;     // For EMPLOYEE - who they report to
    private String reportingManagerName; // Manager's name

    private Long managerId;              // For MANAGER - who they report to (if any)
    private String managerName;

    // ============ Employee Specific Fields ============
    private String employeeId;           // Employee code/ID
    private LocalDate dateOfJoining;     // Joining date
    private Double salary;               // Monthly/Annual salary
    private Integer experienceYears;     // Years of experience

    // ============ Manager Specific Fields ============
    private String teamName;             // Name of the team
    private Integer teamSize;            // Number of team members
    private Double monthlyTarget;        // Monthly sales target
    private Double achievedTarget;       // Target achieved so far
    private Double targetAchievementPercentage; // Achievement %
    private Boolean canApproveLeaves;    // Can approve leave requests
    private Boolean canAssignLeads;      // Can assign leads to team members
    private Boolean canApproveExpenses;  // Can approve expenses

    // ============ Admin Specific Fields ============
    private Integer accessLevel;         // 1-10 access level
    private Boolean canManageUsers;      // Can create/edit/delete users
    private Boolean canManageRoles;      // Can assign roles
    private Boolean canManageCompanies;  // Can manage companies (Super Admin only)
    private String adminCode;            // Admin identification code

    // ============ Customer Specific Fields ============
    private String customerCompanyName;  // Company name for customer
    private String gstNumber;            // GST number
    private String industry;             // Industry type
    private String website;              // Website URL
    private String address;              // Full address
    private String city;                 // City
    private String state;                // State
    private String country;              // Country
    private String pincode;              // Postal code
    private BigDecimal totalPurchases;   // Total purchase amount
    private Integer totalOrders;         // Number of orders
    private Integer loyaltyPoints;       // Loyalty points earned
    private String customerTier;         // BRONZE, SILVER, GOLD, PLATINUM
    private String customerStatus;       // ACTIVE, INACTIVE, VIP
    private Long assignedToId;           // Assigned sales person ID
    private String assignedToName;       // Assigned sales person name

    // ============ Helper Methods for Assignment ============

    /**
     * Check if user can be assigned leads (EMPLOYEE or MANAGER)
     */
    public boolean canBeAssignedLeads() {
        return "EMPLOYEE".equals(userType) || "MANAGER".equals(userType);
    }

    /**
     * Check if user can assign leads to others (ADMIN or MANAGER)
     */
    public boolean canAssignLeadsToOthers() {
        return "ADMIN".equals(userType) || "MANAGER".equals(userType);
    }

    /**
     * Get display name for assignment dropdown
     */
    public String getAssignmentDisplayName() {
        if (fullName != null && !fullName.isEmpty()) {
            return fullName + " (" + userType + ")";
        }
        if (firstName != null || lastName != null) {
            return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "") + " (" + userType + ")";
        }
        return username + " (" + userType + ")";
    }

    /**
     * Get user type badge color for UI
     */
    public String getUserTypeBadgeColor() {
        switch (userType) {
            case "SUPER_ADMIN": return "purple";
            case "ADMIN": return "info";
            case "MANAGER": return "warning";
            case "EMPLOYEE": return "success";
            case "CUSTOMER": return "primary";
            default: return "secondary";
        }
    }

    // ============ Helper Methods for UI Display ============

    public boolean hasCustomerFields() {
        return "CUSTOMER".equals(userType);
    }

    public boolean hasEmployeeFields() {
        return "EMPLOYEE".equals(userType) || "MANAGER".equals(userType);
    }

    public boolean hasManagerFields() {
        return "MANAGER".equals(userType);
    }

    public boolean hasAdminFields() {
        return "ADMIN".equals(userType) || "SUPER_ADMIN".equals(userType);
    }

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

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    // ============ Display Helpers ============

    public String getShortName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName.substring(0, 1) + ".";
        }
        if (firstName != null) return firstName;
        if (lastName != null) return lastName;
        return username;
    }

    public String getUserInitials() {
        String initial = "";
        if (firstName != null && !firstName.isEmpty()) {
            initial += firstName.charAt(0);
        }
        if (lastName != null && !lastName.isEmpty()) {
            initial += lastName.charAt(0);
        }
        if (initial.isEmpty() && username != null && !username.isEmpty()) {
            initial += username.charAt(0);
        }
        return initial.toUpperCase();
    }

    public String getTargetAchievementDisplay() {
        if (targetAchievementPercentage != null) {
            return String.format("%.1f%%", targetAchievementPercentage);
        }
        return "0%";
    }

    public String getTierIcon() {
        if (customerTier == null) return "⭐";
        switch (customerTier) {
            case "BRONZE": return "🥉";
            case "SILVER": return "🥈";
            case "GOLD": return "🥇";
            case "PLATINUM": return "💎";
            default: return "⭐";
        }
    }
}