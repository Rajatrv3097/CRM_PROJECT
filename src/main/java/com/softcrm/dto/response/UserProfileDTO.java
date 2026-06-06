package com.softcrm.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String fullName;
    private String userType;
    private String avatarUrl;
    private String department;
    private String designation;
    private String companyName;
    private Long companyId;
    private Boolean isEmailVerified;
    private Boolean isTwoFactorEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    // Customer specific
    private String gstNumber;
    private String industry;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String customerTier;
    private String customerStatus;
    private java.math.BigDecimal totalPurchases;
    private Integer totalOrders;
    private Integer loyaltyPoints;
}