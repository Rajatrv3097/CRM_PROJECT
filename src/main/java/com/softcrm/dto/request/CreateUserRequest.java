package com.softcrm.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
public class CreateUserRequest {

    // Core fields (required for all)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
    private String password;

    private String firstName;
    private String lastName;

    @NotBlank(message = "User type is required")
    private String userType; // ADMIN, CUSTOMER, EMPLOYEE, MANAGER, SUPER_ADMIN

    private Long companyId;
    private Set<String> roles;

    // Common fields
    private String department;
    private String designation;

    // Admin/SuperAdmin fields
    private Integer accessLevel;
    private Boolean canManageUsers;
    private Boolean canManageRoles;
    private String adminCode;
    private Long managedCompanyId;
    private Boolean canManageCompanySettings;
    private Integer systemAccessLevel;
    private Boolean canManageAllTenants;
    private Boolean canAuditLogs;
    private Boolean canConfigureSystem;

    // Employee fields
    private String employeeId;
    private LocalDate dateOfJoining;
    private Double salary;
    private String reportingManager;
    private Long managerId;
    private String bankAccountNumber;
    private String panNumber;

    // Manager fields
    private String teamName;
    private Integer teamSize;
    private String reportingTo;
    private Double monthlyTarget;
    private Double achievedTarget;
    private Boolean canApproveLeaves;

    // Customer fields
    private String companyName;  // Customer's company name
    private String gstNumber;
    private String industry;
    private String website;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private Long assignedTo;  // Sales person ID
    private String customerSource;
    private String customerTier;
}